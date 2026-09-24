package it.pixelbox.cmwatch.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** La configurazione Firebase del progetto dell'utente: arriva dal QR (telefono) o da `hello` (orologio). Nessun segreto. */
@Serializable
data class FirebaseConfig(
    val apiKey: String,
    val projectId: String,
    val appId: String,
    val databaseUrl: String,
    val topic: String = DEFAULT_TOPIC,
) {
    /** Il numero del progetto, cioè il mittente FCM, sta dentro l'id dell'app: `1:<numero>:android:<hash>`. */
    val senderId: String get() = appId.split(':').getOrElse(1) { "" }

    /** Stesso progetto Firebase: il topic da solo non chiede di riavviare Firebase. */
    fun sameProject(o: FirebaseConfig): Boolean =
        apiKey == o.apiKey && projectId == o.projectId && appId == o.appId && databaseUrl == o.databaseUrl

    fun toJson(): String = json.encodeToString(serializer(), this)

    fun compact(): QrFirebase = QrFirebase(k = apiKey, p = projectId, a = appId, d = databaseUrl, t = topic)

    companion object {
        const val DEFAULT_TOPIC = "watch"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(s: String?): FirebaseConfig? = s?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }

        /** I nomi delle risorse che il plugin google-services genera nell'app. */
        val RESOURCE_NAMES = listOf("google_api_key", "project_id", "google_app_id", "firebase_database_url")

        /** Configurazione dai valori di google-services (build con il file dentro); null se ne manca uno. */
        fun fromResources(v: Map<String, String?>): FirebaseConfig? {
            val (k, p, a, d) = RESOURCE_NAMES.map { v[it]?.takeIf(String::isNotBlank) ?: return null }
            return FirebaseConfig(apiKey = k, projectId = p, appId = a, databaseUrl = d)
        }
    }
}

/** La stessa configurazione con le chiavi brevi del QR e di `hello` (design 24/09). */
@Serializable
data class QrFirebase(val k: String, val p: String, val a: String, val d: String, val t: String) {
    fun config(): FirebaseConfig = FirebaseConfig(apiKey = k, projectId = p, appId = a, databaseUrl = d, topic = t)
}
