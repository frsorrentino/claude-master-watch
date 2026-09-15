package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult

/**
 * «Riprendi» dura 30-60 s sul PC (contratto 1.9): cosa dice il tasto nel frattempo, perché al tocco deve succedere
 * subito qualcosa (Franz, 15/09 19:14). Vale solo per una sessione a cui l'orologio ha mandato `reopen`.
 */
object ReopenText {
    sealed interface Status {
        data object Starting : Status

        /** `text` null = il comando non è arrivato al PC. */
        data class Failed(val text: String?) : Status
    }

    /** `gone`: la sessione è ancora chiusa nello stato; tornata viva (o sparita con un nome nuovo) il tasto torna normale. */
    fun status(result: CmdResult?, gone: Boolean, notDelivered: Boolean): Status? = when {
        notDelivered -> Status.Failed(null)
        result == null -> Status.Starting
        !result.ok -> Status.Failed(result.text)
        gone -> Status.Starting
        else -> null
    }
}
