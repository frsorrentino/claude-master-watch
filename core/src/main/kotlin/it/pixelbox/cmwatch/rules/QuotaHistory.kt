package it.pixelbox.cmwatch.rules

/**
 * «Ritmo della finestra» (proposta 33, fase 1): l'orologio registra un campione della quota a ogni stato che riceve, e da
 * quei campioni si disegna la linea della finestra di 5 ore con la proiezione al reset. Serve a decidere se fermarsi,
 * quindi vale la regola del progetto: niente numeri inventati. Con meno di due campioni, o senza consumo, la proiezione
 * non c'è e la schermata mostra solo la linea.
 */
object QuotaHistory {
    /** La finestra di Claude: cinque ore che finiscono al reset. */
    const val WINDOW_S = 5L * 3600

    data class Sample(val ts: Long, val pct: Int)

    /** `points`: la linea da disegnare. `projected`: dove si arriva al reset con il ritmo attuale, se si può dire. */
    data class Pace(val points: List<Sample>, val projected: Int?, val at: Long?)

    /** Solo i campioni della finestra corrente, in ordine di tempo: quelli di una finestra passata gonfierebbero il ritmo. */
    fun window(samples: List<Sample>, resetAt: Long): List<Sample> {
        val start = resetAt - WINDOW_S
        return samples.filter { it.ts >= start && it.ts <= resetAt }.sortedBy { it.ts }
    }

    /**
     * Ritmo medio fra il primo e l'ultimo campione della finestra, esteso fino al reset. Il risultato si ferma a 100:
     * oltre non si va, la finestra è finita.
     */
    fun pace(samples: List<Sample>, resetAt: Long, now: Long): Pace {
        val points = window(samples, resetAt)
        if (points.size < 2) return Pace(points, null, null)
        val first = points.first()
        val last = points.last()
        val secondi = last.ts - first.ts
        val punti = last.pct - first.pct
        // Fermi o in calo (una finestra nuova riparte da zero): nessuna proiezione.
        if (secondi <= 0 || punti <= 0) return Pace(points, null, null)
        val perSecondo = punti.toDouble() / secondi
        val restano = (resetAt - maxOf(now, last.ts)).coerceAtLeast(0)
        val proiettato = (last.pct + perSecondo * restano).toInt().coerceIn(0, 100)
        return Pace(points, proiettato, resetAt)
    }
}
