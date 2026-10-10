package it.pixelbox.cmwatch.rules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * La scheda della modalità live che il telefono manda al watch (specifica live, §3): la notizia in corso o l'interazione
 * aperta. Il watch è un telecomando: mostra la scheda, vibra secondo `buzz` e rimanda i tocchi (`LiveTap`). `options` =
 * le opzioni di una domanda, i Prossimi di un esito (con `blocking` = il «!») o i candidati di una scelta; `until` = la
 * scadenza in ms epoch di un'istruzione in partenza o di una doppia conferma, per il conto alla rovescia.
 */
@Serializable data class LiveCard(
    val seq: Long, val kind: Kind, val title: String = "", val text: String = "",
    val options: List<String> = emptyList(), val blocking: List<Boolean> = emptyList(),
    val buzz: Buzz = Buzz.NONE, val until: Long = 0, val onlyBlocking: Boolean = false,
    /**
     * Con la live tranquilla, le sessioni come nella pillola aperta del telefono: chi chiede, chi lavora, chi è ferma
     * (Franz, 10/10 10:50: «la schermata live dell'orologio deve avere informazioni simili a quelle del telefono»).
     */
    val rows: List<LivePanel.Row> = emptyList(),
) {
    /** OFF = live spenta; IDLE = coda vuota; NEWS = una notizia senza tasti propri. */
    enum class Kind { OFF, IDLE, NEWS, QUESTION, OUTCOME, APPROVAL, CONFIRM, TELL, PICK }

    /** LONG = livello 1, SHORT = livello 2, DONE = conferma arrivata a 2 secondi. */
    enum class Buzz { NONE, SHORT, LONG, DONE }
}

/** Un tocco sul watch. `index` = l'opzione, il Prossimo o il candidato, da 1; `text` = il dettato di «Parla». */
@Serializable data class LiveTap(val action: Action, val index: Int = 0, val text: String? = null) {
    enum class Action { OPTION, STEP, PICK, REPEAT, LATER, SKIP, SAY, APPROVE, PRESS, RELEASE, CANCEL, ROUND, ONLY_BLOCKING, STOP, ACTIONS }
}

/** I messaggi fra telefono e watch (MessageClient di Wear OS). */
object LiveWire {
    const val CARD = "/cmwatch/live/card"
    const val TAP = "/cmwatch/live/tap"
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(c: LiveCard): ByteArray = json.encodeToString(LiveCard.serializer(), c).toByteArray()
    fun encode(t: LiveTap): ByteArray = json.encodeToString(LiveTap.serializer(), t).toByteArray()
    fun card(b: ByteArray): LiveCard = json.decodeFromString(LiveCard.serializer(), b.decodeToString())
    fun tap(b: ByteArray): LiveTap = json.decodeFromString(LiveTap.serializer(), b.decodeToString())
}
