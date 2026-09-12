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

data class Settings(
    val paired: Boolean = false,
    val uid: String? = null,
    val host: String? = null,
    val deviceName: String = "watch-pixel5",
    val wrappedKey: String? = null,
    val ttsMinChars: Int = 120,
    val hapticQuestion: Boolean = true,
    val hapticOutcome: Boolean = true,
    val hapticGone: Boolean = true,
    val complicationAccount: String = "personale",
    val demoFixture: String = "state-1-question",
    val seenQuestions: Set<String> = emptySet(),
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("cmwatch")

class Prefs(private val ctx: Context) {
    private object K {
        val paired = booleanPreferencesKey("paired"); val uid = stringPreferencesKey("uid"); val host = stringPreferencesKey("host")
        val deviceName = stringPreferencesKey("deviceName"); val wrappedKey = stringPreferencesKey("wrappedKey")
        val ttsMinChars = intPreferencesKey("ttsMinChars")
        val hapticQuestion = booleanPreferencesKey("hapticQuestion"); val hapticOutcome = booleanPreferencesKey("hapticOutcome")
        val hapticGone = booleanPreferencesKey("hapticGone"); val complicationAccount = stringPreferencesKey("complicationAccount")
        val demoFixture = stringPreferencesKey("demoFixture")
        val seenQuestions = stringSetPreferencesKey("seenQuestions")
    }

    val flow: Flow<Settings> = ctx.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            paired = p[K.paired] ?: d.paired, uid = p[K.uid], host = p[K.host],
            deviceName = p[K.deviceName] ?: d.deviceName, wrappedKey = p[K.wrappedKey],
            ttsMinChars = p[K.ttsMinChars] ?: d.ttsMinChars,
            hapticQuestion = p[K.hapticQuestion] ?: d.hapticQuestion, hapticOutcome = p[K.hapticOutcome] ?: d.hapticOutcome,
            hapticGone = p[K.hapticGone] ?: d.hapticGone,
            complicationAccount = p[K.complicationAccount] ?: d.complicationAccount,
            demoFixture = p[K.demoFixture] ?: d.demoFixture,
            seenQuestions = p[K.seenQuestions] ?: d.seenQuestions,
        )
    }

    suspend fun current(): Settings = flow.first()

    suspend fun update(block: (Settings) -> Settings) {
        val s = block(current())
        ctx.dataStore.edit { p ->
            p[K.paired] = s.paired
            s.uid?.let { p[K.uid] = it } ?: p.remove(K.uid)
            s.host?.let { p[K.host] = it } ?: p.remove(K.host)
            p[K.deviceName] = s.deviceName
            s.wrappedKey?.let { p[K.wrappedKey] = it } ?: p.remove(K.wrappedKey)
            p[K.ttsMinChars] = s.ttsMinChars
            p[K.hapticQuestion] = s.hapticQuestion; p[K.hapticOutcome] = s.hapticOutcome; p[K.hapticGone] = s.hapticGone
            p[K.complicationAccount] = s.complicationAccount
            p[K.demoFixture] = s.demoFixture
            p[K.seenQuestions] = s.seenQuestions.toList().takeLast(50).toSet()
        }
    }
}
