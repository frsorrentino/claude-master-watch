# Film corto, piano 1: fondamenta e prima bozza

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** una composizione `Short` che rende il corto da 72 battiti con la sua musica e la sua tavolozza, usando i meccanismi che il motore ha già (bozza 0), senza cambiare di un pixel il film lungo.

**Architecture:** il motore del film smette di leggere una scaletta fissa di modulo e la riceve dalla composizione (prop + contesto React). Una tavolozza opzionale nella scaletta sostituisce i colori per atto. Il corto ha la sua scaletta JSON, validata dalle stesse regole, e la sua musica tagliata dalla traccia originale.

**Tech Stack:** Remotion 4.0.490, React 19, TypeScript con `node --test`, Python 3 (numpy) per l'audio, ffmpeg.

**Spec:** `docs/plans/2026-09-23-film-corto-design.md`

## Global Constraints

- Remotion resta alla 4.0.490; nessun pacchetto nuovo in questo piano.
- 30 fps, 1920×1080, 110 bpm; il film si scrive in battiti, i fotogrammi si ricavano solo da `beats.ts`.
- Il film lungo (composizione `Film`, 150 battiti, 81,8 s) non cambia: stessi pixel, stesso audio.
- Testi del film in inglese; commenti e documenti in italiano; commit in inglese, file per nome (mai `git add -A`).
- `npm run check` verde a ogni commit.
- Nessun file di terzi (musica, modelli 3D) in git: restano in `tools/promo/materiali/` o in `public/audio/` (ignorata).

## Review Focus

1. **Il film lungo non deve cambiare** quando il motore diventa parametrico: 4 fotogrammi resi prima e dopo, confrontati pixel per pixel (Task 1, passo 5).
2. **L'attacco della parte forte cade sulla pressione del «yes»** (battito 24 del corto): misurato sulla musica tagliata (Task 3, passo 3) e sulla resa (Task 5, passo 3).
3. **Al fotogramma 0 il display è già spento:** il sonno che parte prima dell'inizio del film non deve mostrare un lampo acceso (Task 5, passo 2).
4. **Nessuna clip più corta della sua scena:** `check.ts` lo controlla anche sulla scaletta del corto (Task 4, passo 3).
5. **Le frasi hanno il tempo di leggersi:** le regole di `validateTimeline` valgono anche per il corto (Task 4, passo 2).

---

### Task 1: la scaletta come parametro del film

**Files:**
- Create: `tools/promo/remotion/src/film/cut.ts`
- Test: `tools/promo/remotion/src/film/cut.test.ts`
- Modify: `tools/promo/remotion/src/film/Film.tsx` (righe 44-48, 50-53, 205-209)

**Interfaces:**
- Produces: `gridOf(t: Timeline): Grid`, `framesOf(t: Timeline): number`, `FilmTimeline` (contesto React con la scaletta), `Film: React.FC<{ stems?: Stems; timeline?: Timeline }>`.

- [ ] **Step 1: scrivere il test**

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { framesOf, gridOf } from "./cut.ts";
import { validateTimeline } from "./timeline.ts";

const film = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));

test("la griglia di una scaletta viene dalla scaletta stessa", () => {
  assert.deepEqual(gridOf(film), { bpm: 110, fps: 30, offsetSeconds: film.offsetSeconds });
});
test("il film lungo dura 2455 fotogrammi, come la resa consegnata il 23/09", () => {
  assert.equal(framesOf(film), 2455);
});
```

- [ ] **Step 2: verificare che fallisca**

Run: `cd tools/promo/remotion && node --test src/film/cut.test.ts`
Expected: FAIL, `Cannot find module './cut.ts'`

- [ ] **Step 3: implementare**

`cut.ts`:
```ts
/** Una scaletta porta con sé la sua griglia: il film lungo e il corto ne hanno una ciascuno. */
import { beatToFrame } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { totalBeats } from "./timeline.ts";
import type { Timeline } from "./timeline.ts";

export const gridOf = (t: Timeline): Grid => ({ bpm: t.bpm, fps: t.fps, offsetSeconds: t.offsetSeconds });
export const framesOf = (t: Timeline): number => beatToFrame(gridOf(t), totalBeats(t));
```

In `Film.tsx`: le costanti di modulo `TIMELINE`/`GRID` diventano `FILM_TIMELINE` e un contesto; `SceneView` e `Film` leggono la scaletta dal contesto e ricavano la griglia con `gridOf`. All'interno delle due funzioni i nomi locali restano `TIMELINE` e `GRID`, così il resto del codice non cambia.
```tsx
export const FILM_TIMELINE = validateTimeline(raw);
export const FilmTimeline = React.createContext<Timeline>(FILM_TIMELINE);
export const filmFrames = (): number => framesOf(FILM_TIMELINE);
// in SceneView, prima riga:
const TIMELINE = React.useContext(FilmTimeline);
const GRID = gridOf(TIMELINE);
// Film:
export const Film: React.FC<{ stems?: Stems; timeline?: Timeline }> = ({ stems, timeline = FILM_TIMELINE }) => {
  useFilmFonts();
  const TIMELINE = timeline;
  const GRID = gridOf(timeline);
  return (<FilmTimeline.Provider value={timeline}> ...contenuto di prima... </FilmTimeline.Provider>);
};
```

- [ ] **Step 4: test e controlli**

Run: `node --test src/film/cut.test.ts && npm run check`
Expected: PASS; «scaletta valida: 16 scene, 150 battiti, 81.8 s»

- [ ] **Step 5: il film lungo è identico** (Review Focus 1)

Prima del passo 3 si rendono `out/regressione/prima/f{150,850,1300,2300}.png` con `npx remotion still Film <file> --frame=N --gl=egl`; dopo il passo 3 gli stessi in `out/regressione/dopo/`. Poi:
```bash
python3 -c "
import numpy as np, subprocess
for f in (150, 850, 1300, 2300):
    a, b = (np.frombuffer(subprocess.run(['ffmpeg','-v','error','-i',f'out/regressione/{d}/f{f}.png','-f','rawvideo','-pix_fmt','rgb24','-'],capture_output=True).stdout, np.uint8) for d in ('prima','dopo'))
    print(f, 'pixel diversi:', int((a != b).sum()))
    assert (a == b).all()
"
```
Expected: 0 pixel diversi su tutti e quattro.

- [ ] **Step 6: commit**

```bash
git add tools/promo/remotion/src/film/cut.ts tools/promo/remotion/src/film/cut.test.ts tools/promo/remotion/src/film/Film.tsx
git commit -m "refactor(promo): the film engine takes its timeline from the composition; the long film renders pixel-identical"
```

### Task 2: una tavolozza per scaletta

**Files:**
- Modify: `tools/promo/remotion/src/film/theme.ts` (dopo `ACT_BG`), `tools/promo/remotion/src/film/timeline.ts` (tipo `Timeline`, `validateTimeline`), `tools/promo/remotion/src/film/Backdrop.tsx`, `tools/promo/remotion/src/film/Film.tsx` (riga 145)
- Test: `tools/promo/remotion/src/film/theme.test.ts` (nuovo), `tools/promo/remotion/src/film/timeline.test.ts`

**Interfaces:**
- Produces: `Palette = Partial<Record<Act, [string, string, string, string]>>`, `actColors(act: Act, palette?: Palette): [string, string, string, string]`, `Timeline.palette?`, prop `colors` di `Backdrop`.

- [ ] **Step 1: test**

`theme.test.ts`:
```ts
import test from "node:test";
import assert from "node:assert/strict";
import { ACT_BG, actColors } from "./theme.ts";

const NAVY: [string, string, string, string] = ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"];
test("senza tavolozza ogni atto ha i suoi colori di sempre", () => {
  for (const a of ["open", "know", "act", "control", "close"] as const) assert.deepEqual(actColors(a), ACT_BG[a]);
});
test("la tavolozza della scaletta sostituisce solo gli atti che nomina", () => {
  assert.deepEqual(actColors("act", { act: NAVY }), NAVY);
  assert.deepEqual(actColors("control", { act: NAVY }), ACT_BG.control);
});
```
In `timeline.test.ts`, un caso in più:
```ts
test("una tavolozza con un colore che non è un colore è un errore", () => {
  const t = { ...base, palette: { act: ["#3A4468", "blu", "#14172A", "rgb(30,35,60)"] } };
  assert.throws(() => validateTimeline(t), /tavolozza/);
});
```
(`base` è la scaletta minima valida già usata in `timeline.test.ts`.)

- [ ] **Step 2: verificare che fallisca**

Run: `node --test src/film/theme.test.ts src/film/timeline.test.ts`
Expected: FAIL (`actColors` non esiste; la tavolozza sbagliata passa)

- [ ] **Step 3: implementare**

`theme.ts`:
```ts
export type Palette = Partial<Record<Act, [string, string, string, string]>>;
/** I colori di un atto: quelli della scaletta se li dà, altrimenti quelli di sempre. */
export const actColors = (act: Act, palette?: Palette): [string, string, string, string] => palette?.[act] ?? ACT_BG[act];
```
`timeline.ts`: `Timeline` riceve `palette?: Palette`; in `validateTimeline`:
```ts
const COLOR = /^#[0-9A-Fa-f]{6}$|^rgb\(\d{1,3},\s?\d{1,3},\s?\d{1,3}\)$/;
for (const [act, cols] of Object.entries(t.palette ?? {})) if (!Array.isArray(cols) || cols.length !== 4 || cols.some((c) => !COLOR.test(c))) bad.push(`tavolozza: i colori di «${act}» non sono 4 colori validi`);
```
`Backdrop.tsx`: nuova prop `colors?: [string, string, string, string]`, usata al posto di `ACT_BG[act]` quando c'è. `Film.tsx`: `<Backdrop ... colors={actColors(scene.act, TIMELINE.palette)} />`.

- [ ] **Step 4: test e controlli**

Run: `npm run check`
Expected: PASS, 150 battiti; poi il confronto del Task 1 passo 5 ripetuto: 0 pixel diversi.

- [ ] **Step 5: commit**

```bash
git add tools/promo/remotion/src/film/theme.ts tools/promo/remotion/src/film/theme.test.ts tools/promo/remotion/src/film/timeline.ts tools/promo/remotion/src/film/timeline.test.ts tools/promo/remotion/src/film/Backdrop.tsx tools/promo/remotion/src/film/Film.tsx
git commit -m "feat(promo): a timeline can carry its own palette"
```

### Task 3: la musica del corto

**Files:**
- Modify: `tools/promo/audio/cut_track.py` (opzione `--out`)
- Create (fuori da git): `tools/promo/remotion/public/audio/music.short.wav`

**Interfaces:**
- Produces: `music.short.wav`, 16 battute a 110 bpm con il primo battito a 0,025 s.

- [ ] **Step 1: opzione `--out`**

In `cut_track.py`, prima del calcolo di `dst`:
```python
out = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--out=")), None)
segs = [tuple(int(v) for v in s.split("-")) for s in sys.argv[3:] if not s.startswith("--")]
dst = Path(out).resolve() if out else Path(__file__).resolve().parent.parent / "remotion/public/audio/music.wav"
```

- [ ] **Step 2: tagliare**

Run:
```bash
cd tools/promo/audio && python3 cut_track.py ../materiali/audio/musica2/beats-brick-by-brick-trending-advertising-279931.mp3 ../materiali/audio/musica2/beats-brick-by-brick-trending-advertising-279931.mp3.card.json 0-1 0-13 43-45 --out=../remotion/public/audio/music.short.wav
```
Expected: `music.short.wav: 34.9x s · bpm 110.0`

- [ ] **Step 3: misurare** (Review Focus 2)

Run: `python3 track_card.py ../remotion/public/audio/music.short.wav`
Expected: 110 bpm, 16 battute; energia delle battute 0-4 sotto −15 dB, dalla 5 alla 11 sopra −12 dB (la 5 è l'attacco: battito 4 + 5×4 = 24 del film), la 12 sotto −15 dB (battito 52), la 13 sopra −12 dB (56), le 14-15 il finale.

- [ ] **Step 4: commit** (solo lo script: la musica resta fuori da git)

```bash
git add tools/promo/audio/cut_track.py
git commit -m "feat(promo): cut_track writes to a chosen file"
```

### Task 4: la scaletta del corto e la sua composizione

**Files:**
- Create: `tools/promo/remotion/src/film/timeline.short.json`
- Modify: `tools/promo/remotion/src/film/check.ts`, `tools/promo/remotion/src/film/Film.tsx`, `tools/promo/remotion/src/Root.tsx`
- Test: `tools/promo/remotion/src/film/short.test.ts` (nuovo)

**Interfaces:**
- Consumes: `framesOf`, `Film` con `timeline` (Task 1), `Timeline.palette` (Task 2), `music.short.wav` (Task 3).
- Produces: `SHORT_TIMELINE`, composizione `Short`.

- [ ] **Step 1: test**

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline } from "./timeline.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));
test("il corto dura 72 battiti: 68 di racconto e musica, 4 di silenzio sul cartello", () => assert.equal(totalBeats(short), 72));
test("la pressione del «yes» cade sull'attacco della parte forte, al battito 24", () => {
  const answer = short.scenes.find((s) => s.id === "answer")!;
  const press = (answer.fx ?? []).find((f) => f.kind === "longPress")!;
  assert.equal(answer.at + press.at, 24);
  assert.equal((short.musicDelayBeats ?? 0) + 5 * 4, 24);   // 5 battute d'introduzione
});
test("la seconda vibrazione cade sulla battuta quieta (52) e «Shipped.» sulla ripresa (56)", () => {
  assert.equal(short.scenes.find((s) => s.id === "done")!.at, 52);
  assert.equal(short.scenes.find((s) => s.id === "shipped")!.at, 56);
});
```

- [ ] **Step 2: la scaletta** (Review Focus 5)

`timeline.short.json` (scene prese dal film lungo, con i tempi del corto):
```json
{
  "bpm": 110, "fps": 30, "offsetSeconds": 0.01,
  "music": "audio/music.short.wav", "musicDelayBeats": 4, "musicDelayFrames": 0,
  "palette": {
    "open": ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
    "know": ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
    "act": ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
    "control": ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
    "close": ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"]
  },
  "scenes": [
    { "id": "wake", "at": 0, "len": 2, "act": "know", "watch": { "view": "front", "clip": "scenes/s0_face.mp4", "clipStart": 1.0, "freeze": true }, "sleep": { "len": 6, "musicBackBeats": 2 } },
    { "id": "asks", "at": 2, "len": 3, "act": "know", "watch": { "view": "front", "clip": "scenes/n_speaks.mp4", "clipStart": 11.2 }, "text": { "lines": ["It asks."], "accent": "asks." }, "fx": [{ "kind": "shake", "at": 0.5 }] },
    { "id": "title", "at": 5, "len": 5, "act": "know", "watch": { "view": "front", "clip": "scenes/n_speaks.mp4", "clipStart": 12.836 }, "text": { "lines": ["Claude Code,", "on your wrist."], "accent": "wrist." } },
    { "id": "speaks", "at": 10, "len": 10, "act": "know", "watch": { "view": "front", "clip": "scenes/n_speaks.mp4", "clipStart": 16.549 }, "text": { "lines": ["It speaks."], "accent": "speaks.", "at": 0.5 }, "fx": [{ "kind": "tap", "at": 4.0, "x": 347, "y": 90 }, { "kind": "spoken", "at": 4.5, "len": 9.5, "voice": "question.wav", "words": "question.words.json" }] },
    { "id": "answer", "at": 20, "len": 7, "act": "act", "watch": { "view": "front", "column": "right", "clip": "scenes/n_answer.mp4", "clipStart": 15.554, "steady": true }, "fx": [{ "kind": "longPress", "at": 4.0, "len": 1.5 }, { "kind": "optionsBuild", "at": 0.5, "len": 6.5, "yes": [26, 167, 428, 111], "no": [26, 287, 428, 109], "yesLabel": "1 · yes", "noLabel": "2 · no", "pressAt": 4.0 }], "takeover": { "len": 2.5, "x": 960, "y": 413, "w": 920, "h": 239, "r": 119, "tilt": 0, "color": "#D3E3FD", "toColor": "#242A42", "body": "plain", "tint": "grow" } },
    { "id": "loop", "at": 27, "len": 14, "act": "act", "watch": { "view": "side", "clip": "scenes/n_follow.mp4", "clipStart": 8.0, "freeze": true, "enter": "riseIn" }, "fx": [{ "kind": "float", "at": 1, "len": 12, "cards": [{ "name": "payments-api", "age": "0 m", "text": "Deployed 2.8.0, smoke tests green", "badge": "#3C81F2", "icon": "check" }, { "kind": "text", "lines": ["Say what's next."], "accent": "next." }, { "name": "payments-api", "age": "0 m", "text": "Great. Now update the changelog and tag the release", "badge": "#3C81F2", "icon": "play", "hold": 5.6, "dictation": { "tap": 4.5 } }] }, { "kind": "spoken", "at": 5.0, "len": 6.0, "voice": "say.wav", "words": "say.words.json" }], "takeover": { "len": 6.5, "x": 960, "y": 466, "w": 560, "h": 279, "r": 55, "color": "#000000", "toColor": "#23272E", "body": "screen", "words": ["Great. Now update the", "changelog and tag the", "release"], "press": 13.5 }, "bgFrom": "#242A42" },
    { "id": "watch", "at": 41, "len": 6, "act": "control", "watch": { "view": "front", "clip": "scenes/n_watch_fit.mp4", "clipStart": 0, "rate": 1.0, "fadeOut": 1.0 }, "text": { "lines": ["Watch it work."], "accent": "work." }, "fx": [{ "kind": "terminalPlane", "at": 3.5, "len": 2.5, "rect": [26, 163, 427, 150], "header": "Terminal · 07:36", "title": "payments-api — claude", "lines": ["⏺ Reading the checklist", "Read(RELEASE.md)", "⎿ Read 38 lines", "⏺ Updating the changelog", "Edit(CHANGELOG.md)", "⎿ Updated CHANGELOG.md with 9 additions", "⏺ Tagging the release", "Bash(git tag v2.8.0)"], "every": 1.2, "start": 5, "keep": true, "prompt": "Great. Now update the changelog and tag the release", "promptAt": 0.55, "path": "~/code/payments-api", "model": "Opus 5 · Claude Max", "status": "⏵⏵ bypass permissions on", "times": [0.8, 1.0, 2.35, 2.5, 3.1, 3.2, 3.75, 4.4] }], "bgFrom": "#23272E", "bgKeep": true },
    { "id": "glance", "at": 47, "len": 5, "act": "control", "watch": { "view": "front", "clip": "scenes/n_list.mp4", "clipStart": 11.6 }, "text": { "lines": ["Every session,", "at a glance."], "accent": "glance." } },
    { "id": "done", "at": 52, "len": 4, "act": "control", "watch": { "view": "front", "clip": "scenes/s3_done.mp4", "clipStart": 0.5, "freeze": true }, "fx": [{ "kind": "shake", "at": 0.5 }] },
    { "id": "shipped", "at": 56, "len": 4, "act": "control", "watch": { "view": "front", "clip": "scenes/s3_done.mp4", "clipStart": 0.5, "freeze": true }, "text": { "lines": ["Shipped.", "From your wrist."], "accent": "Shipped." } },
    { "id": "end", "at": 60, "len": 12, "act": "close", "watch": { "view": "threeQuarter", "clip": "scenes/s1_list.mp4", "clipStart": 0.2, "still": "icon/black.png" }, "endCard": true }
  ]
}
```

- [ ] **Step 3: `check.ts` su tutte e due le scalette** (Review Focus 4)

`check.ts` ripete i suoi controlli (clip esistenti, clip abbastanza lunghe) per `timeline.json` e `timeline.short.json`, e stampa una riga per ciascuna.

- [ ] **Step 4: la composizione**

`Film.tsx`:
```tsx
import shortRaw from "./timeline.short.json";
export const SHORT_TIMELINE = validateTimeline(shortRaw);
export const ShortFilm: React.FC<{ stems?: Stems }> = ({ stems }) => <Film stems={stems} timeline={SHORT_TIMELINE} />;
```
`Root.tsx`:
```tsx
<Composition id="Short" component={ShortFilm} defaultProps={{ stems: "all" as const }} fps={30} width={1920} height={1080} durationInFrames={framesOf(SHORT_TIMELINE)} />
```

- [ ] **Step 5: test e controlli**

Run: `node --test src/film/short.test.ts && npm run check`
Expected: PASS; due righe «scaletta valida», la seconda con 11 scene, 72 battiti, 39.3 s.

- [ ] **Step 6: commit**

```bash
git add tools/promo/remotion/src/film/timeline.short.json tools/promo/remotion/src/film/short.test.ts tools/promo/remotion/src/film/check.ts tools/promo/remotion/src/film/Film.tsx tools/promo/remotion/src/Root.tsx
git commit -m "feat(promo): the short film exists as its own composition, 72 beats on the long film's scenes"
```

### Task 5: bozza 0

**Files:** nessuno nel repo; tutto in `tools/promo/out/review/` (ignorata).

- [ ] **Step 1: anteprima**

Run: `cd tools/promo/remotion && npx remotion render Short ../out/review/corto-bozza-0.mp4 --scale=0.5 --gl=egl --codec h264 --crf 23` (se il cartello usa il 3D, i fotogrammi del logo passano in `swangle`, come nel film lungo).

- [ ] **Step 2: il display è spento al fotogramma 0** (Review Focus 3)

Estrarre i fotogrammi 0, 10, 36 (notifica) e guardarli: display scuro ai primi due, acceso al terzo.

- [ ] **Step 3: la musica cade sulla pressione** (Review Focus 2)

Misurare sull'audio della resa l'attacco al battito 24 (fotogramma `beatToFrame(griglia del corto, 24)`): salto di energia di almeno 6 dB fra la battuta prima e quella dopo.

- [ ] **Step 4: foglio di fotogrammi** — uno per scena, per Franz.

---

## Piani successivi (dal design, sezione 3 e 6)

- **Piano 2, il movimento:** camera che si avvicina e arretra (risveglio, titolo, shipped); terminale in prospettiva con la camera che lo segue; il terminale che diventa la card sul polso e la lista che si compone; card ✓ con il tratto disegnato; il ✓ che diventa il logo con l'orologio che resta nel cartello; tasti in profondità; parallasse della corsia; camera che respira. Si scrive dopo la bozza 0, sui file che questo piano lascia.
- **Piano 3, la prova 3D:** T0-T3 del design, sezione 6. Composizione di prova separata, file del modello fuori da git.
