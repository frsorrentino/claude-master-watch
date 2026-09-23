# Film corto: design (documento A)

23/09/2026. Il corto si aggiunge al film da 81,8 s, che resta com'è. Le decisioni sono di Franz, prese il 22 e il 23/09 una per una. Questo documento le raccoglie insieme all'analisi del movimento. **Revisione del 23/09 pomeriggio:** dopo le bozze 2-7 Franz ha rifatto apertura e finale (14:23) e ha approvato la fusione fra le sue note e i punti 1-8 dell'analisi, più la prova del punto 10 (14:44-14:52). Le sezioni 2-5, 7 e 8 sono già quelle nuove. L'analisi completa (inventario degli strumenti con la documentazione ufficiale, confronto con i quattro film di esempio) è stata consegnata a Franz il 23/09 alle 00:40.

## 1. Obiettivo

In meno di 40 s, lo stile motion graphic dei film di prodotto di Apple e Google: scene di effetto, sempre al servizio del racconto, con passaggi fluidi da una scena all'altra. Il messaggio principale arriva nei primi 3 secondi: il titolo apre il film.

**Il criterio di Franz (23/09):** i movimenti nello spazio sono il cuore della richiesta, non un extra:
- la camera che si muove;
- oggetti e card in prospettiva;
- scene che si compongono un pezzo alla volta;
- un elemento che porta al successivo.

Nei film di esempio la continuità conta più dell'effetto: Ask ha 2 tagli in 87 s, LangEase nessuno in 33 s.

## 2. Decisioni prese

| Tema | Decisione | Quando |
|---|---|---|
| Rapporto con l'84 s | Film separato; l'84 s (oggi 81,8 s, su www) resta com'è | 22/09 15:27 |
| Tavolozza | Un solo blu notte, quello di «It speaks», in tutte le scene. Il «yes» vira nel blu mentre cresce | 22/09 15:33 |
| Struttura | Una storia sola; ogni inquadratura usata una volta sola | 22/09 15:45, 15:54 |
| Apertura | L'orologio in ambient a luce ferma, senza il respiro del film lungo (lì serviva allo stop and go della musica, qui non c'è), con il titolo «Claude Code, on your wrist.»; poi la notifica e «It asks.», con un tratto più corto fino a «It speaks.» | 23/09 14:23, 14:34 |
| Terminale | Intero, con le righe e i tempi del film lungo | 23/09 07:10 |
| Colpo della musica | Sull'animazione dopo il «yes», cioè sulla card «Deployed» che entra con la corsia; non sulla pressione | 23/09 10:42, 11:28 |
| Schermate del polso | In fila, senza tornare a una schermata già lasciata | 23/09 13:01 |
| Finale | Via la card del rilascio con la seconda vibrazione e via «Shipped. From your wrist.»: il tempo va alla carrellata «Every session, at a glance.» | 23/09 14:23 |
| Esito | L'esito del lavoro resta come riga della lista sul polso: la card di payments-api dice «Released 2.8.0…» con il ✓ disegnato | 23/09 14:44 |
| Carrellata | Blink a tempo fra le schermate del polso (lista, Panoramica); l'ultima transizione è la tapparella a 3 barre del film lungo, nata dalla scheda Context | 23/09 14:23, 13:25 |
| Cartello | Logo ritagliato sul blu, senza disco nero; il cinturino esce intero dal bordo in alto, senza sfumare | 23/09 14:23 |
| Fusione con l'analisi | Punti 1-8 dell'analisi del movimento, più la prova del punto 10 (sfocatura di movimento); fuori il 9 (3D, in attesa) e l'11-12 | 23/09 14:44-14:52 |
| Testo glance | «Every session, at a glance.» | 22/09 21:04 |
| Musica | «Brick By Brick», 110 bpm, taglio della sezione 4 | 22/09 20:55; 23/09 |
| Notifica | Il Tethys del Pixel, una volta sola. Senza riga di credito | 22/09 20:59; 23/09 14:23 |
| Loudness | Guadagno costante più limitatore (`audio/normalize.py`); AAC con il coder «fast» | 22/09 21:34; 23/09 01:40 |
| 3D | In attesa della scelta di Franz sul modello (sezione 6) | 23/09 |

## 2 bis. Revisione del 23/09 sera, dopo la bozza 12 (Franz, 21:13-21:24)

Il volo del terminale è approvato. Cambiano apertura, frasi, carrellata e finale; le scelte di Franz sono quelle consigliate.

- **Apertura:** via la scena ambient. Il film parte sul quadrante acceso con la complication di Claude Master, senza la scritta «payments-api» (`n_face.mp4`, dai primi 2,8 s di `out/clips3/n_face_to_tile.mp4`, ore 12:45, complication «11 · 5h»): 1,5 battiti, poi la notifica con la vibrazione.
- **Frasi:** «Claude has a question.» sulla notifica; «Hear it out.» sul tocco di ▶, che arriva 4,5 battiti dopo la notifica invece di 8.
- **Carrellata:** tre blink allo stesso passo di 2 battiti: lista, Work, Open questions (la schermata delle sessioni che aspettano una risposta), Context.
- **Finale:** la seconda metà della tapparella va nel nero invece che nel chiaro; poi lo slogan «Claude Code, on your wrist.» su nero; il logo arriva sull'accordo finale; il cartello torna su nero, come nel film lungo.
- **Stesso orologio fra terminale e lista:** l'orologio non sfuma; il terminale si ritira dietro l'orologio e si vede attraverso lo schermo, poi la lista si compone attorno alla card (piano 6).
- **Musica:** taglio `0-1 0-11 40-45 --fadein=3`: una battuta d'attacco, l'introduzione, il colpo sulla card «Deployed» al 20, la parte forte fino al 60 (le battute 40-42 portano al finale come nel brano), il finale piano sotto lo slogan, l'accordo finale al 64 sul logo, silenzio dal 68.

| Battiti | Scena |
|---|---|
| 0–1,5 | quadrante con la complication |
| 1,5–5,5 | notifica, «Claude has a question.» |
| 5,5–12 | tocco su ▶ a 6, «Hear it out.», la voce legge la domanda |
| 12–19 | risposta, «yes», espansione |
| 19–33 | corsia di profilo, «Deployed» sul colpo al 20, dettatura |
| 33–43,5 | terminale «Watch it work.» con la riga dell'esito |
| 43,5–48 | volo del terminale, lista, «Every session, at a glance.» |
| 48–50 · 50–52 · 52–58 | Work · Open questions · Context, blink a 48, 50 e 52; tapparella di 5 battiti |
| 58–64 | lo slogan su nero |
| 64–73,5 | cartello su nero: logo, nome al 68, avvisi dal 69,5 |

Durata: 73,5 battiti, 40,1 s.

## 3. Scaletta (71 battiti = 38,7 s a 110 bpm)

Le frasi seguono la regola del film: le parole entrano a mezzo battito l'una e la frase resta ferma 2 battiti. I numeri fra parentesi sono i punti dell'analisi del movimento.

| Battiti | s | Scena | Cosa si vede | Testo |
|---|---|---|---|---|
| 0–5 | 0–2,7 | apertura | orologio frontale in ambient, luce ferma, già nella colonna di destra; la camera si avvicina piano (1); silenzio | «Claude Code, on your wrist.» |
| 5–9 | 2,7–4,9 | asks | notifica Pixel: il display si accende sulla domanda; la camera si ferma di colpo sul tremito (1); la frase arriva da sfocata a nitida; la musica entra al 7 | «It asks.» |
| 9–19 | 4,9–10,4 | speaks | tocco su ▶ al battito 4; l'onda della voce nasce dal tasto ▶ ed entra nel quadro (8); la voce della domanda da 4,5 | «It speaks.» |
| 19–26 | 10,4–14,2 | answer | «1 · yes» / «2 · no» si staccano dal vetro con l'ombra sul display (6); pressione lunga al 23, il «yes» affonda, cresce e vira nel blu mentre la musica si ferma (25,25–25,75); sfocatura di movimento sull'espansione (10, prova) | — |
| 26–40 | 14,2–21,8 | corsia | l'orologio laterale sale sulla risalita; la card «Deployed 2.8.0, smoke tests green» sul colpo della musica (27); le card salgono a velocità diverse e la camera con loro (5); «Say what's next.»; dettatura con la voce | «Say what's next.» |
| 40–50,5 | 21,8–27,5 | lavoro | il terminale del PC, inclinato di circa 20°, con la camera che scorre accanto al cursore mentre Claude scrive (2); il polso ripete le righe; righe e tempi del film lungo | «Watch it work.» |
| 50,5–55 | 27,5–30,0 | lista | la finestra del terminale si rimpicciolisce ed entra nel display come card di payments-api in cima alla lista, «Released 2.8.0 and tagged v2.8.0», con il ✓ che si disegna mentre si posa (3); sfocatura di movimento sul volo (10, prova); sotto la card storefront e blog | «Every session, at a glance.» |
| 55–57 | 30,0–31,1 | Work | blink sul battito (il primo cade sulla battuta): il display si riapre su «Work» della Panoramica | — |
| 57–63 | 31,1–34,4 | Context | blink: «Context» sul display; a sinistra la scheda Context del film lungo, con le barre che si riempiono (62 %, 18 %, 4 %), leggibile per due battiti e mezzo; da 59,5 il resto del quadro sfuma e le 3 barre, piene, crescono fino a diventare i listelli della tapparella, che si chiude sul taglio | — |
| 63–71 | 34,4–38,7 | cartello | i listelli si voltano fino a 65,5 e scoprono il cartello; il logo ritagliato sul blu, senza disco nero, compare mentre si voltano; l'orologio di tre quarti col cinturino intero fuori dal bordo in alto; «Claude Master» da 65, «Free. Open source.», repo; avvisi da 67, leggibili per 4 battiti | — |

**Su tutto il corto:** la camera respira in ogni inquadratura ferma, con una deriva dell'1–2 % (7); l'ambient dell'apertura resta fermo.

**Blink:** quelli del film lungo, le palpebre che si chiudono sul battito e si riaprono sulla schermata dopo. La frase di «lista» se ne va prima del blink; nessuna parola vola in alto.

**Tempi della carrellata (scrivendo il piano 3, 23/09 15:30):** la tapparella del film lungo non può durare meno di 6 battiti (regola nata dalle note di Franz del 18 e 21/09) e comincia 3,5 battiti prima del taglio; perché la scheda Context si legga prima, «Context» parte a 57 e dura 6 battiti. Il volo del terminale sta dentro la scena «lista», che comincia a 50,5 con il titolo.

## 4. Musica e suoni

**Taglio in uso (23/09 sera, note di Franz delle 18:15 e delle 19:43):** `0.25-1 0-1 0-1 0-11 42-45 --fadein=3`. La musica parte col video e sale dal silenzio in 3 battiti; il colpo della parte forte resta al 27. La battuta 42, che nel brano precede il finale, porta la parte forte fino al 59. Il finale piano (43) sta sotto la tapparella e il colpo finale (44) cade al 63, quando la tapparella si apre e nasce il logo; al 67, quando arriva il nome, è già silenzio. Il taglio e la tabella qui sotto sono quelli del primo design.

**Taglio** dalla traccia originale (scheda: 110 bpm, primo battito a 0,025 s): battute `0-1`, `0-11`, `9-10`, `11-12`, `43-45`. Sono 16 battute, 64 battiti: la musica entra al battito 7 del film, due battiti dopo la notifica, e finisce con l'ultimo.

| Battiti del film | Traccia |
|---|---|
| 0–7 | silenzio: il titolo in ambient, poi la notifica |
| 7–27 | introduzione, 5 battute: la battuta 0 ripetuta, poi 1–3; nell'ultima la musica si ferma a 25,25–25,75 mentre il «yes» riempie il quadro, e risale mentre l'orologio laterale sale |
| 27 | colpo della parte forte, sulla card «Deployed» |
| 27–59 | parte forte: battute 4–10, poi la 9 ripetuta (55–59), un giro A/B intero: il primo blink cade sulla sua battuta; 12 dB sotto le voci |
| 59–63 | battuta quieta (11) con la sua risalita, sotto la tapparella che si chiude |
| 63–71 | il finale vero (43–44) sotto il cartello, fino all'ultimo fotogramma |

**Suoni:** gli stessi del film lungo: tocchi, pressione lunga, soffio all'apertura del terminale, tick delle righe. La notifica è quella del Pixel, una volta. Voci: `question.wav` e `say.wav`, già registrate.

## 5. Architettura

- **Composizione `Short`** accanto a `Film`, con la sua scaletta (`timeline.short.json`) e la sua musica (`music.short.wav`); motore parametrico, tavolozza per scaletta. Fatto nei piani 1-2.
- **Il film lungo non cambia:** ogni pezzo nuovo è un campo opzionale della scaletta, senza valori di default che tocchino le scene esistenti. Verifica: i 4 fotogrammi di regressione identici dopo ogni pezzo.
- **Piano 3, la struttura** (una bozza intera alla fine):
  1. apertura: ambient a luce ferma già dal fotogramma 0 e già nella colonna di destra, titolo sopra, che resta fino alla notifica; il respiro dell'ambient si spegne con un campo della scaletta;
  2. scene spostate secondo la sezione 3; via «done» e «shipped»;
  3. la card ✓ di payments-api ferma in cima alla lista (senza ancora il volo dal terminale): l'esito c'è fin dalla prima bozza;
  4. carrellata in tre scene con i blink del film lungo, solo palpebre;
  5. scheda Context con le barre che diventano la tapparella, e la tapparella che apre il cartello: la tapparella del film lungo dura 7 battiti, qui 6 (il minimo) a cavallo del taglio, da 59,5 a 65,5;
  6. cartello: logo ritagliato e cinturino che esce dal bordo, come opzioni che il film lungo non usa, e un passo del cartello che parte mentre i listelli si voltano («Claude Master» a 2 battiti dal taglio, avvisi a 4);
  7. musica tagliata secondo la sezione 4.
- **Piano 4, il movimento** (una bozza dopo ogni punto, per giudicarli uno alla volta), in ordine d'impatto:
  1. (3) takeover al contrario: la finestra del terminale entra nel display e diventa la card ✓;
  2. (2) terminale in prospettiva con la camera che scorre;
  3. (6) tasti che si staccano dal vetro;
  4. (5) parallasse della corsia;
  5. (7) camera che respira;
  6. (8) onda della voce dal ▶;
  7. (1) «It asks.» che arriva da sfocata a nitida sul tremito;
  8. (10) sfocatura di movimento, prima sull'espansione del «yes», poi sul volo del terminale: si misura il costo di resa e si confronta il colore prima e dopo; resta solo se Franz la approva.
- **Consegna:** `deliver.sh` riceve il nome della composizione. Due versioni: piena e sotto i 10 MB.

## 6. Il 3D dell'orologio

La differenza principale rispetto agli esempi è l'orologio che ruota nello spazio. Oggi l'orologio è fotografato da 4 angolazioni fisse.

**Strada scelta: B.** Il modello si anima dentro Remotion (`@remotion/three`), con la camera nella stessa timeline del racconto e la nostra UI sul display come texture. Non si usano sequenze rese in Blender, perché ogni cambio di tempi chiederebbe una nuova resa.

**Prova prima di decidere**, con la scansione del Pixel Watch 4 già in casa: `stampa-3d/riferimenti/cgtrader-scan-pw4/GooglePixelWatchScan.stl`, 965 108 triangoli, senza cinturino. Le inquadrature scelte sono quelle dove il cinturino non si vede: fronte e tre quarti ravvicinati.
- **T0:** scansione ridotta a circa 50k e circa 150k triangoli, vetro come superficie separata con UV, export GLB. Con Blender senza schermo; i file in `tools/promo/materiali/3d-prova/`, fuori da git.
- **T1:** una composizione di prova fuori dal film. Orbita da tre quarti a fronte in 90 fotogrammi. Si misurano i secondi a fotogramma a 1080p in `swangle` e con `--gl=angle`.
- **T2:** il fronte 3D alla stessa misura della foto frontale, affiancati e ad alternanza, più un taglio foto → 3D → foto.
- **T3:** un fotogramma EEVEE di Blender, come metro di realismo.

**Soglie:**
- al massimo circa 10 s a fotogramma;
- Franz non distingue il 3D dalla foto, oppure accetta lo scarto;
- licenza valida.

**Provenienza della scansione** (Franz, 23/09): comprata sullo store di Bambu Lab per un altro progetto, nonostante il nome della cartella dica CGTrader. Prima di usarla fuori dalla prova vanno lette le condizioni di quell'acquisto. Se la prova regge, per la versione finale c'è il Pixel Watch 5 45 mm Matte Black di 3DModels.org: 50 €, licenza Standard, da tenere fuori da git.

**Dove servirebbe:** l'orologio che si gira verso di noi mentre si sveglia (battiti 0–5) e che si allontana nel cartello (battiti 60–68).

## 7. Verifica

1. `npm run check` verde, con tutte e due le scalette; ogni pezzo con i suoi test scritti prima.
2. Il film lungo identico: i 4 fotogrammi di regressione, pixel per pixel, dopo ogni pezzo.
3. Una bozza a mezza risoluzione con audio alla fine del piano 3 e dopo ogni punto del piano 4.
4. Misure sulla bozza: il colpo della musica sulla card «Deployed»; lo stacco dentro l'espansione del «yes»; la tapparella sull'inizio del finale; l'ambient senza variazioni di luce prima della notifica.
5. Il file finale: 38,7 s, −14 LUFS, picco ≤ −1 dB, sincronia 0,0 ms, versione leggera sotto i 10 MB, nessuno scatto dell'AAC.

## 8. Fuori perimetro

- Il 3D dell'orologio (9): aspetta la scelta di Franz sul modello.
- Effetti WebGL (11), 3D fotorealistico da reel (12), particellari, lampi di luce, lettere che esplodono.
- Il ✓ che diventa il logo: nel finale nuovo la tapparella lega le barre al marchio.

Si riaprono solo su richiesta di Franz.
