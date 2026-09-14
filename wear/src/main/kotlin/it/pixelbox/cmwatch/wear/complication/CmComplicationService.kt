package it.pixelbox.cmwatch.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.ComplicationTexts
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.push.BadgeBitmap

/** Tre tipi, una sorgente (design, sezione 2): SHORT_TEXT «1?», RANGED_VALUE anello quota 5 h, LONG_TEXT «❓ ledger-api · Deploy now?». */
class CmComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = ContractJson.decodeState(assets.open("contract/state-1-question.json").bufferedReader().readText())
        return build(type, preview, fresh = true, account = "personale", seen = emptySet())
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val app = application as CmApp
        val snap = app.repo.snapshot.value
        val prefs = app.prefs.current()
        return build(request.complicationType, snap.state, snap.freshness is Freshness.Fresh, prefs.complicationAccount, prefs.seenQuestions)
    }

    private fun build(type: ComplicationType, state: State?, fresh: Boolean, chosen: String, seen: Set<String>): ComplicationData? {
        // Contratto 1.8: se l'account scelto non c'è, l'anello mostra quello personale (per tipo, non per nome).
        val account = state?.let { st -> it.pixelbox.cmwatch.rules.Accounts.resolve(st, chosen) } ?: chosen
        val first = state?.sessions?.firstOrNull { s -> s.question?.let { q -> q.id !in seen } == true } ?: state?.sessions?.firstOrNull()
        val icon = if (first != null) MonochromaticImage.Builder(Icon.createWithBitmap(BadgeBitmap.draw(it.pixelbox.cmwatch.rules.Badge.of(first.account, first.color, first.state, first.icon, first.accountKind), mono = true))).build()
        else MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_app_mono)).build()
        val stale = getString(R.string.complication_stale)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text(ComplicationTexts.short(state, fresh, seen)), text(ComplicationTexts.long(state, fresh, stale, seen = seen)))
                .setMonochromaticImage(icon).setTapAction(open(ComplicationTexts.tapTarget(state), 1)).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text(ComplicationTexts.long(state, fresh, stale, seen = seen)), text(ComplicationTexts.long(state, fresh, stale, seen = seen)))
                .setMonochromaticImage(icon).setTapAction(open(ComplicationTexts.tapTarget(state), 2)).build()
            ComplicationType.RANGED_VALUE -> {
                val r = ComplicationTexts.ranged(state, account)
                RangedValueComplicationData.Builder(r.value, 0f, r.max, text(r.text))
                    .setText(text(r.text)).setTitle(text(account)).setMonochromaticImage(icon).setTapAction(open("cmwatch://quota", 3)).build()
            }
            else -> null
        }
    }

    private fun text(s: String) = PlainComplicationText.Builder(s).build()

    private fun open(uri: String, code: Int): PendingIntent =
        PendingIntent.getActivity(this, code, Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    companion object {
        fun requestUpdate(app: CmApp) {
            ComplicationDataSourceUpdateRequester.create(app, ComponentName(app, CmComplicationService::class.java)).requestUpdateAll()
        }
    }
}
