# Film corto, piano 3: la struttura nuova

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** il corto con la struttura approvata il 23/09 pomeriggio: apertura in ambient col titolo, carrellata con l'esito e i blink, tapparella verso il cartello, cartello con logo ritagliato e cinturino che sborda; 71 battiti, 38,7 s.

**Architecture:** ogni comportamento nuovo del motore è un campo opzionale della scaletta (`sleep.breath`, `out: "lids"`, `doneCard.slot`, `endPace: "blinds"`, `logoCutout`, `strapBleed`), letto da una funzione pura testata con `node --test`; il film lungo non usa nessuno di questi campi e resta identico. La scaletta del corto (`timeline.short.json`) si riscrive scena per scena; la musica si ritaglia con `cut_track.py`.

**Tech Stack:** Remotion 4.0.490, TypeScript, `node --test`, Python 3 (`tools/promo/audio`).

**Spec:** `docs/plans/2026-09-23-film-corto-design.md` (sezioni 3, 4 e 5, «Piano 3»)

**Scene che non cambiano** (si spostano solo di un battito prima, nel Task 1): la risposta col «yes» («answer», 19–26), la corsia con l'orologio di profilo e la dettatura («loop», 26–40), il terminale «Watch it work.» («watch», 40–50,5). I loro movimenti nuovi sono del piano 4.

## Global Constraints

- Il film lungo resta identico: i 4 fotogrammi di regressione (`out/regressione/prima/f{150,850,1300,2300}.png`) pixel per pixel, dopo ogni task che tocca il motore.
- Campi nuovi solo opzionali, con un errore in italiano in `validateTimeline` se il valore è fuori regola.
- Testi del film in inglese; commenti in italiano; commit in inglese, con i file per nome (mai `git add -A` o cartelle).
- La musica resta fuori da git (`public/audio/` è ignorata); nessun push.
- Il corto dura 71 battiti (38,7 s), sotto i 40 s.

## Review Focus

1. **Il titolo d'apertura e «It asks.» non stanno mai insieme sullo schermo:** il titolo esce prima della notifica (Task 1, fotogrammi di controllo nel Task 4).
2. **L'ambient dell'apertura non cambia luce prima della notifica:** misura della luminosità del display nei fotogrammi 0–82 (Task 4).
3. **La card dell'esito copre esattamente la riga di payments-api:** a 146 unità dall'alto, alta come la riga (due righe di testo), senza toccare storefront (Task 2, fotogramma di controllo nel Task 4).
4. **La scheda Context si legge almeno due battiti prima della tapparella, e i listelli nascono dalle sue 3 barre** (Task 2, test sui tempi; Task 4, fotogrammi).
5. **Nel cartello il bordo alto della foto non entra mai in quadro e il logo parte senza disco nero; il cartello del film lungo non cambia** (Task 3, test sul bordo per tutta la chiusura; regressione del fotogramma 2300).

---

### Task 1: l'apertura in ambient col titolo, e tutto il resto un battito prima

**Files:**
- Modify: `tools/promo/remotion/src/film/ui/sleep.ts` (`sleepAt`)
- Modify: `tools/promo/remotion/src/film/ui/sleep.test.ts`
- Modify: `tools/promo/remotion/src/film/timeline.ts` (`SleepCue`, `validateTimeline`)
- Modify: `tools/promo/remotion/src/film/timeline.test.ts`
- Modify: `tools/promo/remotion/src/film/Film.tsx` (`SceneView`: chiamata a `sleepAt`, `titleOn`)
- Modify: `tools/promo/remotion/src/film/timeline.short.json`
- Modify: `tools/promo/remotion/src/film/short.test.ts`
- Fuori da git: `tools/promo/remotion/public/audio/music.short.wav`

**Interfaces:**
- Produces: `sleepAt(p: number, len?: number, breathe = true): Sleep`; `SleepCue.breath?: boolean`; musica del corto dal battito 7 (`musicDelayBeats: 7`), colpo al 27.

- [ ] **Step 1: i test**

In `ui/sleep.test.ts`, in fondo:
```ts
test("senza respiro l'ambient resta a luce ferma fino al taglio (corto, Franz 23/09 14:34)", () => {
  for (let p = HUSH; p < SLEEP_CUT; p += 0.01) {
    assert.ok(about(sleepAt(p, 8, false).light, AMBIENT), `a ${p.toFixed(2)} la luce è ${sleepAt(p, 8, false).light}`);
    assert.ok(about(sleepAt(p, 8, false).halo, HALO), `a ${p.toFixed(2)} l'alone è ${sleepAt(p, 8, false).halo}`);
  }
  assert.ok(sleepAt(SLEEP_CUT, 8, false).light > 1.2, "la notifica riaccende il display come sempre");
});
test("con il respiro, com'è nel film lungo, la luce dell'ambient sale sui puntini", () => {
  const top = Math.max(...Array.from({ length: 60 }, (_, k) => sleepAt(HUSH + (SLEEP_CUT - HUSH) * (k / 60), 8).light));
  assert.ok(top > AMBIENT + 0.01, `il respiro arriva a ${top}`);
});
```
In `timeline.test.ts`, in fondo:
```ts
test("il respiro del sonno si spegne con false; altri valori sono un errore", () => {
  const t = base();
  t.scenes = [
    { id: "wake", at: 0, len: 5, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, text: { lines: ["Claude Code,", "on your wrist."], accent: "wrist.", at: 0.5 }, sleep: { len: 8, titleLead: 0, breath: "no" } },
    { id: "asks", at: 5, len: 4, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, text: { lines: ["It asks."], accent: "asks." } },
  ];
  assert.match(problems(t).join("\n"), /wake: il respiro del sonno è «no»: serve true o false/);
  t.scenes[0].sleep.breath = false;
  assert.deepEqual(problems(t), []);
});
```
In `short.test.ts`: aggiungere gli import
```ts
import { HUSH, SLEEP_CUT } from "./ui/sleep.ts";
import { dollyAt } from "./dolly.ts";
```
sostituire il primo test con
```ts
test("il corto dura 71 battiti (38,7 s), con la musica fino all'ultimo fotogramma", () => assert.equal(totalBeats(short), 71));
```
(il valore vale dal Task 2 in poi: in questo task il test fallisce anche dopo lo Step 3, perché «glance», «done» e «shipped» ci sono ancora; va verde nel Task 2)

cancellare il test «la seconda vibrazione cade sulla battuta quieta (60) e «Shipped.» sulla ripresa (64)» (la musica nuova non ha più la ripresa; le scene che lo usavano spariscono nel Task 2), e nel test «il colpo della musica cade sull'entrata della corsia…» sostituire le prime righe fino a `assert.equal(drop, 28);` con
```ts
  // taglio 0-1 0-11 9-10 11-12 43-45 dal battito 7: cinque battute d'introduzione, il colpo (la battuta 4 della traccia) al 27.
  // Lo stacco (la musica si ferma a 2,25-2,75 dell'ultima battuta d'introduzione) cade mentre il «yes» riempie il quadro
  const answer = byId("answer"), loop = byId("loop");
  const fl = (loop.fx ?? []).find((f) => f.kind === "float") as { at: number };
  const drop = (short.musicDelayBeats ?? 0) + 5 * 4, stop = drop - 4 + 2.25;
  const burst = loop.at - TAKEOVER_CUT * answer.takeover!.len, filled = burst + 0.42 * answer.takeover!.len;   // takeoverAt: cresce in 0-0,42
  assert.equal(drop, 27);
```
nel test «la corsia comincia dopo l'espansione…» sostituire i numeri
```ts
  assert.equal(loop.at + fl.at, 27);
  assert.equal(loop.at + fl.at + fl.len, 39);
  assert.equal(loop.at + say.at, 31);
  assert.equal(loop.at + fl.cards.find((c) => c.dictation)!.dictation!.tap, 30.5);
  assert.equal(loop.at + loop.takeover!.press!, 39.5);
  assert.equal(loop.at + loop.len, 40);
```
e aggiungere
```ts
test("l'apertura: il titolo sull'orologio in ambient a luce ferma, poi la notifica e «It asks.» (Franz, 23/09 14:23)", () => {
  const wake = byId("wake"), asks = byId("asks"), speaks = byId("speaks");
  assert.deepEqual(wake.text?.lines, ["Claude Code,", "on your wrist."]);
  assert.equal(wake.sleep?.breath, false);
  assert.equal(wake.sleep?.titleLead, 0);
  // già ad ambient al fotogramma 0: la finestra del sonno comincia prima del film e il display cala entro HUSH (ui/sleep.ts)
  const windowStart = wake.at + wake.len - SLEEP_CUT * wake.sleep!.len;
  assert.ok(windowStart + HUSH * wake.sleep!.len <= 0, `il display arriva ad ambient al battito ${windowStart + HUSH * wake.sleep!.len}`);
  assert.equal(asks.at, wake.at + wake.len);
  assert.ok(speaks.at + (speaks.text?.at ?? 0) - asks.at <= 5, "da «It asks.» a «It speaks.» al più 5 battiti");
  assert.equal(short.scenes.find((s) => s.id === "title"), undefined);
});
test("la camera passa da una scena all'altra senza scatti: apertura, «It asks.», «It speaks.»", () => {
  const [a, b, c] = ["wake", "asks", "speaks"].map(byId);
  assert.equal(dollyAt(a.watch!.dolly, 1), dollyAt(b.watch!.dolly, 0));
  assert.equal(dollyAt(b.watch!.dolly, 1), dollyAt(c.watch!.dolly, 0));
});
```

- [ ] **Step 2: verificare che falliscano**

Run: `cd tools/promo/remotion && node --test src/film/ui/sleep.test.ts src/film/timeline.test.ts src/film/short.test.ts 2>&1 | grep -E "^✖|ℹ (pass|fail)"`
Expected: FAIL su «senza respiro…», «il respiro del sonno si spegne…», «l'apertura…», «la camera passa…», «il colpo della musica…», «la corsia comincia…», «il corto dura 71 battiti…».

- [ ] **Step 3: il motore**

`ui/sleep.ts`, firma e respiro:
```ts
export const sleepAt = (p: number, len?: number, breathe = true): Sleep => {
```
```ts
  // `breathe` false: l'ambient resta a luce ferma. Il respiro serviva allo stop and go della musica del film lungo; nel
  // corto non c'è (Franz, 23/09 14:34)
  const breath = !breathe ? 0 : len ? breathOnDots((p - SLEEP_CUT) * len) : breathAt(p);
```
`timeline.ts`, in `SleepCue` (dopo `titleLead?: number`):
```ts
; breath?: boolean
```
cioè il tipo diventa `{ len: number; musicBackBeats?: number; musicFrom?: number; musicFadeIn?: number; titleLead?: number; breath?: boolean }`, con il commento `// \`breath\`: false = ambient a luce ferma, senza il respiro sui puntini (corto, 23/09)` in coda alla riga. In `validateTimeline`, dopo la riga «la scena «…» addormenta il display ma non c'è una scena dopo…»:
```ts
    if (s.sleep?.breath !== undefined && typeof s.sleep.breath !== "boolean") say(`il respiro del sonno è «${s.sleep.breath}»: serve true o false`);
```
`Film.tsx`, in `SceneView`:
```ts
  const sleep = nap ? sleepAt(sleepP(frame, spanFrames(GRID, (napOwn ? scene : prev).at, nap.len), napOwn, total), nap.len, nap.breath !== false) : null;
```
```ts
  // con `titleLead` la frase della scena dopo aspetta la notifica: la frase di chi dorme resta fino al taglio ed esce con la
  // sua uscita normale (corto, 23/09: il titolo apre il film in ambient). Senza, com'era nel film lungo
  const titleOn = sleep && napOwn && nap.titleLead === undefined ? sleep.title : 1;
```

- [ ] **Step 4: la scaletta del corto**

In `timeline.short.json`: `"musicDelayBeats": 7`; le scene da «wake» a «watch» diventano (le altre si spostano di un battito prima: «glance» a 50,5, «done» a 59, «shipped» a 63, «end» a 67, provvisori fino al Task 2):
```json
    {"id": "wake", "at": 0, "len": 5, "act": "know", "watch": {"view": "front", "clip": "scenes/s0_face.mp4", "clipStart": 1.0, "freeze": true, "dolly": {"from": 1, "to": 1.08}}, "text": {"lines": ["Claude Code,", "on your wrist."], "accent": "wrist.", "at": 0.5}, "sleep": {"len": 8, "musicBackBeats": 2, "titleLead": 0, "breath": false}},
    {"id": "asks", "at": 5, "len": 4, "act": "know", "watch": {"view": "front", "clip": "scenes/n_speaks.mp4", "clipStart": 11.2, "dolly": {"from": 1.08, "to": 1.12, "ease": "out"}}, "text": {"lines": ["It asks."], "accent": "asks."}, "fx": [{"kind": "shake", "at": 0.5}]},
    {"id": "speaks", "at": 9, "len": 10, "act": "know", "watch": {"view": "front", "clip": "scenes/n_speaks.mp4", "clipStart": 16.549, "dolly": {"from": 1.12, "to": 1, "ease": "out"}}, "text": {"lines": ["It speaks."], "accent": "speaks.", "at": 0.5}, "fx": [{"kind": "tap", "at": 4.0, "x": 347, "y": 90}, {"kind": "spoken", "at": 4.5, "len": 8.5, "voice": "question.wav", "words": "question.words.json"}]},
```
«answer» `"at": 19`, «loop» `"at": 26`, «watch» `"at": 40`, con tutto il resto invariato. La scena «title» sparisce.

- [ ] **Step 5: la musica**

Run:
```bash
cd tools/promo/audio && python3 cut_track.py ../materiali/audio/musica2/beats-brick-by-brick-trending-advertising-279931.mp3 ../materiali/audio/musica2/beats-brick-by-brick-trending-advertising-279931.mp3.card.json 0-1 0-11 9-10 11-12 43-45 --out=../remotion/public/audio/music.short.wav && python3 track_card.py ../remotion/public/audio/music.short.wav | tail -1
```
Expected: 16 battute; energie: 5 battute d'introduzione sotto −15 dB, dalla 5 alla 12 sopra −12 dB, la 13 (battito 59) sotto −15 dB, la 14 e la 15 il finale.

- [ ] **Step 6: test e controlli**

Run: `cd tools/promo/remotion && npm run check > /tmp/claude-1000/check-p3t1.log 2>&1; grep -E "^✖|ℹ (tests|pass|fail)|scaletta" /tmp/claude-1000/check-p3t1.log`
Expected: un solo fallimento, «il corto dura 71 battiti…» (75 battiti: si chiude nel Task 2); tutti gli altri verdi, le due scalette valide.

- [ ] **Step 7: il film lungo non cambia**

Run (da `tools/promo/remotion`):
```bash
rm -rf out/regressione/dopo && mkdir -p out/regressione/dopo && for f in 150 850 1300 2300; do npx remotion still Film out/regressione/dopo/f$f.png --frame=$f --gl=egl > /dev/null 2>&1; done; python3 -c "
import numpy as np, subprocess
ok=True
for f in (150, 850, 1300, 2300):
    a, b = (np.frombuffer(subprocess.run(['ffmpeg','-v','error','-i',f'out/regressione/{d}/f{f}.png','-f','rawvideo','-pix_fmt','rgb24','-'],capture_output=True).stdout, np.uint8) for d in ('prima','dopo'))
    n=int((a != b).sum()) if a.size==b.size else -1; print(f, 'pixel diversi:', n); ok = ok and n==0
print('IDENTICI' if ok else 'DIVERSI')"
```
Expected: `IDENTICI`

- [ ] **Step 8: commit**

```bash
git add tools/promo/remotion/src/film/ui/sleep.ts tools/promo/remotion/src/film/ui/sleep.test.ts tools/promo/remotion/src/film/timeline.ts tools/promo/remotion/src/film/timeline.test.ts tools/promo/remotion/src/film/Film.tsx tools/promo/remotion/src/film/timeline.short.json tools/promo/remotion/src/film/short.test.ts
git commit -m "feat(promo): the short opens on the ambient watch with its title, then the notification and It asks"
```

### Task 2: la carrellata: lista con l'esito, «Work», «Context» e la tapparella

**Files:**
- Modify: `tools/promo/remotion/src/film/timeline.ts` (`Scene.out`, fx `doneCard`, `validateTimeline`)
- Modify: `tools/promo/remotion/src/film/timeline.test.ts`
- Modify: `tools/promo/remotion/src/film/ui/doneCard.ts`, `tools/promo/remotion/src/film/ui/doneCard.test.ts`, `tools/promo/remotion/src/film/ui/DoneCard.tsx`
- Modify: `tools/promo/remotion/src/film/Fx.tsx` (caso `doneCard`)
- Modify: `tools/promo/remotion/src/film/Film.tsx` (sequenza del blink)
- Modify: `tools/promo/remotion/src/film/timeline.short.json`
- Modify: `tools/promo/remotion/src/film/short.test.ts`

**Interfaces:**
- Consumes: la scaletta del Task 1 (terminale 40–50,5, musica dal 7).
- Produces: `Scene.out?: "blink" | "lids"`; fx `doneCard` con `slot?: number`; `doneCardPlace(rise: number, slot?: number): { y: number; scrim: number }`; scene «list» (50,5–55), «work» (55–57), «context» (57–63), «end» a 63.

- [ ] **Step 1: i test**

In `ui/doneCard.test.ts`, import `doneCardPlace` accanto a `doneCardAt`, e in fondo:
```ts
test("nella lista la card sta nella sua riga, senza velo sul display (corto, 23/09 14:44)", () => {
  assert.deepEqual(doneCardPlace(0, 146), { y: 146, scrim: 0 });
  assert.deepEqual(doneCardPlace(1, 146), { y: 146, scrim: 0 });
});
test("senza riga la card sale da sotto e il display si scurisce, com'era", () => {
  assert.deepEqual(doneCardPlace(0), { y: 480, scrim: 0 });
  assert.deepEqual(doneCardPlace(1), { y: 134, scrim: 1 });
});
```
In `timeline.test.ts`, in fondo:
```ts
test("le sole palpebre («lids») sono un passaggio valido anche con una frase, che se ne va prima", () => {
  const t = base(); t.scenes[0].out = "lids";
  assert.deepEqual(problems(t), []);
  t.scenes[0].out = "wink";
  assert.match(problems(t).join("\n"), /open: passaggio «wink» sconosciuto/);
});
test("la card ✓ in una riga della lista sta dentro il display", () => {
  const t = base(); t.scenes[1].fx.push({ kind: "doneCard", at: 0, name: "payments-api", age: "0 m", text: "Released 2.8.0", slot: 400 });
  assert.match(problems(t).join("\n"), /list: la riga della card ✓ è a 400: fra 0 e 310/);
  t.scenes[1].fx[1].slot = 146;
  assert.deepEqual(problems(t), []);
});
```
In `short.test.ts`: aggiungere l'import `import { BLIND_CUT } from "./ui/blinds.ts";`; cancellare i test «attorno alla card ✓ l'orologio resta nella stessa colonna…», «sul polso le schermate vanno in fila…» e «la Panoramica torna nel corto…» (le scene che controllavano non ci sono più); aggiungere
```ts
test("la carrellata: lista con l'esito, «Work», «Context», con i blink fra l'una e l'altra (Franz, 23/09 14:23)", () => {
  const list = byId("list"), work = byId("work"), ctx = byId("context");
  assert.equal(list.at, 50.5);
  assert.equal(list.at + list.len, work.at);
  assert.equal(work.at + work.len, ctx.at);
  assert.equal(list.out, "lids");                                    // la frase se ne va prima delle palpebre
  assert.equal(work.out, "blink");
  assert.equal(work.text, undefined);                                // senza frase il blink sono le sole palpebre
  for (const b of [work.at, ctx.at]) assert.ok(Number.isInteger(b), `il blink al battito ${b} non cade su un battito`);
  for (const s of [list, work, ctx]) assert.equal(watchColumn(s), watchColumn(list), `${s.id} sposta l'orologio`);
  const card = (list.fx ?? []).find((f) => f.kind === "doneCard") as { slot?: number; text: string; at: number };
  assert.equal(card.slot, 146);                                      // la riga di payments-api in n_list.mp4 a 11,6 s
  assert.equal(card.at, 0);
  assert.match(card.text, /^Released 2\.8\.0/);
  assert.equal(list.watch!.clip, "scenes/n_list.mp4");
  assert.equal(list.watch!.clipStart, 11.6);
  assert.equal(list.watch!.freeze, true);
  for (const s of [work, ctx]) assert.equal(s.watch!.clip, "scenes/n_overview_fit.mp4");
  assert.ok(Math.abs(ctx.watch!.clipStart! - 10.64) <= 0.2, "sul display la scheda Context, dove cade nel film lungo (10,64 s)");
  for (const id of ["title", "glance", "done", "shipped"]) assert.equal(short.scenes.find((s) => s.id === id), undefined, `c'è ancora «${id}»`);
});
test("la scheda Context si legge prima che parta la tapparella, e le sue 3 barre diventano i listelli", () => {
  const ctx = byId("context"), end = byId("end");
  const aside = (ctx.fx ?? []).find((f) => f.kind === "aside") as { at: number; panel: string; out?: string; rows: unknown[] };
  assert.equal(aside.panel, "context");
  assert.equal(aside.out, "bars");
  assert.equal(aside.rows.length, 3);
  assert.equal(ctx.blinds?.len, 6);
  const blindStart = ctx.at + ctx.len - BLIND_CUT * ctx.blinds!.len;
  assert.ok(blindStart - (ctx.at + aside.at) >= 2, `la scheda resta ${blindStart - (ctx.at + aside.at)} battiti prima della tapparella`);
  assert.equal(ctx.at + ctx.len, end.at);
});
test("il taglio sul cartello cade sull'inizio del finale della musica, e il film finisce con la musica", () => {
  // taglio 0-1 0-11 9-10 11-12 43-45 dal battito 7: 14 battute prima del finale (43-44), che dura 2 battute
  const end = byId("end");
  assert.equal((short.musicDelayBeats ?? 0) + (1 + 11 + 1 + 1) * 4, end.at);
  assert.equal((short.musicDelayBeats ?? 0) + (1 + 11 + 1 + 1 + 2) * 4, totalBeats(short));
});
```

- [ ] **Step 2: verificare che falliscano**

Run: `cd tools/promo/remotion && node --test src/film/ui/doneCard.test.ts src/film/timeline.test.ts src/film/short.test.ts 2>&1 | grep -E "^✖|ℹ (pass|fail)"`
Expected: FAIL su `doneCardPlace` (non esiste), «le sole palpebre…», «la card ✓ in una riga…», e sui tre test nuovi del corto.

- [ ] **Step 3: il motore**

`timeline.ts`: nel tipo `Scene`, `out?: "blink" | "lids"`; nel fx `doneCard`, `slot?: number` (commento: `// \`slot\`: la card sta ferma in una riga della lista, a quell'altezza del display, senza velo (corto, 23/09)`); in `validateTimeline` la riga dei passaggi diventa
```ts
    if (s.out !== undefined && s.out !== "blink" && s.out !== "lids") say(`passaggio «${s.out}» sconosciuto`);
```
e, dentro il ciclo sugli effetti (accanto al controllo «il terminale ha … righe ma … tempi»):
```ts
      if (f.kind === "doneCard" && f.slot !== undefined && !(f.slot >= 0 && f.slot <= 310)) say(`la riga della card ✓ è a ${f.slot}: fra 0 e 310`);
```
`ui/doneCard.ts`, in fondo:
```ts
/** Dove sta la card e quanto si scurisce il display. Senza `slot` sale da sotto il bordo (a riposo al centro, card alta
 *  213) e il display si scurisce con lei; con `slot` sta ferma nella sua riga della lista e il display resta com'è. */
export const doneCardPlace = (rise: number, slot?: number): { y: number; scrim: number } =>
  slot === undefined ? { y: 134 + (1 - rise) * 346, scrim: rise } : { y: slot, scrim: 0 };
```
`ui/DoneCard.tsx`:
```tsx
import React from "react";
import { useCurrentFrame } from "remotion";
import { UiCard } from "./UiCard.tsx";
import { doneCardAt, doneCardPlace } from "./doneCard.ts";

/** La card ✓ sul display (spazio 480): sale mentre il display si scurisce, oppure sta ferma in una riga della lista (`slot`);
 *  in tutti e due i casi il ✓ si disegna (ui/doneCard.ts). */
export const DoneCard: React.FC<{ name: string; age: string; text: string; badge?: string; beat: number; rest?: boolean; slot?: number }> = ({ name, age, text, badge, beat, rest, slot }) => {
  const f = useCurrentFrame();
  const { rise, draw } = doneCardAt(f, beat, rest);
  const { y, scrim } = doneCardPlace(rise, slot);
  return (
    <div style={{ position: "absolute", inset: 0 }}>
      <div style={{ position: "absolute", inset: 0, background: "rgba(8,9,12,0.82)", opacity: scrim }} />
      <div style={{ position: "absolute", left: 26, top: y }}>
        <UiCard w={428} name={name} age={age} text={text} badge={badge} icon="check" checkDraw={draw} />
      </div>
    </div>
  );
};
```
`Fx.tsx`, caso `doneCard`: aggiungere `slot={e.slot}` alle props di `<DoneCard …>`.
`Film.tsx`, la sequenza dei blink:
```tsx
      {TIMELINE.scenes.filter((s) => s.out === "blink" || s.out === "lids").map((s) => (
        <Sequence key={`blink-${s.id}`} from={beatToFrame(GRID, s.at + s.len) - BLINK_FRAMES} durationInFrames={BLINK_FRAMES + 12} layout="none"><Blink word={s.out === "blink" ? s.text?.accent ?? "" : ""} cut={BLINK_FRAMES} from={[387, 597]} to={[684, 140]} /></Sequence>
      ))}
```
(con «lids» la frase esce con la sua uscita normale, 8 fotogrammi prima del taglio, e le palpebre si chiudono negli ultimi 5: `leave`, `hideAccentFrom` e `fadeFrom` guardano solo «blink» e restano come sono)

- [ ] **Step 4: la scaletta del corto**

In `timeline.short.json` le scene «glance», «done» e «shipped» lasciano il posto a:
```json
    {"id": "list", "at": 50.5, "len": 4.5, "act": "control", "watch": {"view": "front", "column": "right", "clip": "scenes/n_list.mp4", "clipStart": 11.6, "freeze": true}, "text": {"lines": ["Every session,", "at a glance."], "accent": "glance."}, "fx": [{"kind": "doneCard", "at": 0, "slot": 146, "name": "payments-api", "age": "0 m", "text": "Released 2.8.0 and tagged v2.8.0", "badge": "#3C81F2"}], "out": "lids"},
    {"id": "work", "at": 55, "len": 2, "act": "control", "watch": {"view": "front", "column": "right", "clip": "scenes/n_overview_fit.mp4", "clipStart": 6.3, "freeze": true}, "out": "blink"},
    {"id": "context", "at": 57, "len": 6, "act": "control", "watch": {"view": "front", "column": "right", "clip": "scenes/n_overview_fit.mp4", "clipStart": 10.64, "freeze": true}, "fx": [{"kind": "aside", "at": 0, "len": 6, "panel": "context", "rows": [{"name": "payments-api", "pct": 62}, {"name": "storefront", "pct": 18}, {"name": "blog", "pct": 4}], "out": "bars"}], "blinds": {"len": 6}},
```
e «end» passa a `"at": 63` (resto invariato fino al Task 3).

- [ ] **Step 5: le due schermate della Panoramica**

Run (da `tools/promo/remotion`):
```bash
S=/tmp/claude-1000/p3; mkdir -p $S; for t in 6.3 10.64; do ffmpeg -v error -y -ss $t -i public/scenes/n_overview_fit.mp4 -frames:v 1 -vf scale=240:-1 $S/overview-$t.png; done; ffmpeg -v error -y -i $S/overview-6.3.png -i $S/overview-10.64.png -filter_complex hstack $S/overview.png
```
Expected: guardando `$S/overview.png`, a sinistra la scheda «Work» («Now 1 working», le tre barre, «1 waits for you · 1 working») intera; a destra la scheda «Context» con payments-api 62 %, storefront 18 %, blog 4 %. Se «Work» è tagliata, spostare `clipStart` di «work» di 0,1 s per volta finché è intera, e segnarlo come decisione.

- [ ] **Step 6: test e controlli**

Run: `cd tools/promo/remotion && npm run check > /tmp/claude-1000/check-p3t2.log 2>&1; grep -E "^✖|ℹ (tests|pass|fail)|scaletta" /tmp/claude-1000/check-p3t2.log`
Expected: tutti verdi; «timeline.short.json: 10 scene, 71 battiti, 38.7 s».

- [ ] **Step 7: il film lungo non cambia**

Lo stesso comando del Task 1, Step 7. Expected: `IDENTICI`.

- [ ] **Step 8: commit**

```bash
git add tools/promo/remotion/src/film/timeline.ts tools/promo/remotion/src/film/timeline.test.ts tools/promo/remotion/src/film/ui/doneCard.ts tools/promo/remotion/src/film/ui/doneCard.test.ts tools/promo/remotion/src/film/ui/DoneCard.tsx tools/promo/remotion/src/film/Fx.tsx tools/promo/remotion/src/film/Film.tsx tools/promo/remotion/src/film/timeline.short.json tools/promo/remotion/src/film/short.test.ts
git commit -m "feat(promo): the short's carrellata — the outcome as a list row, blinks to Work and Context, and the three bars become the blinds"
```

### Task 3: il cartello: logo ritagliato, cinturino che sborda, passo dopo la tapparella

**Files:**
- Modify: `tools/promo/remotion/src/film/endCard.ts`, `tools/promo/remotion/src/film/endCard.test.ts`
- Modify: `tools/promo/remotion/src/film/moves.ts` (`closingAt`, `BLEED_LIFT`, `screenAt`), `tools/promo/remotion/src/film/moves.test.ts`
- Modify: `tools/promo/remotion/src/film/PhotoWatch.tsx` (`Ui`, `ThreeQuarter`, `Props`)
- Modify: `tools/promo/remotion/src/film/Film.tsx` (`closing`, props di `PhotoWatch`)
- Modify: `tools/promo/remotion/src/film/timeline.ts` (`Scene.logoCutout`, `Scene.strapBleed`, `validateTimeline`), `tools/promo/remotion/src/film/timeline.test.ts`
- Modify: `tools/promo/remotion/src/film/timeline.short.json`, `tools/promo/remotion/src/film/short.test.ts`

**Interfaces:**
- Consumes: «end» a 63, tapparella di «context» che si volta fino a 65,5 (Task 2).
- Produces: `EndPace` con `"blinds"`; `closingAt(beats: number, lift = 0.2): Closing`; `BLEED_LIFT = 0.3`; `screenAt(c: Closing, cutout: boolean): number`; props `screenOpacity?: number` e `bleed?: boolean` di `PhotoWatch`; `Scene.logoCutout?: boolean`, `Scene.strapBleed?: boolean`.

- [ ] **Step 1: i test**

`endCard.test.ts`, in fondo:
```ts
test("dopo la tapparella il cartello parte a 2 battiti dal taglio e gli avvisi a 4 (corto, 23/09)", () => {
  assert.deepEqual(END_PACE.blinds, { start: 2, sub: 1, repo: 1.5, notes: 2 });
  assert.equal(notesAt("blinds"), 4);
});
```
(se `END_PACE` o `notesAt` non sono già importati nel file, aggiungerli all'import da `./endCard.ts`)

`moves.test.ts`: aggiungere agli import `import { readFileSync } from "node:fs";`, `import { THEME } from "./theme.ts";` e, da `./moves.ts`, `closingAt, BLEED_LIFT, screenAt`; in fondo:
```ts
test("con il cinturino che sborda l'orologio del cartello sale di più: il bordo alto della foto resta sempre fuori quadro", () => {
  const Q = JSON.parse(readFileSync(new URL("./mockup.geometry.json", import.meta.url), "utf8")).q34;
  const k = THEME.q34GlassPx / (2 * Q.b);
  const top = (p: { y: number; scale: number }) => 540 + p.y * 1080 - Q.cy * k * p.scale;   // bordo alto della foto, in pixel del quadro
  for (let b = 0; b <= 8; b += 0.25) assert.ok(top(closingAt(b, BLEED_LIFT).pose) < -40, `al battito ${b} il bordo è a ${top(closingAt(b, BLEED_LIFT).pose).toFixed(0)} px`);
  assert.equal(closingAt(8).pose.y, -0.2);   // senza, com'è nel film lungo
});
test("col logo ritagliato il display nero compare con la cassa, non prima: il logo parte da solo sul blu", () => {
  assert.equal(screenAt(closingAt(0), true), 0);
  assert.equal(screenAt(closingAt(8), true), 1);
  assert.equal(screenAt(closingAt(0), false), 1);
});
```
`timeline.test.ts`, in fondo:
```ts
test("logo ritagliato e cinturino che sborda valgono solo sul cartello", () => {
  const t = base(); t.scenes[1].logoCutout = true; t.scenes[1].strapBleed = true;
  assert.match(problems(t).join("\n"), /list: logo ritagliato e cinturino che sborda valgono solo sul cartello/);
  t.scenes[1].endCard = true; t.scenes[1].endPace = "blinds";
  assert.deepEqual(problems(t), []);
});
```
`short.test.ts`: nel test «gli avvisi del cartello si leggono e «It asks.» non è già scritta al fotogramma 0» sostituire `assert.equal(byId("end").endPace, "compact");` con
```ts
  const end = byId("end");
  assert.equal(end.endPace, "blinds");
  assert.equal(end.logoCutout, true);
  assert.equal(end.strapBleed, true);
  assert.ok(end.len - notesAt(end.endPace) >= 4, `gli avvisi restano ${end.len - notesAt(end.endPace)} battiti`);
```
e aggiungere l'import `import { notesAt } from "./endCard.ts";`.

- [ ] **Step 2: verificare che falliscano**

Run: `cd tools/promo/remotion && node --test src/film/endCard.test.ts src/film/moves.test.ts src/film/timeline.test.ts src/film/short.test.ts 2>&1 | grep -E "^✖|ℹ (pass|fail)"`
Expected: FAIL sui quattro test nuovi e su quello degli avvisi.

- [ ] **Step 3: il motore**

`endCard.ts`:
```ts
export type EndPace = "normal" | "compact" | "blinds";
export const END_PACE: Record<EndPace, { start: number; sub: number; repo: number; notes: number }> = {
  normal: { start: 4, sub: 4, repo: 7, notes: 9 },
  compact: { start: 1, sub: 1.5, repo: 2.5, notes: 3.5 },
  // dopo la tapparella (corto, 23/09 pomeriggio): i listelli si voltano per 2,5 battiti dal taglio; il nome arriva a
  // tapparella quasi aperta e gli avvisi restano 4 battiti su 8
  blinds: { start: 2, sub: 1, repo: 1.5, notes: 2 },
};
```
`moves.ts`: la firma di `closingAt` e la posa diventano
```ts
/** Di quanto sale l'orologio del cartello quando il cinturino deve uscire intero dal bordo in alto (corto, 23/09): con
 *  0,2 il bordo della foto finiva a 63 px dall'alto e la sfumatura lo nascondeva; con 0,3 resta 45 px fuori quadro. */
export const BLEED_LIFT = 0.3;
export const closingAt = (beats: number, lift = 0.2): Closing => {
```
```ts
    pose: { x: 0, y: -lift * out, scale: 1.7 + 0.9 * near + (0.5 - 1.7) * out, tilt: 0 },
```
e in fondo al file:
```ts
/** Il display nero del cartello: col logo ritagliato compare con la cassa (`body`), così il logo nasce da solo sul blu. */
export const screenAt = (c: Closing, cutout: boolean): number => (cutout ? c.body : 1);
```
`PhotoWatch.tsx`: in `Props` aggiungere `screenOpacity?: number; bleed?: boolean`; `Ui` riceve `screenOpacity?: number` e il suo `return` diventa
```tsx
  <div style={{ position: "absolute", inset: 0, borderRadius: "50%", overflow: "hidden", background: screenOpacity === undefined ? "#000" : `rgba(0,0,0,${screenOpacity})` }}>
    <div style={{ position: "absolute", inset: 0, opacity: screenOpacity ?? 1 }}>
      {still ? <Img src={staticFile(still)} style={{ width: "100%", height: "100%" }} /> : freeze ? <Freeze frame={0}>{video}</Freeze>
        : hold ? <Freeze frame={hold} active={now < hold}><Sequence from={hold} layout="none">{video}</Sequence></Freeze> : video}
    </div>
    {overlay ? <div style={{ position: "absolute", left: 0, top: 0, width: 480, height: 480, transformOrigin: "0 0", scale: "var(--k)" }}>{overlay}</div> : null}
    {/* ombra interna: lo schermo sta sotto la cupola, ai bordi scurisce */}
    <div style={{ position: "absolute", inset: 0, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(0,0,0,0) 80%, rgba(0,0,0,.55) 100%)", opacity: screenOpacity ?? 1 }} />
  </div>
```
(senza `screenOpacity` il fondo resta `#000` e le opacità valgono 1: i pixel del film lungo non cambiano)

In `ThreeQuarter` aggiungere `screenOpacity, bleed` alle props destrutturate; la maschera del div esterno diventa
```tsx
      /* il cinturino finisce con la foto: sfuma nel buio prima che il bordo entri in quadro; con `bleed` esce intero dal bordo
         in alto (corto, 23/09) e sfuma solo in basso */
      maskImage: bleed ? "linear-gradient(180deg, #000 0%, #000 89%, rgba(0,0,0,0) 100%)" : "linear-gradient(180deg, rgba(0,0,0,0) 0%, #000 11%, #000 89%, rgba(0,0,0,0) 100%)" }}>
```
e la chiamata a `Ui` riceve `screenOpacity={screenOpacity}`.

`Film.tsx`: import `BLEED_LIFT, screenAt` da `./moves.ts` accanto a `closingAt`;
```ts
  const closing = scene.endCard ? closingAt(frame / beat, scene.strapBleed ? BLEED_LIFT : undefined) : null;
```
e sul `<PhotoWatch …>` della vista frontale e di tre quarti, accanto a `contentOpacity={closing?.logo}`:
```tsx
screenOpacity={closing && scene.logoCutout ? screenAt(closing, true) : undefined} bleed={scene.strapBleed}
```
`timeline.ts`: nel tipo `Scene` aggiungere `logoCutout?: boolean; strapBleed?: boolean;` e in `validateTimeline`, accanto alla regola degli avvisi del cartello:
```ts
    if ((s.logoCutout || s.strapBleed) && !s.endCard) say("logo ritagliato e cinturino che sborda valgono solo sul cartello");
```

- [ ] **Step 4: la scaletta del corto**

In `timeline.short.json`, la scena «end»:
```json
    {"id": "end", "at": 63, "len": 8, "act": "close", "watch": {"view": "threeQuarter", "clip": "scenes/s1_list.mp4", "clipStart": 0.2, "still": "icon/black.png"}, "endCard": true, "endPace": "blinds", "logoCutout": true, "strapBleed": true}
```

- [ ] **Step 5: test e controlli**

Run: `cd tools/promo/remotion && npm run check > /tmp/claude-1000/check-p3t3.log 2>&1; grep -E "^✖|ℹ (tests|pass|fail)|scaletta" /tmp/claude-1000/check-p3t3.log`
Expected: tutti verdi, le due scalette valide.

- [ ] **Step 6: il film lungo non cambia (anche il suo cartello: fotogramma 2300)**

Lo stesso comando del Task 1, Step 7. Expected: `IDENTICI`.

- [ ] **Step 7: commit**

```bash
git add tools/promo/remotion/src/film/endCard.ts tools/promo/remotion/src/film/endCard.test.ts tools/promo/remotion/src/film/moves.ts tools/promo/remotion/src/film/moves.test.ts tools/promo/remotion/src/film/PhotoWatch.tsx tools/promo/remotion/src/film/Film.tsx tools/promo/remotion/src/film/timeline.ts tools/promo/remotion/src/film/timeline.test.ts tools/promo/remotion/src/film/timeline.short.json tools/promo/remotion/src/film/short.test.ts
git commit -m "feat(promo): the short's end card — the logo cut out on the blue, the strap running off the top, notices after the blinds"
```

### Task 4: la bozza intera e le misure

**Files:**
- Create (fuori da git): `tools/promo/out/review/corto-bozza-8.mp4`

**Interfaces:**
- Consumes: la scaletta completa dei Task 1-3.

- [ ] **Step 1: resa**

Run (da `tools/promo/remotion`, a macchina libera; con il carico sopra 16 si aspetta):
```bash
for i in 1 2; do npx remotion render Short ../out/review/corto-bozza-8.mp4 --scale=0.5 --gl=egl --codec h264 --crf 23 --concurrency=2 --timeout=180000 > /tmp/claude-1000/bozza8.log 2>&1 && break; done; ffprobe -v error -show_entries format=duration -of csv=p=0 ../out/review/corto-bozza-8.mp4
```
Expected: 38,7 s circa.

- [ ] **Step 2: le misure**

Run:
```bash
cd tools/promo/out/review && python3 - <<'E'
import subprocess, numpy as np
V = "corto-bozza-8.mp4"; FPS, BPM, OFF, LAG = 30, 110, 0.01, 0.0427
fr = lambda b: round((OFF + b * 60 / BPM) * FPS)
x = np.frombuffer(subprocess.run(["ffmpeg","-v","error","-i",V,"-ac","1","-ar","48000","-f","f32le","-"],capture_output=True).stdout, np.float32)
t = lambda b: OFF + LAG + b * 60 / BPM
db = lambda a, b: 10*np.log10((x[int(t(a)*48000):int(t(b)*48000)]**2).mean()+1e-12)
print("colpo al 27: %+.1f dB" % (db(27, 31) - db(23, 27)))
print("stacco 25,25-25,75: %.0f dB contro %.0f del battito prima" % (db(25.25, 25.75), db(24, 25)))
print("battute:", " ".join(f"{b}:{db(b, b+4):.0f}" for b in range(7, 71, 4)))
raw = subprocess.run(["ffmpeg","-v","error","-i",V,"-frames:v",str(fr(5)),"-vf","scale=240:135","-f","rawvideo","-pix_fmt","gray","-"],capture_output=True).stdout
im = np.frombuffer(raw, np.uint8).reshape(-1, 135, 240).astype(float)
d = im[:, 30:80, 150:200].mean(axis=(1, 2))
print("display in ambient, fotogrammi 0-%d: da %.1f a %.1f" % (len(d) - 1, d.min(), d.max()))
E
```
Expected: colpo al 27 di almeno +6 dB; lo stacco almeno 10 dB sotto il battito prima; le battute dal 63 in giù verso il finale; il display in ambient con meno di 2 livelli di differenza fra minimo e massimo (col respiro erano 3,6).

- [ ] **Step 3: i fotogrammi**

Run:
```bash
cd tools/promo/out/review && python3 - <<'E'
import subprocess
fr = lambda b: round((0.01 + b * 60 / 110) * 30)
beats = [1, 3, 5.6, 9.8, 24, 27.2, 45, 51, 53, 55.6, 58.5, 60.8, 63.8, 65, 67.5, 70.5]
sel = "+".join(f"eq(n\\,{fr(b)})" for b in beats)
subprocess.run(["ffmpeg","-v","error","-y","-i","corto-bozza-8.mp4","-vf",f"select='{sel}',scale=480:-1,tile=4x4","-frames:v","1","/tmp/claude-1000/p3/bozza8.png"], check=True)
print(beats)
E
```
Expected, guardando il foglio:
- 1 e 3: orologio in ambient, «Claude Code, on your wrist.» a sinistra, niente «It asks.»;
- 5,6: display acceso sulla domanda, «It» o «It asks.», il titolo d'apertura già uscito;
- 9,8: «It speaks.»;
- 24 e 27,2: il «yes» premuto, poi la card «Deployed» sulla corsia;
- 45: il terminale;
- 51 e 53: sul polso la card «Released 2.8.0 and tagged v2.8.0» esattamente al posto della riga di payments-api, sotto storefront intera; «Every session, at a glance.»;
- 55,6: «Work» sul display, niente frase;
- 58,5: «Context» sul display e la scheda Context a sinistra con le barre;
- 60,8: i listelli della tapparella;
- 63,8 e 65: il logo sul blu senza disco nero, i listelli che si voltano;
- 67,5 e 70,5: l'orologio di tre quarti col cinturino che esce dal bordo in alto, senza sfumatura; «Claude Master», «Free. Open source.», repo, avvisi.

Ogni fotogramma che non corrisponde è un difetto: si corregge prima di mandare la bozza (systematic-debugging), con il suo test.

- [ ] **Step 4: consegna della bozza a Franz**

La bozza va a Franz con l'elenco dei blocchi già approvati che cambiano, in battiti e secondi (regola del 21/09): apertura rifatta; da «It speaks.» al terminale tutto un battito prima; «glance», card ✓ e «Shipped.» sostituiti dalla carrellata; cartello rifatto; musica con una battuta d'introduzione in meno e il finale dal 63.
