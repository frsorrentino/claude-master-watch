package it.pixelbox.cmwatch.mobile

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Il registro dei crash e degli ANR (Franz, 05/10 14:08: «un log per capire come mai l'app va in crash»). Due fonti:
 * - le eccezioni non gestite, scritte prima che il processo muoia;
 * - all'avvio, il motivo per cui Android ha chiuso l'app l'ultima volta (`ApplicationExitInfo`, Android 11+): crash, ANR
 *   con la traccia dei thread, memoria finita.
 * Il file sta in `Android/data/<pacchetto>/files/crash-log.txt`, leggibile con adb; ogni voce va anche in logcat
 * col tag `cmwatch-crash`. Il file si tiene sotto i 2 MB, togliendo le voci più vecchie.
 */
object CrashLog {
    private const val TAG = "cmwatch-crash"
    private const val MAX_BYTES = 2_000_000
    private const val SEEN = "crash_log_seen"

    fun install(ctx: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { write(ctx, "CRASH nel thread «${t.name}»\n" + Log.getStackTraceString(e)) }
            previous?.uncaughtException(t, e)
        }
        runCatching { lastExits(ctx) }.onFailure { Log.w(TAG, "exit reasons", it) }
    }

    /** I motivi delle chiusure precedenti non ancora scritti: ANR e crash con la loro traccia, gli altri in una riga. */
    private fun lastExits(ctx: Context) {
        if (Build.VERSION.SDK_INT < 30) return
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return
        val prefs = ctx.getSharedPreferences("ui", Context.MODE_PRIVATE)
        val seen = prefs.getLong(SEEN, 0L)
        val exits = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 10).filter { it.timestamp > seen }.sortedBy { it.timestamp }
        for (x in exits) {
            val kind = when (x.reason) {
                ApplicationExitInfo.REASON_ANR -> "ANR"
                ApplicationExitInfo.REASON_CRASH -> "CRASH"
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH NATIVO"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "MEMORIA FINITA"
                ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "TROPPE RISORSE"
                ApplicationExitInfo.REASON_USER_REQUESTED -> "CHIUSA DALL'UTENTE"
                ApplicationExitInfo.REASON_EXIT_SELF -> "USCITA"
                ApplicationExitInfo.REASON_SIGNALED -> "SEGNALE ${x.status}"
                else -> "MOTIVO ${x.reason}"
            }
            val serious = x.reason in setOf(ApplicationExitInfo.REASON_ANR, ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE, ApplicationExitInfo.REASON_LOW_MEMORY, ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE)
            val trace = if (x.reason == ApplicationExitInfo.REASON_ANR) runCatching { x.traceInputStream?.bufferedReader()?.use { it.readText() } }.getOrNull()?.let { mainThread(it) } else null
            val pss = "pss ${x.pss / 1024} MB, rss ${x.rss / 1024} MB"
            if (serious) write(ctx, "$kind alle ${stamp(x.timestamp)} · ${x.description.orEmpty()} · $pss" + (trace?.let { "\n$it" } ?: ""), at = x.timestamp)
            else Log.i(TAG, "$kind alle ${stamp(x.timestamp)}")
        }
        exits.maxOfOrNull { it.timestamp }?.let { prefs.edit().putLong(SEEN, it).apply() }
    }

    /** Dalla traccia di un ANR il thread principale e quello del disegno: il resto sono i thread di sistema. */
    private fun mainThread(trace: String): String {
        val blocks = trace.split("\n\n")
        val keep = blocks.filter { b -> b.startsWith("\"main\"") || b.startsWith("\"RenderThread\"") }
        return (keep.ifEmpty { blocks.take(3) }).joinToString("\n\n").take(20_000)
    }

    private fun stamp(t: Long) = SimpleDateFormat("dd/MM HH:mm:ss", Locale.ITALY).format(Date(t))

    private fun write(ctx: Context, text: String, at: Long = System.currentTimeMillis()) {
        Log.e(TAG, text)
        val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        val f = File(dir, "crash-log.txt")
        val entry = "===== ${stamp(at)} · versione ${runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull()} · ${Build.MODEL} =====\n$text\n\n"
        val old = if (f.exists()) f.readText() else ""
        val all = old + entry
        f.writeText(if (all.length > MAX_BYTES) all.takeLast(MAX_BYTES) else all)
    }
}
