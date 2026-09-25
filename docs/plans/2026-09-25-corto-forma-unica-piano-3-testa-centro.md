# Corto «forma unica», piano 3: testa e centro (battiti 0-49,5) — piano di esecuzione

> **Per chi esegue:** `superpowers:executing-plans`, TDD dove c'è logica pura; guardie a pixel dove si toglie un disegno.
> Task 1-4 motore e contenuti (esecutore); Task 5-6 regia (sessione).

**Obiettivo:** la forma accesa su tutto il corto (`shape: { from: 0 }`): dal display del quadrante alla notifica, alla voce,
ai tasti, al «yes» che diventa sfondo, alle schede della corsia, alla dettatura, allo schermo dell'invio, al ✓ che si apre
sul terminale, e da lì alla card ✓ della lista (dove comincia il piano 2). I pezzi vecchi che la forma sostituisce non si
disegnano più (`inShape`), ma restano nella scaletta come fonte dei dati e dei suoni.

**Specifica:** `2026-09-25-corto-forma-unica-design.md` §2 (righe 0-47,5), §3, §8 (1a-c, 2, 4, 5). Piani 1-2 per le
interfacce di `shape.ts`, `Shape.tsx`, `ShapeContent.tsx`, `displayPlace.ts`.

## Vincoli globali

- Quelli dei piani 1-2. In più: la registrazione vera resta visibile sul display (la notifica si stacca come COPIA, §8.1a).
- Contenuti costruiti con i componenti che già disegnano quei pezzi (UiCard, UiOption, UiWave, SpokenWords,
  UiDictation, il corpo `screen` di Takeover): stesso aspetto, impaginati alla misura della chiave (controscala).
- `bump()` dei tasti → molla ζ 0,7 (§8.4) dentro il contenuto `options`.
- Le rese «prima» di testa e centro esistono già (`prima-loop.mp4`, `prima-watch.mp4`, `dopo-blur-*`): nessuna resa qui.

## Task 1: `show` — la forma che coincide col display senza coprirlo

- `ShapeKey.show?: number` (0-1, default 1): molla critica con pinza 0-1, `ShapeState.show`. La scatola si disegna con
  `opacity: show`, il contenuto con la sua opacità per `show`. A 0 la forma c'è (misura, posizione, camera) ma non copre:
  il display vero È la forma (face), o lo è il fondo della scena (il terminale grigio).
- Validazione: numero finito 0-1. Test: molla critica mai fuori 0-1; `show` 0 → opacità 0 nel disegno (test puro su una
  funzione `boxStyle(s)` o equivalente).

## Task 2: `inShape` sui pezzi vecchi

Un flag `inShape?: boolean` (come già su `aside` e `doneCard`) che toglie il disegno e lascia dati e suoni:
- `optionsBuild` (Heroes: i tasti fuori dal display);
- `spoken` (WatchText: parole e onda sotto il titolo) — la voce (audio) resta;
- `float`: sulle singole schede (`cards[i].inShape`): Floating non le disegna ma tiene i tempi della corsia (le scritte
  «It keeps you in the loop.» e «Say what's next.» restano dove sono);
- `takeIn` (il volo del terminale nella lista);
- `Scene.takeover.inShape`: Film non monta il `Takeover`; i suoni (`tick`, `whoosh`) e lo spegnimento della scena sotto
  (`underTakeover`) restano.
Guardia: con tutti i flag assenti `Short` è identico al pixel (fotogrammi 100, 250, 400, 560, 700).

## Task 3: contenuti della testa

In `ShapeContent.tsx`, ciascuno sul tempo della sua chiave (`beat − key.at`), dati dalla scena della chiave:
- `notify`: la notifica (intestazione con badge, «payments-api», età; testo «Staging is green. Deploy 2.8.0?»), inchiostro
  `inkOn(colore della chiave)`.
- `voice`: in alto il ▶ (cerchio pieno dell'inchiostro, triangolo nel colore della chiave), a destra le parole della voce
  (`SpokenWords` del `spoken` della scena) e sotto l'onda (`UiWave`); in basso le due opzioni piccole, «1 · yes» e «2 · no».
- `options`: i due tasti `UiOption` («1 · yes» primario, «2 · no») uno sotto l'altro nella scatola, che entrano con la
  molla ζ 0,7 (un solo scavalco del 4,6 %, §8.4) sfalsati di un quarto di battito.
- `yes`: il solo tasto «1 · yes» primario a tutta scatola, con l'anello della pressione lunga (`ring` di `UiOption`)
  che corre da `key.at` per la durata del `longPress` della scena.

## Task 4: contenuti del centro

- `card1`, `card2`: `UiCard` (w 428, scala `w/428`) con i dati delle schede della corsia (`float.cards` 1 e 2: nome,
  età, testo, badge, icona).
- `dict`: la dettatura come la disegna Floating (`UiDictation` con tocco sul microfono e parole di `say.wav`, sui tempi
  del `dictation` della scheda), a misura della chiave.
- `screen`: il corpo `screen` del Takeover (le righe di `takeover.words` e il ✓ grande), a tutto quadro.
- `check`: il ✓ bianco in un cerchio, a misura della chiave.
Guardie: nessuna (disegno nuovo); ogni contenuto con uno still a 0,25× su `ShapeBoard` o `Short` quando la regia avrà la
traccia (Task 5).

## Task 5 (regia): la traccia della testa e del centro

Chiavi (battiti; `rect` nel quadro salvo gli agganciati):
- 0 display agganciato, `show` 0, zoom 1;
- 2 notifica `[150, 690, 720, 170]` crema, `show` 1, `len` 1,5 (si stacca dal display: `show` sale mentre si stringe);
- 6 voce `[150, 470, 720, 420]`, gesto tap;
- 12 tasti `[460, 250, 1000, 560]`, fondo `#1b1f26`, `len` 1,5;
- 16 «yes» `[500, 294, 920, 239]` `#d3e3fd`, gesto longPress, `len` 1,5;
- 17,5 campo `#242a42`; 19 card1 (`len` 2,5); 22 card1 su; 24 card2; 25,5 card2 su; 26,5 dettatura; 34,5 schermo nero;
  36,5 ✓ (gesto send, `len` 0,5); 37 terminale a tutto quadro `#23272e` (`len` 1); 37,5 `show` 0 (lo è il fondo della
  scena); 47 `show` 1 (`len` 0,5: la finestra torna forma sopra il terminale); 47,5 card ✓ agganciata (piano 2).
- Scene: `inShape` su optionsBuild, spoken, schede della corsia (non le scritte), takeover di answer e loop, takeIn.
- `shape: { from: 0 }` nella composizione.

## Task 6 (regia): tavola, resa, confronto

Tavola per battito di tutto il corto a 0,25×, revisione con l'advisor, correzioni; resa completa del corto (una sola)
e prima/dopo intero con `affianca.sh`.
