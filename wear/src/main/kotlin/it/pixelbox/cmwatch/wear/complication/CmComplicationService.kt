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

/** Tre tipi, una sorgente (design, sezione 2): SHORT_TEXT «1?», RANGED_VALUE anello quota 5 h, LONG_TEXT «❓ ledger-api · Deploy now?». */
class CmComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = ContractJson.decodeState(assets.open("contract/state-1-question.json").bufferedReader().readText())
        return build(type, preview, fresh = true, account = "personale")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val app = application as CmApp
        val snap = app.repo.snapshot.value
        val account = app.prefs.current().complicationAccount
        return build(request.complicationType, snap.state, snap.freshness is Freshness.Fresh, account)
    }

    private fun build(type: ComplicationType, state: State?, fresh: Boolean, account: String): ComplicationData? {
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_notification)).build()
        val stale = getString(R.string.complication_stale)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text(ComplicationTexts.short(state, fresh)), text(ComplicationTexts.long(state, fresh, stale)))
                .setMonochromaticImage(icon).setTapAction(open(ComplicationTexts.tapTarget(state), 1)).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text(ComplicationTexts.long(state, fresh, stale)), text(ComplicationTexts.long(state, fresh, stale)))
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
