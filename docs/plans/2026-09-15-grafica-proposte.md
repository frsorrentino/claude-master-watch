# Grafica e animazioni: proposte (15/09/2026)

Richiesta di Franz (15/09 23:28): solo aspetti grafici e di dettaglio, animazioni e piccole migliorie, partendo da quello
che offre l'ultima Wear OS. Fatto: giro su tutte le schermate (snapshot Paparazzi del 15/09 e scatti dal polso), censimento
dei componenti usati, controllo delle API nelle librerie in cache (Wear Compose 1.6.2, ProtoLayout 1.4.2, Tiles 1.6.2).

## Cosa abbiamo oggi

- Liste: `TransformingLazyColumn` con `morph` (deformazione ai bordi) su tutte le schermate. Bene, è il cuore di M3 Expressive.
- Animazioni presenti: il badge che pulsa (`SessionBadge`, `rememberInfiniteTransition`), gli archi della quota (`Gauge`,
  `tween`), una `AnimatedVisibility` nella tile. Nient'altro: cambi di stato, arrivi di righe, invii e caricamenti
  scattano senza transizione.
- Componenti M3 con forme animate disponibili ma non usate: `IconButton`/`IconToggleButton` (`animatedShapes`). I bottoni
  larghi (`Button`, `FilledTonalButton`) non hanno forme animate nella 1.6.2: lì il movimento va dato in altro modo.
- Wear OS 7 (il sistema del Pixel Watch 5): le Ongoing Activity sono sostituite dalle **Live Updates**; nuovi **Wear
  Widgets** (Glance + RemoteCompose) accanto alle tile; Wear Compose 1.6 aggiunge lo snapping e il layout rovesciato alla
  `TransformingLazyColumn`.

Dettagli visti negli scatti: il nome della sessione va a capo a metà («ledger- / api», «atlas-sh / op») accanto a ▶; il
grigio secondario `text2` (#9AA3B2) è poco contrastato anche fuori dal Terminale (età, righe della Timeline, «Chiedo al PC»).

## Proposte, in ordine di valore per costo

Regole per tutte: rispettano `LocalReduceMotion` (con «riduci animazioni» attivo restano solo i cambi netti), si fermano
in ambient, durate brevi (150-300 ms) e molle morbide del motion scheme M3, niente animazioni in loop tranne il badge che
già c'è.

### A. Piccole, subito visibili

1. **▶ che diventa ■ con la forma che si trasforma.** `SpeakButton` passa a `IconToggleButton` con
   `IconToggleButtonDefaults.animatedShapes()`: premuto si schiaccia, in lettura diventa un quadrato arrotondato con ■.
   È l'esempio stesso di M3 Expressive (play/pausa). Tutte le schermate con ▶.
2. **Il nome su una riga.** Nella testata il nome si rimpicciolisce a scalini (14 → 11 sp, `TextAutoSize.StepBased`) prima
   di andare a capo; se non basta, va a capo solo al trattino. Domanda, Terminale, Scheda.
3. **Le righe nuove del Terminale entrano, non compaiono.** I blocchi arrivati con l'aggiornamento dal vivo entrano con
   dissolvenza e allungamento (`animateItem` sulla `TransformingLazyColumn`); l'ora nel divisore cambia con una
   dissolvenza sulle cifre (`AnimatedContent`).
4. **Il paragrafo letto si accende piano.** Il fondo del paragrafo in lettura passa con `animateColorAsState` invece di
   scattare; la lista scorre con la molla del motion scheme.
5. **Le sessioni si riordinano scorrendo.** Quando una sessione sale in cima perché ha una domanda, o scende perché ha
   finito, la card scivola al suo posto (`animateItem`) invece di saltare. L'icona di stato (▶ ✓ ❓ ✗) passa dall'una
   all'altra con dissolvenza e scala (`AnimatedContent`).
6. **Conferma animata dopo un invio.** Risposta a una domanda, prompt da «Scrivi», Riprendi: `ConfirmationDialog` di
   successo (spunta che si disegna, 1,5 s) o di fallimento, oltre alla vibrazione che c'è già.
7. **Contrasto del grigio secondario.** Portare `text2` da #9AA3B2 a un grigio più chiaro in tutta l'app (audit
   dei punti in cui è usato), come fatto oggi nel Terminale.

### B. Medie

8. **Caricamenti con la sagoma del contenuto.** Al posto di «Chiedo al PC», righe segnaposto con il luccichio
   (`Modifier.placeholder` + `placeholderShimmer`) nella forma della risposta e del terminale.
9. **Quota che si riempie all'apertura.** Gli archi partono da zero e arrivano al valore con una molla; dato vecchio =
   arco che sbiadisce. Da decidere insieme alla revisione della pagina Quota già in lista (card «Aggiornato»).
10. **Stato accanto all'ora.** In `TimeText`, testo curvo dopo l'ora quando qualcosa aspetta: «❓ 1» in ambra. Si vede da
    ogni schermata dell'app senza tornare alla lista.
11. **Tile che si accende.** ProtoLayout 1.4: dissolvenza in entrata quando la tile diventa visibile (helper di fade della
    `Transformation`), barre della quota che si riempiono con `DynamicFloat` animato (già in lista dal 15/09), il badge della
    sessione in attesa che pulsa una volta.
12. **Stretch in fondo alla lista.** L'effetto overscroll della `TransformingLazyColumn` (parametro `overscrollEffect`),
    come nelle app Google, al posto dell'arresto secco.

### C. Più grandi, da valutare a parte

13. **Live Updates al posto dell'Ongoing Activity** per la sessione seguita (Wear OS 7): stato in tempo reale nel
    quadrante e nel launcher, con l'API che Google usa ora.
14. **Wear Widget** (Glance + RemoteCompose) accanto alla tile: card piccola e grande, animazioni più ricche e meno
    batteria. È una seconda superficie, non un ritocco.
15. **Colore dinamico dal quadrante** solo per l'accento (non per gli stati, che devono restare gli stessi di Telegram).
16. **Scorrimento a scatti** nel Terminale e nella Risposta (snapping della `TransformingLazyColumn` 1.6): un paragrafo
    per volta al centro.

## Come si verifica

Ogni proposta approvata: snapshot Paparazzi prima e dopo (in CI), prova al polso con «riduci animazioni» acceso e spento,
riga nella checklist `docs/verifiche/`. Le animazioni si giudicano solo sul polso: gli snapshot mostrano lo stato finale.

## Stato del primo giro (Franz, 15/09 23:40: «A e B approvate»)

A1-A7 e B8-B11 in lavorazione. B12 non serve: la `TransformingLazyColumn` e lo `ScreenScaffold` della 1.6.2 chiamano già
`rememberOverscrollEffect` di default (controllato nel bytecode), l'effetto elastico c'è. Trovato lungo la strada: ogni
risultato di comando faceva vibrare, anche le catture del Terminale dal vivo ogni 3 s; ora vibrano solo le azioni
dell'utente (`Repo.userResults`, senza `screen` e `last`).

## Secondo giro (15/09 23:50)

Stesso metodo: API cercate nelle librerie in cache o nelle note ufficiali; dove non è verificato, è scritto.

### D. Movimento e dettaglio

17. **Corona che scatta.** La corona del Pixel Watch muove la Domanda un'opzione per volta, con lo scatto aptico del
    sistema (`RotaryScrollableDefaults.snapBehavior` sulla lista), e le altre liste con il tic aptico a ogni voce.
18. **Domanda sì/no come coppia.** Con due sole opzioni, i due tasti affiancati in un `ButtonGroup`: quello premuto si
    allarga e l'altro si stringe, come il tastierino di Wear OS 6. Con tre o più opzioni resta la colonna.
19. **Numeri che rotolano.** Percentuali della quota, età («5 m» → «6 m»), contatori della tile nell'app: la cifra nuova
    scorre dall'alto (`AnimatedContent` per cifra), invece di sostituirsi.
20. **La domanda importante si fa notare una volta.** Il bordo rosso delle domande HIGH fa un solo respiro all'arrivo,
    poi resta fermo (come il gauge al 100 %).
21. **Lettura con la barra.** Mentre la voce legge, una sottile `LinearProgressIndicator` sotto ▶ dice a che punto della
    risposta è; toccare un paragrafo la fa saltare.
22. **Recap che si scrive.** Il recap delle 20:00 entra riga per riga (`FadingExpandingLabel` / `AnimatedText`, che
    rispettano «riduci animazioni»).
23. **Ambient curato.** Un giro su ogni schermata in ambient con `LocalAmbientModeManager` (Wear Compose 1.6): solo
    contorni, niente riempimenti grandi, testo secondario spento, badge senza respiro; oggi lo fa solo il badge.
24. **Avvio dell'app.** Icona animata nella splash (SplashScreen con vettore animato, 500 ms) invece del lampo nero.

### E. Funzioni della piattaforma che non usiamo ancora

25. **Scorrere fra le sessioni.** Dalla Scheda, swipe orizzontale alla sessione successiva (`HorizontalPagerScaffold` con
    indicatore di pagina e `AnimatedPage`), senza tornare alla lista. Lo swipe da sinistra resta «indietro».
26. **Azioni sotto il dito.** Sulla riga della lista, trascinando a sinistra appaiono «Segui» e «Chiudi»
    (`SwipeToReveal`, con zona di bordo che non litiga con lo swipe di ritorno); oggi Segui è solo la pressione lunga, che
    non si scopre.
27. **Impostazioni con i controlli di Wear.** Soglia di lettura con `Slider` a gradini o `Stepper`, voce con `Picker`,
    al posto delle righe da toccare più volte.
28. **Complicazione a segmenti.** Nuovo tipo `WEIGHTED_ELEMENTS`: un anello diviso per stato delle sessioni (ambra chi
    aspetta, blu chi lavora, verde chi ha finito), oppure la quota come `RANGED_VALUE` con la rampa di colore
    verde → ambra → rosso. Il quadrante sceglie quale mostrare.
29. **Live Updates per la sessione seguita** (Wear OS 7, sostituiscono le Ongoing Activity): stato e tempo del turno sul
    quadrante e nel launcher; con la `ProgressStyle` di Android 16 i segmenti del turno (da verificare sul Pixel Watch 5).
30. **Tile con piccola animazione Lottie** (ProtoLayout 1.3+): il badge di chi aspetta che pulsa una volta quando la tile
    diventa visibile; arco della quota con gradiente a spazzata.
31. **Aptica più parlante.** Vibrazioni diverse per domanda, esito, errore e conferma con le primitive del sistema
    (`HapticFeedbackConstants.CONFIRM` / `REJECT`, trame composte), provate una per una al polso.

Priorità suggerita: 17, 19, 21, 26, 25 (valore alto, rischio basso), poi 28 e 29 (nuove superfici), il resto a
piacere. Colore dinamico (15) e Wear Widget (14) restano nel terzo gruppo.

## Terzo giro: Scheda e Quota con grafici (Franz, 16/09 00:14)

Dati verificati: il contratto non porta il contesto della sessione (niente in `Session`, niente in `cm-relay.py`); la
quota per account sì; gli eventi (`/events`, 7 giorni) sono già salvati sull'orologio. Nella Quota l'ultima card è
«Aggiornato» (`BriefCards`, glifo SYNC): ora e nome della macchina, sempre uguali. In questa sessione non avevo scritto
idee per sostituirla: quelle qui sotto sono nuove.

Da Google Health (maggio 2026, fonti: blog Google, the5krunner) si prende quello che le fonti mostrano: anelli di
avanzamento verso un obiettivo **settimanale** (il cardio load ha sostituito il traguardo giornaliero), card rettangolari
arrotondate, grafici del sonno puliti a bande. Si evita quello che le stesse fonti criticano: muri di testo, anelli
sbilanciati accanto ai rettangoli, grafici sfocati.

32. **Scheda: riquadro «Quota e contesto».** In fondo alla Scheda una card brief: anello del contesto della sessione
    («62 %», ambra oltre il 75 %, rosso oltre il 90 %) e le due pillole della quota del suo account («5h 13 %»,
    «7d 36 %»). La quota c'è già; il contesto richiede una richiesta a claude-master (contratto 1.11, campo `context` in
    percentuale, dal consumo di token della trascrizione).
33. **Quota, al posto di «Aggiornato» — «Ritmo della finestra».** Linea della quota di 5 ore dall'inizio della finestra a
    ora, e tratteggiata la proiezione fino al reset: «a questo ritmo 64 % alle 16:00». Il numero grande è la proiezione.
    Serve la storia della quota, che l'orologio può registrare da sé a ogni stato ricevuto (Room), senza contratto.
34. **Quota, alternativa — «Oggi».** Barre per ora della giornata: quante sessioni hanno lavorato in ogni ora, dagli
    eventi già salvati (lanci, esiti, domande). Si legge come le barre dei passi.
35. **Quota, alternativa — «Settimana».** Sette barre del consumo settimanale giorno per giorno con la linea del limite,
    come l'obiettivo settimanale di Google Health. Stessa storia registrata dall'orologio.
36. **Ora e macchina** non spariscono: diventano una riga piccola in fondo alla pagina («aggiornato ora · penguin»),
    grigia; in rosso solo se il dato è vecchio.
37. **Scheda: la giornata della sessione.** Una banda orizzontale divisa per stato nelle ultime ore (blu lavora, ambra
    aspetta, verde ferma), disegnata come le fasi del sonno, dagli eventi della sessione.

Consiglio: 33 al posto di «Aggiornato» (è l'unico dato che la pagina oggi non dà e che cambia una decisione: fermarsi o
no), più la riga 36; 32 con la richiesta di contratto; 34, 35 e 37 dopo, se 33 funziona.

## Quarto giro: idee dalle app Google e Apple (Franz, 16/09 00:17)

Fonti: articoli su Messages, Calendar, Keep, Gmail, Weather, Maps, Home, YouTube Music e Gemini per Wear OS con M3
Expressive (Android Authority, 9to5Google, Android Police), watchOS 26 (Apple Newsroom, MacRumors, MacStories), linee
guida Apple per Watch. Si prende l'idea, non il marchio; dove l'API per un'app di terze parti non è verificata, è scritto.

### Da Google

38. **Messages → il filo del tuo testo nel colore della sessione.** Messages tinge appena le bolle; noi, senza bolle,
    tingiamo il filo accanto alle tue righe del Terminale con il colore del badge della sessione invece del blu fisso:
    si riconosce di chi è il terminale anche senza leggere il nome.
39. **Messages / Maps → tasti con icona e testo.** Le azioni della Scheda (Scrivi, Terminale, Riprendi) con l'icona
    accanto alla parola, come le opzioni delle liste di Messages e i moduli di Maps.
40. **Keep → due tasti grandi affiancati nella tile.** Quando una sessione aspetta, la tile mostra «1 · sì» e «2 · no»
    (o «Rispondi» / «Apri») come i due tasti grandi della tile «Crea nota» di Keep; con più di due opzioni resta «Rispondi».
41. **Weather → più tile, una per compito.** Come le tre tile del Meteo: tile «Domanda» (solo quando c'è), tile «Quota»
    (anelli e reset), tile «Sessioni» (quella di oggi). Ognuno mette nel carosello quelle che vuole.
42. **Maps → moduli colorati per le cose frequenti.** In Avvia, i progetti usati più spesso in cima come moduli
    colorati con il badge, il resto in lista; nel Menu le voci come moduli, non come righe uguali.
43. **Home → complicazione per una sessione.** Come Home ha la complicazione del singolo dispositivo: una complicazione
    con lo stato della sessione seguita (badge e età), che apre la sua Scheda.
44. **Gemini → il bagliore in basso.** Gemini accende una piccola luce in fondo al quadrante quando ascolta e la allarga
    quando capisce che parli. Da noi: un bagliore sottile al bordo inferiore della Scheda mentre la sessione lavora, che
    si allarga per un attimo quando arriva l'esito. Solo in app, spento in ambient.
45. **YouTube Music → forme espressive.** Il tasto «Sfoglia» a forma di nuvola: M3 Expressive ha forme oltre a tondo e
    quadrato. Proposta prudente: solo per il tasto di bordo del Menu, non per i badge (tondo e quadrato dicono l'account).

### Da Apple

46. **Anelli concentrici (Attività).** Per ogni account un solo gauge con due anelli: fuori le 5 ore, dentro la
    settimana. Più compatto delle due righe di oggi, riconoscibile al volo; lo stesso disegno può diventare una
    complicazione. È l'idea Apple più pertinente.
47. **Smart Stack → il suggerimento che si apre.** In cima alla lista, quando serve, una card «suggerimento»
    («ledger-api aspetta da 5 min») che scorrendo si apre nella domanda intera, come i suggerimenti di watchOS 26.
48. **Glanceable, regola di Apple.** Un giro su ogni schermata: la prima vista risponde a «chi ha bisogno di me» in due
    secondi. Candidati: Quota (oggi il numero grande è la percentuale, non «quando riparte»), Timeline, Impostazioni.
49. **Non pertinenti:** Liquid Glass (la trasparenza su sfondo nero OLED non mostra nulla e costa batteria; al massimo un
    filo di luce sul bordo alto delle card, da provare); il gesto del polso di watchOS (su Wear OS non c'è un'API
    pubblica equivalente per le app, da verificare); le Live Activities sono le nostre Live Updates (29).

Consiglio: 46 (anelli concentrici, anche come complicazione), 40 (sì/no nella tile), 38 (filo nel colore della
sessione), 44 (bagliore mentre lavora); poi 41 e 43 come nuove superfici.
