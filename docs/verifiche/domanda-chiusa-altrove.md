# Verifica: domanda chiusa dal terminale o dal telefono (14/09/2026)

Bug segnalato da Franz alle 22:10: se a una domanda sull'orologio si risponde dal terminale o dal telefono, sul polso
resta aperta. Alle 22:12: deve sparire da ogni superficie, entro pochi secondi.

Cause, due:

- PC: il flag `waiting/<sid>` lo toglievano solo UserPromptSubmit, Stop e una risposta dal polso; una risposta da
  tastiera lo lasciava fino alla fine del turno. Correzione in carico a claude-master: PostToolUse toglie il flag e fa
  il push; lo stato senza domanda genera l'evento `answered` e la sveglia FCM (contratto invariato).
- Orologio: nessuno chiudeva la notifica della domanda quando la domanda spariva dallo stato. Ora `Wake.plan` emette
  `CloseQuestion` e `Notifier.closeQuestion` la chiude se è ancora quella della domanda, anche ad app aperta.

| # | Superficie | Come si aggiorna | Test | Dal vivo |
|---|---|---|---|---|
| 1 | Notifica della domanda | `Wake.Action.CloseQuestion` → `Notifier.closeQuestion` | `WakeTest.aQuestionAnsweredElsewhereClosesItsNotification` | ✓ con riserva: Franz dal polso, «è sparita» (07:53, riferito da master); non precisato se guardava la notifica o la lista |
| 2 | Schermata Domanda aperta | senza domanda mostra «già risposta altrove», vibra, si chiude dopo 1,5 s; le opzioni spariscono | comportamento esistente (`QuestionScreen`) | non verificato: schermata non aperta durante la prova |
| 3 | Lista e scheda: badge ❓ e ordine | ricalcolo dallo stato | `QuestionClosedTest` (badge, ordine) | non verificato una per una |
| 4 | Tile: conteggio e bottone «Rispondi» | ricalcolo dallo stato, `RefreshTile` | `QuestionClosedTest` (tile) | non verificato |
| 5 | Complication | ricalcolo dallo stato, `RefreshComplications` | `QuestionClosedTest` (complication) | non verificato |
| 6 | Vibrazione e lettura | una domanda chiusa non genera `Notify(QUESTION)` | `WakeTest.sameQuestionDoesNotNotifyTwice` | non verificato |
| 7 | PC: flag, `claude-master sessions`, relay, bot Telegram | claude-master `5be7112` (0.4.7): PostToolUse toglie `waiting/<sid>` e fa il push, l'evento `answered` sveglia l'orologio via FCM, Telegram perde i bottoni | H5c, R7, A4d (suoi, 28/28) | ✓ relay: domanda 07:52:33, risposta da tastiera (`claude-master answer`) 07:52:58, stato senza domanda ed evento `answered` 07:53:06, 8 s. Telegram non verificato (S02 sospesa) |

Prova del 15/09/2026, condotta da master: sessione di prova con il plugin 0.4.7, orologio con la build `9fce709`
installata alle 07:40. Da rifare guardando le superfici 2-6 una per una.

Prova dal vivo, quando la correzione del relay è attiva: una domanda da questa sessione, risposta dal terminale;
entro pochi secondi sul polso nessuna notifica, nessun ❓ in lista, tile e complication.
