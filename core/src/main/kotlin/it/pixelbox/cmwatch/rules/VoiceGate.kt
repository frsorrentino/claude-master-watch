package it.pixelbox.cmwatch.rules

/**
 * Le frasi della live prima che la voce di sistema sia pronta (dal vivo 09/10 22:25: «la live è attiva da diversi minuti
 * ma non dice nulla»). Il motore della sintesi si collega in qualche secondo: l'avviso «Live accesa» e il primo recap,
 * detti prima, si perdevano e contavano come detti. Ora aspettano e partono appena la voce è pronta, nell'ordine; se la
 * voce non arriva entro l'attesa si lasciano andare, come prima, perché la live non resti ferma ad aspettare.
 */
class VoiceGate {
    enum class Say { NOW, LATER, SKIP }

    private var ready = false
    private var gaveUp = false
    private val waiting = mutableListOf<String>()

    /** Una frase: subito, in attesa della voce, o lasciata andare (la voce non è arrivata in tempo). */
    @Synchronized fun say(text: String): Say = when {
        ready -> Say.NOW
        gaveUp -> Say.SKIP
        else -> { waiting += text; Say.LATER }
    }

    /** La voce è pronta: le frasi in attesa, nell'ordine in cui erano state dette. */
    @Synchronized fun ready(): List<String> {
        ready = true
        return waiting.toList().also { waiting.clear() }
    }

    /** L'attesa è finita senza voce: quante frasi si lasciano andare. Con la voce già pronta, nessuna. */
    @Synchronized fun giveUp(): Int {
        if (ready) return 0
        gaveUp = true
        return waiting.size.also { waiting.clear() }
    }

    /** Zitta: le frasi in attesa non partono più. */
    @Synchronized fun clear() { waiting.clear() }
}
