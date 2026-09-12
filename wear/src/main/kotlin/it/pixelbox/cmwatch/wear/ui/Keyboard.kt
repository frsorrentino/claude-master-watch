package it.pixelbox.cmwatch.wear.ui

import android.app.RemoteInput
import android.content.Intent
import androidx.wear.input.RemoteInputIntentHelper

/** Tastiera di sistema Wear OS (detta già): niente microfono in-app (design, «Decisioni fisse»). */
object Keyboard {
    const val KEY = "text"

    fun intent(label: String): Intent {
        val i = RemoteInputIntentHelper.createActionRemoteInputIntent()
        val ri = RemoteInput.Builder(KEY).setLabel(label).setAllowFreeFormInput(true).build()
        RemoteInputIntentHelper.putRemoteInputsExtra(i, listOf(ri))
        return i
    }

    fun result(data: Intent?): String? =
        data?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(KEY)?.toString() }?.trim()?.takeIf { it.isNotEmpty() }
}
