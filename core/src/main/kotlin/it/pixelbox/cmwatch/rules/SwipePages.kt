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
}
