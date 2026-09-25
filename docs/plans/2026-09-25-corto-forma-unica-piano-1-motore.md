# Corto «forma unica», piano 1: il motore della forma — piano di esecuzione

> **Per chi esegue:** sotto-skill richiesta `superpowers:executing-plans` (o `subagent-driven-development`), passi con
> caselle `- [ ]`. TDD su ogni pezzo: test rosso, codice, test verde, commit.

**Obiettivo:** la forma unica esiste come funzione pura del fotogramma (traccia `shape`, `shapeAt`, `contentAt`,
`cameraAt`, blur solo sulla scatola) con i suoi test, e si vede da sola su fondo piatto (composizione `ShapeBoard`)
senza cambiare di un pixel la composizione `Short` di oggi.

**Architettura:** tre moduli puri (`shape.ts`, `camera.ts`, `shapeDisplay.ts`) sopra `spring.ts`; un componente
`Shape.tsx` che li disegna; la validazione della traccia entra in `validateTimeline`. Il film monta forma e camera
solo con `shape` acceso (spento per `Short` fino al passo 3 della specifica).

**Stack:** TypeScript, React 19, Remotion 4.0.490, `@remotion/motion-blur`, test con `node --test` (TS nativo di Node,
import con estensione `.ts`).

**Specifica:** `docs/plans/2026-09-25-corto-forma-unica-design.md` (§5 motore, §3 testi, §4 camera, §8 decisioni
vincolanti). Chi esegue legge anche quella.

Cartella di lavoro: `tools/promo/remotion/`. Tutti i percorsi sotto sono relativi a lei.

## Vincoli globali

- Quadro 1920×1080, 30 fps; la griglia è `timeline.short.json` (110 bpm, `offsetSeconds` 0,01). Tempi della traccia in
  battiti, chiavi su mezzi battiti.
- Molle: `springEase` ζ 0,8 su posizioni, misure, raggio; `springSettle` (ζ 1) su opacità, colori, peso dell'aggancio,
  camera. Un valore di opacità o colore sopra 1 dà CSS non valido (handoff 25/09).
- Scambio del contenuto: esce il vecchio in 0,25 battiti (sfocatura 0→6 px, scivolo 0→−8 px, opacità 1→0), poi entra
  il nuovo in 0,25 battiti (sfocatura 6→0, scivolo +8→0, opacità 0→1). Mai sovrapposti.
- Il testo dentro la forma non scala con la forma (controscala): si impagina alla misura della SUA chiave.
- Camera: margine 12 % del quadro, scala fra 1 e 1,3; niente `will-change` sugli strati scalati.
- Motion blur solo sulla scatola (riempimento e bordo): 4 campioni a 180°, 8 dove un vertice si sposta più di 24 px in
  un fotogramma. Mai sul testo, mai sulle scene.
- `Short` resta identica: nessun cambio visibile finché `shape` non si accende (passo 3).
- Commenti nel codice in italiano, nello stile dei file vicini (perché, non cosa). Commit in inglese, `feat(promo): …` /
  `test(promo): …`, solo file per nome (mai `git add -A` o di cartella).
- Niente rese complete: solo `npm test`, `npx tsc --noEmit`, e per la tavola `remotion render` della sola `ShapeBoard`.
- Mai `pkill -f`/`pgrep -f` con un pattern che compare nella propria riga di comando.

## Da tenere d'occhio in revisione

1. Chiave agganciata al display mentre il display si muove (deriva, tremito): la forma deve restare IDENTICA al display
   (scarto < 0,01 px), non una copia che lo insegue in ritardo. Test in Task 1.
2. Due scambi di contenuto troppo vicini: la validazione deve rifiutarli (meno di 0,5 battiti fra le chiavi), non
   sovrapporre i testi. Test in Task 2.
3. Forma più grande del quadro (il «yes» che diventa sfondo): la camera non deve rimpicciolire né mostrare il bordo
   della scena. Test in Task 2.
4. Primo fotogramma e ultimo: la forma c'è con misura positiva anche prima della prima chiave e dopo l'ultima. Test in
   Task 1.
5. Colori in maiuscolo/minuscolo e con canali estremi (#000000, #FFFFFF): la miscela non esce da 0-255 e resta
   esadecimale valido. Test in Task 1.

---

### Task 1: `shape.ts` — stato e contenuto della forma, funzioni pure

**File:**
- Modifica: `src/film/spring.ts` (`Change` con `zeta` facoltativo)
- Modifica: `src/film/beats.ts` (`frameToBeat`)
- Crea: `src/film/shape.ts`
- Test: `src/film/shape.test.ts`, `src/film/spring.test.ts` (un caso in più)

**Interfacce prodotte:**
```ts
// beats.ts
export const frameToBeat = (g: Grid, frame: number): number;          // inverso continuo di beatToFrame (senza arrotondare)
// spring.ts
export type Change = { at: number; to: number; len: number; zeta?: number };   // zeta del singolo cambio, se no quello di springs()
// shape.ts
export type Rect = [number, number, number, number];                  // x, y, w, h in px del quadro, prima della camera
export type Gesture = "tap" | "longPress" | "swipe" | "send";
export type ShapeKey = { at: number; rect?: Rect; anchor?: "display"; r: number; color: string; content?: string;
  len?: number; ease?: "spring" | "settle"; zoom?: number; gesture?: Gesture };
export type ShapeState = { x: number; y: number; w: number; h: number; r: number; color: string; anchor: number };
export type Display = (beat: number) => Rect;                         // il rettangolo del display a quel battito
export type ContentState = { id: string; key: ShapeKey; opacity: number; blur: number; dy: number };
export const SWAP_OUT = 0.25, SWAP_IN = 0.25, SWAP_BLUR = 6, SWAP_SLIDE = 8, KEY_LEN = 1;
export const shapeAt: (keys: readonly ShapeKey[], beat: number, display: Display) => ShapeState;
export const contentAt: (keys: readonly ShapeKey[], beat: number) => ContentState | null;
export const keyRect: (k: ShapeKey, display: Display) => Rect;        // rect della chiave, o il display al suo battito
export const shapeSpeed: (keys: readonly ShapeKey[], beat: number, display: Display, beatsPerFrame: number) => number;
export const blurSamples: (speed: number) => 4 | 8;                   // 8 sopra 24 px/fotogramma
export const BLUR_FAST = 24;
```

**Regole di calcolo:**
- Rettangolo libero: per ogni componente (x, y, w, h) `springs(beat, keyRect(k0)[i], changes)` con un cambio per
  chiave successiva: `{ at: k.at, to: keyRect(k)[i], len: k.len ?? KEY_LEN, zeta: k.ease === "settle" ? 1 : ZETA }`.
  Raggio libero allo stesso modo sul campo `r`.
- Peso dell'aggancio `anchor`: `springs` su 0/1 (1 se la chiave ha `anchor`), sempre ζ 1, stessa `len`.
- Rettangolo finale = libero + (display(beat) − libero)·anchor; raggio finale = r libero + (display(beat).w/2 − r libero)·anchor.
  Con anchor = 1 esatto il risultato è ESATTAMENTE `display(beat)` (springEase vale 1 da t ≥ 1).
- Colore: tre canali, ognuno `springs` ζ 1 sui valori 0-255, arrotondati e limitati a 0-255, riscritti `#rrggbb` minuscolo.
- Prima della prima chiave: lo stato della prima chiave. Dopo l'ultima: lo stato di riposo dell'ultima.
- Contenuto: fra chiavi consecutive con `content` diverso (anche da/verso `undefined`) c'è uno scambio al battito della
  chiave nuova `b`: il vecchio esce in [b, b+SWAP_OUT] (opacità `1 − springSettle(p)`, sfocatura `SWAP_BLUR·p`,
  dy `−SWAP_SLIDE·p`), il nuovo entra in [b+SWAP_OUT, b+SWAP_OUT+SWAP_IN] (opacità `springSettle(p)`, sfocatura
  `SWAP_BLUR·(1−p)`, dy `SWAP_SLIDE·(1−p)`). Fuori dagli scambi: il contenuto dell'ultima chiave passata, opacità 1.
  `null` se quel contenuto è `undefined` o nella metà vuota dello scambio. `key` è la chiave che porta il contenuto
  (per impaginarlo alla sua misura).
- `shapeSpeed`: massimo spostamento dei quattro vertici fra `beat − beatsPerFrame` e `beat`, in px.

- [ ] **Step 1: test rossi** in `src/film/shape.test.ts` (import come `spring.test.ts`: `node:test`, `node:assert/strict`):

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { SWAP_IN, SWAP_OUT, blurSamples, contentAt, shapeAt, shapeSpeed } from "./shape.ts";
import type { Display, Rect, ShapeKey } from "./shape.ts";
import { beatToFrame, frameToBeat } from "./beats.ts";

const still: Display = () => [1000, 222, 636, 636];
const drifting: Display = (b) => [1000 + 3 * Math.sin(b), 222 + 2 * Math.cos(b * 1.7), 636, 636];
const keys: ShapeKey[] = [
  { at: 0, anchor: "display", r: 318, color: "#000000" },
  { at: 2, rect: [150, 300, 700, 160], r: 40, color: "#F4F2EC", content: "notify", gesture: "tap" },
  { at: 4, rect: [150, 300, 700, 420], r: 40, color: "#f4f2ec", content: "voice" },
  { at: 8, rect: [-100, -100, 2120, 1280], r: 0, color: "#FFFFFF", len: 2 },
  { at: 12, anchor: "display", r: 318, color: "#000000", ease: "settle" },
];
const near = (a: number, b: number, eps = 0.01) => assert.ok(Math.abs(a - b) < eps, `${a} ≠ ${b}`);

test("agganciata al display è IDENTICA al display anche mentre si muove", () => {
  for (let b = 0; b < 2; b += 0.05) {
    const s = shapeAt(keys, b, drifting), d = drifting(b);
    near(s.x, d[0]); near(s.y, d[1]); near(s.w, d[2]); near(s.h, d[3]); near(s.r, d[2] / 2);
  }
  const s = shapeAt(keys, 20, drifting), d = drifting(20);
  near(s.x, d[0]); near(s.w, d[2]);
});

test("fra due chiavi la forma va a molla e a riposo è l'ultima chiave", () => {
  const mid = shapeAt(keys, 2.5, still);
  assert.ok(mid.w > 636 * 0.5 && mid.h < 636, `a metà corsa ${mid.w}×${mid.h}`);
  const rest = shapeAt(keys, 3.99, still);
  near(rest.x, 150, 1); near(rest.h, 160, 1);
});

test("c'è sempre: misura positiva prima della prima chiave, dopo l'ultima e in ogni fotogramma", () => {
  const g = { bpm: 110, fps: 30, offsetSeconds: 0.01 };
  for (let f = -5; f <= beatToFrame(g, 16); f++) {
    const s = shapeAt(keys, frameToBeat(g, f), drifting);
    assert.ok(s.w >= 1 && s.h >= 1, `fotogramma ${f}: ${s.w}×${s.h}`);
  }
});

test("colori: esadecimale minuscolo valido, mai fuori da 0-255, anche fra nero e bianco", () => {
  for (let b = 0; b < 16; b += 0.03) assert.match(shapeAt(keys, b, still).color, /^#[0-9a-f]{6}$/);
  assert.equal(shapeAt(keys, 3.99, still).color, "#f4f2ec");
});

test("contenuto: il vecchio esce, poi entra il nuovo, mai due insieme", () => {
  assert.equal(contentAt(keys, 1), null);
  const out = contentAt(keys, 4 + SWAP_OUT / 2)!;
  assert.equal(out.id, "notify"); assert.ok(out.opacity < 1 && out.dy < 0 && out.blur > 0);
  assert.equal(contentAt(keys, 4 + SWAP_OUT + 0.001)?.id ?? "voice", "voice");
  const inn = contentAt(keys, 4 + SWAP_OUT + SWAP_IN / 2)!;
  assert.equal(inn.id, "voice"); assert.ok(inn.dy > 0 && inn.blur > 0);
  const hold = contentAt(keys, 6)!;
  assert.deepEqual([hold.id, hold.opacity, hold.blur, hold.dy], ["voice", 1, 0, 0]);
  assert.deepEqual(hold.key.rect, [150, 300, 700, 420] as Rect);
});

test("motion blur: 8 campioni solo sopra 24 px per fotogramma", () => {
  assert.equal(blurSamples(24), 4); assert.equal(blurSamples(24.1), 8);
  const bpf = 110 / 60 / 30;
  assert.equal(shapeSpeed(keys, 6, still, bpf), 0);                   // ferma
  assert.ok(shapeSpeed(keys, 8.3, still, bpf) > 24);                   // il campo che esplode
});

test("frameToBeat è l'inverso di beatToFrame", () => {
  const g = { bpm: 110, fps: 30, offsetSeconds: 0.01 };
  for (const b of [0, 0.5, 12, 73.5]) assert.ok(Math.abs(frameToBeat(g, beatToFrame(g, b)) - b) < 1 / 30 * 110 / 60);
});
```

E in `spring.test.ts`:
```ts
test("springs: ogni cambio può avere il suo smorzamento", () => {
  const v = (t: number) => springs(t, 0, [{ at: 0, to: 1, len: 1, zeta: 1 }]);
  for (let t = 0; t <= 1; t += 0.01) assert.ok(v(t) <= 1 + 1e-9);   // critico: mai sopra
});
```

- [ ] **Step 2:** `npm test 2>&1 | tail -20` → i nuovi test falliscono (moduli/esportazioni mancanti).
- [ ] **Step 3:** implementa `frameToBeat` (`((frame / g.fps) - g.offsetSeconds) * g.bpm / 60`), `zeta` per cambio in
  `springs` (`c.zeta ?? zeta`), e `shape.ts` secondo le regole sopra. Commento di testa del file: cos'è la forma
  (specifica §1) e perché è una somma di molle (funzione pura, mai ricreata fra le scene).
- [ ] **Step 4:** `npm test 2>&1 | tail -20` → tutti verdi, vecchi compresi.
- [ ] **Step 5:** commit `feat(promo): the shape as a pure function of the frame — springs per key, display anchor, content swap`
  con `git add src/film/spring.ts src/film/spring.test.ts src/film/beats.ts src/film/shape.ts src/film/shape.test.ts`.

### Task 2: `camera.ts`, `shapeDisplay.ts` e validazione della traccia

**File:**
- Crea: `src/film/camera.ts`, `src/film/camera.test.ts`
- Crea: `src/film/shapeDisplay.ts`
- Modifica: `src/film/shape.ts` (`validateShape`), `src/film/timeline.ts` (`shape?: ShapeKey[]` in `Timeline`, chiamata in `validateTimeline`)
- Test: `src/film/shape.test.ts` (validazione)

**Interfacce:**
- Consuma: `ShapeKey`, `Rect`, `Display`, `keyRect`, `shapeAt` (Task 1); `springs`, `ZETA` (`spring.ts`); `watchColumn`,
  `Timeline`, `totalBeats` (`timeline.ts`); `THEME` (`theme.ts`); `geo.front` (`mockup.geometry.json`).
- Produce:
```ts
// camera.ts
export const CAMERA_MARGIN = 0.12, CAMERA_MAX = 1.3;
export type Cam = { cx: number; cy: number; s: number };   // punto del quadro al centro dello schermo, e scala
export const cameraTarget: (rect: Rect, zoom?: number, W?: number, H?: number) => Cam;   // W 1920, H 1080
export const cameraAt: (keys: readonly ShapeKey[], beat: number, display: Display) => Cam;
export const cameraCss: (c: Cam, W?: number, H?: number) => string;   // con transform-origin 0 0
// shapeDisplay.ts
export const frontDisplayRect: (cx: number, cy?: number) => Rect;      // display frontale a riposo, centro (cx, cy=540)
export const restDisplay: (t: Timeline) => Display;                     // display della scena di quel battito, a riposo
// shape.ts
export const validateShape: (keys: unknown, total: number) => string[];   // problemi in italiano, [] se va bene
```

**Regole:**
- `cameraTarget`: `s = zoom ?? clamp(min(W(1−2m)/w, H(1−2m)/h), 1, CAMERA_MAX)`. Centro: il minimo spostamento dal centro
  del quadro che tiene il rect dentro il margine: per l'asse x `lo = x + w − (W/2 − mW)/s`, `hi = x + (W/2 − mW)/s`;
  `cx = lo ≤ hi ? clamp(W/2, lo, hi) : x + w/2`. Poi, prima di tutto il resto, `cx` si limita a `[W/(2s), W − W/(2s)]`
  (la camera non mostra mai fuori dalla scena; a s = 1 è sempre al centro). Uguale per y con H.
- `cameraAt`: `cx`, `cy`, `s` sono `springs` ζ 1 (la camera non scavalca) sui `cameraTarget(keyRect(k), k.zoom)` delle
  chiavi, stessa `len` delle chiavi.
- `cameraCss`: `translate(${W/2}px, ${H/2}px) scale(${s}) translate(${-cx}px, ${-cy}px)`.
- `frontDisplayRect`: lato `d = 2 · geo.front.displayR · THEME.frontGlassPx / (2 · geo.front.glassR)` (≈ 636 px),
  `[cx − d/2, cy − d/2, d, d]`. È lo stesso conto di `toFrame` in `Film.tsx`: il display a riposo.
- `restDisplay(t)`: la scena che contiene il battito (l'ultima se oltre), `frontDisplayRect(watchColumn(scena) · 1920)`.
  È il display «a riposo» per tavola e test; la geometria vera (pose, deriva, tre quarti) arriva ai passi 3-4.
- `validateShape`: traccia assente = nessun problema. Altrimenti, in italiano come gli altri messaggi di `timeline.ts`:
  array non vuoto; prima chiave a 0; `at` mezzi battiti, strettamente crescenti, minori di `total`; esattamente uno fra
  `rect` e `anchor`; `anchor` solo `"display"`; `rect` di quattro numeri finiti con w, h > 0; `r ≥ 0`; colore
  `/^#[0-9A-Fa-f]{6}$/`; `len > 0`; `ease` in {spring, settle}; `zoom` fra 1 e 1,3; `gesture` fra i quattro; uno
  scambio di contenuto a meno di `SWAP_OUT + SWAP_IN` battiti dalla chiave prima è un errore («testi sovrapposti»).
- `validateTimeline` aggiunge i problemi di `validateShape(t.shape, totalBeats(t))` con prefisso `forma: `.
  `shape.ts` NON importa `timeline.ts` (niente cicli): `shapeDisplay.ts` sì.

- [ ] **Step 1: test rossi.** `src/film/camera.test.ts`:

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { CAMERA_MARGIN, CAMERA_MAX, cameraAt, cameraTarget } from "./camera.ts";
import type { Rect, ShapeKey } from "./shape.ts";

const W = 1920, H = 1080;
const onScreen = (r: Rect, c: { cx: number; cy: number; s: number }) =>
  [(r[0] - c.cx) * c.s + W / 2, (r[1] - c.cy) * c.s + H / 2, r[2] * c.s, r[3] * c.s];

test("un rect piccolo si avvicina fino a 1,3 e resta dentro il margine", () => {
  const r: Rect = [1500, 700, 300, 120];
  const c = cameraTarget(r);
  assert.equal(c.s, CAMERA_MAX);
  const [x, y, w, h] = onScreen(r, c);
  assert.ok(x >= W * CAMERA_MARGIN - 0.5 && x + w <= W * (1 - CAMERA_MARGIN) + 0.5, `x ${x}..${x + w}`);
  assert.ok(y >= H * CAMERA_MARGIN - 0.5 && y + h <= H * (1 - CAMERA_MARGIN) + 0.5, `y ${y}..${y + h}`);
});

test("un rect più grande del quadro: scala 1, camera ferma al centro, niente bordo della scena", () => {
  const c = cameraTarget([-100, -100, 2120, 1280]);
  assert.deepEqual(c, { cx: W / 2, cy: H / 2, s: 1 });
});

test("la camera non mostra mai fuori dalla scena, a nessuna scala", () => {
  for (const r of [[0, 0, 200, 200], [1720, 880, 200, 200], [900, 0, 100, 50]] as Rect[]) {
    const c = cameraTarget(r);
    assert.ok(c.cx - W / (2 * c.s) >= -1e-6 && c.cx + W / (2 * c.s) <= W + 1e-6);
    assert.ok(c.cy - H / (2 * c.s) >= -1e-6 && c.cy + H / (2 * c.s) <= H + 1e-6);
  }
});

test("zoom esplicito: vince sulla misura (1 = camera ferma sul display del quadrante)", () => {
  assert.deepEqual(cameraTarget([1004, 222, 636, 636], 1), { cx: W / 2, cy: H / 2, s: 1 });
});

test("cameraAt va a molla critica: scala mai oltre il bersaglio, continua", () => {
  const keys: ShapeKey[] = [
    { at: 0, rect: [1004, 222, 636, 636], r: 318, color: "#000000", zoom: 1 },
    { at: 2, rect: [1500, 700, 300, 120], r: 40, color: "#ffffff" },
  ];
  let prev = cameraAt(keys, 0, () => [0, 0, 1, 1]);
  for (let b = 0; b <= 4; b += 1 / 16) {
    const c = cameraAt(keys, b, () => [0, 0, 1, 1]);
    assert.ok(c.s <= CAMERA_MAX + 1e-9 && c.s >= 1 - 1e-9);
    assert.ok(Math.abs(c.s - prev.s) < 0.1 && Math.abs(c.cx - prev.cx) < 80);
    prev = c;
  }
});
```

In `shape.test.ts`:
```ts
import { validateShape } from "./shape.ts";
test("validazione: una traccia buona passa, gli errori si elencano in italiano", () => {
  assert.deepEqual(validateShape(keys, 20), []);
  assert.deepEqual(validateShape(undefined, 20), []);
  const bad = validateShape([
    { at: 0.5, rect: [0, 0, 10, 10], anchor: "display", r: -1, color: "red" },
    { at: 0.5, rect: [0, 0, 0, 10], r: 0, color: "#000000", zoom: 2, gesture: "crown" },
    { at: 0.75, rect: [0, 0, 10, 10], r: 0, color: "#000000" },
  ], 20);
  for (const piece of ["prima chiave", "rect", "anchor", "colore", "raggio", "crescent", "zoom", "gesto", "mezzi battiti"])
    assert.ok(bad.some((m) => m.includes(piece)), `manca un errore su «${piece}»: ${bad.join(" | ")}`);
});
test("validazione: due scambi di contenuto a meno di mezzo battito sono testi sovrapposti", () => {
  const p = validateShape([
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "a" },
    { at: 0.5, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "b" },
    { at: 1, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "c" },
  ], 20);
  assert.deepEqual(p, []);   // 0,5 battiti bastano: con chiavi su mezzi battiti crescenti la sovrapposizione è impossibile,
  // ma la regola resta esplicita perché SWAP_OUT/SWAP_IN possono cambiare
  assert.ok(validateShape([
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "a" },
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "b" },
  ], 20).some((m) => m.includes("sovrappo")));
});
```
(I messaggi devono contenere le parole cercate: «prima chiave», «rect», «anchor», «colore», «raggio», «crescent…»,
«zoom», «gesto», «mezzi battiti».)

- [ ] **Step 2:** `npm test 2>&1 | tail -20` → rossi.
- [ ] **Step 3:** implementa `camera.ts`, `shapeDisplay.ts`, `validateShape`, il campo `shape` in `Timeline` e la chiamata in `validateTimeline`.
- [ ] **Step 4:** `npm test 2>&1 | tail -5` e `npx tsc --noEmit` → verdi, zero errori.
- [ ] **Step 5:** commit `feat(promo): camera that frames the shape with a fixed margin, shape track validation`
  (file per nome: `src/film/camera.ts src/film/camera.test.ts src/film/shapeDisplay.ts src/film/shape.ts src/film/shape.test.ts src/film/timeline.ts`).

### Task 3: `Shape.tsx`, la tavola `ShapeBoard` e l'innesto spento nel film

**File:**
- Crea: `src/film/Shape.tsx`, `src/film/ShapeBoard.tsx`
- Modifica: `src/film/Film.tsx` (prop `shape`), `src/Root.tsx` (composizione `ShapeBoard`)

**Interfacce:**
- Consuma: `shapeAt`, `contentAt`, `shapeSpeed`, `blurSamples`, `BLUR_SHUTTER` (180, da `Film.tsx` — spostalo in
  `shape.ts` come `SHAPE_SHUTTER = 180` e fai importare quello a `Film.tsx` solo se serve; altrimenti costante locale),
  `cameraAt`, `cameraCss`, `restDisplay`, `frameToBeat`, `gridOf`.
- Produce:
```tsx
export type ShapeContent = (id: string, w: number, h: number) => React.ReactNode;   // disegna il contenuto alla misura della sua chiave
export const Shape: React.FC<{ keys: readonly ShapeKey[]; g: Grid; display: Display; content?: ShapeContent }>;
export const CameraFrame: React.FC<{ keys: readonly ShapeKey[]; g: Grid; display: Display; children: React.ReactNode }>;
export const ShapeBoard: React.FC;   // composizione: la sola forma del corto su fondo piatto
```

**Disegno:**
- `Shape` è un `AbsoluteFill` con due strati, entrambi dentro la stessa trasformazione di camera (`cameraCss`,
  `transformOrigin: "0 0"`, senza `will-change`):
  1. la scatola: `<CameraMotionBlur samples={blurSamples(speed)} shutterAngle={180}>` attorno a un `div` assoluto
     `left x, top y, width w, height h, borderRadius r, background color`. Dentro `CameraMotionBlur` la scatola legge
     `useCurrentFrame()` da sola (il blur rende i sotto-fotogrammi spostando il frame), quindi calcolo di stato e camera
     DENTRO il figlio del blur, non fuori.
  2. il contenuto (senza blur): un `div` assoluto sul rect corrente con `overflow: hidden` e lo stesso `borderRadius`;
     dentro, centrato, un blocco grande come `keyRect(content.key)` (la controscala: non segue w/h correnti), con
     `opacity`, `filter: blur(${blur}px)` (solo se > 0), `transform: translateY(${dy}px)`, e `content(id, w, h)`.
- `CameraFrame`: un `AbsoluteFill` con la trasformazione di camera, che avvolge le scene.
- `ShapeBoard`: fondo `#1b1f2e`; il contorno sottile (1 px, `rgba(255,255,255,.25)`) del display a riposo della scena
  corrente; `Shape` con `SHORT_TIMELINE.shape ?? []`, `restDisplay(SHORT_TIMELINE)`, e un contenuto di prova che
  scrive l'id al centro (Inter/font del film, 28 px, colore `#14203A` su forme chiare, bianco su scure: basta la
  luminanza del colore della chiave); in alto a sinistra, fuori camera, il battito (una cifra decimale) e l'id della
  scena, 24 px, `rgba(255,255,255,.6)`. Registrata in `Root.tsx` come `ShapeBoard`, 1920×1080, 30 fps,
  `durationInFrames={framesOf(SHORT_TIMELINE)}`.
- `Film`: nuova prop `shape = false`. Se `shape && TIMELINE.shape`: le `Sequence` delle scene e dei passaggi stanno
  dentro `CameraFrame`, e `Shape` si disegna sopra di esse (sotto `Whip` e titoli dei sonni, che restano come sono).
  `ShortFilm` passa `shape={false}` finché il passo 3 non lo accende. Con `shape` spento il JSX prodotto è quello di oggi.

- [ ] **Step 1:** un test puro dove c'è logica fuori dai componenti (la luminanza per il colore del testo di prova, se
  la scrivi come funzione: `inkOn("#f4f2ec") === "#14203A"`, `inkOn("#000000") === "#ffffff"`) in `shape.test.ts`; rosso.
- [ ] **Step 2:** implementa `Shape.tsx`, `ShapeBoard.tsx`, la prop in `Film.tsx`, la composizione in `Root.tsx`.
- [ ] **Step 3:** `npm test 2>&1 | tail -5`, `npx tsc --noEmit`, `npx eslint src/film/Shape.tsx src/film/ShapeBoard.tsx src/film/Film.tsx src/Root.tsx` → verdi.
- [ ] **Step 4:** prova che `Short` non è cambiata: `npx remotion still Short out/review/_shape-guard.png --frame=600 --scale=0.25`
  prima e dopo il cambio (prima = `git stash push -- src` sul codice di Task 3 soltanto, poi `git stash pop`), e
  `python3 -c "from PIL import Image, ImageChops as C; a,b=Image.open('A'),Image.open('B'); print(C.difference(a.convert('RGB'),b.convert('RGB')).getbbox())"` → `None`.
  Se la macchina è carica (load > 12) salta questo passo e dillo: lo rifà chi coordina.
- [ ] **Step 5:** commit `feat(promo): Shape layer with box-only motion blur and counter-scaled content, ShapeBoard composition; film wiring off by default`
  (file per nome).

### Task 4 (regia, in sessione): la traccia del corto e la tavola

Non va a un esecutore: è giudizio di forma e di tempi (specifica §2, §8.5 «nessun battito senza un cambio di stato o
di bersaglio»).

- [ ] Scrivere `shape` in `src/film/timeline.short.json` dalla tabella della specifica §2: una chiave per stato, gesti
  dove esistono, colori per atto (know chiaro `#f4f2ec`, act blu, control grigio terminale, close nero e corallo),
  raggi che cambiano davvero, `zoom: 1` su face ed end.
- [ ] Test sulla traccia vera in `short.test.ts`: forma presente in ogni fotogramma; nessuno spostamento sopra 4 px fra
  due fotogrammi fuori dalla finestra `[at, at + len]` di una chiave; contenuti mai sovrapposti; chiavi di cambio sul
  battito 1 delle battute dove la specifica lo chiede; identità col display a riposo in face (0-1,5) e nell'ultimo
  battito di end.
- [ ] Tavola: `remotion render ShapeBoard out/review/forma.mp4 --scale=0.25` (leggera, fondo piatto, niente foto),
  poi `python3 out/review/battiti.py out/review/forma.mp4 --start 0 -o out/review/forma.battiti.png`.
- [ ] Guardarla, correggere, e mandarla a Franz (passo 2 della specifica: giudica il percorso prima delle scene).
