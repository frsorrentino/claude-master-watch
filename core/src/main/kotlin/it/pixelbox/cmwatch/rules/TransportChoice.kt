package it.pixelbox.cmwatch.rules

/** Quale Transport usare: Firebase solo se accoppiato, con la chiave nel vault e google-services.json presente; altrimenti il finto. */
object TransportChoice {
    enum class Kind { FAKE, FIREBASE }
    /** `demo`: la demo per i video, accesa solo via adb (Franz, 16/09 16:05); vince anche da accoppiati, l'accoppiamento resta. */
    fun pick(paired: Boolean, hasKey: Boolean, firebase: Boolean, demo: Boolean = false): Kind =
        if (!demo && paired && hasKey && firebase) Kind.FIREBASE else Kind.FAKE
}
