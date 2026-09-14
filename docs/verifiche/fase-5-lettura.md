# Verifica dal vivo: lettura vocale del testo intero (14/09/2026)

Build con il contratto 1.4 (`last`) e `SpeakService` in primo piano. Prova al polso con il relay acceso.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| 1 | Esito: il ▶ legge solo la frase d'esito, il titolo | Scheda, Esito, ▶ | ✅ 14/09 15:51, build `59e6525` |
| 2 | Terminale: il ▶ accanto al nome legge gli ultimi tre blocchi sopra il prompt, senza comandi di shell né statusline | Scheda, Terminale, ▶ | ✅ 14/09 15:51 |
| 3 | Scheda: «Ascolta la risposta» legge la risposta intera dal PC, oltre i 600 caratteri, e mentre legge dice «Ferma la lettura» | Scheda, Ascolta | ✅ «Ferma» 14/09 14:59, fermate da Franz; letture arrivate in fondo 15:15, voce scelta in Impostazioni |
| 4 | Domanda: il ▶ accanto al testo legge domanda e opzioni numerate | una domanda aperta | ✅ 14/09 16:02, lettura di 31 s finita da sola, poi risposta dal polso (relay 16:02:44, «answered 1») |
| 5 | Recap: «Ascolta il recap» legge per progetto cosa ha fatto e il prossimo passo | Menu, Recap | ✗ 14/09 20:28: «Niente da riassumere», nessun tasto. Lo stato vivo alle 20:29 ha 0 voci di recap (9 sessioni, 10 progetti, 8402 byte): il relay taglia per prime le voci del recap sopra gli 8 KB (contratto 1.6). Richiesta a claude-master |
| 6 | Notifica di esito: «Leggi» legge la risposta intera senza aprire l'app | notifica di un esito | ✅ 14/09 17:18, confermato da Franz |
| 7 | Notifica di domanda: «Leggi» legge domanda e opzioni | notifica di una domanda | ✅ 14/09 16:01, notifica 16:00:25, lettura di 31 s finita da sola |
| 8 | A schermo spento la lettura continua fino in fondo | ▶, poi polso giù | ✅ 14/09 16:06, 80 s di voce con lo schermo spento o in ambient, confermato da Franz |
| 9 | Nella notifica da cui è partita la lettura, «Leggi» diventa «Ferma» e un tocco ferma subito | «Leggi» su una notifica di esito o di domanda | ✅ 14/09 17:18 (`114dbd4`), confermato da Franz. Riformulata alle 16:08: «Lettura in corso» è una notifica in corso e Wear OS non la mostra nell'elenco |
| 10 | PC spento: dopo 5 s legge il testo corto che l'orologio ha già | relay fermo, ▶ su un esito | |
| 11 | Markdown e codice non si leggono a simboli; il codice si annuncia | una risposta con codice | ✅ 14/09 15:51, «Ascolta la risposta» su una risposta con un blocco Kotlin |

Relay `2e0a2df` (ritardo degli eventi) dal vivo, 14/09: domanda 20:29:57, push che la porta 20:30:05; risposta dalla
notifica 20:30:38, push con «answered» 20:30:44, 6 s dopo (prima aspettava il prompt successivo, circa 3 minuti).
Esito: stop 20:31:56 (`outcome.at`), push con l'esito 20:32:04, 8 s dopo. ✅ entrambe le metà della correzione.

Lint vital: ✅ in CI sul codice installato (`c32a946`, run 34880878681, `:wear:lintVitalRelease`, BUILD SUCCESSFUL
18:32 UTC; prima su `0725313`, `41adb72`, `a83a0ad`, `544623e`, `472868f`, `114dbd4`, `59e6525`, `586a9bd` e `03d4305`). In locale non gira: `lintVitalAnalyzeRelease` si blocca oltre 10 minuti sulla VM da 6 GB.
