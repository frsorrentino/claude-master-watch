# Film corto: design (documento A)

23/09/2026. Il corto si aggiunge al film da 81,8 s, che resta com'è. Le decisioni sono di Franz, prese il 22 e il 23/09 una per una. Questo documento le raccoglie insieme all'analisi del movimento. L'analisi completa (inventario degli strumenti con la documentazione ufficiale, confronto con i quattro film di esempio) è stata consegnata a Franz il 23/09 alle 00:40.

## 1. Obiettivo

In 37 s, lo stile motion graphic dei film di prodotto di Apple e Google: scene di effetto, sempre al servizio del racconto, con passaggi fluidi da una scena all'altra. Il messaggio principale arriva nei primi 3 secondi.

**Il criterio di Franz (23/09):** i movimenti nello spazio sono il cuore della richiesta, non un extra:
- la camera che si muove;
- oggetti e card in prospettiva;
- scene che si compongono un pezzo alla volta;
- un elemento che porta al successivo.

Nei film di esempio la continuità conta più dell'effetto: Ask ha 2 tagli in 87 s, LangEase nessuno in 33 s.

## 2. Decisioni prese

| Tema | Decisione | Quando |
|---|---|---|
| Rapporto con l'84 s | Film separato; l'84 s (oggi 81,8 s) resta com'è | 22/09 15:27 |
| Apertura | L'azione: il polso vibra e arriva la domanda; il titolo entro 2,7 s | 22/09 15:30 |
| Tavolozza | Un solo blu notte, quello di «It speaks», in tutte le scene. Si passa al bianco e nero cambiando pochi valori. Nessun momento chiaro: il «yes» vira nel blu mentre cresce | 22/09 15:33 |
| Struttura | Una storia sola, in tre blocchi, ogni inquadratura usata una volta sola | 22/09 15:45, 15:54 |
| Chiusura | Card ✓ con una seconda vibrazione, poi «Shipped. From your wrist.»; nel cartello il ✓ diventa il logo (versione A) | 22/09 16:21; 23/09 00:35 |
| Testo glance | «Every session, at a glance.» | 22/09 21:04 |
| Musica | «Brick By Brick», 110 bpm, taglio nuovo (sezione 4) | 22/09 20:55, 20:59 |
| Notifica | Il Tethys del Pixel, lo stesso del film lungo. Senza riga di credito | 22/09 20:59 |
| Loudness | Guadagno costante più limitatore (`audio/normalize.py`); AAC con il coder «fast» | 22/09 21:34; 23/09 01:40 |
| 3D | Prova con la scansione gratuita del Pixel Watch 4 prima di decidere. Se regge, Franz compra il modello del Pixel Watch 5 per la versione finale (sezione 6) | 23/09 01:21 |

## 3. Scaletta (72 battiti = 39,3 s a 110 bpm)

**Modifica di Franz, 23/09 07:10, dopo la bozza 2:** il terminale torna intero, come nel film lungo, e le scene dopo si accorciano per fargli posto. Le righe sotto sono già quelle nuove.

Le frasi seguono la regola del film: le parole entrano a mezzo battito l'una e la frase resta ferma 2 battiti.

| Battiti | s | Scena | Cosa si vede | Testo | Movimento (analisi, priorità) |
|---|---|---|---|---|---|
| 0–2 | 0–1,1 | risveglio | orologio frontale, display spento, silenzio | — | la camera si avvicina lentamente (1) |
| 2–5 | 1,1–2,7 | asks | vibrazione con la notifica Pixel; il display si accende sulla domanda | «It asks.» | la camera si ferma sul tremito |
| 5–10 | 2,7–5,5 | titolo | domanda ferma sul quadrante | «Claude Code, on your wrist.» | la camera arretra e scopre l'orologio (1) |
| 10–20 | 5,5–10,9 | speaks | tocco su ▶ al battito 4, la voce della domanda da 4,5 | «It speaks.» | l'onda della voce nasce dal ▶ (8) |
| 20–27 | 10,9–14,7 | answer | «1 · yes» / «2 · no»; pressione lunga al battito 24; il «yes» cresce e riempie il quadro mentre la musica si ferma (lo stacco, 26,25–26,75) | — | i tasti si staccano dal vetro (6); il «yes» cresce e vira nel blu |
| 27–41 | 14,7–22,4 | corsia | orologio laterale che sale sulla risalita; card «Deployed 2.8.0, smoke tests green» **sul colpo della musica, al battito 28** (Franz, 23/09 11:28: il colpo va sull'animazione dopo la pressione, non sulla pressione); «Say what's next.»; dettatura con la voce; ✓ | «Say what's next.» | parallasse e carrellata verso l'alto (5); il ✓ apre il terminale |
| 41–51,5 | 22,4–28,1 | lavoro | terminale del PC e orologio frontale, con le righe e i tempi del film lungo | «Watch it work.» | terminale in prospettiva, la camera scorre accanto al cursore (2) |
| 51,5–56 | 28,1–30,5 | glance | la lista delle sessioni sul polso | «Every session, at a glance.» | il terminale rimpicciolisce e diventa la card di payments-api; la lista si compone (3) |
| 56–60 | 30,5–32,7 | fatto | card ✓ «Released 2.8.0…» con la seconda vibrazione | — | il segno ✓ si disegna (4) |
| 60–64 | 32,7–34,9 | shipped | orologio con la card ✓ | «Shipped. From your wrist.» | la camera arretra (1) |
| 64–72 | 34,9–39,3 | cartello | l'orologio rimpicciolisce e sale; logo, «Free. Open source.», repo, avvisi | — | il ✓ diventa il logo (4) |

**Su tutto il film:** la camera respira in ogni inquadratura ferma, con una deriva dell'1–2 % (7). Sfocatura di movimento solo sui 2–3 scatti veloci, dopo una prova su un tratto (10).


## 4. Musica e suoni

**Taglio** dalla traccia originale (scheda: 110 bpm, primo battito a 0,025 s): battute `0-1`, `0-1`, `0-11`, `11-13`, `43-45`. Sono 17 battute, 68 battiti: la musica parte al battito 4 del film e finisce con l'ultimo. La battuta 0 suonata tre volte allunga l'introduzione di una battuta, così il colpo cade sull'entrata della corsia (28) e non sulla pressione del «yes»; da lì in poi la traccia scorre com'è.

| Battiti del film | Traccia |
|---|---|
| 0–4 | silenzio: solo la notifica |
| 4–28 | introduzione, 6 battute: la battuta 0 tre volte, poi 1–3; nell'ultima la musica si ferma a 26,25–26,75 e risale |
| 28 | colpo della parte forte, sulla card «Deployed» della corsia |
| 28–56 | parte forte, battute 4–10, 12 dB sotto le voci |
| 56–60 | battuta quieta (11), sulla card ✓ |
| 60–64 | ripresa forte (12), su «Shipped.» |
| 64–72 | il finale vero (43–44) sotto il cartello, fino all'ultimo fotogramma |

**Suoni:** gli stessi del film lungo: tocchi, pressione lunga, soffio all'apertura del terminale, tick delle righe. La notifica è quella del Pixel, due volte. Voci: `question.wav` e `say.wav`, già registrate.

## 5. Architettura

- **Composizione `Short`** accanto a `Film`, con la sua scaletta (`timeline.short.json`) e la sua musica (`music.short.wav`). La scaletta passa per le stesse regole di `validateTimeline` e per `check.ts`.
- **Il motore diventa parametrico.** Oggi `Film.tsx` legge una costante di modulo (`TIMELINE`, `GRID`); diventa un contesto passato dalla composizione.
  - **Verifica:** l'84 s deve restare identico, fotogramma per fotogramma, su un campione di fotogrammi resi prima e dopo il riordino.
- **Tavolozza per scaletta.** Un campo `palette` sostituisce `ACT_BG` per quella scaletta. Il corto usa lo stesso blu per tutti gli atti.
- **Pezzi nuovi**, ciascuno con i suoi test scritti prima:
  1. `sleep` che parte già al buio;
  2. camera che si avvicina e arretra per scena (zoom da / a, con curva);
  3. terminale in prospettiva con la camera che lo segue;
  4. takeover al contrario (il terminale diventa una card sul display);
  5. card ✓ sul display con il tratto che si disegna (`@remotion/paths`, `evolvePath`);
  6. il ✓ che diventa il logo (`interpolatePath` sul logo SVG);
  7. tasti in profondità;
  8. parallasse della corsia;
  9. camera che respira (`@remotion/noise` o le curve che ci sono).
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

1. `npm run check`: verde, con tutte e due le scaletta.
2. L'84 s identico dopo il riordino del motore (campione di fotogrammi confrontati pixel per pixel).
3. I fotogrammi chiave del corto, uno per scena, approvati da Franz prima della resa intera.
4. Anteprima a mezza risoluzione con audio.
5. Il file finale: 37,1 s (o 39,3), −14 LUFS, picco ≤ −1 dB, sincronia 0,0 ms, versione leggera sotto i 10 MB, nessuno scatto dell'AAC (decodificato confrontato con il WAV).

## 8. Fuori perimetro

Effetti WebGL (`@remotion/effects`, da installare e mai provati qui), particellari, lampi di luce, lettere che esplodono, 3D fotorealistico da reel. Si riaprono solo su richiesta di Franz.
