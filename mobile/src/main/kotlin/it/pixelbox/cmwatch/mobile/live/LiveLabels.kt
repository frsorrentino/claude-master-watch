package it.pixelbox.cmwatch.mobile.live

import android.content.Context
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.LiveDesk
import it.pixelbox.cmwatch.rules.LiveFeed
import java.time.ZoneId

/** Le frasi della modalità live, da strings.xml nella lingua dell'app. */
object LiveLabels {
    fun lang(ctx: Context): LiveDesk.Lang = LiveDesk.Lang(feed(ctx), words(ctx), ZoneId.systemDefault())

    fun feed(ctx: Context) = LiveFeed.Labels(
        code = ctx.getString(R.string.tts_code),
        numbers = ctx.resources.getStringArray(R.array.live_numbers).toList(),
        question = ctx.getString(R.string.live_q), option = ctx.getString(R.string.live_option),
        more = ctx.getString(R.string.live_more), approval = ctx.getString(R.string.live_approval),
        where = ctx.getString(R.string.live_where), outcome = ctx.getString(R.string.live_outcome),
        next = ctx.getString(R.string.live_next), step = ctx.getString(R.string.live_step),
        gone = ctx.getString(R.string.live_gone), restartFailed = ctx.getString(R.string.live_restart_failed),
        quota = ctx.getString(R.string.live_quota), quotaNoReset = ctx.getString(R.string.live_quota_no_reset),
        busy = ctx.getString(R.string.live_busy), idle = ctx.getString(R.string.live_idle),
        launched = ctx.getString(R.string.live_launched), recap = ctx.getString(R.string.live_recap),
    )

    fun words(ctx: Context) = LiveDesk.Words(
        alreadyAnswered = ctx.getString(R.string.live_already_answered),
        notUnderstood = ctx.getString(R.string.live_not_understood),
        notFound = ctx.getString(R.string.live_not_found),
        whatToSay = ctx.getString(R.string.live_what_to_say),
        sending = ctx.getString(R.string.live_sending),
        cancelled = ctx.getString(R.string.live_cancelled),
        approved = ctx.getString(R.string.live_approved),
        pickItem = ctx.getString(R.string.live_pick_item),
        confirm = ctx.getString(R.string.live_confirm),
        masterAsked = ctx.getString(R.string.live_master_asked),
        masterSlow = ctx.getString(R.string.live_master_slow),
        linkLost = ctx.getString(R.string.live_link_lost),
        linkBack = ctx.getString(R.string.live_link_back),
        nobodyWaiting = ctx.getString(R.string.live_nobody_waiting),
        nothingNew = ctx.getString(R.string.live_nothing_new),
        failed = ctx.getString(R.string.live_failed),
        outcomeLabel = ctx.getString(R.string.outcome_label),
    )
}
