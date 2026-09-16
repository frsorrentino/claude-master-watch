# Verifica dal vivo: lettura vocale del testo intero (14/09/2026)

Build con il contratto 1.4 (`last`) e `SpeakService` in primo piano. Prova al polso con il relay acceso.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| 1 | Esito: il ▶ legge solo la frase d'esito, il titolo | Scheda, Esito, ▶ | ✅ 14/09 15:51, build `59e6525` |
| 2 | Terminale: il ▶ accanto al nome legge gli ultimi tre blocchi sopra il prompt, senza comandi di shell né statusline | Scheda, Terminale, ▶ | ✅ 14/09 15:51 |
| 3 | Scheda: «Ascolta la risposta» legge la risposta intera dal PC, oltre i 600 caratteri, e mentre legge dice «Ferma la lettura» | Scheda, Ascolta | ✅ «Ferma» 14/09 14:59, fermate da Franz; letture arrivate in fondo 15:15, voce scelta in Impostazioni |
| 4 | Domanda: il ▶ accanto al testo legge domanda e opzioni numerate | una domanda aperta | ✅ 14/09 16:02, lettura di 31 s finita da sola, poi risposta dal polso (relay 16:02:44, «answered 1») |
| 5 | Recap: «Ascolta il recap» legge per progetto cosa ha fatto e il prossimo passo | Menu, Recap | ✗ 14/09 20:28: «Niente da riassumere», nessun tasto. Lo stato vivo alle 20:29 ha 0 voci di recap (9 sessioni, 10 progetti, 8402 byte): il relay taglia per prime le voci del recap sopra gli 8 KB (contratto 1.6). Corretto da claude-master alle 20:59 (contratto 1.7, `5c11fdb`, attivo); seconda causa, il riassunto delle 20:00 vuoto dal 12/09 perché il cron non trovava `claude` (`8f2e9ff`), attiva solo con il rilascio della 0.4.7. Da riprovare dopo il primo recap non vuoto |
| 6 | Notifica di esito: «Leggi» legge la risposta intera senza aprire l'app | notifica di un esito | ✅ 14/09 17:18, confermato da Franz |
| 7 | Notifica di domanda: «Leggi» legge domanda e opzioni | notifica di una domanda | ✅ 14/09 16:01, notifica 16:00:25, lettura di 31 s finita da sola |
| 8 | A schermo spento la lettura continua fino in fondo | ▶, poi polso giù | ✅ 14/09 16:06, 80 s di voce con lo schermo spento o in ambient, confermato da Franz |
| 9 | Nella notifica da cui è partita la lettura, «Leggi» diventa «Ferma» e un tocco ferma subito | «Leggi» su una notifica di esito o di domanda | ✅ 14/09 17:18 (`114dbd4`), confermato da Franz. Riformulata alle 16:08: «Lettura in corso» è una notifica in corso e Wear OS non la mostra nell'elenco |
| 10 | PC spento: dopo 5 s legge il testo corto che l'orologio ha già | relay fermo, ▶ su un esito | |
| 11 | Markdown e codice non si leggono a simboli; il codice si annuncia | una risposta con codice | ✅ 14/09 15:51, «Ascolta la risposta» su una risposta con un blocco Kotlin |

Relay `2e0a2df` (ritardo degli eventi) dal vivo, 14/09: domanda 20:29:57, push che la porta 20:30:05; risposta dalla
notifica 20:30:38, push con «answered» 20:30:44, 6 s dopo (prima aspettava il prompt successivo, circa 3 minuti).
Esito: stop 20:31:56 (`outcome.at`), push con l'esito 20:32:04, 8 s dopo. ✅ entrambe le metà della correzione.

Lint vital: ✅ in CI anche con le stringhe inglesi (`b6c92d7`, run 34894981374, BUILD SUCCESSFUL 20:51 UTC).
Prima: ✅ in CI (`42867a4`, run 34893744588, `:wear:lintVitalRelease`, BUILD SUCCESSFUL 20:42 UTC), dopo la
correzione di `setup-android` (il pacchetto «tools» sparito dall'SDK rompeva ogni run dalle 20:29 UTC); comprende
`fe99eff` (domanda chiusa altrove) e `fa5ecc5`; prima su `15d80a4`, `c32a946`, `0725313`, `41adb72`, `a83a0ad`, `544623e`, `472868f`, `114dbd4`, `59e6525`, `586a9bd` e `03d4305`). In locale non gira: `lintVitalAnalyzeRelease` si blocca oltre 10 minuti sulla VM da 6 GB.

## Terminale copione e dal vivo (design 15/09, `docs/plans/2026-09-15-terminale-copione-design.md`)

| # | Cosa | Come | Esito |
|---|------|------|-------|
| T1 | Le tue righe (prompt, risposte a una domanda) hanno il filo azzurro e il testo azzurro chiaro; la prosa di Claude è in chiaro; strumenti e output in mono grigio; niente bolle | Scheda, Terminale, su una sessione con un prompt recente | ✅ 15/09 23:25 (strumenti e output poco contrastati: grigio schiarito da `text2` a `briefSecondary`), build `3a356a5`, confermato da Franz («per il resto tutto ok») |
| T2 | Il divisore dice «Terminale · HH:mm» con l'ora dell'ultima cattura | come T1 | ✅ 15/09 23:25, build `3a356a5`, confermato da Franz («per il resto tutto ok») |
| T3 | Con il Terminale aperto su una sessione che lavora, le righe nuove arrivano senza toccare nulla, al massimo una cattura ogni 3 s | Terminale aperto, polso fermo | ✅ 15/09 23:25, build `3a356a5`, confermato da Franz («per il resto tutto ok») |
| T4 | Sceso in fondo, la vista segue le righe nuove; risalito, resta dov'è | scroll durante T3 | ✅ 15/09 23:25, build `3a356a5`, confermato da Franz («per il resto tutto ok») |
| T5 | A fine turno la Risposta in cima si aggiorna da sola | T3 fino allo stop della sessione | ✅ 15/09 23:25, build `3a356a5`, confermato da Franz («per il resto tutto ok») |
| T6 | Niente tasto Aggiorna; ▶ accanto al nome, che non va a capo; la prosa va a capo dove va a capo il polso | Terminale | ✅ 15/09 23:25, build `3a356a5`, confermato da Franz («per il resto tutto ok») |

## Grafica e movimento, primo giro (proposte A1-A7, B8-B11, `docs/plans/2026-09-15-grafica-proposte.md`)

Build `7898035`, installata il 16/09 alle 00:07:35. Da provare con «riduci animazioni» spento e poi acceso.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| G1 | ▶ si schiaccia al tocco e in lettura diventa un quadrato chiaro con ■ | Domanda o Terminale, ▶ | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G2 | Il nome resta su una riga accanto a ▶ («ledger-api», «atlas-shop»), più piccolo se serve | Domanda, Scheda, Terminale | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G3 | Nel Terminale dal vivo i blocchi nuovi entrano in dissolvenza, i vecchi restano fermi; l'ora del divisore sfuma | Terminale aperto su una sessione che lavora | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G4 | Il paragrafo letto si accende piano | Terminale, ▶ sulla Risposta | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G5 | Nella lista le card scivolano al loro posto quando cambia l'ordine; il badge sfuma da ▶ a ✓ o ❓ | lista, una sessione che finisce o fa una domanda | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G6 | Dopo una risposta o un prompt: spunta «Fatto» (o croce «Non riuscito») e vibrazione; nessuna vibrazione a Terminale aperto | rispondere a una domanda; Terminale aperto 30 s | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G7 | Grigio secondario più leggibile (età, dettagli, Timeline) | lista, Timeline | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G8 | Al posto di «Chiedo al PC» tre righe che luccicano | aprire il Terminale | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G9 | Gli archi della Quota si riempiono con una molla; con il dato vecchio si spengono piano | Quota | ✗ 16/09 01:00: il riempimento finiva mentre la schermata entrava (registrazione dal polso, 7 %). Corretto in `9b806ad` (parte dopo ~400 ms), da riprovare |
| G10 | Accanto all'ora «❓ n» in ambra quando qualcuno aspetta | qualsiasi schermata dell'app con una domanda aperta | ✅ 16/09 01:00, build `7898035`, confermato da Franz |
| G11 | Nella tile la barra della quota cresce da sinistra quando la tile si carica | scorrere fino alla tile | ✗ 16/09 01:00: `animate(0,1)` partiva alla preparazione della tile, prima dello swipe. Corretto in `9b806ad` (cresce a tile visibile, `isLayoutVisible`), da riprovare |

## Fase 1, primo blocco (proposte 19, 20, 33-35, 38, 39, 44)

Build del 16/09 01:51. Da provare con «riduci animazioni» spento e poi acceso.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| F1 | Il numero grande della Quota rotola quando cambia (7 % → 8 %) | Quota, con il PC che lavora | |
| F2 | Gli archi si riempiono quando la loro card entra nell'inquadratura, non prima | Quota, scorrendo dall'alto | |
| F3 | Il bordo rosso di una domanda a rischio alto fa un respiro solo all'arrivo | una domanda HIGH | |
| F4 | Nella Scheda di una sessione che lavora c'è un bagliore in fondo, che respira piano e sparisce quando si ferma | Scheda di una sessione attiva | |
| F5 | Nel Terminale il filo delle tue righe ha il colore della sessione, lo stesso del badge | Terminale di due sessioni diverse | |
| F6 | Il tasto «Riavvia» e gli altri tasti con icona si leggono bene, icona e parola allineate | Scheda di una sessione chiusa | |

## Fase 1, secondo blocco (conferma nostra, box quota, anello concentrico, aptica)

Build `44381b5`, installata il 16/09 alle 03:27:00.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| H1 | La conferma è nostra: velo scuro, cerchio con campanella accesa in ambra o barrata in grigio, frase su una riga sotto; si chiude da sola dopo poco più di un secondo o al tocco | pressione lunga su una riga della lista | |
| H2 | «Ti avviso» e «Non ti avviso» si leggono intere, senza testo curvo e senza forma ruotata | come H1, nei due versi | |
| H3 | La vibrazione di «segui» sale e quella di «non seguire» scende, e si distinguono dall'invio | come H1, a occhi chiusi | |
| H4 | Nella Scheda c'è il riquadro con la quota dell'account di quella sessione | Scheda di una sessione qualsiasi | |
| H5 | L'anello del riquadro ha due cerchi: fuori le 5 ore, dentro la settimana; niente icona dell'orologio | come H4 e nella pagina Quota | |
| H6 | Gli anelli si riempiono quando la card entra nell'inquadratura, quello interno un attimo dopo l'esterno | Quota, scorrendo dall'alto | |
