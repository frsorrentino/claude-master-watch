package it.pixelbox.cmwatch.contract

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Il rapporto della notte (schema `team-supervisor/night-report` v1), il `text` dell'op `night` (contratto 1.44): i dati
 * della pagina «Notte» (specifica docs/proposte/2026-10-07-pagina-notte.md, approvata da Franz il 07/10 alle 21:50).
 * Ogni campo ha un valore di default: un relay più nuovo può aggiungerne senza rompere la lettura.
 */
@Serializable data class NightReport(
    val schema: String = "", val v: Int = 1,
    @SerialName("generated_at") val generatedAt: Long = 0,
    val date: String = "",
    val window: NightWindow = NightWindow(),
    val attention: NightAttention = NightAttention(),
    val timeline: List<NightEntry> = emptyList(),
    val projects: List<NightProject> = emptyList(),
)

@Serializable data class NightWindow(
    val start: Long = 0, val end: Long = 0,
    @SerialName("start_source") val startSource: String? = null,
    @SerialName("last_message") val lastMessage: NightMessage? = null,
)

@Serializable data class NightMessage(val at: Long = 0, val session: String? = null, val origin: String? = null, val text: String = "")

@Serializable data class NightAttention(
    val questions: List<NightQuestion> = emptyList(),
    val approvals: List<NightApproval> = emptyList(),
    val unblock: List<NightUnblock> = emptyList(),
)

@Serializable data class NightQuestion(
    val session: String = "", val project: String? = null, val id: String? = null, val text: String = "",
    val options: List<Option> = emptyList(), @SerialName("asked_at") val askedAt: Long? = null,
)

@Serializable data class NightApproval(
    val task: String = "", val title: String = "", val project: String? = null, val what: String? = null, val where: String? = null,
    @SerialName("requested_at") val requestedAt: Long? = null,
)

@Serializable data class NightUnblock(val session: String = "", val project: String? = null, val text: String = "")

@Serializable data class NightEntry(
    @Serializable(with = NightKindSerializer::class) val kind: Kind = Kind.OTHER,
    val id: String = "", val title: String = "", val project: String? = null,
    val start: Long = 0, val end: Long? = null,
    /** `ok`, `stopped`, `failed`, `running`; altri valori si mostrano come «fermo». */
    val outcome: String = "", val detail: String? = null, val report: String? = null,
    val live: Boolean = false, val counts: NightCounts? = null,
) {
    enum class Kind { SESSION, NIGHT_JOB, OTHER }
}

@Serializable data class NightCounts(val prompts: Int = 0, val tests: Int = 0, val commits: Int = 0)

@Serializable data class NightProject(
    val name: String = "", val path: String? = null, val parts: List<NightPart> = emptyList(),
    @SerialName("parts_total") val partsTotal: Int = 0,
    @SerialName("waiting_on") val waitingOn: List<String> = emptyList(),
    val next: String? = null, val events: List<NightEvent> = emptyList(),
)

@Serializable data class NightPart(val title: String = "", val state: String = "")

@Serializable data class NightEvent(val at: Long = 0, val kind: String = "", val text: String = "", val ok: Boolean? = null, val ref: String? = null)

/** `session`, `night_job`; un tipo nuovo del relay diventa OTHER invece di rompere la lettura. */
object NightKindSerializer : KSerializer<NightEntry.Kind> {
    override val descriptor = PrimitiveSerialDescriptor("NightKind", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): NightEntry.Kind = when (decoder.decodeString()) {
        "session" -> NightEntry.Kind.SESSION
        "night_job" -> NightEntry.Kind.NIGHT_JOB
        else -> NightEntry.Kind.OTHER
    }
    override fun serialize(encoder: Encoder, value: NightEntry.Kind) = encoder.encodeString(
        when (value) { NightEntry.Kind.SESSION -> "session"; NightEntry.Kind.NIGHT_JOB -> "night_job"; NightEntry.Kind.OTHER -> "other" },
    )
}
