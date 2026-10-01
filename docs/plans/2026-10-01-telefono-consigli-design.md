# Telefono: i consigli scritti dalla sessione (design, 01/10/2026)

Franz, 01/10 23:34: la frase rapida «via» fissa era generica e fuori luogo (tolta in `f65ddfe`); i consigli in più li
deve scrivere la sessione stessa quando si ferma, senza un modello o una sessione dedicati. 23:40: niente chip.
23:48: aspetto C, «sotto la risposta».

## Chi li scrive

La sessione, nella stessa risposta con cui chiude il turno e si ferma: un'ultima riga

    Prossimi: pusha e installa · scrivi il piano della casa master · correggi il foglio Lancia

- al massimo tre consigli, separati da « · », ognuno sotto i 40 caratteri, scritti come li scriverebbe Franz;
- concreti e legati a quello che la risposta ha appena detto; niente riga se non c'è un passo sensato;
- solo quando la sessione si ferma in attesa di Franz, non a metà lavoro.

La regola la mette l'hook di claude-master in tutte le sessioni, come la riga di esito. È una richiesta a claude-master
(una per volta: parte dopo il tetto delle sessioni).

## Come li legge l'app

- `NextSteps` in `core` (TDD): dall'ultima risposta di Claude toglie la riga `Prossimi:` e ne ricava i consigli; una
  riga malformata resta testo. Funziona anche senza il relay, sulla trascrizione che l'app ha già.
- Più avanti il relay può mettere gli stessi consigli nello stato (`sessions[].next_steps`) per il widget e le
  notifiche; per la chat non serve.

## Come si vedono (C)

- Sotto l'**ultima** risposta di Claude, prima di ora e tasti: l'etichetta «Prossimi» e i consigli in colonna, ognuno
  una riga di testo nel colore delle azioni preceduta da «↳». Mai sulle risposte precedenti.
- Tocco = il testo va nel campo, da modificare; pressione lunga = invio subito.
- Spariscono appena si scrive nel campo o si manda un messaggio.
- Il suggerimento di Claude Code (contratto 1.23) passa dalla pillola sopra la barra al **campo vuoto**: testo
  attenuato, come dopo ❯ nel terminale, con un piccolo «Usa» che lo mette nel campo. Solo a sessione ferma, come oggi.
- La riga `Prossimi:` non si vede mai nel testo della risposta, come «Watch:».

## Test

- Unit su `NextSteps`: riga valida, più di tre consigli (si tengono i primi tre), consigli troppo lunghi (si scartano),
  riga assente, riga in mezzo al testo (non conta: solo l'ultima riga non vuota).
- Provino Paparazzi della chat con i consigli sotto l'ultima risposta e il suggerimento nel campo.
- Dal vivo: tocco, pressione lunga, sparizione scrivendo.
