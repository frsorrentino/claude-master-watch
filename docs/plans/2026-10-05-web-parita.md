# Web app: parità con l'app Android (Franz, 05/10 15:07: «serve che sia almeno altrettanto rifinito»)

Ogni voce si chiude solo confrontando la resa della web app (chromium headless, stessa misura) con il provino Android
indicato, fianco a fianco.

- [x] Badge: forma per account, colore della sessione, glifi Lucide, contrasto (provino `SummaryListTest_summaryOpen`)
- [x] Home: righe della lista come `SummaryList` (età, contesto, gruppi con riga, «Fuori dalle sessioni», chiuse)
- [x] Home della master: ultimo esito, Per te, quote, barra della master nella home (`SessionSheetTest_masterHomeA`, `SummaryListTest_dockMaster`).
  «Per te» completo: domande, turni finiti, contesto, resoconto della notte, notte con il foglio «Aggiungi alla notte», prossimi passi del recap con Avvia, invii programmati, «+N altre» nel foglio.
- [x] Testata della sessione: pillola modello · effort col foglio delle scelte, anello 5h con azzeramento, anello ctx col suo foglio, menu ⋮ a pannello, obiettivo, priorità e finestra (`sheetBusyWithGoal`). I comandi (modello, effort, segui, /exit) aspettano il trasporto: per ora restano nella pagina.
- [x] Chat: markdown e link, tabelle (griglia o schede), righe degli strumenti e passaggi raggruppati, file, costo del turno, stato dei messaggi, copia e ▶, riga dal vivo (`sheetTranscript`, `sheetTables`). Regole ChatFeed, Markdown, MarkdownTable, ToolText, ChatRules, Links, OutcomeLine, Preposition portate coi test Kotlin. Il ▶ provato dal vivo (05/10 16:30) con un clic vero: legge in italiano, diventa ■ e torna ▶ a fine lettura; un clic simulato dà `not-allowed`, perché Chrome vuole un gesto dell'utente.
- [ ] Domanda: scheda con opzioni, prima piena, Parliamone, Consenti tutto (`sheetQuestion`)
- [ ] Campo: + allega (immagini e file), suggerimento con Usa, Prossimi, Ricorrenti, slash (`sheetNextStepsTyping`, `masterRecurring`)
- [ ] Lettura a voce con mini-controller (Web Speech API), velocità e voce (`ReadingPillTest_*`)
- [ ] Colonne affiancate come sul tablet, trascinabili, con le larghezze a scatti (`TabletShellTest_tabletDesk`)
- [ ] Impostazioni e schema dei collegamenti (`settingsDevicesB`)
- [ ] Ricerca, Registro, Panoramica, Lancia
