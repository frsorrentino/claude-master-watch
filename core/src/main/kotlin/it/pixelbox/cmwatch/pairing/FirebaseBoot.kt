package it.pixelbox.cmwatch.pairing

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Firebase avviato dall'app, non dalla build (design 24/09, «Orologio: Firebase a runtime»). Una volta per processo: con
 * un progetto diverso l'app si riavvia, perché riavviare Firebase dentro un processo avviato è fragile (spike B).
 */
object FirebaseBoot {
    /** Quella arrivata dal telefono vince su quella dentro la build. */
    fun pick(stored: FirebaseConfig?, bundled: FirebaseConfig?): FirebaseConfig? = stored ?: bundled

    @Volatile var active: FirebaseConfig? = null
        private set

    /** I valori del plugin google-services, letti per nome: esistono solo se la build aveva il file. */
    @SuppressLint("DiscouragedApi")
    fun bundled(ctx: Context): FirebaseConfig? = FirebaseConfig.fromResources(FirebaseConfig.RESOURCE_NAMES.associateWith { n ->
        ctx.resources.getIdentifier(n, "string", ctx.packageName).takeIf { it != 0 }?.let(ctx::getString)
    })

    /** Avvia Firebase con la configurazione scelta; null se non ce n'è una o se l'avvio fallisce. */
    @Synchronized fun start(ctx: Context, stored: FirebaseConfig?): FirebaseConfig? {
        active?.let { return it }
        val cfg = pick(stored, bundled(ctx)) ?: return null
        val opts = FirebaseOptions.Builder()
            .setApiKey(cfg.apiKey).setApplicationId(cfg.appId).setProjectId(cfg.projectId)
            .setDatabaseUrl(cfg.databaseUrl).setGcmSenderId(cfg.senderId)
            .build()
        return runCatching { FirebaseApp.initializeApp(ctx.applicationContext, opts); cfg }
            .onFailure { android.util.Log.w("cmwatch", "firebase: ${it.message}") }
            .getOrNull()
            ?.also { active = it }
    }

    /** Stesso progetto, topic nuovo: basta aggiornare quello attivo. */
    fun retopic(topic: String) { active = active?.copy(topic = topic) }
}
