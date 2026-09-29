# Nuovo pacchetto su Play: i passi di Franz (29/09/2026)

Deciso da Franz il 29/09 alle 15:54: l'app pubblicata si chiama `com.francescosorrentino.cmaster` e non più
`it.pixelbox.cmwatch`. Il cambio è minimo: cambia l'applicationId del telefono e dell'orologio, mentre namespace e
package Kotlin restano `it.pixelbox.cmwatch`, interni e invisibili su Play. versionCode 21 (telefono) e 22 (orologio),
versionName 0.2.

Su Play il nome del pacchetto è permanente: serve un'app nuova. La vecchia `it.pixelbox.cmwatch` resta nella Console
con il suo test interno (11 e 12) e il nome non si potrà più riusare. I testi da incollare sono quelli di sempre, in
`2026-09-26-play-test-interno-checklist.md` (B1, B2, C1, C2, D1) e in `docs/play/listing.json`.

L'ordine conta: prima l'app nella Console e il relay, poi la release, poi i dispositivi.

## 1. Nuova app nella Play Console (circa 45 minuti)

- [ ] **La vecchia app, per non confondersi.** Tutte le app → `it.pixelbox.cmwatch` → Test interno → Tester: togliere
      la spunta all'elenco «Interni». Se la Console offre di eliminare l'app, si può fare; altrimenti resta lì,
      inattiva. «Claude Master App» come titolo: se la Console lo rifiuta perché già usato nell'account, rinominare
      prima la vecchia in «Claude Master App (old)».
- [ ] **Crea app:**
      - nome «Claude Master App»;
      - lingua predefinita «English (United States) – en-US»;
      - App, Gratuita, e le due dichiarazioni.
      Il pacchetto non si sceglie qui: lo fissa il primo bundle caricato (passo 3).
- [ ] **Scheda dello Store → Scheda principale:**
      - testi B1 per en-US, poi la traduzione «Italiano – it-IT» con i testi B2;
      - icona `docs/play/icon-512.png`;
      - grafica in evidenza `docs/play/feature-graphic-1024x500.png`;
      - screenshot del telefono da `docs/play/screenshot/phone/`;
      - screenshot dell'orologio da `docs/play/screenshot/watch/`.
- [ ] **Impostazioni avanzate → Fattori di forma → Aggiungi → Wear OS**, poi «Opt in to Wear OS and agree to the review
      policy».
- [ ] **Norme → Contenuti dell'app**, come la sezione C della checklist del 26/09:
      - Norme sulla privacy: `https://github.com/frsorrentino/claude-master-watch/blob/master/PRIVACY.md`;
      - Accesso all'app: testo C1;
      - Annunci: no;
      - Classificazione dei contenuti: tutte le risposte «no»;
      - Pubblico di destinazione: 18 e oltre;
      - Sicurezza dei dati: risposte C2;
      - App governative, finanziarie, sanitarie, notizie, VPN, ID pubblicità: no.
- [ ] **Account di servizio.** Utenti e autorizzazioni → `play-publisher@francesco-sorren-1474290000543.iam.gserviceaccount.com` →
      Autorizzazioni app → aggiungere la nuova app con gli stessi permessi della vecchia:
      - «Visualizza informazioni sull'app»;
      - «Rilascia nei canali di test»;
      - «Gestisci canali di test e modifica elenchi di tester».
      La chiave resta la stessa. Me lo dici e verifico con `scripts/play.py status`, che ora punta al pacchetto nuovo.

## 2. Il relay: pacchetto nuovo in Firebase (5 minuti, nel terminale Linux)

- [ ] In `~/.config/claude-master/config.json`, sotto `relay`, cambiare `"app_package": "it.pixelbox.cmwatch"` in
      `"app_package": "com.francescosorrentino.cmaster"`.
- [ ] `claude-master relay setup --dry-run`: deve dire che registrerà l'app Android `com.francescosorrentino.cmaster`.
      Se invece dice che l'app c'è già con i dati di prima, fermarsi e dirmelo: è una richiesta per claude-master.
- [ ] `claude-master relay setup`: registra l'app nuova nel progetto Firebase, senza impronta SHA, e riscrive
      `relay.firebase_app` e `google-services.json` nella cartella del relay. Gli altri passi risultano già fatti e
      vengono saltati.
- [ ] `claude-master doctor`: nessun FAIL sul relay.

## 3. Prima release interna dalla Console (10 minuti)

- [ ] I due bundle, `mobile-release.aab` (versionCode 21) e `wear-release.aab` (versionCode 22), li scarico io dalla CI
      e te li metto in una cartella dei file Linux. La CI li tiene solo un giorno.
- [ ] **Test → Test interno → Tester → Crea elenco email.** Nome «Interni», il tuo indirizzo Gmail. Salva e spunta
      l'elenco. **Canale di feedback:** il tuo indirizzo email.
- [ ] **Crea nuova release.**
      - Firma delle app di Google Play: accettare la chiave generata da Google. Il nostro keystore diventa la chiave di
        caricamento, la stessa della vecchia app.
      - Caricare `mobile-release.aab`. Il bundle dell'orologio va sulla traccia di test interno Wear OS: la vecchia app
        l'ha messo lì (`wear:internal`). Se la Console lo propone nella stessa release, va bene uguale.
      - Nome della release «0.2 (21/22)», note D1.
- [ ] **Salva → Rivedi release → Avvia l'implementazione del test interno.** Se il bundle dell'orologio sta nella
      traccia Wear OS, avviare anche quella.
- [ ] **Copia link** nella scheda Tester, aprirlo sul telefono con il tuo account e accettare.

## 4. Dispositivi: via la vecchia app, accoppiamento nuovo (15 minuti)

La vecchia app e la nuova hanno pacchetti diversi e starebbero una accanto all'altra. La chiave dell'accoppiamento sta
nel Keystore della vecchia app e non passa alla nuova.

- [ ] **Orologio:** tenere premuta l'icona «Claude Master Watch» → Disinstalla. In alternativa: Impostazioni → App →
      Claude Master Watch → Disinstalla.
- [ ] **Telefono:** Impostazioni → App → Claude Master App → Disinstalla. Se ci sono due icone, togliere quella
      installata prima.
- [ ] **Telefono:** dal link del passo 3, installare Claude Master App dal Play Store. L'app dell'orologio arriva da
      sola dal Play Store dell'orologio, o dalla sezione «App sull'orologio» del Play Store del telefono.
- [ ] **PC:** `claude-master relay pair`, il QR nuovo contiene l'app Firebase nuova.
- [ ] **Telefono:** Claude Master App → inquadrare il QR, seguire i passi fino ad «Accoppiato». L'orologio riceve la
      chiave dal telefono.
- [ ] **Orologio:** la lista delle sessioni si riempie entro un minuto. La tile e la complication vanno rimesse, perché
      appartenevano all'app disinstallata.

Se qualcosa si ferma, basta la foto dello schermo e il punto della lista: riparto da lì.
