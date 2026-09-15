package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

/**
 * «Ritmo della finestra» (proposta 33, fase 1): dai campioni della quota registrati dall'orologio, la linea della finestra
 * di 5 ore e la proiezione al reset. Serve a decidere se fermarsi, quindi la proiezione non si inventa: senza due campioni
 * o senza consumo non c'è numero.
 */
class QuotaHistoryTest {
    private val reset = 1789521000L            // fine della finestra
    private val start = reset - QuotaHistory.WINDOW_S
    private fun s(offsetS: Long, pct: Int) = QuotaHistory.Sample(start + offsetS, pct)

    @Test fun laFinestraTieneSoloICampioniDopoIlSuoInizio() {
        val fuori = QuotaHistory.Sample(start - 60, 90)
        val dentro = listOf(s(0, 4), s(3600, 12))
        assertEquals(dentro, QuotaHistory.window(listOf(fuori) + dentro, reset))
    }

    @Test fun iCampioniRestanoInOrdineDiTempo() {
        val out = QuotaHistory.window(listOf(s(3600, 12), s(0, 4)), reset)
        assertEquals(listOf(4, 12), out.map { it.pct })
    }

    @Test fun laProiezioneEstendeIlRitmoFinoAlReset() {
        // 10 % a inizio finestra, 20 % un'ora dopo: 10 punti all'ora. Da lì al reset mancano 4 ore → 20 + 40 = 60 %.
        val p = QuotaHistory.pace(listOf(s(0, 10), s(3600, 20)), reset, now = start + 3600)
        assertEquals(60, p.projected)
        assertEquals(reset, p.at)
    }

    @Test fun laProiezioneNonSuperaIlCento() {
        val p = QuotaHistory.pace(listOf(s(0, 40), s(3600, 80)), reset, now = start + 3600)
        assertEquals(100, p.projected)
    }

    @Test fun senzaConsumoNessunaProiezione() {
        val p = QuotaHistory.pace(listOf(s(0, 12), s(3600, 12)), reset, now = start + 3600)
        assertNull(p.projected)
    }

    @Test fun conUnSoloCampioneNessunaProiezione() {
        val p = QuotaHistory.pace(listOf(s(0, 12)), reset, now = start + 600)
        assertNull(p.projected)
        assertEquals(listOf(12), p.points.map { it.pct })
    }

    @Test fun laProiezioneUsaSoloLaFinestraCorrente() {
        // Un campione della finestra precedente, altissimo, non deve gonfiare il ritmo.
        val vecchio = QuotaHistory.Sample(start - 7200, 95)
        val p = QuotaHistory.pace(listOf(vecchio, s(0, 10), s(3600, 20)), reset, now = start + 3600)
        assertEquals(60, p.projected)
    }
}
