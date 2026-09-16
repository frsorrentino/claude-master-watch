package it.pixelbox.cmwatch.transport

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * N1 (Franz, 16/09 08:13): «PC fermo da 17 min» mentre il relay pubblicava ogni minuto e i comandi passavano. Lo stream
 * di `/state` aspettava all'infinito (`readTimeout(0)`): dopo un cambio di rete la connessione resta mezza aperta,
 * nessun errore arriva e il ciclo di riconnessione non scatta mai. RTDB manda un keep-alive ogni 30 s circa, quindi un
 * silenzio lungo vuol dire connessione morta: lo stream deve chiudersi con errore, così `resilient` la riapre.
 */
class RtdbStreamSilenceTest {
    private val server = MockWebServer()

    @After fun down() = server.shutdown()

    @Test fun unoStreamCheTaceSiChiudeConErrore() = runBlocking {
        val primo = "event: put\ndata: {\"path\":\"/\",\"data\":null}\n\n"
        // Il primo evento arriva, poi il server resta zitto per un'ora con la connessione aperta: come una rete cambiata.
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/event-stream")
                .setBody(primo + " ".repeat(64))
                .throttleBody(primo.length.toLong(), 1, TimeUnit.HOURS)
        )
        server.start()
        val rtdb = Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t" }, streamSilenceMs = 300)
        val ricevuti = ArrayList<SseEvent>()
        val errore = runCatching { withTimeout(10_000) { rtdb.stream("state").collect { ricevuti += it } } }.exceptionOrNull()
        assertEquals("put", ricevuti.firstOrNull()?.event)
        assertTrue("atteso errore di rete, arrivato: $errore", errore is TransportException.Network)
    }
}

/**
 * Crash del 16/09 13:57 al polso, durante un cambio di rete: `Rtdb.put` lasciava uscire `SocketTimeoutException` così
 * com'era, il `Repo` raccoglie solo `TransportException` e l'app si chiudeva. Ogni errore di rete delle chiamate REST
 * deve arrivare come `TransportException.Network`, come già fa lo stream.
 */
class RtdbNetworkErrorTest {
    private val server = MockWebServer()

    @After fun down() = server.shutdown()

    private fun rtdbCheNonRisponde(): Rtdb {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        server.start()
        val lento = okhttp3.OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS).build()
        return Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t" }, client = lento)
    }

    @Test fun putSenzaRispostaEUnErroreDiRete() = runBlocking {
        val e = runCatching { rtdbCheNonRisponde().put("cmd/x", "{}") }.exceptionOrNull()
        assertTrue("atteso errore di rete, arrivato: $e", e is TransportException.Network)
    }

    @Test fun getSenzaRispostaEUnErroreDiRete() = runBlocking {
        val e = runCatching { rtdbCheNonRisponde().get("state") }.exceptionOrNull()
        assertTrue("atteso errore di rete, arrivato: $e", e is TransportException.Network)
    }
}
