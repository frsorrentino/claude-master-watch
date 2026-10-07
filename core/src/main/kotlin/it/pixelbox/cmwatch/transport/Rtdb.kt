package it.pixelbox.cmwatch.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SseEvent(val event: String, val data: String)

/** Firebase Realtime Database via REST (`<path>.json?auth=<token>`) e streaming SSE. Nessun SDK. */
class Rtdb(
    private val baseUrl: String,
    private val token: suspend () -> String?,
    client: OkHttpClient = OkHttpClient(),
    /**
     * Quanto silenzio dello stream vuol dire connessione morta. RTDB manda un keep-alive ogni 30 s circa: con l'attesa
     * infinita di prima, dopo un cambio di rete la connessione restava mezza aperta senza errori e l'orologio diceva
     * «PC fermo da 17 min» mentre il relay pubblicava ogni minuto (N1, Franz 16/09 08:13).
     */
    streamSilenceMs: Long = 90_000,
) {
    // Le REST brevi entro 10 s (piano prestazioni, Task 4: con 30 s i limiti di tile, sveglia e comandi non valevano);
    // caricamenti e pezzi di file, fino a 10 MB su LTE, entro 120 s.
    private val http = client.newBuilder().callTimeout(10, TimeUnit.SECONDS).build()
    private val slowHttp = client.newBuilder().callTimeout(120, TimeUnit.SECONDS).build()
    private val streaming = client.newBuilder().readTimeout(streamSilenceMs, TimeUnit.MILLISECONDS).build()
    private val json = "application/json; charset=utf-8".toMediaType()

    private suspend fun url(path: String, query: Map<String, String> = emptyMap()): String {
        val q = (query + ("auth" to (token() ?: ""))).entries.joinToString("&") { (k, v) -> "$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }
        return "$baseUrl/$path.json?$q"
    }

    /**
     * Una chiamata che la coroutine può interrompere: `execute()` dentro `withContext(IO)` non si fermava alla
     * cancellazione, e una rete lenta teneva ferma la tile o la sveglia fino al timeout (piano prestazioni, Task 4).
     * Il corpo si legge sul thread di OkHttp; la cancellazione chiude la chiamata.
     */
    private suspend fun <T> exec(client: OkHttpClient, request: Request, read: (Response) -> T): T =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) { cont.resumeWith(Result.failure(e)) }
                override fun onResponse(call: okhttp3.Call, response: Response) { cont.resumeWith(runCatching { response.use(read) }) }
            })
        }

    /** Corpo della risposta, o null se il nodo non esiste (RTDB risponde `null`). `slow`: i pezzi dei file. */
    suspend fun get(path: String, query: Map<String, String> = emptyMap(), slow: Boolean = false): String? =
        net("GET $path") {
            exec(if (slow) slowHttp else http, Request.Builder().url(url(path, query)).get().build()) { r ->
                val body = r.body.string()
                if (!r.isSuccessful) { android.util.Log.w("cmwatch", "GET $path: HTTP ${r.code} $body"); throw TransportException.Network("GET $path: HTTP ${r.code}") }
                body.takeIf { it != "null" && it.isNotBlank() }
            }
        }

    /** `slow`: il caricamento di /share, fino a 10 MB. */
    suspend fun put(path: String, body: String, slow: Boolean = false): String =
        net("PUT $path") {
            exec(if (slow) slowHttp else http, Request.Builder().url(url(path)).put(body.toRequestBody(json)).build()) { r ->
                if (!r.isSuccessful) { android.util.Log.w("cmwatch", "PUT $path: HTTP ${r.code}"); throw TransportException.Network("PUT $path: HTTP ${r.code}") }
                r.body.string()
            }
        }

    suspend fun delete(path: String) =
        net("DELETE $path") {
            exec(http, Request.Builder().url(url(path)).delete().build()) { r ->
                if (!r.isSuccessful) throw TransportException.Network("DELETE $path: HTTP ${r.code}")
            }
        }

    /**
     * Gli errori di rete di Java (timeout, connessione caduta) diventano `TransportException.Network`, l'unico errore che
     * il resto dell'app raccoglie. Prima uscivano così com'erano e chiudevano l'app: crash del 16/09 13:57 al polso,
     * un `SocketTimeoutException` su `put` durante un cambio di rete.
     */
    private inline fun <T> net(what: String, block: () -> T): T =
        try { block() } catch (e: IOException) { throw TransportException.Network("$what: ${e.message}") }

    /** Eventi SSE di RTDB (`put`, `patch`, `keep-alive`, `cancel`, `auth_revoked`). Si chiude con errore alla caduta della connessione. */
    fun stream(path: String): Flow<SseEvent> = callbackFlow {
        val request = Request.Builder().url(url(path)).header("Accept", "text/event-stream").build()
        val source: EventSource = EventSources.createFactory(streaming).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                trySend(SseEvent(type ?: "message", data))
            }
            override fun onClosed(eventSource: EventSource) { close() }
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(TransportException.Network("stream $path: ${t?.message ?: "HTTP ${response?.code}"}"))
            }
        })
        awaitClose { source.cancel() }
    }
}
