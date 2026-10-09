package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State

/**
 * Le pagine dello scorrimento laterale (Franz, 03/10 15:59: «switchare tra home e sessioni come tra sessioni, la home
 * sempre come prima scheda»): `null` è il riepilogo, sempre per primo; poi le sessioni vive nell'ordine della regia,
 * senza la master, che sta nel riepilogo. La sessione aperta è sempre una pagina, anche la master o una chiusa.
 */
object SwipePages {
    fun of(state: State?, open: String?): List<String?> {
        val live = state?.let { st ->
            PhoneBoard.sections(st, withMaster = false).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }.map { it.name }
        }.orEmpty()
        return listOf<String?>(null) + live + listOfNotNull(open?.takeIf { it !in live })
    }

    /**
     * Le pagine mentre si scorre fra le sessioni (Franz, 09/10 15:46: al rilascio «una disposizione a scatti»): quelle che
     * c'erano restano nel loro ordine, le nuove in fondo nell'ordine della regia, quelle sparite escono. La regia si rifà
     * solo tornando al riepilogo.
     */
    fun stable(prev: List<String?>, fresh: List<String?>): List<String?> =
        prev.filter { it in fresh } + fresh.filter { it !in prev }

    /** Che cosa fa una pagina ferma: niente, o aprire una sessione (`null` = il riepilogo). */
    sealed interface Move {
        data object Stay : Move
        data class Open(val name: String?) : Move
    }

    /**
     * La pagina su cui lo scorrimento si è fermato decide la sessione aperta, ma solo dopo uno scorrimento: la prima pagina
     * vista quando il contenuto si riattiva è quella di prima e non deve «correggere» una scelta fatta dal menu in alto, da
     * un avviso o da «Fai controllare» (dal vivo 03/10 16:40: la sessione tornava subito quella vecchia).
     */
    fun afterSettle(pages: List<String?>, settled: Int, open: String?, initial: Boolean): Move {
        if (initial || settled !in pages.indices) return Move.Stay
        val n = pages[settled]
        return if (n == open) Move.Stay else Move.Open(n)
    }

    /** Che cosa fa il pager a ogni cambio di pagina ferma, pagine, sessione aperta o scorrimento. */
    sealed interface Step {
        data object None : Step
        /** Uno scorrimento del dito si è fermato su un'altra pagina: quella diventa la sessione aperta. */
        data class Open(val name: String?) : Step
        /** La pagina ferma non è quella della sessione aperta: il pager ci torna. */
        data class ScrollTo(val page: Int) : Step
    }

    /**
     * Una decisione sola fra scorrimento e riallineamento (segnalazione del 07/10 16:09). Le pagine seguono lo stato delle
     * sessioni e si riordinano spesso: il pager poteva restare sulla pagina di una sessione mentre quella aperta, e quindi
     * riletta, era un'altra, e la chat a schermo non si aggiornava più. Se la pagina ferma è cambiata per uno scorrimento,
     * comanda la pagina (`afterSettle`); altrimenti il pager si riallinea alla sessione aperta, ma mai mentre scorre.
     * `prevSettled` null = il contenuto si è appena riattivato: la pagina vista è quella di prima e non annulla la scelta
     * del menu (dal vivo 03/10 16:40).
     */
    fun step(prevSettled: Int?, settled: Int, scrolling: Boolean, pages: List<String?>, open: String?): Step {
        if (scrolling) return Step.None
        if (prevSettled != null && prevSettled != settled) {
            val move = afterSettle(pages, settled, open, initial = false)
            if (move is Move.Open) return Step.Open(move.name)
        }
        val i = pages.indexOf(open)
        return if (i < 0 || i == settled) Step.None else Step.ScrollTo(i)
    }
}
