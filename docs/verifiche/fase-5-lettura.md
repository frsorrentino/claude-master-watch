# Verifica dal vivo: lettura vocale del testo intero (14/09/2026)

Build con il contratto 1.4 (`last`) e `SpeakService` in primo piano. Prova al polso con il relay acceso.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| 1 | Esito: il ▶ legge quello che si vede, il titolo e poi il testo sotto, niente di più | Scheda, Esito, ▶ | |
| 2 | Terminale: il ▶ legge la risposta mostrata accanto | Scheda, Terminale, ▶ | |
| 3 | Scheda: «Ascolta la risposta» legge la risposta intera dal PC, oltre i 600 caratteri, e mentre legge dice «Ferma la lettura» | Scheda, Ascolta | ✅ «Ferma» 14/09 14:59, fermate da Franz |
| 4 | Domanda: il ▶ accanto al testo legge domanda e opzioni numerate | una domanda aperta | |
| 5 | Recap: «Ascolta il recap» legge per progetto cosa ha fatto e il prossimo passo | Menu, Recap | |
| 6 | Notifica di esito: «Leggi» legge la risposta intera senza aprire l'app | notifica di un esito | |
| 7 | Notifica di domanda: «Leggi» legge domanda e opzioni | notifica di una domanda | |
| 8 | A schermo spento la lettura continua fino in fondo | ▶, poi polso giù | |
| 9 | «Ferma» nella notifica «Lettura in corso» ferma subito | durante una lettura | |
| 10 | PC spento: dopo 5 s legge il testo corto che l'orologio ha già | relay fermo, ▶ su un esito | |
| 11 | Markdown e codice non si leggono a simboli; il codice si annuncia | una risposta con codice | |

Lint vital: ✅ in CI sul codice installato (`03d4305`, run 34844107945, `:wear:lintVitalRelease`, BUILD SUCCESSFUL
12:39 UTC). In locale non gira: `lintVitalAnalyzeRelease` si blocca oltre 10 minuti sulla VM da 6 GB.
