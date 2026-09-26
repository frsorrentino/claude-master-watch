# Play Console: cosa fa Franz, in ordine (26/09/2026)

Pezzo 2 del design `2026-09-24-app-telefono-fondamenta-design.md`. Qui ci sono solo i passi che richiedono Franz nella
Play Console. Tutto il resto lo preparo io: i bundle firmati, la privacy policy, le risposte per la sicurezza dei dati,
i testi della scheda e le immagini. Requisiti verificati il 26/09 sulle pagine di aiuto di Play (fonti in fondo).

## Il vincolo dei tempi

- **Test chiuso:** l'account è personale e creato dopo il 13/11/2023. Prima della produzione servono almeno 12 tester
  iscritti senza interruzioni per 14 giorni. Chi si iscrive e poi esce prima dei 14 giorni non conta.
- **Uso reale:** dal 2026 Play controlla anche che i tester abbiano usato davvero l'app. Senza PC, i tester usano la
  modalità Demo del telefono, che va costruita (pezzo 3).
- **Revisione:** la domanda di accesso alla produzione si revisiona di solito in 7 giorni o meno.
- **Conto alla rovescia:** se il test chiuso parte entro il 04/10, la produzione arriva verso il 25/10. Il conto è
  revisione del test chiuso (1-3 giorni), più 14 giorni, più circa 7 giorni di revisione.
- **Target SDK:** dal 31/08/2026 le app nuove devono avere target 36 sul telefono e almeno 35 su Wear OS. Oggi tutte e
  due le app hanno target 36: siamo già in regola.

## 1. Oggi, senza aspettare nulla da me

Stato al 26/09 alle 20:42, verificato dal master nella Console:
- l'account è personale, «Francesco Sorrentino», e ha già 3 app con test interni: l'identità è verificata;
- la verifica sviluppatori Android dice che tutte le app Play sono registrate. `it.pixelbox.cmwatch` si registra da sola
  quando si crea l'app (passo 3).
- Gli APK distribuiti fuori da Play (le build firmate da noi e installate con `adb`) vanno registrati con la loro chiave
  nella pagina «Verifica dello sviluppatore Android». La scadenza del 30/09/2026 vale per Brasile, Indonesia, Singapore e
  Thailandia; nel resto del mondo dal 2027.

1. **Stato dell'account.** Aprire la Play Console e controllare che l'identità risulti verificata. Dal settembre 2026 i
   nuovi account personali devono verificare l'identità e dimostrare di avere un telefono Android, con l'app Play
   Console. La verifica richiede da 2 a 5 giorni lavorativi: se manca, è il primo passo.
2. **Tester.** Cominciare a raccogliere almeno 15 indirizzi Gmail, 3 in più dei 12 per chi si perde. Due strade:
   - un Google Group: i tester entrano da soli e la lista si gestisce fuori da Play;
   - una lista di indirizzi dentro la Play Console.
   Il Google Group è più comodo se i tester arrivano a ondate. Ogni tester deve avere un telefono Android; l'orologio è
   facoltativo. Deciso da Franz il 26/09: misto, cioè conoscenti più un servizio di tester per arrivare a 15.
3. **Crea l'app.** Tutte le app → Crea app. Servono:
   - il nome «Claude Master App» (deciso da Franz il 26/09). La famiglia è Claude Master App, Claude Master Watch e
     Claude Master Plugin. La scheda è una sola per telefono e orologio, quindi il titolo su Play è uno. Sull'orologio
     l'icona si chiama «Claude Master Watch»;
   - la lingua predefinita: inglese, deciso da Franz il 26/09, con l'italiano come traduzione;
   - «App», «Gratuita» e le due dichiarazioni. «Gratuita» non si può più cambiare in «a pagamento».
   - Rischio: «Claude» è un marchio di Anthropic, e la revisione può contestarlo (impersonificazione, proprietà
     intellettuale). Per ridurlo, la prima riga della descrizione dice «Unofficial companion for Claude Code, not
     affiliated with Anthropic», e l'icona non usa il logo di Claude. Se Play rifiuta, si rinomina solo il titolo: il
     pacchetto resta.

## 2. Quando ti mando il primo bundle (dopo il render, build locale o CI)

4. **Firma di Play.** Al primo caricamento, Play genera la chiave di firma dell'app. Il nostro keystore di release
   diventa la chiave di caricamento.
   - Conseguenza: l'app scaricata da Play ha una firma diversa da quella installata oggi con `adb`. Prima di installarla
     da Play bisogna disinstallare le versioni di sviluppo, su telefono e orologio, e rifare l'accoppiamento.
   - Con il pairing di oggi, la release sul polso non si tocca finché Franz non lo decide.
5. **Test interno.** Test → Test interno → Crea release. Caricare i due bundle che ti mando: `mobile` e `wear`, stesso
   pacchetto `it.pixelbox.cmwatch`, versionCode diversi. Aggiungere sé stessi come tester. Non serve la revisione
   completa: serve per provare subito l'installazione da Play, anche sull'orologio.
6. **Wear OS.** Impostazioni avanzate → Fattori di forma → Aggiungi fattore di forma → Wear OS. Poi «Opt in to Wear OS
   and agree to the review policy».
   - Uso le tracce comuni, non quella dedicata a Wear OS, così i tester sono una lista sola. Se la Console impone la
     traccia dedicata, si mette la stessa lista di tester anche lì.
   - La descrizione deve nominare «Wear OS», altrimenti la revisione rifiuta il fattore di forma.

## 3. Contenuti dell'app (Norme → Contenuti dell'app), con i testi che ti preparo

7. **Privacy policy:** incollare l'URL che ti mando. È una pagina statica sul repository pubblico, su GitHub Pages.
8. **Accesso all'app:** «Alcune funzioni sono limitate». Incollare le istruzioni per i revisori: l'uso reale richiede un
   PC con claude-master, quindi la revisione passa dalla modalità Demo. Nessun login.
9. **Annunci:** no.
10. **Classificazione dei contenuti:** questionario IARC, categoria «Utilità / Produttività». Tutte le risposte sono
    «no».
11. **Pubblico di destinazione:** 18 anni e oltre, non rivolta ai bambini.
12. **Sicurezza dei dati:** incollare le risposte che ti preparo. In breve:
    - dati raccolti: identificatori del dispositivo per Firebase (uid anonimo, token FCM);
    - contenuti cifrati da un capo all'altro, sul Firebase dell'utente e non su un server nostro;
    - nessuna condivisione con terzi, nessuna pubblicità;
    - cifratura in transito: sì.
13. **App governative, finanziarie, sanitarie, notizie:** no a tutte.

## 4. Scheda dello Store

14. Incollare i testi e caricare le immagini che ti mando:
    - descrizione breve (80 caratteri) e descrizione completa, in italiano e in inglese;
    - icona 512×512;
    - grafica in evidenza 1024×500;
    - almeno 2 screenshot del telefono;
    - screenshot dell'orologio: quadrati, almeno 384×384, senza cornice rotonda disegnata.

## 5. Test chiuso: il collo di bottiglia

15. **Traccia.** Test → Test chiuso → Crea traccia. Aggiungere il Google Group o la lista di indirizzi, e un canale di
    feedback (un indirizzo email basta). Caricare la stessa release del test interno, promuovendola.
16. **Revisione.** Invia per la revisione. Il test chiuso passa dalla revisione vera: le Norme e la scheda devono
    essere complete, compreso Wear OS.
17. **Link ai tester.** A revisione passata, mandare il link di adesione ai tester. Ognuno deve fare tre cose: aprire il
    link, accettare, installare dal Play Store sul telefono.
    - Il conto dei 14 giorni parte dal dodicesimo tester iscritto.
    - Nei 14 giorni ogni tester deve aprire l'app più volte: la modalità Demo basta.
18. **Aggiornamenti durante il test.** Si possono caricare nuove versioni nella stessa traccia senza azzerare il
    conto. Le correzioni della checklist dal vivo entrano così.

## 6. Produzione

19. **Richiesta.** Dopo 14 giorni con almeno 12 tester: Dashboard → Richiedi l'accesso alla produzione. Il questionario
    chiede tre cose, e le risposte te le preparo con i dati del test:
    - com'è andato il test: reclutamento, funzioni usate, feedback;
    - l'app: pubblico, valore, installazioni stimate nel primo anno;
    - la prontezza: cosa è cambiato dopo il test.
20. **Rilascio.** Dopo l'approvazione: Produzione → Crea release. Conviene un rilascio graduale, per esempio al 20%.

## Cosa preparo io, e quando

| Cosa | Quando |
|---|---|
| Bundle firmati `mobile` e `wear` (versionCode distinti) | dopo «render finito», o in CI |
| Privacy policy e URL | con la specifica del pezzo 2 |
| Risposte per la sicurezza dei dati, l'accesso all'app e il questionario IARC | con la specifica del pezzo 2 |
| Testi della scheda IT/EN, icona, grafica, screenshot | con la specifica del pezzo 2; gli screenshot del telefono dopo il pezzo 3 |
| Modalità Demo del telefono | pezzo 3, prima del test chiuso |

## Fonti

- [Requisiti di test per i nuovi account personali](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [Requisiti del target API level](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Tracce dedicate per fattore di forma](https://support.google.com/googleplay/android-developer/answer/13295490?hl=en)
- [Qualità delle app Wear OS](https://developer.android.com/docs/quality-guidelines/wear-app-quality)
- [Verifica dello sviluppatore Android, marzo 2026](https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html)
