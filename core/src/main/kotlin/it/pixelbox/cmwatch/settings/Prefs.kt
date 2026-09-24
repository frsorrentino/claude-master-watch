package it.pixelbox.cmwatch.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class Settings(
    val paired: Boolean = false,
    val uid: String? = null,
    val host: String? = null,
    val deviceName: String = "watch-pixel5",
    val wrappedKey: String? = null,
    val ttsMinChars: Int = 120,
    /** Voce della lettura scelta nelle impostazioni; null = la predefinita del motore. */
    val ttsVoice: String? = null,
    val hapticQuestion: Boolean = true,
    val hapticOutcome: Boolean = true,
    val hapticGone: Boolean = true,
    val complicationAccount: String = "personale",
    val demoFixture: String = "state-1-question",
    /** Demo per i video (16/09 16:05): dati finti anche da accoppiati. Si accende e spegne solo via adb, extra `demo`. */
    val demoMode: Boolean = false,
    val seenQuestions: Set<String> = emptySet(),
    /** Configurazione Firebase arrivata dal telefono (JSON di FirebaseConfig); null = quella dentro la build, se c'è. */
    val firebaseJson: String? = null,
    /** Solo telefono: l'accoppiamento (JSON di PairingRecord). */
    val pairingJson: String? = null,
    /** Solo telefono: il QR da riprendere dopo il riavvio per un altro progetto Firebase. */
    val resumeQr: String? = null,
)

/** Le preferenze viste dal codice che si prova sulla JVM (il controller del telefono). */
interface SettingsStore {
    suspend fun current(): Settings
    suspend fun update(block: (Settings) -> Settings)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cmwatch")

class Prefs(private val ctx: Context) : SettingsStore {
    private object K {
        val paired = booleanPreferencesKey("paired"); val uid = stringPreferencesKey("uid"); val host = stringPreferencesKey("host")
        val deviceName = stringPreferencesKey("deviceName"); val wrappedKey = stringPreferencesKey("wrappedKey")
        val ttsMinChars = intPreferencesKey("ttsMinChars")
        val ttsVoice = stringPreferencesKey("ttsVoice")
        val hapticQuestion = booleanPreferencesKey("hapticQuestion"); val hapticOutcome = booleanPreferencesKey("hapticOutcome")
        val hapticGone = booleanPreferencesKey("hapticGone"); val complicationAccount = stringPreferencesKey("complicationAccount")
        val demoFixture = stringPreferencesKey("demoFixture")
        val demoMode = booleanPreferencesKey("demoMode")
        val seenQuestions = stringSetPreferencesKey("seenQuestions")
        val firebaseJson = stringPreferencesKey("firebaseJson"); val pairingJson = stringPreferencesKey("pairingJson"); val resumeQr = stringPreferencesKey("resumeQr")
    }

    val flow: Flow<Settings> = ctx.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            paired = p[K.paired] ?: d.paired, uid = p[K.uid], host = p[K.host],
            deviceName = p[K.deviceName] ?: d.deviceName, wrappedKey = p[K.wrappedKey],
            ttsMinChars = p[K.ttsMinChars] ?: d.ttsMinChars,
            ttsVoice = p[K.ttsVoice],
            hapticQuestion = p[K.hapticQuestion] ?: d.hapticQuestion, hapticOutcome = p[K.hapticOutcome] ?: d.hapticOutcome,
            hapticGone = p[K.hapticGone] ?: d.hapticGone,
            complicationAccount = p[K.complicationAccount] ?: d.complicationAccount,
            demoFixture = p[K.demoFixture] ?: d.demoFixture,
            demoMode = p[K.demoMode] ?: d.demoMode,
            seenQuestions = p[K.seenQuestions] ?: d.seenQuestions,
            firebaseJson = p[K.firebaseJson], pairingJson = p[K.pairingJson], resumeQr = p[K.resumeQr],
        )
    }

    override suspend fun current(): Settings = flow.first()

    /** Lettura-modifica-scrittura: con `hello` e `key` in coroutine diverse, senza lock una scrittura potrebbe perdersi. */
    private val writes = Mutex()

    override suspend fun update(block: (Settings) -> Settings) = writes.withLock {
        val s = block(current())
        ctx.dataStore.edit { p ->
            p[K.paired] = s.paired
            s.uid?.let { p[K.uid] = it } ?: p.remove(K.uid)
            s.host?.let { p[K.host] = it } ?: p.remove(K.host)
            p[K.deviceName] = s.deviceName
            s.wrappedKey?.let { p[K.wrappedKey] = it } ?: p.remove(K.wrappedKey)
            p[K.ttsMinChars] = s.ttsMinChars
            s.ttsVoice?.let { p[K.ttsVoice] = it } ?: p.remove(K.ttsVoice)
            p[K.hapticQuestion] = s.hapticQuestion; p[K.hapticOutcome] = s.hapticOutcome; p[K.hapticGone] = s.hapticGone
            p[K.complicationAccount] = s.complicationAccount
            p[K.demoFixture] = s.demoFixture
            p[K.demoMode] = s.demoMode
            p[K.seenQuestions] = s.seenQuestions.toList().takeLast(50).toSet()
            s.firebaseJson?.let { p[K.firebaseJson] = it } ?: p.remove(K.firebaseJson)
            s.pairingJson?.let { p[K.pairingJson] = it } ?: p.remove(K.pairingJson)
            s.resumeQr?.let { p[K.resumeQr] = it } ?: p.remove(K.resumeQr)
        }
        Unit
    }
}
