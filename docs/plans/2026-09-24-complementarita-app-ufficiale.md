# Complementarità con l'app ufficiale di Claude (24/09/2026, sera)

Analisi chiesta da Franz per la sessione notturna (tramite il master, 22:23). Fonti verificate il 24/09 alle 22:30:
la documentazione di Remote Control su code.claude.com, la raccolta «Claude Mobile apps» del centro assistenza,
le note stampa del 25/02/2026, una ricerca web su Claude e Wear OS, il contratto in `contract/` e il codice in `core/`.

## Cosa fa oggi l'app ufficiale

**Telefono (Android e iOS, stessa app).** Chat con Claude, voce e dettatura, fotocamera, widget nella home,
integrazione con le app del telefono (messaggi, calendario, Health Connect). Scheda **Code**: elenco delle sessioni di
Claude Code (cloud e Remote Control) e, da agosto 2026, la scheda del PC collegato in cima, da cui si sceglie una cartella
e si avvia una sessione su quel PC.

**Remote Control (dal 25/02/2026, ora fuori dall'anteprima, su Pro, Max, Team ed Enterprise).** Il telefono è una
finestra sulla sessione locale: la conversazione, i subagent e i workflow restano sincronizzati fra terminale, browser e
telefono; si mandano messaggi, foto e file; si approvano i permessi degli strumenti; si fermano i subagent. Una sessione
remota per processo interattivo, o più sessioni con `claude remote-control` in modalità server. Tutto passa dall'API di
Anthropic in TLS, nessuna porta aperta sul PC. Notifiche push sul telefono: «Push when Claude decides» (fine di un
lavoro lungo, o su richiesta nel prompt) e «Push when actions required» (permessi e domande); nessuna configurazione per
evento. L'accesso è quello dell'account di claude.ai: **una sessione compare solo nelle app di quell'account**.

**Wear OS.** Nessuna app ufficiale, né su Wear OS né su Apple Watch: il centro assistenza non nomina orologi, e in rete
esistono solo progetti di terzi (monitor di consumo con widget per orologio, client per Galaxy Watch e Pebble, un
«Claude Watch» per Apple Watch). Verificato, non supposto.

## Dove la nostra app aggiunge valore

Tutto ciò che vive sul polso e non esiste nell'ufficiale:

- **Sguardo rapido.** Tile e complicazioni con lo stato di tutte le sessioni, senza aprire nulla.
- **Vibrazione con significato.** Domanda, fine turno, sessione sparita, quota: schemi distinti, come Telegram.
- **Risposta con un tocco.** Le scelte della domanda come bottoni, «consenti tutto», il testo dettato; `answer` e `prompt`
  del contratto.
- **Modello ed effort per sessione** (contratto 1.12), che l'ufficiale non espone da fuori.
- **Più sessioni e più account insieme.** Lo stato porta ogni sessione con `account` e `account_kind`, il colore
  dell'account, la quota per account con la finestra di 5 ore (contratto 1.3 e 1.8). L'app ufficiale mostra solo l'account
  con cui è entrata.
- **Regia del PC**: `launch` con il primo messaggio, `resume`, `follow`, la coda della notte (`night`), il diario
  (`recap`), lo schermo del terminale a richiesta (`screen`).
- **Lettura a voce** dell'ultima risposta (`last`, TTS di sistema).

E sul telefono, nei pezzi 3 e 4 del design: la stessa regia a schermo grande, «Condividi» verso una sessione, il
sostituto di Telegram (diario, notte, quota), la disposizione delle finestre del PC. Nessuna di queste è nell'ufficiale.

## Cosa non duplicare

- **La conversazione intera** con Claude: l'ufficiale la sincronizza in tempo reale, con foto e file. Sul polso non ha
  senso (8 KB di stato, cifratura, batteria); sul telefono sarebbe una copia peggiore. Il contratto porta il link
  `claude.ai/code/session_…` di ogni sessione: «Apri in Claude» è il ponte, già nello stato.
- **L'approvazione dei permessi con il contesto** (il diff, il comando intero): l'ufficiale lo fa; noi abbiamo
  `allow_all` e la risposta alla domanda, che bastano al polso.
- **Avviare una sessione in una cartella dal telefono** con il PC acceso: dall'agosto 2026 lo fa la scheda Code. Il nostro
  `launch` resta perché copre i due account insieme, parte dalla tile e non richiede Remote Control attivo; non va esteso.
- **Foto e file verso la sessione**: l'ufficiale lo fa nella chat. Il nostro «Condividi» (pezzo 3) passa da
  `claude-master report`, che archivia l'immagine nel progetto: è un'altra cosa, resta.

## Le tre funzioni da valutare

| | Utilità al polso | Costo | Sovrapposizione | Raccomandazione |
|---|---|---|---|---|
| (a) Chat della sessione: leggere gli ultimi messaggi, rispondere | Media: al polso serve l'ultima risposta e un modo per replicare, non lo storico | Alto per lo storico: stato oltre gli 8 KB o un nodo nuovo per sessione, cifratura di testi lunghi, più traffico FCM; nullo per quanto c'è già | Totale per lo storico (è il cuore dell'ufficiale) | **Ridotta:** resta com'è. `last` (contratto 1.4) dà l'ultima risposta intera, `prompt`/`answer` rispondono, `link` apre la chat vera nell'ufficiale. Non si aggiunge una lista di messaggi. |
| (b) Testo del terminale: coda del riquadro | Alta e già usata: è l'unico modo di vedere cosa scrive Claude senza aprire il PC; il TerminalScreen esiste (`screen`, `TerminalLive`, `TerminalText`) | Zero nuovo: a richiesta, fuori dallo stato, niente stream | Nessuna: l'ufficiale mostra la conversazione, non il terminale | **Sì, già fatto:** nessuna modifica al contratto. Sul telefono (pezzo 3) lo stesso `screen` a schermo grande; niente stream continuo (batteria, relay). |
| (c) Sessioni di più account insieme, con l'account visibile | Alta: Franz ha due account e l'ufficiale ne mostra uno | Zero nuovo: `account`, `account_kind`, `color`, `quota.<account>` sono già nel contratto (1.3, 1.8) e nella lista sessioni (pallino colorato, `AccountDot`) | Nessuna: l'ufficiale non può | **Sì, già presente:** niente da chiedere al relay. Sul telefono va portato lo stesso pallino e la quota per account nella regia (pezzo 3). |

Nessuna delle tre richiede una modifica al contratto: nessuna richiesta a claude-master da questa analisi.

## Conseguenze per il lavoro di stanotte

1. Le fondamenta restano come sono: accoppiamento dal telefono, Firebase a runtime, quattro schermate del telefono.
2. La cura va sulla grafica e sui dettagli delle schermate esistenti (orologio e telefono), verificate con gli
   screenshot test a 45 mm e a schermo piccolo, e sui minori della revisione finale.
3. Il pezzo 3 (telefono) si costruisce sulla regia e sul terminale a schermo grande, non sulla chat. Le illustrazioni
   contestuali proposte da Franz (vettoriali, in Compose, una volta all'ingresso) entrano nella specifica del pezzo 3.

Fonti: [Remote Control](https://code.claude.com/docs/en/remote-control) · [Claude Mobile apps, centro assistenza](https://support.claude.com/en/collections/9387080-claude-mobile-apps) · [Help Net Security, 25/02/2026](https://www.helpnetsecurity.com/2026/02/25/anthropic-remote-control-claude-code-feature/) · [Claude su Google Play](https://play.google.com/store/apps/details?id=com.anthropic.claude) · progetti di terzi per orologio: [claude-usage-companion](https://github.com/gahingwoo/claude-usage-companion), [ClawWatch](https://github.com/ThinkOffApp/ClawWatch), [Claude su Pebble](https://apps.repebble.com/claude_68fe94c3d004720009e0b41a).
