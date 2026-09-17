# Film promozionale «Il rilascio 2.8.0, dal polso»: progetto

Approvato da Franz sezione per sezione il 17/09/2026 (14:39-18:34). Sostituisce, per la parte di montaggio, il piano del
16/09 (`2026-09-16-video-promo-piano.md`): restano valide la storia del rilascio 2.8.0 e la demo pilotata via adb. Il
primo montaggio muto di stamattina (`0e56c50`, 50 s, sei scene) è il punto di partenza, non il risultato.

## 1. Scopo e destinazione

- **Dove vive**: la pagina dell'app sul sito di Franz, con player (chi guarda preme play). Derivati: README di GitHub
  (MP4 muto sotto i 10 MB, o GIF) e, più avanti, un taglio corto. Social: da valutare in seguito, fuori da questo progetto.
- **Perché**: Franz pubblica gratis questi lavori anche per misurare il livello professionale raggiungibile con questi
  strumenti. Il riferimento sono i film di prodotto di Google e Apple: testi animati, ritmo sulla musica, prodotto vero.
- **Forma**: film in tre atti da 60-70 s (scelta «C»); dagli stessi componenti, dopo, un taglio da 35-45 s a carrellata
  di funzioni. 1920×1080, 30 fps, H.264 + AAC. Testi a schermo in inglese; nessuna versione italiana prevista.

## 2. Copione (~66 s, musica a ~100 battiti al minuto: una battuta = 2,4 s)

Le entrate si scrivono in battiti; le durate sono indicative finché non c'è la traccia.

| Tempo | Momento | Sullo schermo dell'orologio | Testo a schermo | Effetto |
|---|---|---|---|---|
| 0-7 s | Apertura | nessun orologio, fondo nero | «Claude is working.» / «You're not at your desk.» / «That's fine.» | una parola per battito; sull'ultima riga l'orologio sale in quadro (tre quarti), il quadrante si accende; titolo piccolo «Claude Master · for Wear OS» |
| 7-24 s | **Atto 1, Sapere** | 1. lista delle sessioni che scorre | «Every session. One glance.» | |
| | | 2. arriva la notifica della domanda | «It asks.» | due anelli di vibrazione dalla cassa |
| | | 3. si preme ▶ | «It speaks.», poi le parole della domanda una a una | si SENTE l'orologio leggere; musica abbassata |
| 24-46 s | **Atto 2, Agire** | 4. pressione lunga su «yes» | «You answer.» | arco che si chiude attorno alla cassa |
| | | 5. pressione lunga sulla card, campanella | «Follow what matters.» | |
| | | 6. esito «Deployed 2.8.0, smoke tests green» | «Know when it's done.» | facoltativo: ▶ e la voce dice solo «Deployed 2.8.0 to production.» |
| | | 7. dettatura | «Say what's next.» | la frase dettata si compone in grande, con cursore |
| | | 8. terminale dal vivo | «Watch it work.» | le righe nuove escono dallo schermo, grandi, monospazio |
| 46-58 s | **Atto 3, Controllare** | 9. quota e ritmo | «Know your limits.» | «11 %» enorme che conta da zero |
| | | 10. nuova sessione su storefront | «Start something new.» | |
| 58-66 s | Chiusura | quadrante, orologio di tre quarti che rimpicciolisce | «Claude Master» · «Free. Open source.» · indirizzo del repository · in piccolo «An independent project, not affiliated with Anthropic» | |

La voce legge esattamente ciò che legge l'app (`SpeechText.question`): la domanda e le opzioni numerate, «…Deploy
version 2.8.0 to production now? 1, yes. 2, no.». Con una voce di prova dura 11,4 s (misurato): in produzione si chiede
un ritmo più svelto o si tagliano le opzioni, decidendo sull'audio vero. Lo schermo durante la lettura è fermo, quindi la
scena si allunga o si accorcia senza rifare la registrazione.

## 3. Linguaggio visivo

**Testo.** Entrata a maschera parola per parola (sale da una fessura, frenata morbida; esce verso l'alto). Una parola in
colore per frase (blu pastello dell'app), il resto bianco caldo. Due grandezze sole: titolo (~110 px su 1080) e riga di
servizio. Carattere Inter (OFL, incluso nel progetto: Google Sans è proprietario e non si usa); per il terminale Noto Sans
Mono (OFL), parente del monospazio di sistema che l'app usa sull'orologio. Testi che «escono» dall'orologio: frase
dettata, righe del terminale, contatore.

**Orologio: mockup fotografico** (Franz, 16:32-18:34; prove in `tools/promo/out/prototipi-mockup/`, fuori da git).
- Due inquadrature dalle foto di Franz al suo Pixel Watch: **tre quarti** dal lato della corona per apertura e chiusura,
  **frontale** per le scene in cui si legge l'interfaccia. La cassa disegnata del primo montaggio resta come riserva.
- **Frontale**: scontorno «per costruzione». La cassa è un cerchio misurato sulla foto (280 punti sul bordo dell'anello
  scuro, scarto medio 0,45 px) più lo smusso lucido esterno, largo ~8 px (a metà risoluzione): uno scontorno a soglia lo
  tagliava ai lati perché riflette il tessuto chiaro. Lo smusso si «riaccende» per la scena scura (è uno specchio: nel
  film riflette il nostro fondo). Cinturino e anse da soglia sul nero, contorno lisciato; corona con maschera propria
  (l'ombra sotto di lei ha la luminosità del metallo: soglia bassa + chiusura). I pixel di bordo prendono il colore
  dell'interno. Polvere: i pixel molto più chiari della mediana locale nelle zone scure si sostituiscono con la mediana
  (scanalature, corona e riflessi grandi restano).
- **Vetro**: nel frontale la cupola riflette il telefono che scatta, quindi il vetro è **sintetico**: disco nero sopra
  tutta la cupola tranne il bordo vero, video dell'interfaccia, ombra interna, alone in alto a sinistra, finestra
  sfocata, filo di luce sul bordo, lama di luce che scorre con l'inclinazione. Nel tre quarti il telefono non c'è: si
  tengono i **riflessi veri** della foto sopra l'interfaccia, in fusione «schermo».
- **Display**: raggio dell'interfaccia 0,86 del raggio del vetro (i profili di luminosità a schermo spento mettono il
  bordo del pannello a ~0,85; i bordi sono più sottili di come li avevo disegnati). Valore esatto da uno scatto frontale
  a schermo acceso, quando capita.
- **Prospettiva del tre quarti**: da un modello di camera (focale dai dati di scatto, posa ricavata dall'ellisse del
  vetro: ~20 cm, 34°). Le ore 12 seguono il cinturino, le ore 3 la corona; il centro del cerchio proiettato NON è il
  centro dell'ellisse (32 px più verso il lato lontano); convergenza ~11 %. L'interfaccia prende un filo di morbidezza e
  di grana, per non sembrare incollata.
- Il contorno del tre quarti si traccia a mano come percorso vettoriale, una volta: lì l'ombra sul tessuto ha la stessa
  luminosità del metallo e nessuna soglia regge.

**Movimento.** L'orologio è un'immagine piatta nello spazio: avvicinamento fin dentro lo schermo, allontanamento,
scivolata laterale tra gli atti, deriva lenta nelle pause, inclinazione entro 8-10 gradi (oltre sembra finto). Gesti resi
visibili: punto del tocco, arco della pressione lunga, anelli della vibrazione. Nelle scene di lettura il cinturino esce
dal quadro sopra e sotto, così il display resta almeno al 55-60 % dell'altezza.

**Montaggio.** Niente dissolvenze semplici: tagli sul battito o passaggi di continuità. Un colore di fondo per atto (blu
notte, viola profondo, verde-petrolio; nero in apertura e chiusura). Ogni effetto è un componente Remotion; una scaletta
JSON in battiti li comanda (scaletta sbagliata = errore di compilazione). Cambiare traccia o fare il taglio corto vuol
dire cambiare scaletta, non componenti.

## 4. Audio

Limite che decide tutto: **Claude non sente**. Si misura tutto il misurabile; il gusto passa da tre ascolti di Franz.

- **Musica**: elettronica calda e minimale, 95-110 battiti, introduzione rada per l'apertura e una salita dove compare
  l'orologio. 3-4 candidate da Pixabay (licenza: uso in un'opera originale anche su sito e GitHub, senza attribuzione),
  ognuna con scheda misurata: tempo, curva d'energia, mappa degli atti. Tagliata sulle battute (introduzione, corpo, salto
  al finale vero): finisce con il film, non sfuma. La traccia NON entra nel repo pubblico (la licenza vieta di
  ridistribuirla da sola): nel repo il riferimento e la prova della licenza.
- **Suoni d'interfaccia**: Material Design di Google (CC-BY 4.0: riga di credito su pagina e README). Rintocco + colpo
  basso alla notifica, tick sui tocchi, tono che sale e si chiude nella pressione lunga, tick per riga di terminale e
  contatore, un soffio solo sui titoli grandi. Al massimo un suono per battito, tutti ben sotto la musica.
- **Voce dell'orologio**: sintesi di Gemini (`gemini-3.1-flash-tts-preview`, 30 voci, stile a parole). Google non
  rivendica la proprietà dell'audio e non chiede attribuzione; sul piano gratuito i testi possono servire a migliorare i
  prodotti (i nostri sono dimostrativi); i termini chiedono il piano a pagamento a chi offre client dell'API a utenti
  europei, che non è il nostro caso: con la fatturazione attiva la spesa è meno di un centesimo. Scartato ElevenLabs
  gratuito (obbliga «elevenlabs.io» nel titolo e vieta l'uso commerciale); riserva locale Kokoro (Apache 2.0).
  Trattamento «piccolo altoparlante» miscelato con la voce pulita, in due intensità. La voce parte sul fotogramma in cui
  ▶ diventa ■; i tempi parola per parola (whisper, già installato) guidano il testo che compare mentre viene detto. Nei
  crediti: «synthetic voice».
- **Mix**: dentro Remotion, sulla stessa scaletta. Musica −12 dB sotto la voce con rampe morbide; finale a −14 LUFS,
  picco −1 dB. Versione muta per il README dallo stesso render.
- **Narratore**: non entra. Se alla fine Franz vuole provarlo: stesso servizio, altra voce, solo negli spazi liberi.
- **Gemini come «orecchie»: provato e bocciato** (17/09 15:30, budget chiuso). Sette campioni a risposta nota, due
  passate, rotta media di fable-director con contratto d'ascolto (la regola che vieta i giudizi sull'audio sostituita solo
  in un involucro nello scratchpad, con il sì di Franz). `gemini-3.6-flash` era sovraccarico (503): 13 risposte su 14 da
  `gemini-2.5-flash`. Giusto: assenza di voce (nessun parlato inventato) e trascrizione (13/14), cioè quello che whisper
  fa già in locale. Sbagliato in entrambe le passate: voce 12 dB SOTTO la musica («voce dominante, 5/5»), voce filtrata
  («pulita»), voce distorta («pulita», poi «da piccolo altoparlante»), musica abbassata di 12 dB («costante»), tempo
  (vero 100: risposte 120, 125, 128, 130, 60, 0). Le misure locali coprono proprio quelle dimensioni. Da ripetere su
  3.6-flash quando torna disponibile (materiale in `tools/promo/out/prova-orecchie/`); nessuna richiesta al plugin.

## 5. Costruzione

| Blocco | Cosa | Serve l'orologio | Serve Franz |
|---|---|---|---|
| **Foto** | spostare le foto in una cartella materiali fuori da git; scontorno, vetro, posa come moduli in `tools/promo/mockup/`; percorso a mano del tre quarti; immagini di prova | no | (eventuale secondo giro di scatti: luce di finestra, cartoncino bianco, vetro pulito, uno scatto a schermo acceso bianco) |
| **B. Motore** | componenti Remotion, griglia dei battiti, scaletta JSON, sfondi, cartello finale; clip di stamattina come segnaposto | no | **punto 1**: tre fotogrammi di stile + 10 s di movimento muto, con il confronto cassa disegnata / foto |
| **C. Audio** | tracce candidate con scheda, suoni, voce in 2-3 varianti, mix, misure | no | **punti 2 e 3**: scelta della traccia, scelta della voce |
| **A. Momenti mancanti** | «New session» (i tocchi sui progetti non hanno effetto: da indagare con metodo), pressione lunga su «yes» (opzione mezza fuori dal bordo), Segui sulla card, conferma dopo il messaggio dettato (nella clip non compare: difetto vero o tocco mancato?). L'esito che non arrivava come notifica dipende dalla domanda non risolta: stessa notifica, «avvisa una volta sola» | sì, sessioni brevi | no |
| **D. Registrazione** | un momento per volta (la connessione cade: tre volte in una notte), copione che trova i bersagli dall'albero dell'interfaccia e verifica il testo atteso dopo ogni gesto; ripristino completo alla fine | sì, ~30 min, fuori dal caricatore (in carica le notifiche non si aprono a schermo) | **punto 4**: lingua di sistema in inglese, quadrante neutro con la nostra complication, debug wireless acceso |
| **E. Montaggio e consegne** | scaletta con le clip vere, musica tagliata, mix, verifiche, poi il taglio corto | no | **punto 5**: revisione del film finito, immagine e mix |

Ordine: Foto e B subito (B è il rischio maggiore: l'aspetto), C in parallelo, A quando l'orologio è disponibile, D dopo
A, E alla fine. Mai render e Gradle insieme (VM da 6 GB); ogni processo lungo staccato dalla sessione (`setsid nohup`),
perché Claude Code ferma i comandi in background quando giudica bassa la memoria. Render completo ~20 minuti: solo ai
punti di controllo; il resto per scena, con anteprime e tavole di fotogrammi. Sul Python di sistema `scipy` non funziona
con il numpy installato: misure con numpy e il modulo `wave`.

## 6. Verifiche

- Motore: `tsc --noEmit`; scaletta validata alla compilazione; una tavola di fotogrammi chiave per scena; prova della
  griglia: un lampo a ogni battito confrontato con gli attacchi misurati nella musica (scarto sotto i 20 ms, giunta
  compresa).
- Registrazione: asserzioni sul testo atteso dopo ogni gesto (albero dell'interfaccia), in modalità prova.
- Audio: trascrizione automatica della voce da sola e del mix (tutte le parole ritrovate sopra la musica); distacco
  voce/musica ≥ 10 dB misurato sulle tracce separate; fattore di cresta contro la distorsione (voce pulita 14,7 dB,
  distorta 4,4); energia in banda per il filtro; −14 LUFS ±0,5, picco ≤ −1 dB; voce sul fotogramma di ▶→■.
- Film finito: tavola a un fotogramma al secondo; lettura ottica dei fotogrammi contro una lista di parole italiane e di
  dati personali; dimensioni dei file; bordi del mockup guardati a 3-4× (è lì che Franz ha trovato i difetti).
- Modifiche all'app (blocco A): test nel core dove c'è logica, prova al polso per il resto, come sempre.

## 7. Rischi e aspettativa

- Livello raggiungibile: un film da prodotto indipendente curato. Non il render 3D dell'hardware, non musica composta su
  misura, non la voce di un attore.
- L'aspetto si decide presto (punto 1), prima di costruire tutte le scene.
- Foto scattate in interno a ISO alti: grana e luce piatta, che la riduzione a 1080p in gran parte nasconde; un secondo
  giro di scatti migliora il risultato ma non blocca.
- Gemini TTS è in anteprima: può cambiare; c'è la riserva locale.
- Marchi: mostrare il prodotto vero per dire «gira qui» è prassi comune e per un progetto libero il rischio è basso, non
  nullo. Nei crediti: «Wear OS by Google and Pixel Watch are trademarks of Google LLC»; nessun logo Google in vista;
  niente immagini ufficiali di Google. Esiste il modulo di richiesta al loro ufficio marchi (risposta in almeno una
  settimana), se si vuole il dubbio a zero.
- Su YouTube alcune tracce Pixabay generano reclami automatici: si ricontrolla se e quando ci si va.

## 8. Consegne

`release-film.mp4` (1080p, audio, ~66 s, stima 12-18 MB) · taglio muto per il README (< 10 MB) · fotogramma-poster per il
player del sito · crediti (musica, suoni CC-BY, voce sintetica, carattere, marchi) · `docs/promo/licenses.md` con le prove
delle licenze · poi il taglio corto da 35-45 s. I render restano fuori dal repo (`tools/promo/remotion/out/`); dove
ospitare il file per il sito non è deciso. La sessione `francescosorrentino-com` ha già i dati tecnici per impaginare.

## 9. Fuori perimetro

Narratore, versione italiana, formati verticali e quadrati, pubblicazione su YouTube, render 3D vero, 60 fps e sfocatura
di movimento, suoni accordati alla tonalità della traccia. Si riaprono solo su richiesta di Franz.
