# Film corto, piano 2: il movimento nello spazio

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** i movimenti dell'analisi del 23/09 (priorità 1-4) sulla bozza 0 del corto: camera che si avvicina e arretra, card ✓ con il tratto che si disegna.

**Architecture:** ogni movimento è una funzione pura dell'avanzamento (testata con `node --test`), letta da un campo nuovo e opzionale della scaletta; il film lungo non usa i campi nuovi e resta identico.

**Tech Stack:** Remotion 4.0.490, `@remotion/paths` (già installato, 4.0.490), TypeScript, `node --test`.

**Spec:** `docs/plans/2026-09-23-film-corto-design.md` (sezioni 3 e 5)

## Global Constraints

- Il film lungo resta identico: campi nuovi solo opzionali, nessun valore di default che cambi una scena esistente.
- Ogni campo nuovo passa per `validateTimeline` con un errore in italiano se il valore è fuori misura.
- Testi del film in inglese; commenti in italiano; commit in inglese con i file per nome.

## Review Focus

1. **Camera che si avvicina e arretra senza scatti ai tagli:** lo zoom alla fine di una scena deve essere uguale a quello all'inizio della successiva quando le due scene si passano la camera (Task 1, test «continuità»).
2. **Il film lungo non cambia:** nessuna scena del film lungo ha `dolly`; i 4 fotogrammi di regressione restano identici (Task 1, passo 5).
3. **Lo zoom resta nel quadro:** valori fuori da 0,8-1,3 sono un errore della scaletta (Task 1, test di validazione).
4. **Il tratto del ✓ parte sulla vibrazione e finisce entro mezzo battito** (Task 2).
5. **Il ✓ disegnato sta sulla card, non sul vetro vuoto:** la card è sul display nel fotogramma della vibrazione (Task 2, fotogramma di controllo).

---

### Task 1: la camera che si avvicina e arretra (dolly)

**Files:**
- Create: `tools/promo/remotion/src/film/dolly.ts`, `tools/promo/remotion/src/film/dolly.test.ts`
- Modify: `tools/promo/remotion/src/film/timeline.ts` (`WatchCue`, `validateTimeline`), `tools/promo/remotion/src/film/Film.tsx` (calcolo di `zoom` in `SceneView`), `tools/promo/remotion/src/film/timeline.short.json` (scene wake, asks, title, shipped)

**Interfaces:**
- Produces: `type Dolly = { from: number; to: number; ease?: "inOut" | "out" }`, `dollyAt(d: Dolly | undefined, p: number): number` (1 senza dolly), `WatchCue.dolly?`.

- [ ] **Step 1: test**

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { dollyAt } from "./dolly.ts";

test("senza dolly la camera non si muove", () => {
  for (const p of [0, 0.5, 1]) assert.equal(dollyAt(undefined, p), 1);
});
test("il dolly va da «from» a «to» nella scena, fermo prima e dopo", () => {
  const d = { from: 1, to: 1.08 };
  assert.equal(dollyAt(d, -0.2), 1);
  assert.equal(dollyAt(d, 0), 1);
  assert.equal(dollyAt(d, 1), 1.08);
  assert.equal(dollyAt(d, 1.3), 1.08);
  const m = dollyAt(d, 0.5); assert.ok(m > 1.03 && m < 1.05, `a metà ${m}`);
});
test("«out» parte veloce e frena: a metà ha già fatto più di metà strada", () => {
  const d = { from: 1.08, to: 1.12, ease: "out" as const };
  assert.ok(dollyAt(d, 0.5) > 1.1);
});
test("continuità: due scene che si passano la camera non fanno scatti al taglio", () => {
  const a = { from: 1, to: 1.08 }, b = { from: 1.08, to: 1.12, ease: "out" as const };
  assert.equal(dollyAt(a, 1), dollyAt(b, 0));
});
```
In `timeline.test.ts`:
```ts
test("un dolly fuori da 0,8-1,3 è un errore", () => {
  const t = base(); t.scenes[1].watch.dolly = { from: 1, to: 2 };
  assert.match(problems(t).join("\n"), /list: dolly da 1 a 2 fuori da 0,8-1,3/);
});
```

- [ ] **Step 2: verificare che fallisca**

Run: `cd tools/promo/remotion && node --test src/film/dolly.test.ts src/film/timeline.test.ts`
Expected: FAIL (`dolly.ts` non esiste; il dolly fuori misura passa)

- [ ] **Step 3: implementare**

`dolly.ts`:
```ts
/** La camera che si avvicina o arretra dentro una scena (analisi del 23/09, priorità 1): lo zoom va da `from` a `to`
 *  con una curva morbida; «out» parte veloce e frena, per un movimento che si ferma su un evento. */
export type Dolly = { from: number; to: number; ease?: "inOut" | "out" };
const clamp = (t: number) => Math.min(1, Math.max(0, t));
export const dollyAt = (d: Dolly | undefined, p: number): number => {
  if (!d) return 1;
  const t = clamp(p);
  const e = d.ease === "out" ? 1 - (1 - t) ** 3 : t * t * (3 - 2 * t);
  return d.from + (d.to - d.from) * e;
};
```
`timeline.ts`: `WatchCue` riceve `dolly?: Dolly`; in `validateTimeline`, dentro il controllo di `s.watch`:
```ts
if (s.watch.dolly && ![s.watch.dolly.from, s.watch.dolly.to].every((z) => z >= 0.8 && z <= 1.3)) say(`dolly da ${s.watch.dolly.from} a ${s.watch.dolly.to} fuori da 0,8-1,3`);
```
`Film.tsx`, in `SceneView`, dove si calcola `zoom`: lo zoom della scena si moltiplica per il dolly.
```ts
const zoom = (zoom0 + ((next?.watch?.camera === "around" ? AROUND_ZOOM : 1) - zoom0) * mv) * dollyAt(w?.dolly, frame / Math.max(1, total));
```

- [ ] **Step 4: test e controlli**

Run: `npm run check`
Expected: PASS; le due scalette valide.

- [ ] **Step 5: il film lungo non cambia**

I 4 fotogrammi di regressione (piano 1, Task 1, passo 5): 0 pixel diversi.

- [ ] **Step 6: il dolly nel corto**

`timeline.short.json`: `wake` `"dolly": { "from": 1, "to": 1.08 }`; `asks` `"dolly": { "from": 1.08, "to": 1.12, "ease": "out" }`; `title` `"dolly": { "from": 1.12, "to": 1 }`; `shipped` `"dolly": { "from": 1, "to": 0.9 }`. `npm run check` verde.

- [ ] **Step 7: commit**

```bash
git add tools/promo/remotion/src/film/dolly.ts tools/promo/remotion/src/film/dolly.test.ts tools/promo/remotion/src/film/timeline.ts tools/promo/remotion/src/film/timeline.test.ts tools/promo/remotion/src/film/Film.tsx tools/promo/remotion/src/film/timeline.short.json
git commit -m "feat(promo): the camera moves in and out within a scene; the short opens closer and pulls back on its title"
```

### Task 2: la card ✓ con il tratto che si disegna

Si scrive dopo aver letto il componente delle card (`ui/UiCard.tsx`) e il modo in cui il display riceve un'immagine sopra la clip (`PhotoWatch` `overlay`): il task riusa quel canale. Interfaccia prevista: effetto `doneCard` con `at` e testo; il segno ✓ si disegna con `evolvePath` in mezzo battito dalla vibrazione.

## Dopo la revisione finale dei piani 1-2 (23/09)

Correzioni nel commit `6a01fbe`: avvisi del cartello, «It asks.» al fotogramma 0, pressione e attacco al battito 24, orologio fermo attorno alla card ✓.

**Aperto, con la causa verificata:** il quadrato nero attorno all'orologio nel cartello del corto. Viene dai riflessi della vista di tre quarti: `mockup/q34_reflections.png` è RGB, senza trasparenza, e `PhotoWatch.tsx` lo fonde in `screen` dentro un gruppo isolato su fondo trasparente. Sul nero del film lungo non si vede, sul blu del corto sì. Si corregge con il pezzo 6 del design (il ✓ che diventa il logo), con una versione RGBA dei riflessi; il film lungo deve restare identico.

**Minori rimandati:**
- il primo battito della musica perde circa 7 ms, e circa 60 ms sono a −6 dB: `musicBackBeats` 1,5 invece di 2 lo risolve;
- `@remotion/paths` è usato in `UiCard` ma non è dichiarato in `package.json`: oggi arriva con `@remotion/transitions`;
- validazioni incomplete: `dolly.ease` sconosciuto, zoom risultante fuori misura, `doneCard` su una vista non frontale, chiavi della tavolozza;
- una scaletta del corto non valida blocca anche la composizione `Film`, perché le due scalette si validano al caricamento di `Film.tsx`.
