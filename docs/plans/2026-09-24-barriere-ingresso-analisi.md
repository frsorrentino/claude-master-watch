# Barriere d'ingresso: analisi (24/09/2026)

Domanda di Franz: quali sono davvero i requisiti per usare l'app e cosa si può fare per abbassarli (pubblicare l'app,
sostituire il passaggio Firebase, installazione rapida delle dipendenze). I requisiti sono nel README dell'app
(`55fde91`), ricavati dai due README e dal codice di `cm-relay.py` e `cm-relay-crypto.py`.

## Dove sta la barriera oggi, dal più pesante

1. **Compilarsi l'app.** Servono JDK 17, SDK Android 36, il proprio `google-services.json`, `adb` wireless e una sola
   chiave di firma: passare da debug a release obbliga a disinstallare e rifare l'accoppiamento. Per chi non sviluppa
   Android è un muro.
2. **Firebase a mano.** Otto passaggi: progetto, Realtime Database, regole, accesso anonimo, Cloud Messaging, service
   account, registrazione dell'app Android, configurazione del relay.
3. **Dipendenze del PC.** Il modulo Python `cryptography` e `crontab`: il README del plugin non li elenca e oggi escono
   solo come errore, al primo `relay pair` o `relay install`.
4. **Plugin.** Già facile: `claude plugin marketplace add` e `claude plugin install`, poi `init` e `doctor`.

ChromeOS non è un requisito: è solo la piattaforma provata ogni giorno. Su ChromeOS chrome-bridge serve solo per
disporre le finestre, non all'orologio.

## Opzioni

### A. Pubblicare l'app su Play

- È l'unica cosa che toglie il punto 1. Un APK su GitHub Releases non basta: su Wear OS si installa solo con `adb`.
- **Account sviluppatore:**
  - personale creato dopo il 13/11/2023: prima della produzione serve un test chiuso con almeno 12 tester per 14 giorni
    di fila, e per ogni app;
  - di organizzazione, o personale creato prima di quella data: esente.
- **Test interno:** il canale fino a 100 tester si può usare subito, senza accesso alla produzione. Va bene per i primi
  utenti.
- **Il nodo:** un'app pubblicata è una sola per tutti, quindi non può contenere il Firebase di ciascuno. Pubblicare
  presuppone la scelta fra B1 e B2.

### B1. Backend condiviso, gestito da Franz

- **Setup dell'utente:** plugin, `relay pair`, codice a 6 cifre. Niente Firebase, niente Gradle.
- **Privacy:** regge già. Tutto è cifrato da un capo all'altro con AES-256-GCM, e la chiave è concordata con X25519
  all'accoppiamento: il server vede solo dati cifrati, anche quelli degli altri utenti.
- **Cosa cambia:**
  - il PC non può avere il service account, quindi si autentica come l'orologio (anonimo o con un token personalizzato);
  - il push lo manda una Cloud Function quando arriva un evento, perché l'invio FCM chiede credenziali lato server;
  - le regole del database separano i dati per coppia PC-orologio.
- **Costo per Franz:** piano Blaze (a consumo), manutenzione, controllo degli abusi e una privacy policy (che Play chiede
  comunque). Diventa un servizio di cui si risponde.
- **Codice dell'app:** il design mette già il trasporto dietro l'interfaccia `Transport`, quindi lato orologio il cambio
  è contenuto.

### B2. Firebase di ognuno, ma guidato

- Un comando tipo `claude-master relay setup` crea e configura il progetto con la CLI di Firebase o con `gcloud`. Restano
  il login Google e l'installazione della CLI.
- Mantiene «nessun server nostro».
- **Con un'app pubblicata:** l'orologio deve ricevere la configurazione a runtime. Firebase lo permette: si inizializza
  senza `google-services.json`, passando `FirebaseOptions` con chiave API, id del progetto e id dell'app, e togliendo
  l'inizializzazione automatica. Funziona anche FCM, a patto che le tre opzioni obbligatorie ci siano. Da confermare con
  una prova sul polso.
- **Problema aperto:** il codice a 6 cifre non dice all'orologio quale progetto usare, e l'orologio non ha fotocamera.
  Due strade:
  - l'app del telefono (`mobile/`, oggi un segnaposto) legge un QR sul PC e passa la configurazione all'orologio con il
    Data Layer;
  - un piccolo punto d'incontro condiviso, usato solo per l'accoppiamento.

### C. Dipendenze del PC

- `doctor` controlla `cryptography` e `crontab` quando il relay è acceso; `relay pair` e `relay install` li controllano
  prima di fare qualsiasi cosa e indicano il comando per installarli; il README del plugin li elenca.
- Passo successivo, da chiedere a parte: agganciare il relay a `init` (per esempio `init --watch`).
- È lavoro sul plugin: lo fa claude-master, su richiesta di questa sessione.

### Esclusa

La sola rete di casa, senza cloud: fuori casa l'orologio perderebbe tutto.

## Raccomandazione

1. **Subito C.** Costa poco e toglie gli errori a sorpresa. Chiesta a claude-master il 24/09 alle 17:07, dopo l'ok di
   Franz: messaggio `ea9ad5` nella casella, poi rimandato direttamente alla sessione.
   Fatta il 24/09 alle 17:51: commit `29534a8` su claude-master, plugin 0.4.20, non ancora pubblicato. Righe `crypto_ok` e
   `crontab_ok` verificate con il doctor vero del PC.
2. **La decisione vera è B1 o B2, e viene prima di A.** Consigliata B1 con Play: è l'unica strada in cui l'utente non
   tocca né Firebase né Gradle. B2 resta come modalità avanzata per chi non vuole passare dal server di Franz. Il prezzo
   di B1 è gestire un servizio, come costo e come responsabilità.
3. **Poi A.** Test interno subito, poi test chiuso, poi produzione.

## Decisioni aperte (Franz)

- Account Play: personale (creato prima o dopo il 13/11/2023) o di organizzazione Pixelfarm?
- Backend condiviso con Blaze e privacy policy, o si resta su «ognuno il suo Firebase»?

## Fonti

- [Play Console Help: requisiti di test per i nuovi account personali](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [Firebase: opzioni di inizializzazione](https://firebase.google.com/support/guides/init-options)
- [firebase-android-sdk #66: inizializzare FirebaseApp senza google-services.json](https://github.com/firebase/firebase-android-sdk/issues/66)
- [Primetestlab: da 20 a 12 tester](https://primetestlab.com/blog/google-play-changed-20-to-12-testers)
