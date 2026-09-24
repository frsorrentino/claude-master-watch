# App del telefono e accoppiamento senza build: design (24/09/2026)

Approvato da Franz sezione per sezione il 24/09, dalle 18:30 alle 19:06. Nasce dall'analisi delle barriere d'ingresso
(`2026-09-24-barriere-ingresso-analisi.md`). Questa specifica fissa le decisioni, divide il lavoro in quattro pezzi,
definisce aspetto e movimento dell'app del telefono e descrive nel dettaglio il primo pezzo, le fondamenta. I pezzi 2, 3
e 4 avranno ciascuno la sua specifica.

## Decisioni

- **Firebase di ognuno, guidato (B2).** Nessun server di Franz. Il progetto Firebase lo crea `claude-master relay setup`.
- **Account Play personale, creato dopo il 13/11/2023.** Prima della produzione serve un test chiuso con almeno 12 tester
  per 14 giorni di fila.
- **Accoppiamento con un QR letto dall'app del telefono.** Escluse la rete locale (sotto ChromeOS il relay vive in un
  contenitore Linux che la rete di casa non raggiunge, e un orologio collegato al telefono via Bluetooth non è sul Wi-Fi)
  e la stringa digitata sull'orologio (circa 100 caratteri).
- **Il telefono accoppia, l'orologio eredita la chiave (approccio A).** Una sola chiave AES per telefono e orologio, due
  uid in `/allowed`. Esclusi una chiave per dispositivo (il relay dovrebbe cifrare lo stato più volte) e l'orologio che
  passa dal telefono (perderebbe il funzionamento in Wi-Fi e LTE senza telefono).
- **L'app del telefono è un secondo prodotto,** non un accessorio dell'accoppiamento: complementare all'orologio e
  all'app Claude, con funzioni che l'app Claude non ha.

## I quattro pezzi

1. **Fondamenta** (dettaglio più sotto): Firebase configurato a runtime sull'orologio, app minima del telefono che
   accoppia con il QR, relay con QR, due uid e `relay setup`.
2. **Pubblicazione su Play:**
   - una scheda sola per telefono e orologio, con lo stesso pacchetto `it.pixelbox.cmwatch`;
   - firma di Play, privacy policy, sezione Sicurezza dei dati;
   - test interno subito, test chiuso appena le fondamenta reggono. È il collo di bottiglia dei tempi. I tester senza PC
     usano la modalità Demo.
   - La chiave API creata da Firebase per l'app Android non va limitata a un'impronta SHA: la firma di Play è diversa
     da quella di sviluppo.
3. **Telefono 1**, con quello che il relay sa già fare:
   - regia di tutte le sessioni dei due account, con «Apri in Claude» (il link `claude.ai/code/session_…` è già nello
     stato);
   - tastiera per i comandi: lanciare con il primo messaggio, mandare un prompt, `night add`;
   - terminale a schermo grande;
   - «Condividi» di Android verso una sessione, testo o immagine (`claude-master report`);
   - sostituto di Telegram: diario delle 20:00, resoconto della notte, avvisi di quota, domande di riserva. Chiede al
     relay di mandarli anche all'app.
4. **Telefono 2**, con parti nuove:
   - impostazioni dell'orologio, sincronizzate fra telefono e orologio;
   - pagina panoramica con statistiche, più un widget sulla schermata home;
   - modello ed effort per sessione;
   - disposizione delle finestre del PC (`tile`, `merge`, `move`, `layout`). Una miniatura dei monitor, in proporzione e
     come li vede il PC, con le finestre delle sessioni dentro. Si tocca una disposizione («master a sinistra»,
     «griglia», «schede unite», oppure una salvata come «mattina»); le finestre in miniatura scivolano al loro posto come
     anteprima; si conferma e il PC esegue. Serve che il relay pubblichi monitor e finestre e accetti i comandi delle
     finestre: contratto nuovo.

Ordine: 1, poi 2 e 3 in parallelo, poi 4. La checklist della fase 5 resta aperta: le build di queste fondamenta non vanno
sul polso di Franz finché non lo decide lui.

## Aspetto e movimento (tutta l'app del telefono)

**Aspetto: lo stesso dell'orologio.**

- Tema solo scuro: fondo nero e tre gradini di superficie (fondo, riga, card). Niente colori dinamici di Material You.
- Il colore segnala solo ciò che chiede attenzione, con i significati dell'orologio: ambra chi aspetta te, cobalto chi
  lavora, grigio chi è fermo, rosso spento chi è chiuso.
- Il corallo è l'identità. Un solo bottone pieno per schermata, azzurro pastello con testo blu notte.
- I colori di `CmColors` passano in un modulo comune, `:ui-tokens`, usato da orologio e telefono. È uno spostamento
  puro: gli screenshot dell'orologio non devono cambiare.
- Carattere di sistema per il testo, monospazio per comandi e terminale. Icone di stato ❓ ▶ ✓ ✗ come sull'orologio.
- Una riga logica sta su una riga fisica; mai «…» nel corpo dei testi. Testi in `strings.xml`, italiano e inglese.

**Movimento: le regole del sito, con le animazioni che l'orologio ha già.**

- Il movimento dice solo che qualcosa è cambiato: una volta all'ingresso, a un cambio di stato, o legato allo
  scorrimento. Niente animazioni infinite. Unica eccezione, lo stato «lavora», che respira piano come il bagliore
  dell'orologio.
- Effetto caratteristico, il passaggio fra schermate: dalla lista alla scheda la card si allarga fino a diventare la
  scheda, come il «volo» del sito, senza dissolvenza finale. Il gesto indietro di Android mostra dove si torna mentre lo
  si trascina, e la scheda si restringe verso la sua card.
- Numeri che rotolano quando cambiano. Archi e barre che si riempiono entrando nell'inquadratura. Card che scivolano al
  loro posto quando cambia l'ordine. Nel terminale i blocchi nuovi entrano in dissolvenza e i vecchi restano fermi.
- Nell'accoppiamento i tre passi (telefono, orologio, PC) si accendono uno dopo l'altro. Poi «Fatto», con la stessa
  vibrazione dell'orologio.
- Tempi brevi, fra 200 e 350 ms. Molle morbide senza rimbalzo, vicine all'easing del sito `cubic-bezier(0.3, 0, 0.2, 1)`.
- Con «riduci animazioni», o con le animazioni di sistema spente: tutto fermo e completo, lo stato finale subito.

## Fondamenta

### Fatto

Su un progetto Firebase nuovo, creato da `relay setup`:
- telefono e orologio si accoppiano con il QR, senza Gradle e senza la console Firebase;
- un push del relay sveglia l'orologio;
- un comando dall'orologio va a buon fine.

La prova sta nella checklist dal vivo `docs/verifiche/fondamenta-accoppiamento.md`.

### Flusso

Cosa vede l'utente:
- sul PC lancia `claude-master relay pair` e compare un QR;
- sul telefono tocca «Accoppia» e inquadra il QR;
- il telefono mostra i tre passi che si accendono, poi «Fatto»;
- l'orologio mostra «Accoppiato con \<PC\>».

I passi:

1. Il telefono legge il QR, inizializza Firebase con quella configurazione e fa l'accesso anonimo: ottiene il suo uid.
2. Il telefono cerca l'orologio collegato con la capacità `cmwatch_wear`. Se l'app sull'orologio manca, la apre su Play
   direttamente sull'orologio. Finché non siamo su Play, si installa con `adb`.
3. Il telefono manda la configurazione all'orologio (`/cmwatch/pair/hello`). L'orologio inizializza Firebase, fa
   l'accesso anonimo e risponde con il suo uid, il suo nome e una chiave pubblica X25519 temporanea.
4. Il telefono fa lo scambio X25519 con la chiave pubblica del PC presa dal QR e deriva K, con la stessa derivazione di
   oggi. Poi scrive la risposta in `/pair/<id>/watch`: la chiave pubblica del telefono, il suo uid, il nome, il controllo
   HMAC, più l'elenco `uids` e la mappa `names` di telefono e orologio.
5. Il PC verifica il controllo, salva K, scrive i due uid in `/allowed`, cancella `/events` e `/result` e conferma in
   `/pair/<id>/ok`. Tutto come oggi, tranne l'elenco degli uid.
6. Il telefono verifica la conferma e salva K nel suo Keystore. Poi manda K all'orologio (`/cmwatch/pair/key`),
   cifrata per lui. L'orologio la salva nel suo Keystore, si iscrive al topic FCM e conferma.

Casi particolari:

- **Nessun orologio collegato:** si accoppia solo il telefono, con `uids` = [telefono]. Per aggiungere l'orologio si
  rifà l'accoppiamento, perché solo il PC scrive `/allowed`.
- **Orologio che si scollega dopo il passo 5:** il telefono tiene K e un segno «orologio da completare». Al primo
  ricollegamento rifà i passi 3 e 6. Nel passo 3 controlla che l'uid restituito sia quello registrato; se l'orologio ha
  perso i dati e l'uid è diverso, chiede di rifare l'accoppiamento.
- **Build con la configurazione Firebase dentro** (la release attuale di Franz, chi compila da sé): il codice a 6 cifre
  resta come alternativa. `relay pair` mostra sia il QR sia il codice.

### Contenuto del QR

Il QR contiene questo JSON su una riga, con chiavi brevi per tenere piccolo il QR:

```json
{"v":1,"i":"<id>","c":"<pc_pub>","h":"<nome del PC>","e":1790000000,
 "f":{"k":"<chiave API>","p":"<id del progetto>","a":"<id dell'app Android>","d":"<URL del database>","t":"<topic FCM>"}}
```

- `i` è l'id dell'accoppiamento: 16 byte casuali in base64url senza padding (22 caratteri). Prende il posto delle 6
  cifre nel percorso `/pair/<id>`.
- `c` è la chiave pubblica X25519 grezza del PC, in base64, come oggi `pc_pub`.
- `e` è la scadenza in secondi epoch, come oggi `exp`: il telefono può dire «scaduto» senza rete.
- `f` è la configurazione Firebase. Non contiene segreti: la chiave API sta comunque in ogni APK.

`relay pair --text` stampa lo stesso JSON, e il telefono lo accetta anche con «Incolla il codice». Fixture:
`contract/pair-qr.json`.

### Sicurezza

- Il QR, visto sullo schermo del PC, è la fonte di fiducia. La chiave pubblica del PC arriva da lì e non da Firebase,
  quindi nessuno in mezzo può sostituirla.
- L'id di 128 bit non si indovina, a differenza delle 6 cifre.
- K passa dal telefono all'orologio cifrata con una chiave concordata solo fra loro due:
  `kw = HKDF-SHA256(X25519(eph_telefono, eph_orologio), salt vuoto, info "cmwatch-handoff-v1", 32 byte)`;
  `box = base64(nonce di 12 byte ‖ AES-256-GCM(kw, K, aad = uid dell'orologio))`.
  Il canale di Wear OS può passare dal cloud Google quando i due dispositivi non sono vicini: anche lì viaggiano solo dati
  cifrati, come su Firebase. Il canale fra telefono e orologio non è autenticato oltre l'abbinamento di Wear OS. È
  accettato: accetta messaggi solo da un'app con lo stesso pacchetto e la stessa firma.
- Le chiavi temporanee vivono solo in memoria e scadono dopo 5 minuti.

### Orologio: Firebase a runtime

- Nel manifest si toglie l'avvio automatico di Firebase (`FirebaseInitProvider` con `tools:node="remove"`) e FCM non si
  avvia da solo finché Firebase non è pronto.
- All'avvio dell'app, `FirebaseBoot` sceglie la configurazione in quest'ordine:
  1. quella salvata nelle preferenze, arrivata dal telefono;
  2. i valori generati dal plugin `google-services`, se la build li contiene, letti per nome dalle risorse
     (`google_app_id`, `google_api_key`, `project_id`, `firebase_database_url`);
  3. nessuna: Firebase resta spento e l'app mostra l'accoppiamento o la Demo.

  Un solo percorso nel codice per tutte le build. Una release costruita con il suo `google-services.json` e installata
  sopra quella di adesso, con la stessa firma, resta accoppiata.
- `FirebaseConfig` (in `core`, Kotlin puro) contiene chiave API, id del progetto, id dell'app, URL del database e topic.
  Il topic, oggi la costante `FCM_TOPIC = "watch"`, arriva dalla configurazione; `watch` resta il valore per le build
  con la configurazione dentro.
- `TransportChoice` usa Firebase quando c'è una configurazione e Firebase è avviato, al posto di `BuildConfig.FIREBASE`.
- Un servizio in ascolto sul canale di Wear OS risponde a `/cmwatch/pair/hello` e `/cmwatch/pair/key`, che vengono dal
  telefono.
- **Cambio di progetto:** se `hello` porta una configurazione diversa da quella attiva, l'orologio la salva, risponde
  `restart` e chiude il proprio processo. Il messaggio successivo del telefono lo riavvia con la configurazione nuova.
- **Schermata «Accoppia»:**
  - senza configurazione: «Accoppia dal telefono» e un bottone che apre l'app sul telefono, o la sua pagina su Play se
    non c'è;
  - con la configurazione dentro la build: anche «Usa il codice», il flusso di oggi.

  «Rifai l'accoppiamento» nelle Impostazioni porta alla stessa schermata.

### Telefono: app minima

- **Pacchetto:** il modulo `mobile` passa all'applicationId `it.pixelbox.cmwatch`, lo stesso dell'orologio. Senza questo
  il canale di Wear OS fra le due app non funziona. Le due app, anche in sviluppo, si firmano con la stessa chiave.
- **Codice:** usa `core` per accoppiamento, chiave, contratto e accesso a Firebase. Compose Material 3 per telefono.
  Android 13 o più recente, come l'orologio, perché lo scambio X25519 nativo c'è da lì.
- **Librerie:**
  - scanner di codici dei servizi Google Play: niente permesso fotocamera;
  - `play-services-wearable`, per il canale e le capacità;
  - `wear-remote-interactions`, per aprire Play sull'orologio;
  - Firebase Auth. FCM sul telefono arriva con il pezzo 3.
- **Quattro schermate:**
  1. **Non accoppiato:** due righe di spiegazione, bottone pieno «Accoppia», link «Incolla il codice».
  2. **Lettura:** lo scanner di Play.
  3. **Accoppiamento:** tre passi (telefono, orologio, PC) che si accendono uno dopo l'altro. L'errore compare sotto il
     passo che fallisce, con «Riprova».
  4. **Accoppiato:** nome del PC; i due dispositivi con il loro stato («chiave consegnata» o «in attesa
     dell'orologio»); «Rifai l'accoppiamento». È la base su cui il pezzo 3 costruisce la regia.
- **Salvato sul telefono:** K (con `KeyVault`, sotto la sua chiave del Keystore), la configurazione, il nome del PC, gli
  uid e il nodo dell'orologio, il segno «orologio da completare».

### Messaggi fra telefono e orologio

`MessageClient.sendRequest`: richiesta e risposta, con un timeout di 20 s ciascuna. I payload sono JSON con `v: 1`.

| Percorso | Dal telefono | Risposta dell'orologio |
|---|---|---|
| `/cmwatch/pair/hello` | `{"v":1,"f":{k,p,a,d,t}}` | `{"v":1,"uid","name","eph"}`, oppure `{"v":1,"restart":true}`, oppure `{"v":1,"error":"<codice>"}` |
| `/cmwatch/pair/key` | `{"v":1,"host","eph","box"}` | `{"v":1,"ok":true}`, oppure `{"v":1,"error":"<codice>"}` |

Codici d'errore dell'orologio:
- `auth`: accesso anonimo fallito;
- `no_session`: la chiave temporanea è scaduta o l'orologio si è riavviato, e il telefono riparte da `hello`;
- `decrypt`: K non si apre;
- `store`: il Keystore non salva.

Le capacità dichiarate sono `cmwatch_wear` sull'orologio e `cmwatch_phone` sul telefono.

### Richieste al relay

Le fa claude-master, una alla volta, in quest'ordine. Il contratto passa alla 1.15; `v` resta 1.

**R1, `relay pair` con il QR:**
- Il QR si disegna nel terminale con mezzi blocchi e margine, usando un generatore in Python incluso nel plugin: nessuna
  dipendenza nuova. Sotto il QR compare il codice a 6 cifre. `--text` stampa solo il JSON.
- Stesso documento (`pc_pub`, `host`, `exp`) in `/pair/<code>` e in `/pair/<id>`. Il relay li interroga tutti e due e
  vince la prima risposta valida; l'altro nodo si cancella. Scadenza e tentativi sono in comune.
- In `/pair/<…>/watch` si accettano, opzionali, `uids` (fino a 4) e `names` (uid → nome). `/allowed` riceve tutti gli uid,
  `devices.json` li registra con il nome. Senza `uids` si comporta come oggi. Il nodo si chiama ancora `watch`, per
  compatibilità.
- I dati dell'app Firebase stanno nella configurazione del relay, in `relay.firebase_app` (`api_key`, `project_id`,
  `app_id`), oppure si leggono da un `google-services.json` indicato in `relay.google_services`: `project_info`, e il
  client con il pacchetto `it.pixelbox.cmwatch`. Senza questi dati il QR non compare, `relay pair` lo dice e il codice a
  6 cifre funziona; `doctor` mostra un WARN.
- Fixture nei due repository: `pair-qr.json` e `pair-response.json`.

**R2, `relay setup`:**
- Guidato e idempotente, con `--dry-run`. Crea il progetto o ne sceglie uno esistente, poi:
  - crea l'istanza del database e ci mette le regole del README;
  - attiva l'accesso anonimo;
  - registra l'app Android `it.pixelbox.cmwatch` senza impronta SHA, e ne salva i dati in `relay.firebase_app`;
  - scarica la chiave del service account in `~/.claude-master/relay/service-account.json` con permessi 0600;
  - scrive `relay.firebase_url` e `relay.fcm_topic`.
- Dove un passo non si può automatizzare stampa il link esatto della console e aspetta Invio. Quali passi si possono
  automatizzare lo verifica claude-master.
- Alla fine, un riepilogo come quello di `doctor`.

### Errori e testi

Ogni errore ha il suo testo, in italiano e in inglese, e dice cosa fare:

| Errore | Cosa dice |
|---|---|
| QR scaduto | «rilancia relay pair» |
| QR già usato o non valido | «codice non valido: rilancia relay pair» |
| Orologio non trovato | «è collegato al telefono?» |
| App mancante sull'orologio | con il bottone per installarla |
| Rete assente | «nessuna rete: riprova» |
| Il PC non conferma | controllo sbagliato o tempo scaduto |
| Orologio in riavvio | ripresa automatica, non è un errore |
| Orologio con uid diverso | «rifai l'accoppiamento» |

### Test e verifiche

**Spike, prima di tutto il resto.** Il codice dello spike si butta; i risultati vanno nel piano.

- **A, Firebase a runtime.** Emulatore Wear, build senza `google-services.json`. La configurazione del progetto di Franz
  si inietta da un file locale fuori dal repository. Passa se:
  - l'accesso anonimo riesce;
  - l'iscrizione al topic riesce;
  - un `relay push` fa arrivare il messaggio FCM.
- **B, cambio di configurazione.** Con un topic diverso, così la configurazione cambia: salva, chiude il processo,
  riparte con la configurazione nuova.
- **C, canale telefono-orologio.** `sendRequest` fra due app con lo stesso pacchetto e la stessa firma. Coppia di
  emulatori, oppure telefono di Franz con un orologio: si decide nel piano, dopo aver misurato la memoria della VM (6 GB).
  La release sul polso di Franz non si tocca.

**Test unitari, in `core` sulla JVM:**
- lettura di `pair-qr.json`;
- costruzione della risposta con `uids` e `names`, confrontata con `pair-response.json`;
- cifratura della chiave per l'orologio, sia andata e ritorno sia con un vettore fisso;
- `FirebaseConfig` ricavata dai valori di `google-services`;
- decisione «stessa configurazione o riavvio»;
- scadenza del QR.

**Screenshot (Paparazzi):**
- le quattro schermate del telefono, con gli errori;
- la schermata «Accoppia dal telefono» dell'orologio;
- gli screenshot esistenti dell'orologio, identici dopo lo spostamento dei colori in `:ui-tokens`.

**Checklist dal vivo** (`docs/verifiche/fondamenta-accoppiamento.md`):
- Firebase nuovo con `relay setup`, poi accoppiamento;
- un push che sveglia l'orologio e un comando dall'orologio;
- l'accoppiamento rifatto che esclude i dispositivi vecchi;
- la chiave consegnata dopo il ricollegamento dell'orologio;
- l'orologio con la configurazione dentro la build che si accoppia ancora con il codice;
- «riduci animazioni».

### Fuori da questo pezzo

Regia, comandi dal telefono, notifiche sul telefono, pubblicazione su Play, widget, impostazioni sincronizzate,
disposizione delle finestre.
