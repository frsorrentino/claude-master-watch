package it.pixelbox.cmwatch.ui.tokens

import androidx.compose.ui.graphics.Color

/** Token del design (sezione 3): tema scuro unico, cobalto solo su bottone pieno, scroll bar, chip «seguita», anello quota. */
object CmColors {
    val bg = Color(0xFF000000)
    /** Tre gradini di superficie, non uno: sfondo, riga, card. Senza il gradino la profondità non si vede. */
    val surfaceLow = Color(0xFF1B1F26)
    val surface = Color(0xFF23272E)   // più chiaro (Franz, 12/09 20:03): come i tasti del selettore app di Wear OS
    val surfaceHigh = Color(0xFF292F3A)   // card in stile brief, misurata sui suoi fotogrammi
    val line = Color(0xFF2A2E35)
    val text = Color(0xFFF2F4F7)
    // Più chiaro (proposta A7, Franz 15/09 23:28: «un testo che contrasti di più»): da #9AA3B2 a 10:1 sul nero, sempre
    // sotto il bianco del testo principale e sotto `briefSecondary`, così la gerarchia resta.
    val text2 = Color(0xFFB0B8C4)
    val accent = Color(0xFF4C7DFF)
    // Bottone principale come nelle app Google M3 Expressive e come il tasto di bordo della tile (Franz, 16/09 00:41:
    // «colore appropriato» al posto del cobalto): azzurro pastello con testo blu notte, 11:1. Il cobalto resta allo stato
    // «lavora» e ai fili, non ai bottoni.
    val primary = Color(0xFFD3E3FD)
    val onPrimary = Color(0xFF0A2050)
    val accentPressed = Color(0xFF3457D5)
    // Azzurro pastello delle icone delle azioni (proposta del 14/09, confermata da Franz il 15/09 10:42): il cobalto
    // resta ai bottoni pieni, sulle icone era un blu acceso.
    val actionIcon = Color(0xFFA8C7FA)
    val waiting = Color(0xFFFFB020)
    // Giallino della sessione seguita, «come se fosse accesa» (Franz, 14/09 16:27): più chiaro dell'ambra di attesa.
    val followed = Color(0xFFFFE08A)
    val busy = Color(0xFF7FA1FF)
    val idle = Color(0xFF34C759)
    val gone = Color(0xFFFF453A)
    val goneDim = Color(0xFFC2554D)   // rosso desaturato: una sessione chiusa non deve urlare
    // 5:1 sulla superficie: il grigio di prima stava a 3:1 e al sole spariva (review UX, 13/09).
    val stale = Color(0xFF98A2B3)
    val accountAgenzia = Color(0xFFE53935)
    val accountPersonale = Color(0xFF43A047)

    // Stile del «brief mattutino» di Wear OS, misurato sui fotogrammi della sua schermata (13/09 16:25).
    val briefCard = surfaceHigh
    val briefLabel = Color(0xFFBCE4C7)
    val briefBig = Color(0xFFF4F4F4)
    val briefSecondary = Color(0xFFBCC0CB)
    val briefRing = Color(0xFF8BB4F7)
    val briefTrack = Color(0xFF455165)
    val briefChip = Color(0xFF7DCCFB)
    val briefChipInk = Color(0xFF001C33)
    val briefGood = Color(0xFF65C581)
    val briefGoodInk = Color(0xFF072510)
    val briefWarn = Color(0xFFFFC46B)
    val briefWarnInk = Color(0xFF2A1A00)
    val briefAlert = Color(0xFFF2B8B5)
    val briefAlertInk = Color(0xFF5F1412)
    val briefAlertRing = Color(0xFFE5736B)

    // L'anello interno della quota, la settimana (Franz, 16/09 11:52): lavanda, vicino all'azzurro delle 5 ore ma
    // distinguibile a colpo d'occhio. Stesso colore sulla pillola «settimana», che così fa da legenda senza scritte.
    val briefWeek = Color(0xFFB9A6F5)
    val briefWeekInk = Color(0xFF1F1147)

    // Il pallino della pillola del modello (Franz, 16/09 08:29): la famiglia si riconosce dal colore prima del nome.
    // Opus prende il corallo di Claude, che è il colore del marchio; le altre due restano dentro la tavolozza del brief.
    val modelOpus = Color(0xFFD97757)
    val modelSonnet = Color(0xFF8BB4F7)
    val modelHaiku = Color(0xFF65C581)
    val modelOther = Color(0xFFB0B8C4)
}
