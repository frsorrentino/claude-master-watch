# S11, suggerimenti di risposta: piano

Spec: `claude-master/docs/review-2026-09/specifiche/S11-suggerimenti-di-risposta.md` (approvata 14/09/2026 22:10).
Questo file contiene solo il passo 1, chiesto dal mandato notturno del 14/09; l'implementazione aspetta Franz.

## Passo 1: il tasto che Franz vede (15/09/2026 00:40)

**Esito: con ogni probabilità è una risposta generata da Wear OS, non il suggerimento di Claude Code.**

- `Notifier.outcome` (`wear/src/main/kotlin/it/pixelbox/cmwatch/wear/push/Notifier.kt:213-214`): l'azione «Scrivi»
  della notifica di esito ha un `RemoteInput` senza `setChoices` e l'azione non chiama
  `setAllowGeneratedReplies(false)`. In `NotificationCompat.Action.Builder` il valore di default è `true`, quindi
  Wear OS legge il testo della notifica e propone frasi sue come tasti.
  Riga da cercare: `git grep -n 'setLabel(labels.write).build())' -- wear` → `Notifier.kt:214`.
- Lo stesso fenomeno c'è già stato: `ce69806` (13/09 20:09). Sulla notifica di una domanda Franz vedeva «Ok, provo» e
  «Ok ora provo», frasi inventate dal sistema; lì il flag ora è `false` (`Notifier.kt:166`). La notifica di esito
  non è stata toccata.
- Il suggerimento grigio di Claude Code non arriva all'orologio per nessuna strada: il contratto 1.8 non ha un campo
  per lui, `NotificationPlan.outcome` (`core/src/main/kotlin/it/pixelbox/cmwatch/rules/NotificationPlan.kt:77-84`)
  mette `choices = emptyList()`, e `git grep -i -E 'sugger|suggest|ghost|hint'` su core, wear e contract trova solo
  la cornice del terminale che `TerminalText` scarta, più testi d'interfaccia.
- Manca la prova dal vivo: uno screenshot della notifica di esito al polso con il tasto, poi la stessa notifica con il
  flag a `false`, dove il tasto deve sparire. Serve l'orologio.

## Cosa cambia per S11 (decide Franz prima di andare avanti)

Il presupposto della spec, «il tasto esiste già, S11 lo estende», non regge: non c'è un canale del suggerimento
standard da riusare. Due strade:

- **A.** Il suggerimento standard lo porta il relay: claude-master lo legge dallo schermo della sessione (`cm-talk.py`
  riconosce già il grigio SGR 2, T16) e lo mette per primo in `outcome.suggestions`. Rimette nel conto la lettura dallo
  schermo che la spec aveva tolto: circa 2-3 ore in più per claude-master, stima.
- **B.** Niente suggerimento standard: solo i nostri (riga «Prossimi:» e ripiego), fino a 3.

In tutte e due le risposte generate da Wear OS sull'esito vanno spente (`setAllowGeneratedReplies(false)` su
`Notifier.kt:213`): altrimenti accanto ai nostri tasti restano frasi inventate. È una riga; si fa con S11, o prima se
Franz lo preferisce.

Proposta: A. Il suggerimento di Claude Code è spesso il passo successivo giusto, e il riconoscimento del grigio esiste
già nel relay.

## Passi successivi, dopo la decisione

1. Richiesta a claude-master: contratto 1.9 con `outcome.suggestions` (al massimo 3, 40 caratteri ciascuno, senza
   doppioni; con A il suggerimento standard per primo).
2. Fixture 1.9 in `contract/`; test in core: le scelte della notifica di esito sono i suggerimenti, le risposte
   generate sono spente.
3. `Notifier.outcome` con `setChoices` e `setAllowGeneratedReplies(false)`; schermata Esito con i tasti e il testo
   modificabile prima dell'invio.
4. Prova al polso: screenshot della notifica, prompt arrivato alla sessione (`claude-master screen <nome>`).
