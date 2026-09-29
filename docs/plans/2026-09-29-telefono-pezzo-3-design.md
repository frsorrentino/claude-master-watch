# App del telefono, pezzo 3: design (29/09/2026)

Approvato da Franz sezione per sezione il 29/09, dalle 17:39 alle 18:10. È il pezzo 3 di
`2026-09-24-app-telefono-fondamenta-design.md` («Telefono 1»), per intero, compreso ciò che richiede estensioni del
contratto. Aspetto e movimento restano quelli fissati in quella specifica (sezione «Aspetto e movimento»); qui non si
ripetono.

## Perché ora

Il telefono oggi fa solo l'accoppiamento. Senza PC, un revisore di Play o un tester vede soltanto «Accoppia», e la
Console chiede di attestare che i dettagli di accesso aprono tutta l'app. Franz ha deciso il 29/09 di non fare una demo a
parte ma l'app vera: la Demo è la stessa app con i dati del `FakeTransport` al posto di Firebase, come sull'orologio.

## Decisioni di Franz (29/09)

- **Ambito:** tutto il pezzo 3, anche le parti che chiedono nuove versioni del contratto (coda della notte, «Condividi»,
  diario, notte e quota verso l'app).
- **Notifiche:** l'orologio avvisa per primo. Con un orologio accoppiato e collegato, la notifica del telefono compare
  in silenzio; senza orologio, o con l'orologio scollegato, il telefono suona. Rispondere da una parte chiude la
  notifica sull'altra.
- **Navigazione:** due schede in basso, «Sessioni» e «Diario». Accoppiamento e impostazioni dall'icona ⚙ in alto.

## Architettura e flusso dei dati

Il telefono diventa un client completo, come l'orologio. `core` ha già `Repo`, `Transport`, `FirebaseTransport`,
`FakeTransport`, `SwitchableTransport` e le regole di testo (`SessionsText`, `QuestionRules`, `QuotaBar`,
`TerminalText`, `NotificationPlan` e le altre). Il modulo `mobile` aggiunge le schermate Compose e i servizi Android;
ciò che è logica pura va in `core`, dove l'orologio lo può riusare.

- **Dati dal vivo.** Ad app aperta, il telefono legge lo stato da Firebase con lo stesso `FirebaseTransport`
  dell'orologio e la chiave AES dell'accoppiamento, già nel `KeyVault` del telefono. Ad app chiusa non ascolta nulla:
  arriva solo FCM.
- **Notifiche.** Il telefono si iscrive allo stesso topic FCM dell'orologio (`relay.fcm_topic`, dalla configurazione
  dell'accoppiamento). Le regole su cosa notificare sono quelle di `NotificationPlan`. In più una regola nuova in `core`:
  silenzio se il nodo dell'orologio accoppiato risulta collegato (capacità `cmwatch_wear` raggiungibile con
  `play-services-wearable`), suono altrimenti. La chiusura incrociata passa dallo stato: quando la domanda sparisce dallo
  stato, la notifica si chiude su tutti e due.
- **Demo.** «Prova la demo» sulla schermata «Non accoppiato» e l'interruttore «Modalità demo» nelle impostazioni. Il
  telefono passa al `FakeTransport` con le sessioni di esempio dell'orologio: tutte le schermate sono raggiungibili
  senza PC e senza rete. La Demo del telefono non accende quella dell'orologio.
- **Estensioni del contratto.** Quattro richieste a claude-master, una per volta, nell'ordine in cui servono. Ognuna
  alza la versione del contratto; questa sessione aggiorna poi fixture e test.
  1. **R3, coda della notte:** operazioni `night_add` (cartella, prompt) e `night_remove` (id), e la coda di stanotte
     nello stato. Il relay usa `claude-master night add|remove|list`.
  2. **R4, diario, notte e quota verso l'app:** il relay manda come eventi all'app, oltre che a Telegram, il diario delle
     20:00, il resoconto della notte e gli avvisi della guardia (soglia e ripresa all'azzeramento).
  3. **R5, Condividi:** testo o immagine verso una sessione, eseguito con `claude-master report`. L'immagine viaggia
     cifrata come il resto, ridotta dal telefono a 1600 px sul lato lungo in JPEG, in un nodo che il relay cancella dopo
     averla letta.
  4. **R6, riserva di Telegram:** oggi Telegram fa da riserva quando l'orologio non riceve
     (`relay.telegram_fallback_after_s`). Il relay deve contare anche il telefono fra i dispositivi che ricevono.
- **Ordine di lavoro.** Prima tutto ciò che il contratto copre già: regia, scheda sessione, terminale, lancio, Demo,
  notifiche, illustrazioni, impostazioni. Intanto le richieste R3–R6, una alla volta. La scheda «Diario» e «Condividi»
  si accendono quando arrivano le versioni che le supportano.

## Schermate

Regole comuni: un solo bottone pieno per schermata; una riga logica su una riga fisica, mai «…» nel corpo; icone di
stato ❓ ▶ ✓ ✗ con i significati dell'orologio; il tasto ▶ per la lettura a voce accanto ai testi sopra
`tts.min_chars` (120) e a esiti, risposte e domande.

1. **Sessioni (scheda 1, la regia).**
   - In testa la quota di ogni account: pallino del colore dell'account, barra, percentuale, «si azzera alle 18:40».
   - Le card in quest'ordine: chi aspetta te (ambra, ❓), chi lavora (cobalto, ▶, respira piano), chi è ferma (grigio,
     ✓), poi «Chiuse (n)» raccolte in una riga che si apre.
   - Ogni card: pallino dell'account, nome intero, stato con la durata; sotto, cosa sta facendo («modifica Repo.kt») se
     lavora, l'esito breve se è ferma.
   - Le card scivolano al loro posto quando l'ordine cambia.
   - Con dati non freschi, la fascia «dati di 6 min fa».
   - Bottone pieno: «Lancia».
2. **Scheda sessione.** Si apre dalla card con il «volo» (la card si allarga fino a diventare la scheda); il gesto
   indietro la restringe verso la card.
   - In testa: nome, progetto, account, stato.
   - Con una domanda: il testo intero, le opzioni come bottoni tonali, «Consenti tutto» per i permessi, un campo per la
     risposta libera.
   - Ultima risposta ed esito, con ▶.
   - Azioni: «Scrivi un prompt», «Terminale», «Segui» o «Non seguire», «Apri in Claude» (il link della sessione),
     «Riapri» per una sessione chiusa.
   - Bottone pieno secondo il contesto: «Invia» con il campo di testo pieno, «Riapri» per una sessione chiusa, altrimenti
     nessuno.
3. **Terminale.** Il testo del riquadro a schermo intero, monospazio, righe che vanno a capo. «Aggiorna» chiede una
   lettura nuova con `screen`; nessun flusso continuo. I blocchi nuovi entrano in dissolvenza, i vecchi restano fermi.
4. **Lancia** (foglio dal basso). Progetto con i suggerimenti dei progetti già visti nello stato, account, primo
   messaggio su più righe. Bottone pieno «Lancia».
5. **Diario (scheda 2).**
   - Diario: l'ultimo delle 20:00, una frase per progetto con la sua icona, prima chi aspetta; i giorni precedenti si
     aprono sotto.
   - Notte: il resoconto dell'ultima notte; la coda di stanotte, ogni lavoro con cartella e prompt e «Togli»; bottone
     pieno «Aggiungi alla notte», con un foglio per cartella e prompt.
   - Quota: gli avvisi della guardia.
6. **Condividi.** Dal «Condividi» di Android, testo o immagine. Un foglio con le sessioni vive, un campo per il
   messaggio, bottone pieno «Manda».
7. **Impostazioni** (⚙). L'accoppiamento di oggi (PC, dispositivi, «Rifai l'accoppiamento»), «Modalità demo», il
   collegamento alle notifiche di sistema, la privacy, la versione.
8. **Notifiche.** Domanda: fino a tre opzioni come azioni, più «Rispondi» con testo scritto nella notifica. Fine turno:
   l'esito breve. Diario, notte e quota: silenziose.

**Illustrazioni.** Solo dove metà schermo resta vuoto: le tre dell'accoppiamento già decise il 25/09 (non accoppiato,
accoppiamento, accoppiato), più «Nessuna sessione» e «Diario vuoto». Vettori in Compose con i `CmColors`, animati una
volta all'ingresso; con «riduci animazioni» lo stato finale subito.

**Testi.** In `strings.xml`, italiano e inglese insieme: l'app va a tester stranieri, `values-en` non aspetta.

## Errori e casi limite

- **Firebase non raggiungibile o dati vecchi:** la fascia con l'età dei dati, con le soglie dell'orologio (`Wake`,
  `ViewState`). I comandi restano possibili: vanno in coda e partono quando la rete torna.
- **Comando rifiutato o senza risposta** entro il tempo del contratto: barra in basso con il motivo in chiaro e
  «Riprova». Mai un errore tecnico grezzo.
- **Relay più vecchio della funzione:** `v` dello stato vale sempre 1 e non dice la versione del contratto. Ogni
  richiesta R3–R5 aggiunge quindi allo stato un campo che segnala la funzione (per esempio la coda della notte); se il
  campo manca, «Diario» e «Condividi» mostrano «Aggiorna claude-master sul PC» invece di fallire.
- **Chiave o accoppiamento non validi:** si torna a «Non accoppiato» spiegando perché, come oggi.
- **Immagine che non si riduce** (formato non letto): lo dice e, dopo conferma, manda solo il testo.
- **Risposta da notifica senza rete:** resta in coda; la notifica mostra «in attesa di rete».
- **Demo:** nessuna chiamata di rete; comandi e risposte simulati dal `FakeTransport`. Una fascia fissa «Demo» in alto.

## Test e verifiche

- **Unit, con TDD, in `core` e `mobile`:** ordine e raggruppamento delle card; il bottone pieno per ogni stato; la
  regola silenzio o suono; la riduzione delle immagini; la versione del contratto che accende Diario e Condividi;
  l'instradamento dei comandi dalle notifiche.
- **Contratto:** le fixture di R3–R6 in `contract/*.json`, lette identiche dai test Kotlin.
- **Paparazzi:** ogni schermata in Demo e con le fixture, telefono standard e carattere 1,3, illustrazioni nello stato
  finale. Registrati in CI.
- **Percorso Demo:** un test attraversa tutte le schermate in Demo senza rete. È la prova che l'attestazione a Play
  («accesso completo») è vera.
- **Checklist dal vivo** in `docs/verifiche/pezzo-3-telefono.md`:
  1. regia con i due account veri e la quota;
  2. risposta a una domanda dal telefono, e la notifica che sparisce sull'orologio;
  3. telefono in silenzio con l'orologio collegato, che suona con l'orologio spento;
  4. lancio di una sessione e lettura del terminale;
  5. diario delle 20:00 e resoconto della notte arrivati all'app; «Aggiungi alla notte» visto con `night list` sul PC;
  6. «Condividi» di uno screenshot, finito in `docs/segnalazioni/` del progetto;
  7. Demo da installazione pulita, senza PC.

## Fatto

- Test verdi e schermate Paparazzi registrate.
- R3–R6 consegnate da claude-master, fixture e test aggiornati.
- Checklist dal vivo passata.
- In Play Console: «Dettagli di accesso» con l'attestazione, «Pubblico di destinazione» (18+) e l'invio di «Sicurezza
  dei dati» (bozza già salvata il 29/09), a quel punto veri.

## Fuori da questo pezzo

Impostazioni dell'orologio sincronizzate, panoramica con statistiche e widget, modello ed effort per sessione,
disposizione delle finestre del PC: pezzo 4.
