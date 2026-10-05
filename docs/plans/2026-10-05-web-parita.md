# Web app: parità con l'app Android (Franz, 05/10 15:07: «serve che sia almeno altrettanto rifinito»)

Ogni voce si chiude solo confrontando la resa della web app (chromium headless, stessa misura) con il provino Android
indicato, fianco a fianco.

- [x] Badge: forma per account, colore della sessione, glifi Lucide, contrasto (provino `SummaryListTest_summaryOpen`)
- [x] Home: righe della lista come `SummaryList` (età, contesto, gruppi con riga, «Fuori dalle sessioni», chiuse)
- [ ] Home della master: ultimo esito, Per te, quote ad anello (`SessionSheetTest_masterHomeA`)
- [ ] Testata della sessione: pillola modello · effort, anello 5h con azzeramento, anello ctx, menu ⋮ (`sheetBusyWithGoal`)
- [ ] Chat: markdown e tabelle, righe degli strumenti, costo del turno, copia e ▶, riga dal vivo (`sheetTranscript`, `sheetTables`)
- [ ] Domanda: scheda con opzioni, prima piena, Parliamone, Consenti tutto (`sheetQuestion`)
- [ ] Campo: + allega (immagini e file), suggerimento con Usa, Prossimi, Ricorrenti, slash (`sheetNextStepsTyping`, `masterRecurring`)
- [ ] Lettura a voce con mini-controller (Web Speech API), velocità e voce (`ReadingPillTest_*`)
- [ ] Colonne affiancate come sul tablet, trascinabili, con le larghezze a scatti (`TabletShellTest_tabletDesk`)
- [ ] Impostazioni e schema dei collegamenti (`settingsDevicesB`)
- [ ] Ricerca, Registro, Panoramica, Lancia
