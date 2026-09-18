# Revisione del film: dove serve il 3D vero (`@remotion/three`)

Chiesta da Franz il 18/09 alle 21:49. Passata in rassegna tutta la scaletta (14 scene, 152 battiti) chiedendo a ogni
passaggio una sola cosa: **il CSS 3D che già uso (`perspective`, `rotateX`, ombre) basta, o manca qualcosa che solo una
camera vera dà?** Solo tre punti rispondono «manca».

Cosa dà il 3D vero e il CSS no: la **camera** (un punto di vista che si muove e cambia la prospettiva di tutto insieme),
il **parallasse** fra oggetti a profondità diverse, la **luce** che scorre sugli spigoli mentre un pezzo ruota, e il
**video come materiale** (`useOffthreadVideoTexture`: la registrazione vera dell'orologio diventa la superficie di un
oggetto che si piega, si spezza, si allontana). Il CSS deforma un rettangolo; non illumina e non fa volare una camera.

## Rassegna, passaggio per passaggio

| Passaggio | Oggi | Verdetto |
|---|---|---|
| open-3 → glance | parola che cresce, occhio che si chiude e riapre | resta com'è: il momento è pulito e già approvato |
| glance → list | la card lascia il display e resta in mano | CSS basta: un solo oggetto, una sola inclinazione |
| list → asks | la card si posa, il display si scuote | CSS basta |
| asks → speaks | l'onda nasce dal ▶ | 2D: è un tracciato, il 3D non aggiunge |
| speaks → answer | le parole diventano le righe | CSS basta |
| answer → loop | il tasto «1 · yes» prende il quadro e diventa fondo | resta: è il modello dei passaggi, già forte |
| loop → watch | l'ultima scheda della corsia cresce | CSS basta (un oggetto solo) |
| **watch → accounts** | il terminale riempie il quadro, poi stacco sul testo | **3D — «il muro degli account»** |
| accounts → limits | testo pieno, poi la Panoramica | resta: lo stacco netto serve |
| **limits → new** | le due barre si allungano e si dissolvono (bocciato da Franz) | **3D — «la tapparella»** |
| new → close | tuffo dentro lo schermo verso il nero | CSS basta |
| **close** | logo che compare | **3D — il segno estruso** |

## I tre momenti da costruire

### 1. La tapparella (fine Panoramica → «Start the next one») — fatta, primo giro il 18/09 22:25
Le due barre del Context non si dissolvono: **diventano i primi due listelli di una tapparella**. Tutto il quadro —
l'orologio con la registrazione vera sopra, presa come texture — si divide in dodici strisce orizzontali che ruotano
sul loro asse, sfalsate, con la luce che corre sullo spigolo mentre girano; dietro, già in posa, c'è la scena dopo.
Perché è calzante: la Panoramica È una pila di schede; la pila si volta e scopre quello che viene dopo.
Perché serve il 3D: le strisce devono avere spessore e prendere luce, e la camera deve restare ferma mentre girano —
in CSS diventano rettangoli che si schiacciano, senza spigolo.

**Secondo giro (Franz, 22:27-22:28): il collegamento fra il grafico e la transizione non si vedeva.** Rifatto l'inizio con
la sua proposta — il quadro si svuota in dissolvenza (orologio, titolo, numeri) e restano **sole le due barre**, che si
allungano dal loro bordo sinistro e si ingrossano da 16 a 90 px fino a essere listelli; poi fanno un accenno di voltata
(`wink`) e solo dopo nascono gli altri. Tempi: barre sole fino al 34 % dell'arco (1,6 s), accenno 34-44 %, nascita degli
altri 40-66 %, voltata dal 68 %. Il taglio con la scena dopo cade al 66 %, a tapparella chiusa. La Panoramica passa da 32
a 35 battiti (film 84,5 s) perché il pannello Context deve restare in scena prima che la tapparella lo prenda.

### 2. Il muro degli account («Watch it work» → «Every account. One view.»)
Il pannello del terminale, che a quel punto riempie il quadro, resta una lastra sospesa; **la camera arretra** e la
lastra si scopre una di molte, disposte su una griglia leggermente curva, ognuna con il suo account (`Usr`, `Pro`),
le più lontane sfocate. Le parole «Every account. One view.» si compongono davanti al muro, e la camera torna dentro
una sola lastra per entrare nella Panoramica.
Perché serve il 3D: è tutto parallasse e profondità di campo. In CSS le lastre lontane sono solo più piccole, e si vede.
Nota tecnica (da decidere prima di costruirlo): le lastre hanno bisogno di una superficie che si legga come schermo. Tre
strade, in ordine di costo: (a) `useOffthreadVideoTexture` con la registrazione vera su una lastra sola, colore piatto
sulle altre; (b) una `CanvasTexture` disegnata una volta (righe monospazio, nomi di account diversi) e riusata su tutte;
(c) mesh di barre sottili che da lontano leggono come righe. La (b) è la più convincente a parità di lavoro.

### 3. La chiusura — costruita in prova, non ancora nel film
Il segno dell'app **estruso**: nasce di taglio (si vede solo lo spessore, una linea), ruota fino a mostrarsi di faccia
e nel farlo prende la luce corallo; il nome compare sotto. Sostituisce la comparsa piatta di oggi, che Franz ha già
segnalato come slegata.

## Costi e rischi
- `@remotion/three@4.0.490` installato (334 pacchetti, `three` compreso). Il render resta su `--gl=egl`: è il renderer
  che serve a WebGL (misurato: 37 s contro 97 s ogni 30 fotogrammi).
- Regola della skill ufficiale: dentro `<ThreeCanvas>` **niente `useFrame()`**, ogni animazione guidata da
  `useCurrentFrame()`, altrimenti il rendering sfarfalla. Le coreografie restano funzioni pure con i loro test.
- Per la texture dal video serve `useOffthreadVideoTexture` (non `useVideoTexture`, che vale solo nell'anteprima).
- Rischio: tre momenti in 3D su quattordici scene sono un accento, non uno stile. Se diventassero cinque, il film
  cambierebbe faccia e perderebbe il filo «la UI diventa la scena».
