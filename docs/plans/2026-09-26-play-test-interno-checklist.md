# Test interno su Play: checklist per Franz (26/09/2026)

Questi passi richiedono l'account Google di Franz e la Play Console. Ogni passo dice cosa fare e cosa copiare; i testi
da incollare sono in fondo. Il quadro completo, fino alla produzione, è in `2026-09-26-play-console-passi-franz.md`.

Ok di Franz del 26/09 alle 17:55 (via master): test interno, lista tester con un Google Group. Nessun caricamento
nella Console senza Franz.

**Limite di Play:** la traccia di test interno accetta solo liste di indirizzi email, non i Google Group. I gruppi
valgono dal test chiuso in poi. Quindi:
- il gruppo si crea subito, e chi si iscrive entra nel test chiuso;
- nel test interno ci sono solo gli indirizzi scritti a mano (Franz, al massimo qualche persona fidata).

## Manuale o API (Franz, 26/09 19:34)

Dove si può, si passa dalla Google Play Developer API: niente captcha e gestione da remoto. Lo script è
`scripts/play.py`, con i test offline in `scripts/test_play.py`. Usa la chiave dell'account di servizio da un file 0600
fuori dal repository, di default `~/.config/claude-master-watch/play-service-account.json`, e rifiuta una chiave
leggibile da altri o dentro un checkout git.

**Resta per forza manuale** (Franz, nel browser):
- il Google Group (sezione A);
- la creazione dell'app e le dichiarazioni (sezioni B e C): l'API non crea app e non compila le Norme;
- l'account di servizio e i suoi permessi (sezione S qui sotto);
- **la prima release**. Su un'app ancora in bozza l'API accetta solo release in stato «draft», e la prima uscita va
  avviata dalla Console. Verificato il 26/09 sui problemi aperti di fastlane e dei client dell'API: l'errore è «Only
  releases with status draft may be created on draft app». Quindi: primo caricamento e rollout del test interno dalla
  Console (sezione D);
- la lista di indirizzi del test interno: l'API gestisce solo i Google Group.

**Passa all'API**, dopo la prima release:
- i caricamenti successivi: `scripts/play.py upload --track internal mobile-release.aab wear-release.aab --notes "…"`;
- le tracce di test: `scripts/play.py promote --from internal --to alpha`;
- i tester del test chiuso: `scripts/play.py testers --track alpha --group claude-master-testers@googlegroups.com`;
- il rollout: `scripts/play.py rollout --track production --fraction 0.2`, poi `--complete`;
- lo stato: `scripts/play.py status`.

`--dry-run` stampa le richieste senza mandarle. Nessuna chiamata all'API finché Franz non ha creato l'account di
servizio.

## S. Account di servizio per l'API (15 minuti, una volta)

- [ ] <https://console.cloud.google.com> → nuovo progetto «claude-master-play». Un progetto a parte da quello Firebase
      del relay, così la chiave di Play non tocca il bus.
- [ ] **API e servizi → Libreria →** «Google Play Android Developer API» → **Abilita**.
- [ ] **IAM e amministrazione → Account di servizio → Crea**:
      - nome `play-publisher`;
      - nessun ruolo Cloud.
      Poi **Chiavi → Aggiungi chiave → JSON**: si scarica un file.
- [ ] Nel terminale Linux, con il file nei Download condivisi con Linux:
      ```bash
      mkdir -p ~/.config/claude-master-watch
      mv /mnt/chromeos/MyFiles/Downloads/claude-master-play-*.json ~/.config/claude-master-watch/play-service-account.json
      chmod 600 ~/.config/claude-master-watch/play-service-account.json
      ```
- [ ] **Play Console → Utenti e autorizzazioni → Invita nuovi utenti.** Come email, l'indirizzo dell'account di
      servizio, `play-publisher@claude-master-play.iam.gserviceaccount.com`. **Autorizzazioni app → Claude Master App**:
      - «Visualizza informazioni sull'app»;
      - «Rilascia nei canali di test»;
      - «Gestisci canali di test e modifica elenchi di tester»;
      - «Rilascia in produzione», solo quando servirà.
      Poi **Invita**. La pagina «Accesso API» della Console non esiste più: basta l'invito.
- [ ] Dirmelo. Il primo comando sarà `scripts/play.py status`, di sola lettura. I permessi possono metterci qualche
      ora prima di valere.

## A. Google Group dei tester (5 minuti)

Fatto il 26/09 alle 20:37: il gruppo `claude-master-testers@googlegroups.com` esiste, creato dal master con l'account di Franz
(captcha risolto da Franz), e il link è già nel README di claude-master 0.4.27.

- [ ] Aprire <https://groups.google.com> → **Crea gruppo**.
- [ ] Nome «Claude Master testers». Indirizzo email del gruppo `claude-master-testers`: diventa
      `claude-master-testers@googlegroups.com`. Se il nome è preso, scriverne un altro e dirlo a me, perché cambia il
      link del README.
- [ ] Descrizione, da incollare: testo A1 in fondo.
- [ ] Impostazioni della privacy:
      - **Chi può cercare il gruppo:** chiunque sul web;
      - **Chi può partecipare:** chiunque può partecipare;
      - **Chi può visualizzare le conversazioni:** membri del gruppo;
      - **Chi può pubblicare:** solo i gestori;
      - **Chi può visualizzare i membri:** solo i gestori, così gli indirizzi dei tester restano privati.
- [ ] Crea. Il link per iscriversi è `https://groups.google.com/g/claude-master-testers`: tocca «Partecipa al gruppo».

## B. App nella Play Console (10 minuti)

- [ ] <https://play.google.com/console> → controllare che l'identità dell'account risulti verificata. Se non lo è, è
      il primo passo e richiede da 2 a 5 giorni lavorativi.
- [ ] **Crea app**:
      - nome «Claude Master App»;
      - lingua predefinita «English (United States) – en-US»;
      - App, Gratuita;
      - spuntare le due dichiarazioni.
- [ ] **Scheda dello Store → Scheda principale:** incollare i testi B1 (en-US). Aggiungere la traduzione «Italiano –
      it-IT» con i testi B2.
      - Icona e grafica: se la Console le chiede già per il test interno, usare i file che ti mando in `docs/play/`.
        Altrimenti arrivano prima del test chiuso.
- [ ] **Impostazioni avanzate → Fattori di forma → Aggiungi → Wear OS.** Poi «Opt in to Wear OS and agree to the
      review policy». Lasciare le tracce comuni, senza la traccia dedicata a Wear OS.

## C. Contenuti dell'app (Norme → Contenuti dell'app, 20 minuti)

Per il test interno non tutte le sezioni sono obbligatorie. Se il rollout si blocca e ne chiede una, i testi sono qui.

- [ ] **Norme sulla privacy:** URL `https://github.com/frsorrentino/claude-master-watch/blob/master/PRIVACY.md`.
      Funziona solo dopo il push di `PRIVACY.md`, che aspetta il tuo ok.
- [ ] **Accesso all'app:** «Tutte le funzionalità o alcune sono limitate» → aggiungere le istruzioni C1.
- [ ] **Annunci:** «No, la mia app non contiene annunci».
- [ ] **Classificazione dei contenuti:** email di contatto; categoria «Tutte le altre tipologie di app»; «No» a tutte
      le domande (violenza, sesso, linguaggio, sostanze, gioco d'azzardo, contenuti generati dagli utenti condivisi
      con altri, posizione condivisa, acquisti digitali).
- [ ] **Pubblico di destinazione:** solo «18 e oltre». L'app non attira i bambini.
- [ ] **Sicurezza dei dati:** le risposte sono in C2.
- [ ] **App governative, finanziarie, sanitarie, notizie, VPN, ID pubblicità:** no, oppure «non usa l'ID pubblicità».

## D. Test interno (10 minuti, quando ti mando i due bundle)

- [ ] **Test → Test interno → Tester → Crea elenco email.** Nome «Interni»; mettere il tuo indirizzo Gmail e al
      massimo quello di qualche persona fidata. Salva, poi spunta l'elenco.
- [ ] **Canale di feedback:** il tuo indirizzo email.
- [ ] **Crea nuova release.** Al primo caricamento Play propone la firma delle app di Google Play: accetta la chiave
      generata da Google. Il nostro keystore diventa la chiave di caricamento.
- [ ] Caricare i due file:
      - `mobile-release.aab`, versionCode 11;
      - `wear-release.aab`, versionCode 12.
      Stesso pacchetto `it.pixelbox.cmwatch`.
- [ ] Nome della release: «0.1 (11/12)». Note: testo D1.
- [ ] **Salva → Rivedi release → Avvia l'implementazione del test interno.**
- [ ] Copiare il **link di adesione** («Copia link» nella scheda Tester) e mandarmelo.

**Attenzione prima di installare da Play.** L'app di Play è firmata da Google, quindi ha una firma diversa da quella
installata oggi con `adb`. Per installarla da Play:
- disinstallare la versione di sviluppo, su telefono e orologio;
- rifare l'accoppiamento.

La release oggi sul tuo polso resta com'è finché non decidi tu.

## E. Quando il test chiuso parte (più avanti)

- [ ] Il gruppo sulla traccia del test chiuso lo metto io, con l'API: `scripts/play.py testers --track alpha --group
      claude-master-testers@googlegroups.com`. In alternativa, dalla Console: **Test chiuso → Tester → Google Group**.
- [ ] Il link di adesione del test chiuso va nella descrizione del gruppo, al posto di «(Play link coming soon)».

## Testi da incollare

**A1, descrizione del gruppo (inglese):**
> Testers of Claude Master App (Android phone) and Claude Master Watch (Wear OS), an unofficial companion for Claude
> Code. Join, then open the Play testing link below with the same Google account and install the app. No computer
> needed: Demo mode shows every screen. Please open the app a few times a week for 14 days. Your address stays
> private. Play testing link: (Play link coming soon)

**B1, scheda en-US:**
- Nome: `Claude Master App`
- Descrizione breve (80 caratteri max):
  `Steer your Claude Code sessions from your phone and your Wear OS watch.`
- Descrizione completa:
  > Unofficial companion for Claude Code, not affiliated with Anthropic.
  >
  > Claude Master App and Claude Master Watch show every Claude Code session running on your computer, across your
  > accounts, and let you steer them from your phone and your Wear OS watch.
  >
  > • See who is waiting on a question, who is working and who is idle, at a glance on the watch tile.
  > • Answer a question with one tap, send a prompt, launch a session in a project.
  > • Read the terminal of a session and its last answer, or have it read aloud.
  > • Quota of each account with the five-hour window.
  > • The evening diary and the night report on your phone.
  >
  > Requires the free claude-master plugin for Claude Code on your computer and a Firebase project of your own, created
  > by the plugin. Everything travels end-to-end encrypted: neither Google nor the author can read it. No account, no
  > ads, no analytics.
  >
  > Try it without a computer: Demo mode shows every screen with sample sessions.

**B2, scheda it-IT:**
- Descrizione breve:
  `Guida le sessioni di Claude Code dal telefono e dall'orologio Wear OS.`
- Descrizione completa:
  > Compagna non ufficiale di Claude Code, non affiliata ad Anthropic.
  >
  > Claude Master App e Claude Master Watch mostrano tutte le sessioni di Claude Code del tuo computer, di tutti i tuoi
  > account, e le guidano dal telefono e dall'orologio Wear OS.
  >
  > • Chi aspetta una risposta, chi lavora e chi è fermo, a colpo d'occhio nella tile dell'orologio.
  > • Rispondi a una domanda con un tocco, manda un prompt, lancia una sessione in un progetto.
  > • Leggi il terminale di una sessione e la sua ultima risposta, o fattela leggere ad alta voce.
  > • La quota di ogni account con la finestra di cinque ore.
  > • Il diario della sera e il resoconto della notte sul telefono.
  >
  > Richiede il plugin gratuito claude-master per Claude Code sul computer e un progetto Firebase tuo, creato dal plugin.
  > Tutto viaggia cifrato da un capo all'altro: né Google né l'autore possono leggerlo. Nessun account, nessuna
  > pubblicità, nessuna statistica.
  >
  > Provala senza computer: la modalità Demo mostra tutte le schermate con sessioni di esempio.

**C1, istruzioni per l'accesso (inglese):**
> The app steers Claude Code sessions on the user's own computer, paired through a QR code shown by the claude-master
> plugin. No login exists. To review without a computer: on the phone, open the app and tap «Try the demo»; on the
> watch, open the app and tap «Demo». Demo mode shows every screen with sample sessions and simulated answers.

Queste istruzioni valgono solo dopo il pezzo 3:
- la Demo del telefono arriva con il pezzo 3;
- sull'orologio la Demo oggi sta nelle Impostazioni, che si aprono solo da accoppiati. Il pezzo 3 aggiunge «Demo» alla
  schermata «Accoppia dal telefono».

Finché mancano, nel test interno si prova solo l'accoppiamento vero.

**C2, sicurezza dei dati:**
- La tua app raccoglie o condivide uno dei tipi di dati utente richiesti? **Sì.**
- Tutti i dati raccolti sono criptati in transito? **Sì.**
- Offri agli utenti un modo per richiedere l'eliminazione dei dati? **Sì.** I dati stanno nel progetto Firebase
  dell'utente, che lo elimina dalla console; disinstallare o rifare l'accoppiamento li cancella dal dispositivo.
- Tipi di dati: solo **Identificatori del dispositivo o altri ID**. Sono l'uid anonimo, l'id di installazione e il
  token FCM dell'SDK Firebase.
  - Raccolti: sì. Condivisi: no.
  - Trattamento effimero: no.
  - Obbligatori: sì.
  - Scopo: «Funzionalità dell'app».
- Tutto il resto (stato delle sessioni, domande, testi, immagini condivise) viaggia cifrato da un capo all'altro fra PC
  e dispositivi. Per la guida di Play i dati cifrati end-to-end non si dichiarano.

**D1, note della release (en-US):**
> First internal build: pairing with the QR code from `claude-master relay pair`, Firebase configured at runtime.
