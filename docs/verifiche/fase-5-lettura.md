# Verifica dal vivo: lettura vocale del testo intero (14/09/2026)

Build con il contratto 1.4 (`last`) e `SpeakService` in primo piano. Prova al polso con il relay acceso.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| 1 | Esito: il ▶ legge solo la frase d'esito, il titolo | Scheda, Esito, ▶ | ✅ 14/09 15:51, build `59e6525` |
| 2 | Terminale: il ▶ accanto al nome legge gli ultimi tre blocchi sopra il prompt, senza comandi di shell né statusline | Scheda, Terminale, ▶ | ✅ 14/09 15:51 |
| 3 | Scheda: «Ascolta la risposta» legge la risposta intera dal PC, oltre i 600 caratteri, e mentre legge dice «Ferma la lettura» | Scheda, Ascolta | ✅ «Ferma» 14/09 14:59, fermate da Franz; letture arrivate in fondo 15:15, voce scelta in Impostazioni |
| 4 | Domanda: il ▶ accanto al testo legge domanda e opzioni numerate | una domanda aperta | |
| 5 | Recap: «Ascolta il recap» legge per progetto cosa ha fatto e il prossimo passo | Menu, Recap | |
| 6 | Notifica di esito: «Leggi» legge la risposta intera senza aprire l'app | notifica di un esito | |
| 7 | Notifica di domanda: «Leggi» legge domanda e opzioni | notifica di una domanda | |
| 8 | A schermo spento la lettura continua fino in fondo | ▶, poi polso giù | |
| 9 | «Ferma» nella notifica «Lettura in corso» ferma subito | durante una lettura | |
| 10 | PC spento: dopo 5 s legge il testo corto che l'orologio ha già | relay fermo, ▶ su un esito | |
| 11 | Markdown e codice non si leggono a simboli; il codice si annuncia | una risposta con codice | ✅ 14/09 15:51, «Ascolta la risposta» su una risposta con un blocco Kotlin |

Lint vital: ✅ in CI sul codice installato (`59e6525`, run 34850828100, `:wear:lintVitalRelease`, BUILD SUCCESSFUL
13:47 UTC; prima su `586a9bd`, run 34847845524, e su `03d4305`, run 34844107945). In locale non gira: `lintVitalAnalyzeRelease` si blocca oltre 10 minuti sulla VM da 6 GB.
