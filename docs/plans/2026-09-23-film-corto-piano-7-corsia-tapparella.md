# Film corto, piano 7: corsia intera, terminale acceso, quarto blink, tapparella a strisce, card sopra la cornice

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (i piani del corto si eseguono in linea). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** le note di Franz sulla bozza 15 (23/09, 22:17-22:22) e il punto importante della revisione finale dei piani 4-6.

**Spec:** `docs/plans/2026-09-23-film-corto-design.md` §«2 bis», più queste decisioni di Franz:
- la corsia riprende «It keeps you in the loop.» e la card «Running the staging checks for 2.8.0», come nel film lungo; «Deployed» resta sul colpo (+4 battiti, +2,2 s);
- l'orologio del terminale è acceso dal primo fotogramma, non in ambient;
- un quarto blink allo stesso passo prima della tapparella; dopo, restano solo le tre barre del Context, che crescono senza pause fino a tutto schermo;
- nella seconda fase la tapparella si volta a strisce alternate celesti e nere, e finisce tutta nera;
- la card del terminale passa SOPRA la cornice dell'orologio entrando nello schermo.

**Architecture:** scaletta e musica per la corsia e il blink; una clip col display acceso per il terminale (`n_watch_on.mp4`); nel motore, tutto con campi opzionali che il film lungo non usa: `blinds.grow: "linear"` e `blinds.fill` (i listelli partono dalla lunghezza dei dati e crescono di seguito), il voltarsi a 180° con il retro nero quando `blinds.to` è `"black"`, e una terza copia del volo sopra l'orologio. La posizione del volo si prende dagli stessi numeri del transform dell'orologio (zoom, tremito, spostamento verticale), non da un conto a parte.

## Global Constraints

- Il film lungo resta identico al pixel (13 fotogrammi più i due della tapparella, 1940 e 1990 con swangle).
- Battiti nuovi: faccia 0-1,5 · notifica 1,5-5,5 · ▶ 5,5-12 · risposta 12-19 · corsia 19-37 (colpo e «Deployed» al 24) · terminale 37-47,5 · lista 47,5-52 · Work 52-54 · Open questions 54-56 · Context 56-58 · barre 58-61 · slogan 61-68 · cartello 68-77,5 (accordo al 68). 77,5 battiti, 42,3 s.
- Musica: `0-1 0-4 3-11 40-45 --fadein=3`: la battuta 3 due volte, il primo stop sul «yes» (18,25), il secondo 1,75 battiti prima del colpo (22,25), il colpo al 24, il finale piano 64-68 sotto lo slogan, l'accordo al 68, silenzio dal 72.

## Review Focus

1. «Deployed» in evidenza sul colpo al 24; dettatura, voce e invio agli stessi intervalli di prima rispetto al colpo.
2. Il primo fotogramma del terminale ha già lo schermo acceso, senza righe che anticipano il PC.
3. Dal blink al 58 alle barre che crescono: nessuna sosta, velocità costante fino a tutto schermo.
4. La seconda fase: strisce alternate celesti e nere, tutto nero alla fine, nessun lampo chiaro, nessun salto verso lo slogan.
5. La card sopra la cornice mentre entra; l'orologio visibile sul taglio; l'arrivo sotto il vetro senza scatto.

---

### Task 1: la corsia intera e i battiti nuovi

**Files:** `src/film/timeline.short.json`, `src/film/short.test.ts`, `src/film/ui/heroes.ts` (+ test: `laneTimes`), `tools/promo/remotion/public/audio/music.short.wav` (fuori dal repo).

- [ ] Test: `laneTimes(cards, width)` restituisce la frazione della corsia in cui ogni card è in evidenza, con le formule di `Floating.tsx`; nel corto «Deployed» è in evidenza al colpo (24), le card prima di lei sono quelle del film lungo; dettatura (tocco a colpo + 3,5), voce (colpo + 4), invio (colpo + 12,5); durata 77,5; il colpo al 24 e lo stop a 22,25 e 18,25.
- [ ] Scaletta: corsia 19-37, card del film lungo con soste 0,66 e 0,74, dettatura con sosta 5,4; tutto dopo spostato di 4.
- [ ] Musica: `0-1 0-4 3-11 40-45 --fadein=3`, misurata a battiti.

### Task 2: il terminale acceso dal primo fotogramma

**Files:** `tools/promo/screen_on.py` (+ `test_screen_on.py`), `tools/promo/remotion/public/scenes/n_watch_on.mp4` (fuori dal repo), `src/film/timeline.short.json`, `src/film/short.test.ts`.

- [ ] Test (Python, clip sintetica): i fotogrammi neri in testa diventano il primo fotogramma con contenuto, con la banda delle righe nera; dal primo con contenuto in poi la clip è quella di prima.
- [ ] Script e clip: `python3 ../../../screen_on.py n_watch_pinned.mp4 n_watch_on.mp4` (da `public/scenes`); la banda delle righe fra il titolo «Terminal · 17:33» e il tasto Write.
- [ ] Scaletta: la scena `watch` usa `n_watch_on.mp4`; test del terminale aggiornato.

### Task 3: il quarto blink e le barre che crescono di seguito

**Files:** `src/film/timeline.ts` (`BlindsCue.grow?: "linear"`, `fill?: number[]`, validazione), `src/film/ui/blinds.ts` (+ test), `src/film/ui/Blinds.tsx`, `src/film/Film.tsx`, `src/film/timeline.short.json`, `src/film/short.test.ts`.

- [ ] Test: con `grow: "linear"` la larghezza di un listello-barra cresce a velocità costante dalla lunghezza del dato (`fill` × 760) a tutto schermo, senza accenno di voltata; senza, come prima. Context 56-58 con `out: "blink"`; scena `bars` 58-61 senza orologio, con la tapparella di 5 battiti che parte subito dopo il blink.
- [ ] Motore e scaletta.

### Task 4: la tapparella a strisce che finisce nera

**Files:** `src/film/ui/blinds.ts` (+ test), `src/film/ui/Blinds.tsx`.

- [ ] Test: con `to: "black"` i listelli si voltano di 180° (il retro è nero), a righe alterne sfalsate, tutti voltati alla fine (p 1), senza dissolvenza; senza `to`, come prima.
- [ ] Motore: materiale diverso sul fronte (celeste) e sul retro (nero); film lungo identico sui fotogrammi della tapparella.

### Task 5: la card sopra la cornice, con la posizione dell'orologio

**Files:** `src/film/ui/takeIn.ts` (+ test), `src/film/ui/TakeIn.tsx`, `src/film/Film.tsx`, `src/film/timeline.ts`.

- [ ] Test: `takeInFrontAt(p)`: 0 all'inizio (sul taglio si vede l'orologio), 1 mentre la finestra attraversa la cassa, 0 alla fine (l'arrivo è sotto il vetro); la posizione del volo (`dx`, `dy`, `u`) è quella del transform dell'orologio (funzione condivisa); `takeIn` solo con l'orologio di fronte.
- [ ] Motore: terza copia sopra l'orologio (con i Heroes), stessa finestra.

### Task 6: la bozza 16

- [ ] Resa intera (tutti i segmenti cambiano), controlli su colpo, blink, tapparella, arrivo; film lungo sui 15 fotogrammi; consegna con l'avanzamento.
