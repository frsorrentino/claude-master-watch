# Film corto, piano 4 (primo pezzo): il volo del terminale

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (i piani del corto si eseguono in linea, come il piano 3). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** fra il terminale e «Every session, at a glance.» la finestra del terminale rientra nell'orologio e diventa la card ✓ di payments-api in cima alla lista, al posto dello stacco con dissolvenza (Franz, 23/09 20:10 e 20:15: «versione completa»).

**Architecture:** un nuovo momento forte della scena «lista», `fx` di tipo `takeIn`, disegnato dai Heroes sopra l'orologio e sotto la frase. Parte dal quadro intero, col terminale della scena prima fermo al suo stato finale, e arriva al rettangolo della card nella riga `slot` del display, calcolato dalla posa viva dell'orologio come i tasti del «yes». La riga dell'esito vola a parte e si scioglie nel testo della card; all'arrivo la card ✓ (`doneCard`) prende il posto del volo con lo stesso aspetto e disegna il ✓. Funzioni pure in `ui/takeIn.ts`, componente in `ui/TakeIn.tsx`.

**Tech Stack:** Remotion 4.0.490, React, TypeScript, `node:test`.

**Spec:** `docs/plans/2026-09-23-film-corto-design.md`: tabella delle scene, riga 50,5–55 («la finestra del terminale si rimpicciolisce ed entra nel display come card di payments-api in cima alla lista… con il ✓ che si disegna mentre si posa»), e §5, piano 4, punto 1 («takeover al contrario»).

## Global Constraints

- Il film lungo resta identico al pixel: solo campi nuovi e opzionali. Controllo su f150, f475, f499, f524, f850, f900, f1300, f1682, f1710, f1737, f1800, f1840 (rumore noto: 2-7 px sui bordi delle barre del Context), f2300.
- Il terminale del corto resta quello del film lungo, più una riga finale con l'esito.
- La scaletta del corto non cambia durata né tagli: il volo sta dentro la scena «lista» (50,5–55), la musica non si tocca.
- La frase «Every session, at a glance.» resta sopra la finestra in volo e parte a 50,5 come ora.
- Durata del volo: 2 battiti. Il takeover del film lungo ne vuole almeno 2,5 (Franz, 18/09: «servono animazioni che prendano più tempo»), ma nella lista dopo il volo la card deve restare in quadro 2 battiti prima del blink: se Franz lo vuole più lento, si allunga la scena «lista» e si ritaglia la musica.
- Commenti e test in italiano, commit in inglese, `git add` solo per nome.

## Review Focus

1. L'arrivo: nel fotogramma in cui la card ✓ prende il posto del volo, posizione e misura coincidono al pixel.
2. La partenza: al primo fotogramma del volo il quadro è quello dell'ultimo fotogramma del terminale (niente salto).
3. La riga della card nel display: dal primo all'ultimo fotogramma del volo la riga vera sotto («Running the staging checks…») non si vede mai.
4. La frase della lista si legge sopra la finestra che si ritira.
5. Il film lungo: nessuna scena usa `takeIn`, nessun fotogramma cambia.

---

### Task 1: la riga dell'esito nel terminale del corto

**Files:**
- Modify: `tools/promo/remotion/src/film/timeline.short.json` (scena `watch`, `fx[0]`: `lines`, `times`)
- Test: `tools/promo/remotion/src/film/short.test.ts` (test «il terminale del corto è quello del film lungo…»)

**Interfaces:**
- Produces: l'ultima riga del terminale del corto, `"⏺ Released 2.8.0 and tagged v2.8.0"` al battito 9,5 della scena (49,5 nel film); il Task 3 ne legge il testo.

- [ ] **Step 1: il test.** Nel test del terminale, al posto del confronto intero: le righe del corto sono quelle del lungo più l'esito, i tempi quelli del lungo più 9,5, tutto il resto uguale.

```ts
  const tc = (corto.fx ?? []).find((f) => f.kind === "terminalPlane") as { lines: string[]; times: number[] };
  const tl = (lungo.fx ?? []).find((f) => f.kind === "terminalPlane") as { lines: string[]; times: number[] };
  // il corto chiude il lavoro con l'esito, che poi vola nell'orologio (piano 4, volo del terminale)
  assert.deepEqual(tc.lines, [...tl.lines, "⏺ Released 2.8.0 and tagged v2.8.0"]);
  assert.deepEqual(tc.times, [...tl.times, 9.5]);
  const senza = (s: typeof corto) => ({ ...s, watch: { ...s.watch!, clip: "" }, fx: (s.fx ?? []).map((f) => (f.kind === "terminalPlane" ? { ...f, lines: [], times: [] } : f)) });
  assert.deepEqual(senza(corto), senza(lungo));
```

- [ ] **Step 2:** `node --test src/film/short.test.ts` → FAIL: le righe del corto sono 8.
- [ ] **Step 3:** nella scaletta del corto, scena `watch`: aggiungere la riga a `lines` e `9.5` a `times`.
- [ ] **Step 4:** `npm run check` → tutto verde.
- [ ] **Step 5:** still del corto al fotogramma del battito 50,3 (`beatToFrame`): la riga si legge in fondo alle altre; misurare il suo rettangolo nel quadro (serve al Task 3 come controllo della partenza).
- [ ] **Step 6:** commit `feat(promo): the short's terminal ends on the outcome line`.

### Task 2: le funzioni pure del volo

**Files:**
- Create: `tools/promo/remotion/src/film/ui/takeIn.ts`
- Test: `tools/promo/remotion/src/film/ui/takeIn.test.ts`

**Interfaces:**
- Produces:
  - `type Rect = { x: number; y: number; w: number; h: number; r: number }` (angolo in alto a sinistra, pixel del quadro);
  - `slotRect(slot: number, dx: number, dy: number, u: number, lines: number): Rect`: la card della riga `slot` (x 26, larga 428, raggio 42, alta `24 + 36 + LIST_BODY.shift + lines·LIST_BODY.line + 24.5` unità) nel quadro, col display centrato in (`dx`, `dy`) e `u` pixel per unità;
  - `takeInAt(p: number): { shrink: number; terminal: number; line: number; card: number }`, `p` 0-1 lungo il volo;
  - `takeInRect(to: Rect, p: number, frame: { w: number; h: number }): Rect`: dal quadro intero (raggio 0) a `to`.

- [ ] **Step 1: i test.**

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { slotRect, takeInAt, takeInRect } from "./takeIn.ts";
import { LIST_BODY } from "./UiTokens.ts";

const F = { w: 1920, h: 1080 };
test("la card della riga 146 nel quadro: stesso conto di toFrame, alta quanto la card con due righe", () => {
  const r = slotRect(146, 1325, 540, 1.344, 2);
  assert.equal(r.x, 1325 + (26 - 240) * 1.344);
  assert.equal(r.y, 540 + (146 - 240) * 1.344);
  assert.equal(r.w, 428 * 1.344);
  assert.equal(r.h, (24 + 36 + LIST_BODY.shift + 2 * LIST_BODY.line + 24.5) * 1.344);
  assert.equal(r.r, 42 * 1.344);
});
test("il volo parte dal quadro intero e arriva esattamente sulla card", () => {
  const to = slotRect(146, 1325, 540, 1.344, 2);
  assert.deepEqual(takeInRect(to, 0, F), { x: 0, y: 0, w: 1920, h: 1080, r: 0 });
  assert.deepEqual(takeInRect(to, 1, F), to);
});
test("in volo la card resta sempre dentro la finestra: la riga vera sotto non si vede mai", () => {
  const to = slotRect(146, 1325, 540, 1.344, 2);
  for (let i = 0; i <= 40; i++) {
    const r = takeInRect(to, i / 40, F);
    assert.ok(r.x <= to.x + 1e-9 && r.y <= to.y + 1e-9 && r.x + r.w >= to.x + to.w - 1e-9 && r.y + r.h >= to.y + to.h - 1e-9, `p ${i / 40}`);
  }
});
test("il terminale sparisce nella prima metà, la card compare nella seconda, la riga dell'esito vola in mezzo", () => {
  assert.equal(takeInAt(0).terminal, 1);
  assert.equal(takeInAt(0.4).terminal, 0);
  assert.equal(takeInAt(0.5).card, 0);
  assert.equal(takeInAt(0.9).card, 1);
  assert.equal(takeInAt(0).line, 0);
  assert.equal(takeInAt(1).line, 1);
  assert.equal(takeInAt(1).shrink, 1);
});
```

- [ ] **Step 2:** `node --test src/film/ui/takeIn.test.ts` → FAIL: il modulo non c'è.
- [ ] **Step 3:** `ui/takeIn.ts`: `shrink = soft(p)` (parte decisa e si posa lunga: il bordo sinistro libera in fretta la colonna della frase); `terminal = 1 - soft(ramp(p, 0, 0.4))`; `line = soft(ramp(p, 0.05, 0.85))`; `card = soft(ramp(p, 0.55, 0.9))`; `takeInRect` interpola i quattro bordi e il raggio con `shrink`; `slotRect` come nei test.
- [ ] **Step 4:** test verdi.
- [ ] **Step 5:** commit `feat(promo): the take-in flight as pure functions`.

### Task 3: il volo nella scena della lista

**Files:**
- Modify: `tools/promo/remotion/src/film/timeline.ts` (tipo `Fx`: `{ kind: "takeIn"; at: number; len: number; slot: number; name: string; age: string; text: string; badge?: string }`; validazione: la scena prima ha un `terminalPlane`, `len` almeno 1,5, `slot` numero fra 0 e 310)
- Modify: `tools/promo/remotion/src/film/ui/Heroes.tsx` (`isHero` riconosce `takeIn`; nuovo prop `prev?: Scene`), `tools/promo/remotion/src/film/Film.tsx` (passa `prev` ai Heroes)
- Create: `tools/promo/remotion/src/film/ui/TakeIn.tsx`
- Modify: `tools/promo/remotion/src/film/timeline.short.json` (scena `list`: `takeIn` a 0 per 2 battiti; `doneCard` a 2)
- Test: `tools/promo/remotion/src/film/short.test.ts`, `tools/promo/remotion/src/film/timeline.test.ts`

**Interfaces:**
- Consumes: `slotRect`, `takeInAt`, `takeInRect` (Task 2); la riga finale del terminale (Task 1); `UiClaudeCode`, `CC` (terminale), `UiCard` con `body` (card), `LIST_BODY`.

- [ ] **Step 1: i test.** In `short.test.ts`: la scena «lista» ha un `takeIn` a 0 lungo 2 battiti; la `doneCard` parte quando il volo arriva (`at` = 2), stessa riga, stesso testo, stesso badge; il testo del volo è l'ultima riga del terminale senza «⏺ ». In `timeline.test.ts`: un `takeIn` in una scena senza terminale prima è un errore («il volo parte dal terminale della scena prima»); `len` 1 è un errore («il volo dura almeno 1,5 battiti»).
- [ ] **Step 2:** i test falliscono.
- [ ] **Step 3:** il componente. Dentro i Heroes (stessi `dx`, `dy`, `u` dei tasti del «yes»): una finestra assoluta al rettangolo `takeInRect(slotRect(slot, dx, dy, u, 2), p)`, angoli `r`, `overflow: hidden`, fondo `UI.surface` (è lo stesso grigio del terminale); dentro, scalato di `w/1920` dall'angolo in alto a sinistra, il terminale della scena prima com'è al suo ultimo fotogramma (`UiClaudeCode`: intestazione, prompt, tutte le righe accese tranne l'esito, riga di stato), con opacità `terminal`; la card (`UiCard` con `LIST_BODY`, ✓ non disegnato) scalata di `w/(428u)`, con opacità `card`; sopra, la riga dell'esito che va dal suo posto nel terminale al posto del testo della card, in Cousine che sfuma in Roboto a metà, e si scioglie nel testo della card fra 0,6 e 0,85.
- [ ] **Step 4:** `npm run check` → verde.
- [ ] **Step 5:** still del corto al primo fotogramma del volo, a un terzo, a due terzi, all'ultimo e al primo della card ✓: controllare i punti 1-4 della Review Focus; all'arrivo la differenza fra l'ultimo fotogramma del volo e il primo della card ✓, nel rettangolo della card, deve essere zero.
- [ ] **Step 6:** film lungo sui 13 fotogrammi: identico.
- [ ] **Step 7:** commit `feat(promo): the short's terminal flies into the watch and becomes the done card`.

### Task 4: la bozza 12

- [ ] **Step 1:** tratti 3D con `gl3d.py` sulla scaletta del corto, resa a segmenti (egl, swangle per il 3D), audio a parte, montaggio in `tools/promo/out/review/corto-bozza-12.mp4`.
- [ ] **Step 2:** controlli: fotogrammi e durata uguali alla bozza 11; foglio dei fotogrammi del volo.
- [ ] **Step 3:** consegna a Franz con l'avanzamento aggiornato.
