# Grafica, movimento e nuove superfici — piano (16/09/2026)

Franz, 16/09 00:26: «le farei tutte, preservando il design di questa tile, aggiungendo per essa funzioni o nuove
tile». Nella Scheda: contesto, modello e effort della sessione, meglio se selezionabili. Proposte:
`docs/plans/2026-09-15-grafica-proposte.md` (punti 13-49; 1-12 fatti in `7898035`, 12 non necessario).

## Vincoli

- **La tile attuale non cambia disegno.** Le proposte che la toccavano diventano tile nuove (40, 41, 30) o funzioni che
  non alterano la sua forma. Ogni nuova tile è un servizio a sé nel carosello.
- Stati e colori restano quelli di Telegram; il colore dinamico (15) solo per l'accento.
- Ogni animazione: motion scheme M3, «riduci animazioni» rispettato, niente in ambient, niente loop (tranne il badge).
- Una fase si chiude con build release locale, installazione, prove al polso in checklist e snapshot Paparazzi in CI.
- Contratto: una richiesta per volta a claude-master; quella della fase 3 è l'unica di questo piano.

## Fase 1 — solo app, nessun contratto

| # | Cosa | Dove |
|---|------|------|
| 17 | corona: Domanda un'opzione per volta con scatto aptico; tic sulle liste | `QuestionScreen`, liste |
| 18 | sì/no affiancati (`ButtonGroup`) con due opzioni | `QuestionScreen` |
| 19 | numeri che rotolano (percentuali, età, contatori) | componente `RollingText` |
| 20 | bordo HIGH che respira una volta all'arrivo | `QuestionScreen` |
| 21 | barra di lettura sotto ▶ (posizione nella risposta) | `Speaker` → `SpeakButton` |
| 22 | recap riga per riga (`FadingExpandingLabel`/`AnimatedText`) | `RecapScreen` |
| 23 | ambient curato su ogni schermata | tutte |
| 24 | splash con icona animata | `themes`, vettore animato |
| 25 | swipe tra le Schede delle sessioni (`HorizontalPagerScaffold`) | rotta Scheda |
| 26 | `SwipeToReveal` sulla riga: Segui / Chiudi | `SessionRow` |
| 27 | Impostazioni con `Slider`/`Stepper`/`Picker` | `SettingsScreen` |
| 38 | filo delle tue righe nel colore della sessione | `TerminalScreen` |
| 39 | tasti con icona e testo | `WideButton`, Scheda |
| 42 | Avvia: progetti frequenti come moduli colorati; Menu a moduli | `LaunchScreen`, `MenuScreen` |
| 44 | bagliore al bordo mentre la sessione lavora, si allarga all'esito | Scheda |
| 45 | forma espressiva solo per il tasto del Menu | `CmEdgeButton` |
| 46 | anelli concentrici 5h/7d per account | `Gauge`, `QuotaScreen` |
| 47 | card «suggerimento» in cima alla lista che si apre scorrendo | `SessionsScreen` |
| 48 | giro «due secondi» su ogni schermata (Quota: il reset in evidenza) | tutte |
| 33, 36 | Quota: «Ritmo della finestra» al posto di «Aggiornato», ora e macchina in una riga in fondo | `BriefCards`, storia quota in Room |
| 34, 35 | Quota: barre di Oggi (dagli eventi) e della Settimana (dalla storia) | `QuotaScreen` |
| 37 | Scheda: banda della giornata per stato | Scheda, dagli eventi |

Core prima (test): storia della quota (`QuotaHistory`: campioni da ogni stato, finestra 5h e 7 giorni, proiezione al
reset), barre per ora dagli eventi, banda di stato di una sessione, regole di `RollingText`.

## Fase 2 — nuove superfici, tile attuale intatta

| # | Cosa |
|---|------|
| 40, 41, 30 | tile «Domanda»: solo quando qualcuno aspetta, testo della domanda e due tasti grandi affiancati (sì/no o Rispondi/Apri), badge che pulsa una volta con Lottie |
| 41, 46 | tile «Quota»: anelli concentrici per account e ora del reset |
| 28 | complicazione `WEIGHTED_ELEMENTS`: anello diviso per stato delle sessioni |
| 46 | complicazione ad anelli concentrici della quota (`RANGED_VALUE` con rampa di colore dove serve) |
| 43 | complicazione della sessione seguita (badge ed età, apre la Scheda) |

## Fase 3 — contratto 1.11 (richiesta a claude-master, 16/09)

| # | Cosa |
|---|------|
| 32 | Scheda: riquadro con contesto (anello, ambra > 75 %, rosso > 90 %), modello, effort, quota del suo account |
| 32+ | modello ed effort selezionabili dal polso (`Picker`), con conferma animata |

## Fase 4 — piattaforma

| # | Cosa |
|---|------|
| 29, 13 | Live Updates al posto dell'Ongoing Activity per la sessione seguita (Wear OS 7) |
| 31 | aptica diversa per domanda, esito, errore, conferma |
| 16 | scorrimento a scatti in Terminale e Risposta |
| 15 | accento dal colore del quadrante |
| 14 | Wear Widget (Glance + RemoteCompose) piccolo e grande |

## Da decidere strada facendo

- 25 e 26 cambiano gesti: provare al polso che non litighino con lo swipe di ritorno.
- 49: filo di luce sul bordo delle card solo come prova, poi Franz decide.

## Decisioni sul contratto (16/09)

- 00:32, claude-master: `model`, `context`, `effort` si leggono in modo affidabile dalle trascrizioni; `/model` ed `/effort`
  nel pane riscrivono anche il default dell'account in `~/.claude/settings.json`.
- 00:33, questa sessione: 1.11 solo con i campi (`context` assente quando non è certo), niente op e niente `choices`.
- 00:39, Franz: op con ripristino (b) se affidabile, altrimenti (c) con l'effetto sul default detto nel `/result`. Chiesta
  come 1.12 dopo la 1.11. Condizioni per la (b): riscrittura solo delle chiavi toccate, lock sul file per tutta la
  sequenza, rilettura di verifica, ripristino anche senza conferma dal pane. Scelta finale e motivo li dà claude-master.

## Anelli negli snapshot: sistemato (16/09 03:39, `cc708f1`)

L'arco si riempiva solo all'ingresso nell'inquadratura e in Paparazzi, dove la lista non scorre mai, restava vuoto:
le immagini del README mostravano anelli spenti. Ora, finché la lista non ha misurato nulla (primo disegno, e sempre
sotto Paparazzi), le card contano come visibili. Sul polso non cambia niente: lì la lista misura e l'arco aspetta
ancora che la sua card entri.
