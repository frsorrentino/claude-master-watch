package it.pixelbox.cmwatch.wear

import android.content.Context
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.LiveFeed

/** Le frasi della live del telefono, copiate nell'orologio (`say_*`): servono a leggere lo stato di una sessione al tocco. */
object SayLabels {
    fun of(ctx: Context) = LiveFeed.Labels(
        code = ctx.getString(R.string.tts_code),
        numbers = ctx.resources.getStringArray(R.array.say_numbers).toList(),
        question = ctx.getString(R.string.say_q), option = ctx.getString(R.string.say_option),
        more = ctx.getString(R.string.say_more), approval = ctx.getString(R.string.say_approval),
        where = ctx.getString(R.string.say_where), outcome = ctx.getString(R.string.say_outcome),
        next = ctx.getString(R.string.say_next), step = ctx.getString(R.string.say_step),
        gone = ctx.getString(R.string.say_gone), restartFailed = ctx.getString(R.string.say_restart_failed),
        quota = ctx.getString(R.string.say_quota), quotaNoReset = ctx.getString(R.string.say_quota_no_reset),
        busy = ctx.getString(R.string.say_busy), idle = ctx.getString(R.string.say_idle),
        launched = ctx.getString(R.string.say_launched), recap = ctx.getString(R.string.say_recap),
        recapBusy = ctx.getString(R.string.say_recap_busy), recapTask = ctx.getString(R.string.say_recap_task),
        choices = ctx.getString(R.string.say_choices),
        thinking = ctx.getString(R.string.say_thinking), minute = ctx.getString(R.string.say_recap_minute), minutes = ctx.getString(R.string.say_recap_minutes),
    )
}
