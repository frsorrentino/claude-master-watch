# Video promozionali: piano (da approvare prima di registrare)

Richiesta di Franz (16/09, 16:05-18:48): video dell'app al polso per il README e le pagine del progetto, sul modello dei
video di prodotto degli smartwatch. Dopo due giri di prova il metodo è chiaro; questo piano fissa **cosa** raccontare e
**cosa serve** prima di registrare di nuovo. Non si registra niente finché Franz non lo approva.

## Cosa abbiamo imparato dai giri di prova

| Problema visto | Causa | Stato |
|---|---|---|
| Scorrimento a scatti | Corona simulata = scatti agganciati; `screenrecord` a frequenza variabile | Risolto: scorrimento guidato nell'app (demo) e ricodifica a 30 fps costanti |
| Pagina che «prova» a scorrere in fondo | Numero fisso di scatti | Risolto: distanze misurate, un solo movimento per elemento |
| Composizione `ffmpeg` poco convincente | Cassa disegnata con Pillow, zoom meccanico | Scartata: si torna alla cornice HTML (cassa CSS + vetro) |
| Quadrante, schermata debug wireless, avvio a freddo nelle clip | Si registra da prima del riavvio dell'app | Si taglia l'inizio; da evitare alla radice (vedi prerequisiti) |
| «Write» sempre grande sopra il contenuto | `EdgeButton` non si riduce scorrendo | **Da correggere nell'app** |
| Nomi e testi banali | Fixture minime del contratto | Risolto: `DemoText` solo nella demo |
| «Today» conta eventi veri (336) | Gli eventi salvati su Room restano anche in demo | **Da correggere**: in demo solo eventi demo |
| «New session» non mostra la sessione che nasce | La demo non crea sessioni | **Da correggere**: in demo il launch aggiunge la sessione |

## La sequenza principale: «Il rilascio 2.8.0, dal polso» (~75 s)

Franz, 18:56: serve una sequenza concatenata e plausibile, non un flusso isolato. Una storia sola, legata a un filo
conduttore: il rilascio 2.8.0 di payments-api. Ogni scena parte dallo stato in cui finisce la precedente; tra le scene
tagli o didascalie di tempo («A few minutes later»), non salti di stato inspiegati.

| # | Scena | Cosa si vede | Stato alla fine | ~s |
|---|---|---|---|---|
| 0 | Tutto tranquillo | Quadrante con la complication della quota (11 %) | — | 3 |
| 1 | Un'occhiata | Dal quadrante alla tile: storefront al lavoro, quota con i due anelli. Tocco → lista, scorrimento: storefront lavora, blog ferma | nessuna domanda | 9 |
| 2 | Arriva una domanda | Vibrazione, notifica di payments-api: «Refund endpoint is ready… Deploy version 2.8.0 to production now?». Tocco → la domanda. ▶ Ascolta, il testo scorre. Pressione lunga su «yes» → «Sent» | payments-api al lavoro | 14 |
| 3 | La seguo | Lista: pressione lunga su payments-api → «Alerts on», campanella | seguita | 5 |
| 4 | Il deploy è fatto | «A few minutes later». Notifica di esito: «Deployed 2.8.0, smoke tests green». Tocco → Scheda: esito lungo, ▶ Ascolta mentre scorre fino a Quota e Sessione (contesto 62 %) | payments-api ferma | 12 |
| 5 | Il passo dopo, a voce | «Write» compare in fondo → dettatura «Great. Now update the changelog and tag the release» → «Sent». Tocco sulla card → Terminale: le righe nuove arrivano dal vivo | payments-api al lavoro | 12 |
| 6 | Quanta quota resta? | Indietro alla lista → Panoramica: Ritmo 5 ore «at this pace 64 % at 13:10», Now, Contesto | — | 10 |
| 7 | Ne parliamo sul blog | Lista in fondo → «New session» → blog → dettatura «Draft a post about the 2.8.0 release» → la sessione nasce, la sua Scheda «turn in progress» | blog al lavoro | 10 |
| 8 | Chiusura | Ritorno al quadrante (complication), didascalia di chiusura in montaggio | — | 3 |

Perché regge: ogni azione ha un motivo nella scena prima (la domanda porta al deploy, il deploy all'esito, l'esito al passo
successivo, il consumo di quota alla verifica, il rilascio al post). Mostra tutte le funzioni principali senza un elenco:
complication, tile, lista, notifica, ascolto, risposta, seguire, esito, risposta a voce, terminale dal vivo, Panoramica,
nuova sessione.

**Come si registra**: una scena per registrazione, con lo stato della demo preparato a mano tra una e l'altra (vedi
prerequisiti), poi montaggio in sequenza. Una sola ripresa lunga sarebbe fragile: un tocco sbagliato a 60 secondi la butta.

**Ritagli dalla stessa sequenza**, non registrazioni separate:
- **README** (~20 s): scene 2 + 4 + 6, cioè domanda, esito, quota.
- **Pagina del plugin** (~12 s): scena 2 da sola.
- **Clip per funzione**: le singole scene.

## I flussi singoli (materiale per i ritagli)

Ogni flusso è una storia con un inizio reale e un esito visibile. Durate indicative.

### F1 · Una domanda mentre sei lontano dal PC (~20 s) — priorità 1
1. Quadrante con la complication «1 in attesa».
2. Arriva la notifica di payments-api (vibrazione): «Deploy version 2.8.0 to production now?».
3. Tocco sulla notifica → la domanda.
4. ▶ Ascolta: il tasto si accende, il testo scorre lento mentre la voce legge.
5. Pressione lunga su «1 · yes» → «Sent».
6. Lista: payments-api passa a «turn in progress».

### F2 · Quanta quota mi resta? (~20 s) — priorità 2
1. Tocco sulla complication della quota → Panoramica.
2. Scorrimento guidato: Quota ● e ■ → Ritmo 5 ore con proiezione → Now e domande → Contesto → Today.
3. Tocco su «Open questions» → la domanda.

### F3 · Ascolto e risposta a voce (~25 s) — priorità 3
1. Scheda di storefront: esito lungo con ▶.
2. ▶ Ascolta mentre la card scorre fino a Quota e Sessione.
3. «Write» compare alla fine → dettatura «Now update the changelog and open a PR» → «Sent».
4. Terminale: le righe nuove arrivano dal vivo.

### F4 · Controllo veloce dalla tile (~12 s)
1. Dal quadrante alla nostra tile (sessione seguita, quota).
2. «Sessions» → lista → scorrimento guidato → storefront → Scheda.

### F5 · Una nuova sessione dal polso (~18 s)
1. Lista in fondo → «New session» → progetti dal più recente → blog.
2. «Write the first message» → dettatura «Draft a post about our caching results».
3. La sessione nasce → la sua Scheda con «turn in progress».

### Clip brevi di coda (5-8 s ciascuna)
- Seguire una sessione: pressione lunga → «Alerts on» → campanella.
- Sessione chiusa: «Recently closed · 1» → «Resume».
- Impostazioni in stile Google.

## Prerequisiti (da fare prima di registrare)

### Nell'app, solo per la demo (tutti via adb, ignorati senza demo)
1. **Notifica demo**: extra `demo_question` che fa arrivare una domanda finta come dal relay (stato + evento + notifica
   con vibrazione). Serve a F1.
2. **Dettatura simulata**: extra `demo_reply "testo"` che si comporta come il risultato della tastiera/dettatura, così la
   risposta a voce si vede senza pilotare il riconoscimento vocale di Wear. Alternativa: Franz detta davvero durante la
   registrazione.
3. **Launch che crea la sessione**: nella demo `launch` aggiunge la sessione allo stato, così F5 finisce sulla Scheda.
4. **Terminale che cresce**: nella demo, dopo una risposta, `screen` restituisce righe nuove a ogni aggiornamento.
5. **Eventi solo demo**: con la demo accesa «Today» legge solo gli eventi demo.
5b. **Passi della storia**: extra `demo_step` che porta la demo allo stato di ogni scena della sequenza principale, in
   modo riproducibile: `calm` (nessuna domanda), `question` (arriva la domanda del deploy, con notifica), `deployed`
   (payments-api ferma con l'esito «Deployed 2.8.0, smoke tests green», notifica di esito se seguita), `followup` (al
   lavoro sul changelog, terminale che cresce), `blog` (la sessione blog nata dal launch). Così ogni scena si registra da
   uno stato noto e si può rifare da sola.

### Nell'app, per tutti
6. **«Write» che si riduce scorrendo**: l'`EdgeButton` deve collegarsi allo scorrimento della lista (comportamento
   standard di Wear Material 3), invece di restare pieno sopra il contenuto. Verifica al polso prima dei video.

### Sull'orologio (Franz, una volta)
7. **Quadrante neutro** durante le registrazioni, con solo la nostra complication: il quadrante abituale mostra le sue
   complication personali.
8. **Nostra tile al primo posto** del carosello: tra il quadrante e la nostra tile oggi passano tile con dati di salute.
9. **Debug wireless chiuso** sull'orologio prima di iniziare (compariva nelle clip) e batteria sopra il 50 %, lontano dal
   caricatore.

### Audio
10. `screenrecord` non registra l'audio. Per «Ascolta»: o si vede solo il tasto acceso, o la voce si genera a parte con
    lo stesso testo e si aggiunge in montaggio. **Da decidere.**

## Come si registra e si monta

- **Registrazione**: `tools/promo/record.sh` con un copione per flusso (`flow_f1` …), scorrimento guidato, pause di
  lettura, prova con screenshot prima di ogni registrazione vera (`probe`), controllo fotogramma per fotogramma
  dei primi secondi prima del taglio.
- **Montaggio, in due fasi** (Franz, 18:49: «Remotion magari in seguito per completare»):
  - **Fase A — revisione nella pagina HTML**: le clip registrate girano sotto la cassa CSS approvata (nera opaca, vetro)
    nella pagina di prova. È il banco dove si decide se un flusso funziona: ritmo, pause, leggibilità. Costo basso, si
    rifà in minuti.
  - **Fase B — montaggio finale con Remotion**, solo sui flussi approvati in fase A. Remotion 4.0.490 è già installato e
    ha già renderizzato su questa macchina in `~/Desktop/workspaces/personali/video/storytelling-video` (compositore
    arm64, chrome-headless-shell, skill ufficiali). Si copia l'impianto in `tools/promo/remotion` senza toccare
    l'originale. Porta quello che la pagina non fa: file MP4 2:1 e GIF veri, zoom di camera con easing, testi che entrano
    accanto all'orologio («Answer from your wrist»), dissolvenze tra i flussi, voce della lettura in montaggio.
    Trappole note: render con `--concurrency=1` o `2` sui 6 GB; clip già a fps costante (fatto); `<OffthreadVideo>` per le
    registrazioni. Prima una prova di 3 secondi, poi i flussi.
- **Uscite**: una clip per flusso, più un montaggio di ~15 s con i momenti migliori per l'apertura del README.

## Verifica prima di consegnare

- [ ] Nessun fotogramma con quadrante personale, impostazioni, indirizzi, dati di salute (controllo a un fotogramma ogni
      mezzo secondo su tutta la clip, non solo all'inizio).
- [ ] Nessuno scatto: scorrimenti solo guidati, 30 fps costanti.
- [ ] Nessun elemento sovrapposto al contenuto durante lo scorrimento (Write).
- [ ] Ogni flusso finisce su un esito visibile (Sent, turn in progress, Scheda della sessione nata).
- [ ] Testi tutti in inglese, nessuna parola italiana residua.
- [ ] Orologio riportato ai dati veri, italiano, timeout dello schermo originale.

## Decisioni per Franz

1. La sequenza principale «Il rilascio 2.8.0, dal polso»: scene, ordine, frasi dettate e durata (~75 s).
2. Dettatura simulata (prerequisito 2) o dettata dal vivo da te.
3. Audio della lettura: solo il tasto acceso, o voce aggiunta in montaggio.
4. Conferma delle due fasi di montaggio: revisione nella pagina HTML, poi Remotion per i file finali dei flussi approvati.
5. Quando fare i passi sull'orologio (quadrante neutro, tile al primo posto).
