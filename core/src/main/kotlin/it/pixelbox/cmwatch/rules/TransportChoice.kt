package it.pixelbox.cmwatch.rules

/** Quale Transport usare: Firebase solo se accoppiato, con la chiave nel vault e google-services.json presente; altrimenti il finto. */
object TransportChoice {
    enum class Kind { FAKE, FIREBASE }
    fun pick(paired: Boolean, hasKey: Boolean, firebase: Boolean): Kind =
        if (paired && hasKey && firebase) Kind.FIREBASE else Kind.FAKE
}
