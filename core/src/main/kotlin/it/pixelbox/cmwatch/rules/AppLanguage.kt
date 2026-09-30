package it.pixelbox.cmwatch.rules

import java.util.Locale

/** La lingua scelta nelle impostazioni del telefono (restyling 30/09): cambia i testi e la voce che legge. */
object AppLanguage {
    /** `tag` vuoto: nessuna scelta, l'app segue il telefono. */
    enum class Choice(val tag: String) { SYSTEM(""), ITALIAN("it"), ENGLISH("en") }

    /** Dai tag di `LocaleManager.applicationLocales`; una lingua che l'app non ha vale «come il telefono». */
    fun fromTags(tags: String): Choice =
        Choice.entries.firstOrNull { it.tag.isNotEmpty() && tags.startsWith(it.tag) } ?: Choice.SYSTEM

    fun voiceLocale(choice: Choice, system: Locale): Locale =
        if (choice == Choice.SYSTEM) system else Locale.forLanguageTag(choice.tag)
}
