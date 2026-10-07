# Pagina «Notte», fase 1: piano di attuazione

> **Per chi esegue:** `superpowers:executing-plans`, un task alla volta, con il test prima del codice.

**Obiettivo:** la pagina Notte su telefono, tablet e web. Testata con la finestra della notte, «Serve a te», una card per
lavoro o sessione che si apre al tocco, progetti. Dati dall'op `night` del relay (contratto 1.44, richiesta 1 del
07/10 alle 21:58).

**Specifica:** `docs/proposte/2026-10-07-pagina-notte.md`, approvata da Franz il 07/10 alle 21:50 («ok pagina Notte,
il tocco espande»). Mockup: `docs/mockup/2026-10-07-pagina-notte/`.

**Architettura:** il rapporto (`team-supervisor/night-report` v1) si decodifica in `core` (`contract/NightReport.kt`). Una
regola pura (`rules/NightPage.kt`) lo trasforma nel modello della pagina: testata, voci di «Serve a te», card con
icona, orari, durata, posizione sull'asse, progetti. La web ha la stessa regola in `web/src/lib/night.ts`, con gli stessi
casi di test. Le schermate disegnano solo il modello.

## Vincoli

- **Repo pubblico:** la fixture `contract/night-report-sample.json` e la demo usano nomi e testi inventati (ledger-api,
  atlas-shop, field-notes), mai il rapporto vero, che contiene percorsi e dati personali.
- **Mai «…» nel corpo dei testi:** il relay taglia oggi il titolo dei lavori della coda con «…». La regola toglie il
  troncamento e tiene il testo fino all'ultima parola intera. La correzione vera è la richiesta 4 al relay: un titolo breve.
- **Icone di esito** con i significati di Telegram: ✓ ok, ✗ fermo o fallito, ▶ in corso, ❓ domanda aperta.
- **Un solo tasto pieno per schermata:** il primo di «Serve a te»; gli altri tonali.
- **Testi** in `strings.xml` (it, en) e `t.ts`.
- **Build Android su win**, golden in CI con `record=true`, guardati prima del commit.
- Con l'ok di Franz alla pagina sono approvati anche push e release (master, 21:50).

## Fuori da questa fase

- «Conversazione» per sessioni chiuse e lavori della coda, e «Rapporto»: richieste 2 e 3 al relay. In fase 1 il tasto
  «Conversazione» c'è solo per le sessioni vive e apre la chat che esiste già.
- La card del mattino in home: serve che lo stato dica la data dell'ultimo rapporto (chiesto come facoltativo nella
  richiesta 1). Arriva quando lo stato lo porta.
- La qualità dei dati (doppioni, esito «fermo», conteggi): richiesta 4.

## Task N1: fixture e modello del rapporto

- Create: `contract/night-report-sample.json`, sintetica, con tutte le forme viste nel rapporto del 07/10:
  - sessione viva in corso (`end` null, `outcome` running);
  - sessione chiusa ok con conteggi;
  - lavoro della coda fermo (`stopped`), uno riuscito e uno in corso con `report`;
  - `attention.unblock` con un comando;
  - due progetti: uno con parti fatte e da fare ed eventi, uno senza parti con `waiting_on`.
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/contract/NightReport.kt`. Classi `@Serializable` con valori di default:
  ogni campo nuovo del relay si ignora (`ContractJson` ha già `ignoreUnknownKeys`).
- Test: `core/src/test/.../contract/NightReportTest.kt`. La fixture si decodifica, e la timeline ha 6 voci nell'ordine
  del file.

## Task N2: la regola della pagina

- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/NightPage.kt` e `web/src/lib/night.ts`, con gli stessi casi
  in `NightPageTest.kt` e `night.test.ts`.
- Produce: `NightPage.of(report, zone, now, labels): Page`, dove `Page` contiene:
  - `title`, per esempio «Notte del 6-7 ottobre»;
  - `window`, per esempio «Dalle 00:33, ultimo tuo messaggio, alle 02:36 · 2 ore e 3 minuti»;
  - `attention`: lista di `Need(session, text, action)`;
  - `cards`: lista di `Card(id, kind, icon, title, queue, times, duration, line, from, to, live, counts, steps, project)`,
    con `from` e `to` frazioni da 0 a 1 sull'asse della finestra;
  - `axis`: le ore piene e mezze dentro la finestra;
  - `projects`.
- Casi:
  - icona per esito (`ok` ✓, `stopped`, `failed` ✗, `running` ▶);
  - in corso: «dalle 02:01 · in corso» e barra fino alla fine della finestra;
  - durata sotto l'ora in minuti, sopra in «1 h 4 min»;
  - titolo della coda senza «…» finale, tagliato all'ultima parola intera, e senza il nome della cartella prima di « — »,
    che va nella riga sotto;
  - una voce che comincia prima della finestra parte da 0;
  - i passi della card sono gli eventi del progetto dentro la finestra, con l'ora.

## Task N3: web

- Create: `web/src/lib/NightPage.svelte`; voce «Notte» nel menu ≡ (`AppBar.svelte`, `Page` con `night`).
- Dati: `tr.send(newCmd('night', null, null))`, poi `JSON.parse(r.text)`; in demo la fixture. Se il relay rifiuta l'op,
  una riga dice «Il PC non sa ancora leggere il rapporto della notte: aggiorna team-supervisor».
- Tocco sulla card = si apre sul posto e chiude l'aperta; dentro: esito intero, conteggi, passi con l'ora,
  «Conversazione» per le sessioni vive.
- Verifica: `vite build` di prova, poi un'immagine a 390×844 con chromium headless sulla demo, guardata prima di pubblicare.

## Task N4: Android, telefono e tablet

- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/NightScreen.kt`; voce «Notte» nel menu dell'app;
  `CmdOp.NIGHT` in `contract/Model.kt`.
- Test Paparazzi `NightScreenTest`: pagina con card chiuse, una card aperta, la riga «aggiorna team-supervisor».
- Golden registrati in CI e guardati prima del commit.

## Task N5: contratto 1.44 nel README

- Quando team-supervisor conferma la forma dell'op, si scrive il paragrafo 1.44 in `contract/README.md`, con la
  fixture. Se la forma cambia, cambiano la fixture e i test di N1-N2 prima di tutto il resto.
