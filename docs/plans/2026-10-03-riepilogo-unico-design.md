# Riepilogo unico: Sessioni e casa della master fuse (design, 03/10/2026)

Approvato da Franz il 03/10 fra le 13:25 e le 14:04 sui mockup della tela «Riepilogo unico»
(https://claude.ai/artifact/RAxnFysAhU9MJaZHKLbwRV, tavole 4 e 5 come versione finale; 1-3 come prima stesura).

## Perché

Oggi la stessa sessione compare fino a tre volte: card di «Sessioni», «Per te» nella casa della master, menu a tendina
in alto. «Per te» con «Al lavoro» è già una lista delle sessioni per bisogno; «Sessioni» aggiunge solo i dettagli.

## Decisioni

1. **Una schermata sola, «Sessioni · N aperte».** Prende il posto della casa della master e della scheda Sessioni.
   Spariscono la barra delle schede in basso e il pager fra le schede (lo scorrimento fra le chat aperte resta).
2. **Ogni sessione una volta**, nell'ordine del bisogno, con l'intestazione di gruppo e il numero:
   - **Ti aspetta** (mano, ambra): domanda; aperta, il testo intero e le opzioni (`QuestionOptions`, rischio alto con la
     pressione lunga). La master compare qui se aspetta.
   - **Ha finito** (bandierina, verde): stessa regola di oggi in «Per te» (`MasterHome` FINISHED: seguite o scritte dal
     telefono, 12 ore, sparisce quando la apri o le scrivi). Aperta: esito e risposte rapide `Prossimi:`.
   - **Al lavoro** (fulmine, azzurro): ultimo esito, risposte rapide, contesto a destra.
   - **Ferme** (pausa, grigio): una riga breve «nome · da 2 h»; aperta, ultimo esito e risposte rapide.
   - **Chiuse · N**: una riga sola che apre l'elenco delle chiuse con «Riapri».
   - Righe aperte: sotto il testo i dettagli che oggi stanno nelle card (obiettivo, priorità, modello, effort, contesto,
     account) e «Apri la conversazione».
3. **In alto**: titolo «Sessioni · N aperte», lente, menu; sotto la quota dei due account in una riga (`QuotaBars`).
4. **La master in basso**, agganciata sopra «Scrivi alla master»: badge, «MASTER · 12:40 · Opus 5.5 · 57%», il titolo
   dell'ultimo esito su una riga, tondo ▶ (voce) e tondo conversazione. Il campo scrive alla master come oggi.
5. **Menu ridisegnato** (tavola 5): pannello largo sotto la barra, sfondo oscurato; in testa lo stato del collegamento
   («Collegato a penguin · aggiornato ora»), voci grandi con icona su fondo tonale e una riga di spiegazione: Lancia una
   sessione, Registro, Quadro e quota, Cerca nelle conversazioni; in fondo, separata, Impostazioni. Il bottone mobile
   «Lancia» sparisce: Lancia sta nel menu, «Aggiungi alla notte» nella riga della notte.
6. **Righe di servizio** (notte, contesto pieno, resoconto della notte, invii programmati, prossimi passi dei progetti
   senza sessione): una fascia in fondo alla lista, come oggi in «Per te».
7. **Registro** si apre dal menu a tutto schermo, con Indietro che torna al riepilogo.

## Fuori da questo lavoro

- «Da quando sei uscito» scritto dalla master (consulenza del 02/10): dopo la fusione, richiede una regola nell'hook
  della master (richiesta a claude-master).
- Orologio: nessun cambio.
