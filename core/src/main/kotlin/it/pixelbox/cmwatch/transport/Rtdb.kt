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
    private val http = client.newBuilder().callTimeout(30, TimeUnit.SECONDS).build()
    private val streaming = client.newBuilder().readTimeout(streamSilenceMs, TimeUnit.MILLISECONDS).build()
    private val json = "application/json; charset=utf-8".toMediaType()

    private suspend fun url(path: String, query: Map<String, String> = emptyMap()): String {
        val q = (query + ("auth" to (token() ?: ""))).entries.joinToString("&") { (k, v) -> "$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }
        return "$baseUrl/$path.json?$q"
    }

    /** Corpo della risposta, o null se il nodo non esiste (RTDB risponde `null`). */
    suspend fun get(path: String, query: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url(path, query)).get().build()).execute().use { r ->
            val body = r.body.string()
            if (!r.isSuccessful) { android.util.Log.w("cmwatch", "GET $path: HTTP ${r.code} $body"); throw TransportException.Network("GET $path: HTTP ${r.code}") }
            body.takeIf { it != "null" && it.isNotBlank() }
        }
    }

    suspend fun put(path: String, body: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url(path)).put(body.toRequestBody(json)).build()).execute().use { r ->
            if (!r.isSuccessful) { android.util.Log.w("cmwatch", "PUT $path: HTTP ${r.code}"); throw TransportException.Network("PUT $path: HTTP ${r.code}") }
            r.body.string()
        }
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url(path)).delete().build()).execute().use { r ->
            if (!r.isSuccessful) throw TransportException.Network("DELETE $path: HTTP ${r.code}")
        }
    }

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
