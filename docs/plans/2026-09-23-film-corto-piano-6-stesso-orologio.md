# Film corto, piano 6: lo stesso orologio fra terminale e lista

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (i piani del corto si eseguono in linea). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** fra il terminale e la lista l'orologio non sfuma e non cambia: resta fermo al suo posto, il terminale gli entra nello schermo e diventa la card ✓ (Franz, 23/09 21:13: «tra l'orologio del terminale e quello della card c'è ancora una dissolvenza, invece l'effetto voluto è che sia lo stesso»).

**Architecture:** il volo del piano 4 si disegna in due copie della stessa finestra. La prima sta nel quadro, DIETRO l'orologio: si vede attorno all'orologio e sparisce dietro cassa, lunetta e cinturino. La seconda sta DENTRO il display, sotto il vetro, ritagliata dal cerchio dello schermo: è la parte della finestra che «entra» nello schermo. Le due copie hanno lo stesso rettangolo, perché il passaggio dal quadro al display è affine: interpolare nel quadro e poi convertire equivale a convertire e poi interpolare. All'arrivo la copia nel display è la card ✓ nello stesso punto e sotto lo stesso vetro della `doneCard`, quindi il cambio è netto e invisibile, senza dissolvenza. Nell'ultimo battito del terminale lo schermo sfuma nel grigio del terminale (`watch.screenFade`): al taglio lo schermo mostra già quello che la finestra copre.

**Tech Stack:** Remotion 4.0.490, React, TypeScript, `node:test`.

**Spec:** `docs/plans/2026-09-23-film-corto-design.md`, sezione «2 bis», punto «Stesso orologio fra terminale e lista».

## Global Constraints

- Il film lungo resta identico al pixel (13 fotogrammi più i due della tapparella, come nel piano 5).
- Il terminale del corto resta quello del film lungo, salvo la clip col tasto fermo, la riga dell'esito e la sfumatura dello schermo al posto della dissolvenza dell'orologio.
- Tempi e battiti del corto non cambiano (piano 5).

## Review Focus

1. Sul taglio (fotogrammi 707-712 della bozza 13, battito 43,5) l'orologio non si muove, non sfuma e non scatta.
2. Lo schermo: app del terminale, poi grigio, poi la finestra che si ritira, poi la lista attorno alla card; mai un buco, mai il blu dello sfondo dentro lo schermo.
3. L'arrivo: fra l'ultimo fotogramma del volo e il primo della card ✓ la differenza nella card è solo il ✓ che comincia a disegnarsi.
4. Fuori dall'orologio la finestra si ritira come prima; dietro cassa e cinturino non si vede.
5. Il film lungo non cambia.

---

### Task 1: lo schermo del terminale sfuma nel grigio

**Files:** `src/film/timeline.ts` (`WatchCue.screenFade?: number`, validazione), `src/film/ui/takeIn.ts` (+ test: `screenFadeAt`), `src/film/Film.tsx` (overlay del display), `src/film/timeline.short.json` (scena `watch`: via `fadeOut`, `screenFade: 1`), `src/film/short.test.ts`, `src/film/timeline.test.ts`.

- [ ] **Step 1: i test.** `screenFadeAt(frame, total, frames)`: 0 prima degli ultimi `frames` fotogrammi, 1 all'ultimo, crescente. La scaletta: `screenFade` numero positivo, al più la durata della scena. Il corto: la scena `watch` non ha `fadeOut` e ha `screenFade` 1; il test «il terminale del corto è quello del film lungo» ammette queste due differenze.
- [ ] **Step 2:** falliscono.
- [ ] **Step 3:** nel display della scena, sopra la clip e sotto gli effetti, un disco del colore del fondo della scena (`bgFrom`, il grigio del terminale) con opacità `screenFadeAt`.
- [ ] **Step 4:** verdi; commit `feat(promo): the short's terminal screen fades into the terminal grey instead of the watch fading out`.

### Task 2: il volo in due copie, dietro l'orologio e dentro lo schermo

**Files:** `src/film/ui/takeIn.ts` (+ test: `toDisplay`), `src/film/ui/TakeIn.tsx` (due componenti: quadro e display), `src/film/Film.tsx` (il volo nel quadro prima dell'orologio, la copia nel display insieme agli effetti), `src/film/ui/Heroes.tsx` (il volo non è più lì).

- [ ] **Step 1: i test.** `toDisplay(r, dx, dy, u)`: un rettangolo del quadro in unità del display (x `(r.x−dx)/u+240`, misure divise per `u`); per `slotRect` torna esattamente x 26, y `slot`, larghezza 428, raggio 42; convertire il rettangolo del volo a ogni `p` è uguale a interpolare fra il quadro convertito e la card in unità del display.
- [ ] **Step 2:** falliscono.
- [ ] **Step 3:** `TakeInFrame` (la finestra e la riga nel quadro, disegnate prima dell'orologio) e `TakeInScreen` (la stessa finestra e la stessa riga nello spazio 480 del display, dentro l'overlay); fino a `p` 1, poi niente: la `doneCard` c'è già. Tolta la dissolvenza d'arrivo (`HANDOFF`).
- [ ] **Step 4:** verdi; still del corto ai fotogrammi 705-712, 740, 760, 780 e all'arrivo; film lungo sui 15 fotogrammi; commit `feat(promo): the terminal flies behind the same watch and into its screen`.

### Task 3: la bozza 14

- [ ] **Step 1:** resa a segmenti (solo il primo cambia: riusare tratto 3D, coda e audio della bozza 13 se i fotogrammi e i suoni dopo il 900 non cambiano), controlli, consegna.
