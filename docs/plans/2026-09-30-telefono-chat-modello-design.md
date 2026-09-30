# Telefono: invio, modello ed effort, chat con stato (design, 30/09/2026)

Approvato da Franz il 30/09 alle 20:26, con le parti 4 e 5. Richiesta di Franz dopo la prima prova dal vivo del restyling (30/09 20:17): il messaggio inviato spariva, il tasto
«Invia» finiva sotto la tastiera, modello ed effort si vedevano ma non si cambiavano, e un messaggio inviato deve
restare visibile come in una chat, con il suo stato. Scelta di Franz alle 20:25: nella chat anche l'esito del turno.

Il messaggio che spariva è un difetto del relay: la mappa dei nomi dava `claude-master-watch-telefono` invece di
`pix-claude-master-watch-telefono`, e `talk` lasciava il testo in casella rispondendo «ok delivered». La correzione è
chiesta a `pix-claude-master` il 30/09. Da questa parte il contratto non cambia.

## 1. Invio

- Il campo di scrittura diventa una barra fissa in fondo alla scheda sessione, sopra la tastiera (`imePadding`), con il
  tasto «Invia» come icona tonda a destra del campo.
- Un solo bottone pieno per schermata: con una domanda che ha opzioni «Invia» è tonale (la prima opzione è piena),
  altrimenti è pieno. Una sessione chiusa mostra «Riapri» al posto della barra.

## 2. Modello ed effort

- In testata «Opus 5.5» ed «Effort ●●○» diventano toccabili.
- Il tocco apre un foglio con le scelte di `state.choices` (contratto 1.12): modelli `{id, label}`, effort
  `low … max`. L'app oggi non legge `choices`: si aggiunge al modello `State`.
- La scelta manda `model` o `effort` (arg = id o livello) solo a quella sessione. Esito come ogni comando: un rifiuto
  del PC si dice con il suo motivo. Senza `choices` (relay vecchio) o in Demo le voci non sono toccabili.

## 3. Chat con stato

- Sopra la barra di scrittura, i messaggi inviati a quella sessione: prompt e risposte libere a una domanda. Fumetti a
  destra, con l'icona di stato; sotto ogni messaggio elaborato l'esito del suo turno come fumetto di Claude a sinistra,
  con il ▶ della lettura.
- Si salvano sul telefono in Room (tabella nuova, migrazione 2 → 3) per 7 giorni, per nome di sessione.
- Stati, ricavati in `core` dalla conferma del comando e dai turni della sessione:
  - **in invio** (orologio): nessuna risposta del PC ancora;
  - **non consegnato** (✗ rosso): comando fallito o rifiutato; tocco = Riprova;
  - **consegnato** (✓): il PC l'ha scritto nella sessione;
  - **in coda** (✓ grigio): consegnato mentre la sessione finisce un turno partito prima dell'invio;
  - **in lavorazione** (▶ animato): la sessione lavora con un turno iniziato dopo l'invio;
  - **elaborato** (✓✓): quel turno è finito. L'esito della sessione, se è di quel turno, si salva con il messaggio.
- Il passaggio fra gli stati si registra sul messaggio (inizio e fine del turno visti), perché lo stato del PC dice
  solo il turno di adesso.

## 4. Completamento nella ricerca di «Lancia» (Franz, 20:24)

- Un menu a tendina attaccato al campo del progetto, che si apre mentre si scrive (al posto della lista staccata).
- Ordine: prima i nomi che iniziano con il testo, poi quelli che lo contengono, poi quelli che lo hanno nella cartella;
  a pari merito il più recente. Campo vuoto: i 6 usati più di recente.
- Tutti e due gli account insieme, con il pallino dell'account su ogni riga; scegliere un progetto imposta l'account.
  La scelta dell'account in testa resta come filtro facoltativo.
- La parte del nome che corrisponde al testo in evidenza.

## 5. «Accoppia di nuovo» nelle Impostazioni (Franz, 20:26)

- Il bottone pieno in fondo sembrava «Salva». Accoppiati: diventa una voce con l'icona QR dentro la card del PC e
  dell'orologio, non piena; le Impostazioni non hanno un'azione principale.
- In Demo, non accoppiati: «Accoppia» resta pieno ma dentro una card «Collega il PC» in cima alla schermata.

## Test

- `core`, con TDD: l'ordine dei suggerimenti di «Lancia» (inizio, contenuto, cartella, recenti, due account);
  `choices` letto dalle fixture; la regola degli stati della chat, un test per stato e per i passaggi
  (in coda → in lavorazione → elaborato, esito agganciato solo se è del turno); scadenza dopo 7 giorni.
- Room: migrazione 2 → 3 con il test di migrazione.
- Paparazzi: scheda con la chat nei sei stati, barra di scrittura, foglio di modello ed effort, «Lancia» con il
  menu aperto, Impostazioni accoppiate e in Demo; carattere 1,3.
- Dal vivo, dopo la correzione del relay: un prompt dal telefono arriva nella sessione e passa da consegnato a
  elaborato con l'esito; cambio di modello e di effort su una sessione vera.

## Fuori

Conversazione completa del turno (servirebbe un campo nuovo nel contratto); chat sull'orologio.
