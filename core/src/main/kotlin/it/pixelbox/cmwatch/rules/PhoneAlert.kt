package it.pixelbox.cmwatch.rules

/**
 * Chi avvisa (Franz, 29/09): l'orologio per primo. Con l'orologio accoppiato e raggiungibile il telefono mostra in
 * silenzio; altrimenti suona. Gli avvisi di quota, come diario e notte, sono sempre silenziosi sul telefono.
 */
object PhoneAlert {
    enum class Mode { SOUND, SILENT }

    fun mode(kind: Wake.NotifyKind, watchPaired: Boolean, watchReachable: Boolean): Mode = when {
        kind == Wake.NotifyKind.QUOTA -> Mode.SILENT
        watchPaired && watchReachable -> Mode.SILENT
        else -> Mode.SOUND
    }
}
