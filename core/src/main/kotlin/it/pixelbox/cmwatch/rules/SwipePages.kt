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
}
