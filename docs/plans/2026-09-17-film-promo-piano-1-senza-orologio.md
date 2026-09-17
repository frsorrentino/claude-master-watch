# Film promozionale, piano 1 di 2: Foto, Motore, Audio (senza orologio)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> Nota di rotta: l'aspetto (Task 3, 7-10, 12-14) è lavoro visivo iterativo e resta alla sessione principale; delegabili
> solo i Task di logica con test (2, 4, 5, 6, 15 e le funzioni pure dei Task 17 e 20).

**Goal:** arrivare, senza l'orologio al polso, a un film di 66 s completo di motore, mockup fotografico e audio, con le
clip di stamattina come segnaposto, passando dai punti di controllo 1, 2 e 3 di Franz.

**Architecture:** Python (numpy + Pillow) trasforma le foto di Franz in immagini con trasparenza e in un file di misure;
Remotion le compone con il video dell'interfaccia. Il film è una scaletta JSON in battiti, validata a ogni avvio, che
comanda componenti React; fotogrammi e durate si ricavano solo dalla griglia dei battiti. L'audio si misura con numpy,
`wave` e ffmpeg, si mixa dentro Remotion sulla stessa scaletta.

**Tech Stack:** Remotion 4.0.490 · React 19 · TypeScript 5.9 · Node 24 (`node --test` esegue i `.ts` senza dipendenze) ·
Python 3 con numpy 2.4.6 e Pillow 9.4 · ffmpeg (ebur128, loudnorm) · faster-whisper via
`~/.claude/plugins/cache/fsorrentino/fable-director/1.43.1/scripts/transcribe.py` · Gemini TTS.

Progetto di riferimento: `docs/plans/2026-09-17-film-promo-design.md` (approvato: non si ridiscute). Il piano 2
(Momenti mancanti, Registrazione, Montaggio e consegne) si scrive quando l'orologio è disponibile e il punto 1 è chiuso.

## Global Constraints

- 1920×1080, 30 fps, H.264 + AAC. Testi a schermo solo in inglese.
- Caratteri: Inter e Noto Sans Mono (OFL), inclusi nel progetto. Mai Google Sans.
- Display = 0,86 del raggio del vetro. Inclinazione dell'orologio entro 10 gradi. Nelle scene di lettura il display
  occupa almeno il 55 % dell'altezza.
- Niente dissolvenze tra le scene: tagli sul battito. Un colore di fondo per atto; nero in apertura e chiusura.
- Al massimo un suono d'interfaccia per battito. Musica −12 dB sotto la voce. Finale −14 LUFS ±0,5, picco ≤ −1 dB.
- Fuori da git: foto, immagini del mockup, clip, musica, voce, render. Nel repo solo codice, misure, scaletta, licenze.
- `tools/promo/remotion/node_modules` è un collegamento a `video/storytelling-video`: non si tocca, non si installa nulla.
- Mai render e Gradle insieme. Processi lunghi: `setsid nohup … > log 2>&1 &`, poi attesa in primo piano sul log.
- `scipy` non funziona: solo numpy e `wave`.
- Commit in inglese, file aggiunti per nome, mai `git add -A`. Push: nessuno in questo piano.
- Claude non sente: ogni giudizio sull'audio è una misura o un ascolto di Franz.

## Struttura dei file

```
tools/promo/
  materiali/                         (ignorata) foto originali, ritagli, musica, suoni, voce
  mockup/                            pose.py clean.py matte_front.py outline.py export.py test_mockup.py
                                     q34_outline.json
  audio/                             measure.py track_card.py cut_track.py voice.py speaker_fx.sh test_measure.py
  remotion/
    public/mockup/  public/audio/    (ignorate) prodotti di export.py e dell'audio
    public/fonts/                    Inter-SemiBold.ttf Inter-Medium.ttf NotoSansMono-Regular.ttf OFL-*.txt
    src/film/
      beats.ts homography.ts moves.ts timeline.ts sound.ts        logica pura, ognuna con *.test.ts
      check.ts                       valida timeline.json e l'esistenza delle clip
      timeline.json                  LA SCALETTA
      mockup.geometry.json           misure scritte da export.py
      theme.ts fonts.ts              colori, grandezze, caricamento dei caratteri
      WordMask.tsx Backdrop.tsx PhotoWatch.tsx Fx.tsx WatchText.tsx EndCard.tsx Soundtrack.tsx
      Film.tsx Compare.tsx GridTest.tsx
    src/Root.tsx                     registra Film, Compare, GridTest accanto a Release (che resta)
docs/promo/licenses.md               prove delle licenze
```

---

# Parte I — fino al punto di controllo 1

### Task 1: Materiali fuori da git

**Files:**
- Modify: `.gitignore`
- Create (ignorata): `tools/promo/materiali/foto/{originali,}`

- [ ] **Step 1: aggiungere in coda a `.gitignore`**

```gitignore
# Film promozionale: materiali (foto, musica, voce) e prodotti del mockup e dell'audio restano fuori dal repo
tools/promo/materiali/
tools/promo/remotion/public/mockup/
tools/promo/remotion/public/audio/
```

- [ ] **Step 2: spostare le foto e copiare ritagli e misure**

```bash
cd ~/Desktop/workspaces/personali/claude-master-watch
mkdir -p tools/promo/materiali/foto/originali
mv PXL_20260917_15*.jpg tools/promo/materiali/foto/originali/
P=tools/promo/out/prototipi-mockup/foto
cp $P/f5.png $P/f5_clean.png $P/f5_circle.npy $P/q10.png $P/q10_clean.png $P/ui_face.png $P/ui_list.png $P/ui_question.png tools/promo/materiali/foto/
```

`f5` è il quinto scatto (frontale), `q10` il decimo (tre quarti), ritagli 1900×1900 a metà risoluzione; `*_clean` sono
già passati da `clean.despeck`.

- [ ] **Step 3: verificare**

Run: `ls PXL_* 2>/dev/null | wc -l; ls tools/promo/materiali/foto/originali | wc -l; git status --short`
Expected: `0`, `10`, e in `git status` solo `.gitignore` modificato più i tre non tracciati di prima.

- [ ] **Step 4: commit**

```bash
git add .gitignore
git commit -m "chore(promo): keep film materials and generated mockup/audio assets out of the repo"
```

### Task 2: Moduli del mockup

**Files:**
- Create: `tools/promo/mockup/{pose.py,clean.py,matte_front.py}` (copie dei prototipi)
- Test: `tools/promo/mockup/test_mockup.py`

**Interfaces:**
- Produces: `pose.fit() -> (p, err)`, `pose.display_quad(p, k=0.86, depth=0.03) -> [TL,TR,BR,BL]`;
  `python3 matte_front.py <dir>` legge `<dir>/f5_clean.png` e `<dir>/f5_circle.npy`, scrive `f5_body.png`,
  `f5_mask.png`, `f5_case.npy` (`[cx, cy, raggio cassa con smusso]`).

- [ ] **Step 1: scrivere il test che fallisce** — `tools/promo/mockup/test_mockup.py`

```python
"""Le misure approvate il 17/09: se cambiano, il mockup non è più quello che Franz ha visto."""
import subprocess, sys, unittest
from pathlib import Path
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
MAT = HERE.parent / "materiali/foto"

class Posa(unittest.TestCase):
    def test_inclinazione_e_distanza(self):
        import pose
        p, err = pose.fit()
        self.assertLess(err, 1e-6)
        self.assertAlmostEqual(np.degrees(p[3]), 34.0, delta=0.5)
        self.assertAlmostEqual(p[2], 8.99, delta=0.1)

    def test_convergenza_del_display(self):
        import pose
        q = np.array(pose.display_quad(pose.fit()[0]))
        lontano, vicino = np.linalg.norm(q[3] - q[0]), np.linalg.norm(q[2] - q[1])
        self.assertAlmostEqual(lontano / vicino, 0.899, delta=0.01)   # ~11 % di convergenza
        np.testing.assert_allclose(q[0], [441.4, 366.2], atol=1.0)

class Frontale(unittest.TestCase):
    def test_scontorno_per_costruzione(self):
        out = subprocess.run([sys.executable, str(HERE / "matte_front.py"), str(MAT)], capture_output=True, text=True, check=True).stdout
        scarto = float(out.split("scarto medio ")[1].split(" px")[0])
        self.assertLess(scarto, 0.6)
        cx, cy, rc = np.load(MAT / "f5_case.npy")
        m = np.asarray(Image.open(MAT / "f5_mask.png").convert("L"))
        self.assertEqual(m[int(cy), int(cx)], 255)
        self.assertEqual(m[5, 5], 0)
        self.assertGreaterEqual(m[int(cy), int(cx - rc + 3)], 250)      # lo smusso lucido resta dentro, anche ai lati
        self.assertLess(m[int(cy), int(cx - rc - 4)], 10)

if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: eseguirlo e vederlo fallire**

Run: `cd tools/promo/mockup && python3 -m unittest test_mockup -v`
Expected: FAIL, `ModuleNotFoundError: No module named 'pose'`.

- [ ] **Step 3: portare i prototipi**

```bash
cp tools/promo/out/prototipi-mockup/{pose.py,clean.py,matte_front.py} tools/promo/mockup/
```

- [ ] **Step 4: eseguirlo e vederlo passare**

Run: `cd tools/promo/mockup && python3 -m unittest test_mockup -v`
Expected: 3 test OK (il frontale impiega ~1 minuto).

- [ ] **Step 5: commit**

```bash
git add tools/promo/mockup/pose.py tools/promo/mockup/clean.py tools/promo/mockup/matte_front.py tools/promo/mockup/test_mockup.py
git commit -m "feat(promo): photo mockup modules — camera pose, dust cleanup, front matte by construction, with the approved measures as tests"
```

### Task 3: Contorno a mano del tre quarti

Nel tre quarti l'ombra sul tessuto ha la luminosità del metallo: nessuna soglia regge. Il contorno è un poligono in
coordinate di `q10.png`, seminato dalla soglia e corretto a mano guardando gli ingrandimenti.

**Files:**
- Create: `tools/promo/mockup/outline.py`, `tools/promo/mockup/q34_outline.json`

**Interfaces:**
- Produces: `q34_outline.json` = `{"size": 1900, "points": [[x, y], …]}`, in senso orario; `outline.mask(points, n) -> PIL 'L'`.

- [ ] **Step 1: scrivere `outline.py`**

```python
"""Contorno del tre quarti: `seed` lo semina dalla soglia dei prototipi, `check` disegna il poligono sulla foto a 3x in
otto riquadri da guardare uno per uno. I punti si correggono a mano in q34_outline.json e si rilancia `check`."""
import json, sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = Path(__file__).resolve().parent
MAT = HERE.parent / "materiali/foto"
OUT = HERE.parent / "out/contorno"
JSON = HERE / "q34_outline.json"
CX, CY, A, B, ROT = 872., 880., 525., 640., 7.
SHADOW = (7.5, 47.5)                                         # gradi dal centro, in senso orario dalle ore 3: tra la corona e l'ansa in basso

def mask(points, n, ss=4):
    m = Image.new("L", (n * ss, n * ss), 0)
    ImageDraw.Draw(m).polygon([(x * ss, y * ss) for x, y in points], fill=255)
    return m.resize((n, n), Image.LANCZOS).filter(ImageFilter.GaussianBlur(1.2))

def seed():
    im = Image.open(MAT / "q10_clean.png").convert("L").filter(ImageFilter.GaussianBlur(2)); n = im.width
    L = np.asarray(im).astype(np.float32); yy, xx = np.mgrid[0:n, 0:n]
    def ell(cx, cy, a, b, rot):
        c, s = np.cos(np.deg2rad(rot)), np.sin(np.deg2rad(rot)); u = (xx - cx) * c + (yy - cy) * s; v = -(xx - cx) * s + (yy - cy) * c
        return (u / a) ** 2 + (v / b) ** 2
    dark = np.asarray(Image.fromarray(((L < 78) * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.MinFilter(9))) > 0
    m = dark | (ell(CX + 17, CY + 12, A + 17, B + 12, ROT) <= 1) | (ell(1495, 830, 64, 104, 4) <= 1)
    s = Image.fromarray((m * 255).astype(np.uint8)).copy(); ImageDraw.floodfill(s, (int(CX), int(CY)), 77); body = np.asarray(s) == 77
    wall = ell(CX + 30, CY + 14, A + 34, B + 16, ROT) <= 1       # bordo esterno della parete della cassa sul lato della corona (misurato sugli ingrandimenti)
    pts = []
    for deg in np.arange(0, 360, 1.0):                       # il raggio esce dal centro: tengo l'ultimo pixel del corpo
        r = np.arange(0, n * 0.75, 0.5); x = CX + r * np.cos(np.deg2rad(deg)); y = CY + r * np.sin(np.deg2rad(deg))
        ok = (x >= 0) & (x < n - 1) & (y >= 0) & (y < n - 1); hit = np.where(body[y[ok].astype(int), x[ok].astype(int)])[0]
        j = hit.max()
        if SHADOW[0] <= deg <= SHADOW[1]:                     # sotto la corona l'ombra ha la luminosità del metallo: lì vale la parete della cassa, costruita
            j = np.where(wall[y[ok].astype(int), x[ok].astype(int)])[0].max()
        pts.append([round(float(x[ok][j]), 1), round(float(y[ok][j]), 1)])
    JSON.write_text(json.dumps({"size": n, "points": pts}, indent=0)); print(len(pts), "punti in", JSON)

def check():
    d = json.loads(JSON.read_text()); im = Image.open(MAT / "q10_clean.png").convert("RGB"); n = im.width; OUT.mkdir(parents=True, exist_ok=True)
    big = im.resize((n * 3, n * 3), Image.LANCZOS); dr = ImageDraw.Draw(big); P = [(x * 3, y * 3) for x, y in d["points"]]
    dr.line(P + P[:1], fill=(0, 255, 0), width=2)
    for i, (x, y) in enumerate(P):
        dr.ellipse([x - 4, y - 4, x + 4, y + 4], outline=(255, 255, 0)); dr.text((x + 6, y - 6), str(i), fill=(255, 255, 0))
    w = n * 3 // 2
    for k in range(8):                                       # otto riquadri lungo il contorno, non una griglia cieca
        cx, cy = P[k * len(P) // 8]; box = [int(cx - w / 2), int(cy - w / 2), int(cx + w / 2), int(cy + w / 2)]
        big.crop(box).resize((1400, 1400), Image.LANCZOS).save(OUT / f"contorno_{k}.jpg", quality=88)
    cut = Image.new("RGB", (n, n), (150, 60, 160)); cut.paste(im, (0, 0), mask(d["points"], n)); cut.save(OUT / "ritaglio_su_viola.jpg", quality=90)
    print("guardare", OUT)

if __name__ == "__main__":
    {"seed": seed, "check": check}[sys.argv[1]]()
```

- [ ] **Step 2: seminare e guardare**

Run: `cd tools/promo/mockup && python3 outline.py seed && python3 outline.py check`
Expected: `360 punti`, otto `contorno_N.jpg` e `ritaglio_su_viola.jpg` in `tools/promo/out/contorno/`.

- [ ] **Step 3: correggere a mano**

Aprire ogni `contorno_N.jpg` con Read. Dove la linea verde segue l'ombra invece del metallo (atteso: lato in basso a
destra della cassa e sotto la corona), correggere le coordinate dei punti numerati in `q34_outline.json` (coordinate
foto = coordinate nel riquadro ÷ 3 + origine del riquadro; più semplice: spostare il punto di pochi pixel lungo il
raggio e rilanciare `check`). Dove il cinturino tocca il bordo della foto i punti restano sul bordo. Ripetere finché su
`ritaglio_su_viola.jpg` non si vede né alone di tessuto né metallo tagliato. Al massimo 5 giri: se non basta, dirlo a
Franz con gli ingrandimenti.

- [ ] **Step 4: commit**

```bash
git add tools/promo/mockup/outline.py tools/promo/mockup/q34_outline.json
git commit -m "feat(promo): hand-corrected outline of the three-quarter photo, with the magnified check sheets"
```

### Task 4: Esportazione verso Remotion

**Files:**
- Create: `tools/promo/mockup/export.py`, `tools/promo/remotion/src/film/mockup.geometry.json` (scritto dallo script)
- Modify: `tools/promo/mockup/test_mockup.py`

**Interfaces:**
- Consumes: `outline.mask`, `pose.fit`, `pose.display_quad`, i prodotti di `matte_front.py`.
- Produces: `public/mockup/front_body.png` (RGBA), `q34_body.png` (RGBA), `q34_reflections.png` (RGB, nero fuori dal
  display) e

```json
{ "front": { "size": 1900, "cx": 941.0, "cy": 957.5, "caseR": 565.4, "glassR": 556.1, "coverR": 508.8, "displayR": 478.2 },
  "q34":   { "size": 1900, "cx": 872, "cy": 880, "b": 640, "quad": [[441.4,366.2],[1342.5,298.9],[1309.4,1453.1],[403.9,1404.1]] } }
```

- [ ] **Step 1: test che fallisce** — aggiungere a `test_mockup.py`

```python
class Esportazione(unittest.TestCase):
    def test_immagini_e_misure(self):
        import json
        subprocess.run([sys.executable, str(HERE / "export.py")], check=True)
        pub = HERE.parent / "remotion/public/mockup"; g = json.loads((HERE.parent / "remotion/src/film/mockup.geometry.json").read_text())
        self.assertAlmostEqual(g["front"]["displayR"] / g["front"]["glassR"], 0.86, places=3)
        self.assertEqual(len(g["q34"]["quad"]), 4)
        for name in ("front_body.png", "q34_body.png"):
            self.assertEqual(Image.open(pub / name).mode, "RGBA")
        r = np.asarray(Image.open(pub / "q34_reflections.png").convert("L"))
        self.assertEqual(int(r[5, 5]), 0)                         # fuori dal display: nero, neutro nella fusione «schermo»
        self.assertGreater(int(r.max()), 60)                      # dentro: i riflessi veri ci sono
```

Run: `python3 -m unittest test_mockup.Esportazione -v` → FAIL (`export.py` non esiste).

- [ ] **Step 2: scrivere `export.py`**

```python
"""Porta il mockup nel montaggio: immagini con trasparenza in remotion/public/mockup/, misure in src/film/mockup.geometry.json."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter
import outline, pose

HERE = Path(__file__).resolve().parent
MAT = HERE.parent / "materiali/foto"
PUB = HERE.parent / "remotion/public/mockup"
GEO = HERE.parent / "remotion/src/film/mockup.geometry.json"
CH, K = 7.8, 0.86                                  # smusso lucido (come matte_front.py) e raggio del display sul vetro

def grade(im, contrast, red):                      # la stessa correzione dei fotogrammi di prova: contrasto e dominante fredda
    r, g, b = ImageEnhance.Contrast(im).enhance(contrast).split()
    return Image.merge("RGB", (r.point(lambda v: int(v * red)), g, b.point(lambda v: min(255, int(v * 1.05)))))

def front():
    im = grade(Image.open(MAT / "f5_body.png").convert("RGB"), 1.12, 0.96); im.putalpha(Image.open(MAT / "f5_mask.png").convert("L"))
    im.save(PUB / "front_body.png")
    cx, cy, rc = (float(v) for v in np.load(MAT / "f5_case.npy")); glass = rc - CH - 1.5
    return {"size": im.width, "cx": round(cx, 2), "cy": round(cy, 2), "caseR": round(rc, 2), "glassR": round(glass, 2),
            "coverR": round(glass * 0.915, 2), "displayR": round(glass * K, 2)}

def q34():
    photo = Image.open(MAT / "q10_clean.png").convert("RGB"); n = photo.width
    body = grade(photo, 1.10, 0.95); pts = json.loads((HERE / "q34_outline.json").read_text())["points"]
    rgba = body.copy(); rgba.putalpha(outline.mask(pts, n)); rgba.save(PUB / "q34_body.png")
    quad = pose.display_quad(pose.fit()[0], k=K, depth=0.03); m = 960
    disc = Image.new("L", (m, m), 0); ImageDraw.Draw(disc).ellipse([2, 2, m - 3, m - 3], fill=255)
    M, rhs = [], []
    for (x, y), (X, Y) in zip(quad, [(0, 0), (m, 0), (m, m), (0, m)]):      # coefficienti uscita -> ingresso, come vuole PIL
        M.append([x, y, 1, 0, 0, 0, -X * x, -X * y]); rhs.append(X); M.append([0, 0, 0, x, y, 1, -Y * x, -Y * y]); rhs.append(Y)
    co = np.linalg.solve(np.array(M, float), np.array(rhs, float))
    a = np.asarray(disc.filter(ImageFilter.GaussianBlur(2)).transform((n, n), Image.PERSPECTIVE, tuple(co), Image.BICUBIC)).astype(np.float32) / 255
    refl = np.clip((np.asarray(body).astype(np.float32) - 40) * 1.20, 0, 255) * a[..., None]
    Image.fromarray(refl.astype(np.uint8)).save(PUB / "q34_reflections.png")
    return {"size": n, "cx": 872, "cy": 880, "b": 640, "quad": [[round(float(v), 1) for v in q] for q in quad]}

if __name__ == "__main__":
    PUB.mkdir(parents=True, exist_ok=True); GEO.parent.mkdir(parents=True, exist_ok=True)
    GEO.write_text(json.dumps({"front": front(), "q34": q34()}, indent=2) + "\n"); print(GEO.read_text())
```

- [ ] **Step 3: vederlo passare** — `python3 -m unittest test_mockup -v` → 4 test OK.

- [ ] **Step 4: commit**

```bash
git add tools/promo/mockup/export.py tools/promo/mockup/test_mockup.py tools/promo/remotion/src/film/mockup.geometry.json
git commit -m "feat(promo): export the photo mockup to Remotion — alpha cut-outs, true reflections layer, measured geometry"
```

### Task 5: Griglia dei battiti e omografia

**Files:**
- Create: `tools/promo/remotion/src/film/{beats.ts,beats.test.ts,homography.ts,homography.test.ts}`
- Modify: `tools/promo/remotion/tsconfig.json`, `tools/promo/remotion/package.json`

**Interfaces:**
- Produces: `type Grid = { bpm; fps; offsetSeconds }`, `beatToFrame(g, beat): number`, `spanFrames(g, at, len): number`;
  `type Pt = [number, number]`, `type Quad = [Pt, Pt, Pt, Pt]`, `homography(n, dst: Quad): number[]` (9 valori, porta il
  quadrato `[0,n]²` su `dst` nell'ordine TL, TR, BR, BL), `applyH(h, p): Pt`, `toMatrix3d(h): string`.

- [ ] **Step 1: configurazione.** In `tsconfig.json`, dentro `compilerOptions`, aggiungere
  `"allowImportingTsExtensions": true` e portare `"lib"` a `["es2020", "dom"]`. In `package.json`, negli `scripts`:

```json
"test": "node --test \"src/film/*.test.ts\"",
"check": "tsc --noEmit && node --test \"src/film/*.test.ts\" && node src/film/check.ts"
```

Gli import tra file del film usano l'estensione esplicita (`./beats.ts`) e `import type` per i soli tipi: Node esegue
i `.ts` togliendo i tipi e non risolve i nomi senza estensione.

- [ ] **Step 2: test che falliscono**

`beats.test.ts`:
```ts
import test from "node:test";
import assert from "node:assert/strict";
import { beatToFrame, spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";

const g100: Grid = { bpm: 100, fps: 30, offsetSeconds: 0 };

test("a 100 battiti al minuto un battito vale 18 fotogrammi e 110 battiti fanno 66 secondi", () => {
  assert.equal(beatToFrame(g100, 1), 18);
  assert.equal(beatToFrame(g100, 0.5), 9);
  assert.equal(beatToFrame(g100, 110), 1980);
});

test("con un tempo non intero le scene in fila non accumulano deriva", () => {
  const g: Grid = { bpm: 97, fps: 30, offsetSeconds: 0.35 };
  let end = beatToFrame(g, 0);
  for (let b = 0; b < 110; b++) end += spanFrames(g, b, 1);
  assert.equal(end, beatToFrame(g, 110));
});
```

`homography.test.ts`:
```ts
import test from "node:test";
import assert from "node:assert/strict";
import { applyH, homography, toMatrix3d } from "./homography.ts";
import type { Quad } from "./homography.ts";

const QUAD: Quad = [[441.4, 366.2], [1342.5, 298.9], [1309.4, 1453.1], [403.9, 1404.1]];

test("gli angoli del quadrato cadono sugli angoli misurati del display", () => {
  const h = homography(480, QUAD);
  ([[0, 0], [480, 0], [480, 480], [0, 480]] as const).forEach((p, i) => {
    const [x, y] = applyH(h, [p[0], p[1]]);
    assert.ok(Math.abs(x - QUAD[i][0]) < 1e-6 && Math.abs(y - QUAD[i][1]) < 1e-6);
  });
});

test("il centro del cerchio proiettato NON è il centro del quadrilatero: sta verso il lato lontano", () => {
  const [x] = applyH(homography(480, QUAD), [240, 240]);
  const media = (QUAD[0][0] + QUAD[1][0] + QUAD[2][0] + QUAD[3][0]) / 4;
  assert.ok(x < media - 5);
});

test("la matrice CSS è per colonne e ha sedici valori", () => {
  const m = toMatrix3d([1, 0, 10, 0, 1, 20, 0, 0, 1]);
  assert.equal(m, "matrix3d(1,0,0,0,0,1,0,0,0,0,1,0,10,20,0,1)");
});
```

Run: `cd tools/promo/remotion && npm test` → FAIL, `Cannot find module …/beats.ts`.

- [ ] **Step 3: implementazione**

`beats.ts`:
```ts
/** La griglia dei battiti: il film si scrive in battiti; i fotogrammi si ricavano da qui e solo da qui. */
export type Grid = { bpm: number; fps: number; offsetSeconds: number };

export const beatToFrame = (g: Grid, beat: number): number => Math.round((g.offsetSeconds + (beat * 60) / g.bpm) * g.fps);

/** Durata di un tratto come differenza di due posizioni arrotondate: le scene in fila non derivano. */
export const spanFrames = (g: Grid, at: number, len: number): number => beatToFrame(g, at + len) - beatToFrame(g, at);
```

`homography.ts`:
```ts
/** Prospettiva vera del tre quarti: l'interfaccia (un quadrato) finisce sul quadrilatero misurato dal modello di camera. */
export type Pt = [number, number];
export type Quad = [Pt, Pt, Pt, Pt];

const solve = (A: number[][], b: number[]): number[] => {
  const n = b.length;
  const M = A.map((row, i) => [...row, b[i]]);
  for (let c = 0; c < n; c++) {
    let p = c;
    for (let r = c + 1; r < n; r++) if (Math.abs(M[r][c]) > Math.abs(M[p][c])) p = r;
    [M[c], M[p]] = [M[p], M[c]];
    for (let r = 0; r < n; r++) {
      if (r === c) continue;
      const f = M[r][c] / M[c][c];
      for (let k = c; k <= n; k++) M[r][k] -= f * M[c][k];
    }
  }
  return M.map((row, i) => row[n] / row[i]);
};

export const homography = (n: number, dst: Quad): number[] => {
  const src: Quad = [[0, 0], [n, 0], [n, n], [0, n]];
  const A: number[][] = [];
  const b: number[] = [];
  src.forEach(([x, y], i) => {
    const [X, Y] = dst[i];
    A.push([x, y, 1, 0, 0, 0, -X * x, -X * y]); b.push(X);
    A.push([0, 0, 0, x, y, 1, -Y * x, -Y * y]); b.push(Y);
  });
  return [...solve(A, b), 1];
};

export const applyH = (h: number[], [x, y]: Pt): Pt => {
  const w = h[6] * x + h[7] * y + h[8];
  return [(h[0] * x + h[1] * y + h[2]) / w, (h[3] * x + h[4] * y + h[5]) / w];
};

/** CSS vuole la 4×4 per colonne; con `transform-origin: 0 0`. */
export const toMatrix3d = (h: number[]): string =>
  `matrix3d(${[h[0], h[3], 0, h[6], h[1], h[4], 0, h[7], 0, 0, 1, 0, h[2], h[5], 0, h[8]].join(",")})`;
```

- [ ] **Step 4:** `npm test` → 5 test passati. `npx tsc --noEmit` → nessun errore.

- [ ] **Step 5: commit**

```bash
git add tools/promo/remotion/tsconfig.json tools/promo/remotion/package.json tools/promo/remotion/src/film/beats.ts tools/promo/remotion/src/film/beats.test.ts tools/promo/remotion/src/film/homography.ts tools/promo/remotion/src/film/homography.test.ts
git commit -m "feat(promo): beat grid without drift and the display homography, tested with node --test"
```

### Task 6: Scaletta e validazione

**Files:**
- Create: `tools/promo/remotion/src/film/{moves.ts,moves.test.ts,timeline.ts,timeline.test.ts,check.ts,timeline.json}`

**Interfaces:**
- Consumes: `Grid`.
- Produces:

```ts
// moves.ts
export type Move = "riseIn" | "slideIn" | "slideOut" | "pushIn" | "pullOut";
export type Pose = { x: number; y: number; scale: number; tilt: number };   // x, y in frazioni del quadro; tilt in gradi
export const MAX_TILT = 9;
export const MOVE_BEATS = 2;
export function poseAt(frame: number, total: number, moveFrames: number, enter?: Move, exit?: Move): Pose;

// timeline.ts
export type Act = "open" | "know" | "act" | "control" | "close";
export type Fx =
  | { kind: "tap"; at: number; x: number; y: number }               // x, y nello schermo dell'orologio, 0-480
  | { kind: "longPress"; at: number; len: number }
  | { kind: "haptic"; at: number }
  | { kind: "counter"; at: number; len: number; to: number; suffix: string }
  | { kind: "typed"; at: number; len: number; text: string }
  | { kind: "terminal"; at: number; every: number; lines: string[] }
  | { kind: "spoken"; at: number; len: number; voice: string; words: string };   // file in public/audio/
export type WatchCue = { view: "front" | "threeQuarter" | "drawn"; clip: string; clipStart?: number; enter?: Move; exit?: Move };
export type TextCue = { lines: string[]; accent?: string; size?: "title" | "service"; at?: number; sub?: string };
export type Scene = { id: string; at: number; len: number; act: Act; watch?: WatchCue; text?: TextCue; fx?: Fx[]; endCard?: boolean };
export type Timeline = Grid & { music?: string; scenes: Scene[] };
export class TimelineError extends Error { problems: string[] }
export function validateTimeline(raw: unknown): Timeline;      // lancia TimelineError con TUTTI i problemi
export const totalBeats: (t: Timeline) => number;
```

Tutti i tempi dentro una scena (`text.at`, `fx[].at`) sono battiti dall'inizio della scena; mezzi battiti ammessi.

- [ ] **Step 1: test che falliscono**

`moves.test.ts`:
```ts
import test from "node:test";
import assert from "node:assert/strict";
import { MAX_TILT, poseAt } from "./moves.ts";
import type { Move } from "./moves.ts";

const MOVES: Move[] = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut"];

test("l'inclinazione resta entro i 10 gradi in ogni momento di ogni movimento", () => {
  for (const enter of MOVES) for (const exit of MOVES) for (let f = 0; f <= 120; f++) {
    assert.ok(Math.abs(poseAt(f, 120, 36, enter, exit).tilt) <= 10, `${enter}/${exit} @${f}`);
  }
  assert.ok(MAX_TILT <= 10);
});

test("finita l'entrata l'orologio è al centro, a grandezza piena, con la sola deriva lenta", () => {
  const p = poseAt(60, 120, 36, "riseIn", undefined);
  assert.ok(Math.abs(p.x) < 0.02 && Math.abs(p.y) < 0.02 && Math.abs(p.scale - 1) < 0.04);
});

test("riseIn parte da sotto il quadro", () => {
  assert.ok(poseAt(0, 120, 36, "riseIn").y > 0.8);
});
```

`timeline.test.ts`:
```ts
import test from "node:test";
import assert from "node:assert/strict";
import { TimelineError, totalBeats, validateTimeline } from "./timeline.ts";

const base = (): any => ({
  bpm: 100, fps: 30, offsetSeconds: 0,
  scenes: [
    { id: "open", at: 0, len: 8, act: "open", text: { lines: ["Claude is working."], accent: "working." } },
    { id: "list", at: 8, len: 8, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4", enter: "slideIn" },
      text: { lines: ["Every session.", "One glance."], accent: "glance." }, fx: [{ kind: "tap", at: 3, x: 240, y: 300 }] },
  ],
});
const problems = (raw: unknown): string[] => {
  try { validateTimeline(raw); return []; } catch (e) { if (e instanceof TimelineError) return e.problems; throw e; }
};

test("una scaletta giusta passa e dice quanto dura", () => {
  assert.equal(totalBeats(validateTimeline(base())), 16);
});
test("un buco o una sovrapposizione tra scene è un errore", () => {
  const t = base(); t.scenes[1].at = 9;
  assert.match(problems(t).join("\n"), /list: inizia al battito 9, la scena prima finisce a 8/);
});
test("la parola in colore deve essere una parola della frase", () => {
  const t = base(); t.scenes[0].text.accent = "sleeping.";
  assert.match(problems(t).join("\n"), /open: «sleeping\.» non è tra le parole/);
});
test("un effetto fuori dalla sua scena è un errore", () => {
  const t = base(); t.scenes[1].fx[0].at = 8;
  assert.match(problems(t).join("\n"), /list: l'effetto tap al battito 8 esce dalla scena/);
});
test("tipo di effetto, vista, movimento e atto sconosciuti sono errori, tutti insieme", () => {
  const t = base(); t.scenes[1].fx[0].kind = "swipe"; t.scenes[1].watch.view = "side"; t.scenes[1].watch.enter = "spin"; t.scenes[0].act = "intro";
  assert.equal(problems(t).length, 4);
});
test("id doppi, battiti che non sono mezzi, tocchi fuori dallo schermo", () => {
  const t = base(); t.scenes[1].id = "open"; t.scenes[1].fx[0].at = 0.3; t.scenes[1].fx[0].x = 500;
  assert.equal(problems(t).length, 3);
});
test("tre righe al massimo, mai i puntini di sospensione", () => {
  const t = base(); t.scenes[0].text.lines = ["a", "b", "c", "d…"];
  assert.equal(problems(t).length, 3);      // quattro righe, i puntini, e «working.» che non c'è più
});
test("con l'orologio in scena una riga di titolo non supera i 14 caratteri: oltre, finisce sopra la cassa", () => {
  const t = base(); t.scenes[1].text.lines = ["Know your limits."]; t.scenes[1].text.accent = "limits.";
  assert.match(problems(t).join("\n"), /list: la riga «Know your limits\.» ha 17 caratteri, al massimo 14 accanto all'orologio/);
  const solo = base(); solo.scenes[0].text.lines = ["Claude is working hard."]; solo.scenes[0].text.accent = "working";
  assert.deepEqual(problems(solo), []);
});
```

Run: `npm test` → FAIL (moduli mancanti).

- [ ] **Step 2: `moves.ts`**

```ts
/** L'orologio è un'immagine piatta nello spazio: entra, deriva lentamente, esce. Oltre i 10 gradi sembra finto. */
export type Move = "riseIn" | "slideIn" | "slideOut" | "pushIn" | "pullOut";
export type Pose = { x: number; y: number; scale: number; tilt: number };
export const MAX_TILT = 9;
export const MOVE_BEATS = 2;

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const out3 = (t: number) => 1 - Math.pow(1 - t, 3);
const in3 = (t: number) => t * t * t;
const inOut = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

const REST: Pose = { x: 0, y: 0, scale: 1, tilt: 0 };

const moveAt = (m: Move, t: number): Pose => {
  switch (m) {
    case "riseIn": return { x: 0, y: (1 - out3(t)) * 0.95, scale: 0.9 + 0.1 * out3(t), tilt: -6 * (1 - out3(t)) };
    case "slideIn": return { x: (1 - out3(t)) * 0.75, y: 0, scale: 1, tilt: MAX_TILT * (1 - out3(t)) };
    case "slideOut": return { x: -in3(t) * 0.75, y: 0, scale: 1, tilt: -MAX_TILT * in3(t) };
    case "pushIn": return { x: 0, y: 0, scale: 1 + 0.35 * inOut(t), tilt: 0 };
    case "pullOut": return { x: 0, y: 0, scale: 1 - 0.45 * inOut(t), tilt: 4 * inOut(t) };
  }
};

export const poseAt = (frame: number, total: number, moveFrames: number, enter?: Move, exit?: Move): Pose => {
  const a = enter ? moveAt(enter, clamp(frame / moveFrames)) : REST;
  const b = exit ? moveAt(exit, clamp((frame - (total - moveFrames)) / moveFrames)) : REST;
  const p = clamp(frame / Math.max(1, total));
  const drift: Pose = { x: 0.006 * Math.sin(p * Math.PI), y: -0.01 * p, scale: 1 + 0.03 * p, tilt: 1.5 * Math.sin(p * Math.PI) - 0.75 };
  const tilt = Math.max(-10, Math.min(10, a.tilt + b.tilt + drift.tilt));
  return { x: a.x + b.x + drift.x, y: a.y + b.y + drift.y, scale: a.scale * b.scale * drift.scale, tilt };
};
```

- [ ] **Step 3: `timeline.ts`** — i tipi dell'elenco «Produces» qui sopra, poi:

```ts
import type { Grid } from "./beats.ts";
import type { Move } from "./moves.ts";

/** La scaletta: scene in fila, in battiti. Dentro una scena i tempi (text.at, fx[].at) partono dall'inizio della scena; mezzi battiti ammessi. */
export type Act = "open" | "know" | "act" | "control" | "close";
export type Fx =
  | { kind: "tap"; at: number; x: number; y: number }               // x, y nello schermo dell'orologio, 0-480
  | { kind: "longPress"; at: number; len: number }
  | { kind: "haptic"; at: number }
  | { kind: "counter"; at: number; len: number; to: number; suffix: string }
  | { kind: "typed"; at: number; len: number; text: string }
  | { kind: "terminal"; at: number; every: number; lines: string[] }
  | { kind: "spoken"; at: number; len: number; voice: string; words: string };   // file in public/audio/
export type WatchCue = { view: "front" | "threeQuarter" | "drawn"; clip: string; clipStart?: number; enter?: Move; exit?: Move };
export type TextCue = { lines: string[]; accent?: string; size?: "title" | "service"; at?: number; sub?: string };
export type Scene = { id: string; at: number; len: number; act: Act; watch?: WatchCue; text?: TextCue; fx?: Fx[]; endCard?: boolean };
export type Timeline = Grid & { music?: string; scenes: Scene[] };

export class TimelineError extends Error {
  problems: string[];
  constructor(problems: string[]) {
    super(`Scaletta sbagliata:\n- ${problems.join("\n- ")}`);
    this.problems = problems;
  }
}

const ACTS = ["open", "know", "act", "control", "close"];
const VIEWS = ["front", "threeQuarter", "drawn"];
const MOVES = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut"];
const FX = ["tap", "longPress", "haptic", "counter", "typed", "terminal", "spoken"];
const half = (v: unknown): v is number => typeof v === "number" && v >= 0 && Number.isInteger(v * 2);

export const totalBeats = (t: Timeline): number => (t.scenes.length ? t.scenes[t.scenes.length - 1].at + t.scenes[t.scenes.length - 1].len : 0);

export const validateTimeline = (raw: unknown): Timeline => {
  const t = raw as Timeline;
  const bad: string[] = [];
  if (!(t.bpm >= 60 && t.bpm <= 160)) bad.push(`bpm ${t.bpm} fuori da 60-160`);
  if (t.fps !== 30) bad.push(`fps ${t.fps}: il film è a 30`);
  if (typeof t.offsetSeconds !== "number") bad.push("offsetSeconds manca");
  const ids = new Set<string>();
  let end = 0;
  for (const s of t.scenes ?? []) {
    const say = (m: string) => bad.push(`${s.id}: ${m}`);
    if (ids.has(s.id)) say("id doppio");
    ids.add(s.id);
    if (!half(s.at) || !half(s.len) || s.len <= 0) say(`at/len devono essere battiti o mezzi battiti (at ${s.at}, len ${s.len})`);
    if (s.at !== end) say(`inizia al battito ${s.at}, la scena prima finisce a ${end}`);
    end = s.at + s.len;
    if (!ACTS.includes(s.act)) say(`atto «${s.act}» sconosciuto`);
    if (s.watch) {
      if (!VIEWS.includes(s.watch.view)) say(`vista «${s.watch.view}» sconosciuta`);
      for (const m of [s.watch.enter, s.watch.exit]) if (m !== undefined && !MOVES.includes(m)) say(`movimento «${m}» sconosciuto`);
      if (!/^scenes\/[\w.-]+\.mp4$/.test(s.watch.clip)) say(`clip «${s.watch.clip}»: attesa scenes/<nome>.mp4`);
    }
    if (s.text) {
      if (s.text.lines.length < 1 || s.text.lines.length > 3) say(`${s.text.lines.length} righe di testo: da 1 a 3`);
      if (s.text.lines.some((l) => l.includes("…") || l.includes("..."))) say("puntini di sospensione nel testo");
      // accanto all'orologio restano ~735 px: a 110 px sono 14 caratteri (misurato: «Every session.» entra, «Know your limits.» no)
      if (s.watch && (s.text.size ?? "title") === "title") for (const l of s.text.lines) if (l.length > 14) say(`la riga «${l}» ha ${l.length} caratteri, al massimo 14 accanto all'orologio`);
      const words = s.text.lines.flatMap((l) => l.split(" "));
      if (s.text.accent !== undefined && !words.includes(s.text.accent)) say(`«${s.text.accent}» non è tra le parole del testo`);
      if (s.text.at !== undefined && (!half(s.text.at) || s.text.at >= s.len)) say(`il testo al battito ${s.text.at} esce dalla scena`);
    }
    for (const f of s.fx ?? []) {
      if (!FX.includes(f.kind)) { say(`effetto «${f.kind}» sconosciuto`); continue; }
      const len = "len" in f ? f.len : 0;
      if (!half(f.at) || !half(len)) say(`l'effetto ${f.kind} ha tempi che non sono mezzi battiti (at ${f.at})`);
      else if (f.at + len > s.len || f.at >= s.len) say(`l'effetto ${f.kind} al battito ${f.at} esce dalla scena`);
      if (f.kind === "tap" && !(f.x >= 0 && f.x <= 480 && f.y >= 0 && f.y <= 480)) say(`tocco (${f.x}, ${f.y}) fuori dallo schermo 480×480`);
      if ((f.kind === "tap" || f.kind === "longPress" || f.kind === "haptic") && !s.watch) say(`l'effetto ${f.kind} vuole l'orologio in scena`);
    }
  }
  if (bad.length) throw new TimelineError(bad);
  return t;
};
```

- [ ] **Step 4: `check.ts` e la prima scaletta**

```ts
/** `npm run check`: la scaletta vera deve essere valida e ogni clip deve esistere. */
import { existsSync, readFileSync } from "node:fs";
import { totalBeats, validateTimeline } from "./timeline.ts";

const here = new URL(".", import.meta.url).pathname;
const t = validateTimeline(JSON.parse(readFileSync(`${here}timeline.json`, "utf8")));
const missing = t.scenes.filter((s) => s.watch && !existsSync(`${here}../../public/${s.watch.clip}`)).map((s) => `${s.id}: manca public/${s.watch!.clip}`);
if (missing.length) { console.error(missing.join("\n")); process.exit(1); }
console.log(`scaletta valida: ${t.scenes.length} scene, ${totalBeats(t)} battiti, ${((totalBeats(t) * 60) / t.bpm).toFixed(1)} s`);
```

`timeline.json` per il punto 1 (apertura vera + due scene di stile; 32 battiti = 19,2 s):
```json
{
  "bpm": 100, "fps": 30, "offsetSeconds": 0,
  "scenes": [
    { "id": "open-1", "at": 0, "len": 4, "act": "open", "text": { "lines": ["Claude is working."], "accent": "working." } },
    { "id": "open-2", "at": 4, "len": 4, "act": "open", "text": { "lines": ["You’re not", "at your desk."], "accent": "desk." } },
    { "id": "open-3", "at": 8, "len": 5, "act": "open",
      "watch": { "view": "threeQuarter", "clip": "scenes/s0_face.mp4", "enter": "riseIn", "exit": "pushIn" },
      "text": { "lines": ["That’s fine."], "accent": "fine.", "sub": "Claude Master · for Wear OS" } },
    { "id": "list", "at": 13, "len": 7, "act": "know",
      "watch": { "view": "front", "clip": "scenes/s1_list.mp4", "exit": "slideOut" },
      "text": { "lines": ["Every session.", "One glance."], "accent": "glance.", "at": 1 } },
    { "id": "asks", "at": 20, "len": 6, "act": "know",
      "watch": { "view": "front", "clip": "scenes/s2_question.mp4", "clipStart": 5, "enter": "slideIn" },
      "text": { "lines": ["It asks."], "accent": "asks.", "at": 1 }, "fx": [{ "kind": "haptic", "at": 1 }] },
    { "id": "limits", "at": 26, "len": 6, "act": "control",
      "watch": { "view": "front", "clip": "scenes/s6_quota.mp4", "enter": "slideIn" },
      "text": { "lines": ["Know your", "limits."], "accent": "limits.", "at": 1 } }
  ]
}
```

`clipStart` di `asks`: fare `ffmpeg -v error -i public/scenes/s2_question.mp4 -vf fps=1,scale=240:-1,tile=9x1 out/s2_sheet.png`,
guardarlo, e scegliere un secondo in cui la domanda è a schermo e non compare testo di sistema in italiano.

- [ ] **Step 5:** `npm run check` → tutti i test passati e `scaletta valida: 6 scene, 32 battiti, 19.2 s`.

- [ ] **Step 6: commit**

```bash
git add tools/promo/remotion/src/film/moves.ts tools/promo/remotion/src/film/moves.test.ts tools/promo/remotion/src/film/timeline.ts tools/promo/remotion/src/film/timeline.test.ts tools/promo/remotion/src/film/check.ts tools/promo/remotion/src/film/timeline.json
git commit -m "feat(promo): the film as a beat-based cue sheet — validation that lists every problem, watch moves capped at 10 degrees"
```

### Task 7: Caratteri, tema, testo a maschera, fondi

**Files:**
- Create: `tools/promo/remotion/public/fonts/*`, `src/film/{theme.ts,fonts.ts,WordMask.tsx,Backdrop.tsx}`

**Interfaces:**
- Produces: `THEME`, `useFilmFonts(): void`, `<WordMask lines accent size perWordFrames exitAt sub />`, `<Backdrop act />`.

- [ ] **Step 1: caratteri nel progetto**

```bash
cd tools/promo/remotion && mkdir -p public/fonts
cp ~/.local/share/fonts/Inter_24pt-SemiBold.ttf public/fonts/Inter-SemiBold.ttf
cp ~/.local/share/fonts/Inter_24pt-Medium.ttf public/fonts/Inter-Medium.ttf
cp /usr/share/fonts/truetype/noto/NotoSansMono-Regular.ttf public/fonts/
curl -fsSL https://raw.githubusercontent.com/rsms/inter/master/LICENSE.txt -o public/fonts/OFL-Inter.txt
curl -fsSL https://raw.githubusercontent.com/notofonts/latin-greek-cyrillic/main/OFL.txt -o public/fonts/OFL-NotoSansMono.txt
head -3 public/fonts/OFL-*.txt
```
Expected: entrambi i file cominciano con un copyright e citano la «SIL Open Font License». Se un indirizzo risponde 404,
cercare il file di licenza nel repository del carattere e dirlo nel messaggio di commit; mai scrivere la licenza a memoria.

- [ ] **Step 2: `theme.ts`**

```ts
import type { Act } from "./timeline.ts";

export const THEME = {
  white: "#F4F2EC",            // bianco caldo
  accent: "#9EBEFF",           // il blu pastello dell'app
  dim: "#AAB2CD",
  title: 110, service: 44, leftMargin: 150,
  watchX: 0.69,                // centro dell'orologio quando c'è testo a sinistra
  frontGlassPx: 740,           // diametro del vetro nelle scene di lettura: display = 0,86 × 740 = 636 px = 59 % di 1080
  q34GlassPx: 760,
};

/** Un colore per atto: centro, mezzo, bordo del gradiente, e l'alone dietro l'orologio (luce che si SOMMA al fondo: fusione «schermo»). */
export const ACT_BG: Record<Act, [string, string, string, string]> = {
  open: ["#000000", "#000000", "#000000", "rgb(0,0,0)"],
  know: ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
  act: ["#4A3670", "#2C2148", "#17122A", "rgb(44,30,66)"],
  control: ["#1F5A5E", "#15393F", "#0C1F26", "rgb(18,52,54)"],
  close: ["#000000", "#000000", "#000000", "rgb(0,0,0)"],
};
```

- [ ] **Step 3: `fonts.ts`**

```ts
import { useEffect, useState } from "react";
import { continueRender, delayRender, staticFile } from "remotion";

const FACES: [string, string, string][] = [
  ["Inter", "fonts/Inter-SemiBold.ttf", "600"],
  ["Inter", "fonts/Inter-Medium.ttf", "500"],
  ["Noto Sans Mono", "fonts/NotoSansMono-Regular.ttf", "400"],
];

/** Il render aspetta i caratteri: senza, i primi fotogrammi escono con il carattere di ripiego. */
export const useFilmFonts = (): void => {
  const [handle] = useState(() => delayRender("caratteri"));
  useEffect(() => {
    Promise.all(FACES.map(([family, file, weight]) => new FontFace(family, `url(${staticFile(file)})`, { weight }).load().then((f) => document.fonts.add(f))))
      .then(() => continueRender(handle));
  }, [handle]);
};
```

- [ ] **Step 4: `WordMask.tsx`**

```tsx
import React from "react";
import { Easing, interpolate, useCurrentFrame } from "remotion";
import { THEME } from "./theme.ts";

/** Ogni parola sale da una fessura con frenata morbida ed esce verso l'alto. Una sola parola in colore per frase. */
export const WordMask: React.FC<{
  lines: string[]; accent?: string; size?: "title" | "service"; perWordFrames: number; exitAt?: number; sub?: string; align?: "left" | "center";
}> = ({ lines, accent, size = "title", perWordFrames, exitAt, sub, align = "left" }) => {
  const frame = useCurrentFrame();
  const px = size === "title" ? THEME.title : THEME.service;
  const total = lines.reduce((n, l) => n + l.split(" ").length, 0);
  let k = 0;
  const word = (w: string, i: number) => {
    const start = i * perWordFrames;
    const up = interpolate(frame, [start, start + 14], [110, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1) });
    const away = exitAt === undefined ? 0 : interpolate(frame, [exitAt, exitAt + 8], [0, -115], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.in(Easing.cubic) });
    return (
      <span key={i} style={{ display: "inline-block", overflow: "hidden", verticalAlign: "bottom", padding: "0.08em 0 0.16em", marginRight: "0.26em" }}>
        <span style={{ display: "inline-block", translate: `0 ${up + away}%`, color: w === accent ? THEME.accent : THEME.white }}>{w}</span>
      </span>
    );
  };
  const subIn = interpolate(frame, [total * perWordFrames + 6, total * perWordFrames + 20], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <div style={{ fontFamily: "Inter", fontWeight: 600, fontSize: px, lineHeight: 1.04, letterSpacing: "-0.02em", textAlign: align, whiteSpace: "nowrap" }}>
      {lines.map((l, li) => <div key={li}>{l.split(" ").map((w) => word(w, k++))}</div>)}
      {sub ? <div style={{ marginTop: 26, fontWeight: 500, fontSize: THEME.service, letterSpacing: 0, color: THEME.dim, opacity: subIn }}>{sub}</div> : null}
    </div>
  );
};
```

- [ ] **Step 5: `Backdrop.tsx`**

```tsx
import React from "react";
import { AbsoluteFill } from "remotion";
import { ACT_BG, THEME } from "./theme.ts";
import type { Act } from "./timeline.ts";

export const Backdrop: React.FC<{ act: Act; glowX?: number }> = ({ act, glowX = THEME.watchX }) => {
  const [c0, c1, c2, glow] = ACT_BG[act];
  return (
    <AbsoluteFill style={{ background: `radial-gradient(120% 120% at 70% 30%, ${c0} 0%, ${c1} 45%, ${c2} 100%)` }}>
      <AbsoluteFill style={{ mixBlendMode: "screen", background: `radial-gradient(38% 70% at ${glowX * 100}% 50%, ${glow} 0%, rgba(0,0,0,.0) 100%), #000`, opacity: 1 }} />
    </AbsoluteFill>
  );
};
```

- [ ] **Step 6:** `npx tsc --noEmit` → nessun errore. Commit:

```bash
git add tools/promo/remotion/public/fonts tools/promo/remotion/src/film/theme.ts tools/promo/remotion/src/film/fonts.ts tools/promo/remotion/src/film/WordMask.tsx tools/promo/remotion/src/film/Backdrop.tsx
git commit -m "feat(promo): bundled OFL fonts, per-act backdrops and the word-by-word mask title"
```
(`public/fonts` è una cartella di soli cinque file appena creati: controllare con `git status --short` che non entri altro.)

### Task 8: L'orologio fotografico

**Files:**
- Create: `tools/promo/remotion/src/film/PhotoWatch.tsx`

**Interfaces:**
- Consumes: `mockup.geometry.json`, `homography`, `toMatrix3d`, `Watch` (la cassa disegnata, `src/Watch.tsx`, intatta).
- Produces: `<PhotoWatch view clip clipStart glassPx tilt overlay around />`: `overlay` si disegna dentro il display in uno
  spazio 480×480; `around` nello spazio del vetro, quadrato di lato `2 × caseR × 1.5` centrato sulla cassa (solo frontale).

- [ ] **Step 1: `PhotoWatch.tsx`**

```tsx
import React from "react";
import { Img, OffthreadVideo, staticFile } from "remotion";
import geo from "./mockup.geometry.json";
import { homography, toMatrix3d } from "./homography.ts";
import type { Quad } from "./homography.ts";
import { MAX_TILT } from "./moves.ts";
import { Watch } from "../Watch";

type Props = { view: "front" | "threeQuarter" | "drawn"; clip: string; clipStart?: number; glassPx: number; tilt: number; overlay?: React.ReactNode; around?: React.ReactNode };

const Ui: React.FC<{ clip: string; clipStart?: number; overlay?: React.ReactNode }> = ({ clip, clipStart = 0, overlay }) => (
  <div style={{ position: "absolute", inset: 0, borderRadius: "50%", overflow: "hidden", background: "#000" }}>
    <OffthreadVideo src={staticFile(clip)} muted trimBefore={Math.round(clipStart * 30)} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
    {overlay ? <div style={{ position: "absolute", left: 0, top: 0, width: 480, height: 480, transformOrigin: "0 0", scale: "var(--k)" }}>{overlay}</div> : null}
    {/* ombra interna: lo schermo sta sotto la cupola, ai bordi scurisce */}
    <div style={{ position: "absolute", inset: 0, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(0,0,0,0) 80%, rgba(0,0,0,.55) 100%)" }} />
  </div>
);

const Front: React.FC<Props> = ({ clip, clipStart, glassPx, tilt, overlay, around }) => {
  const G = geo.front;
  const k = glassPx / (2 * G.glassR);
  const disc = (r: number): React.CSSProperties => ({ position: "absolute", left: G.cx - r, top: G.cy - r, width: 2 * r, height: 2 * r, borderRadius: "50%" });
  const blade = (tilt / MAX_TILT) * 60;          // la lama di luce scorre con l'inclinazione e da fermo non c'è
  const bladeOn = Math.min(1, Math.abs(tilt) / 3);
  const A = G.caseR * 1.5;
  return (
    <div style={{ width: G.size * k, height: G.size * k, translate: `${-G.cx * k}px ${-G.cy * k}px` }}>
      <div style={{ width: G.size, height: G.size, position: "relative", transformOrigin: "0 0", scale: String(k) }}>
        <Img src={staticFile("mockup/front_body.png")} style={{ position: "absolute", inset: 0, filter: "drop-shadow(26px 34px 34px rgba(4,5,12,.62))" }} />
        {/* vetro sintetico: nero sopra tutta la cupola tranne il bordo vero (la foto lì riflette il telefono) */}
        <div style={{ ...disc(G.coverR + 6), background: "radial-gradient(closest-side, #000 97%, rgba(0,0,0,0) 100%)" }} />
        <div style={{ ...disc(G.displayR), ["--k" as string]: String((2 * G.displayR) / 480) }}><Ui clip={clip} clipStart={clipStart} overlay={overlay} /></div>
        <div style={{ ...disc(G.coverR), overflow: "hidden", mixBlendMode: "screen" }}>
          {/* alone in alto a sinistra, finestra sfocata, lama di luce, filo sul bordo */}
          <div style={{ position: "absolute", inset: 0, background: "radial-gradient(47.5% 31% at 31% 24%, rgba(235,245,255,.18) 0%, rgba(235,245,255,.113) 50%, rgba(235,245,255,.048) 75%, rgba(235,245,255,.013) 90%, rgba(235,245,255,0) 100%)" }} />
          <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0, filter: "blur(18px)", opacity: 0.12 }}>
            <polygon points="25,25 40,20 43,35 27.5,41" fill="#fff" />
            <line x1="33.5" y1="22.5" x2="35.5" y2="38" stroke="#000" strokeWidth="1" /><line x1="26" y1="32.5" x2="41.5" y2="27.5" stroke="#000" strokeWidth="1" />
          </svg>
          <div style={{ position: "absolute", inset: "-20%", rotate: "24deg", translate: `${blade}% 0`, opacity: bladeOn, background: "linear-gradient(90deg, rgba(0,0,0,0) 40%, rgba(255,255,255,.07) 48%, rgba(255,255,255,.11) 50%, rgba(255,255,255,.07) 52%, rgba(0,0,0,0) 60%)" }} />
          <div style={{ position: "absolute", inset: 0, borderRadius: "50%", boxShadow: "inset 5px 5px 7px -3px rgba(235,244,255,.3)" }} />
        </div>
        {around ? <div style={{ position: "absolute", left: G.cx - A, top: G.cy - A, width: 2 * A, height: 2 * A }}>{around}</div> : null}
      </div>
    </div>
  );
};

const ThreeQuarter: React.FC<Props> = ({ clip, clipStart, glassPx, overlay }) => {
  const Q = geo.q34;
  const k = glassPx / (2 * Q.b);
  return (
    <div style={{ width: Q.size * k, height: Q.size * k, translate: `${-Q.cx * k}px ${-Q.cy * k}px`,
      /* il cinturino finisce con la foto: sfuma nel buio prima che il bordo entri in quadro */
      maskImage: "linear-gradient(180deg, rgba(0,0,0,0) 0%, #000 11%, #000 89%, rgba(0,0,0,0) 100%)" }}>
      <div style={{ width: Q.size, height: Q.size, position: "relative", transformOrigin: "0 0", scale: String(k) }}>
        <Img src={staticFile("mockup/q34_body.png")} style={{ position: "absolute", inset: 0, filter: "drop-shadow(30px 36px 36px rgba(4,5,12,.62))" }} />
        <div style={{ position: "absolute", left: 0, top: 0, width: 480, height: 480, transformOrigin: "0 0", transform: toMatrix3d(homography(480, Q.quad as Quad)), filter: "blur(0.4px) brightness(.95)", ["--k" as string]: "1" }}>
          <Ui clip={clip} clipStart={clipStart} overlay={overlay} />
        </div>
        {/* qui il telefono non c'è: i riflessi VERI della foto sopra l'interfaccia */}
        <Img src={staticFile("mockup/q34_reflections.png")} style={{ position: "absolute", inset: 0, mixBlendMode: "screen" }} />
      </div>
    </div>
  );
};

/** Il punto (0,0) del componente è il centro del vetro: chi lo usa lo mette dove vuole e lo inclina. */
export const PhotoWatch: React.FC<Props> = (p) => (
  <div style={{ position: "absolute", left: 0, top: 0, transformOrigin: "0 0", transform: `perspective(1800px) rotateY(${p.view === "threeQuarter" ? p.tilt / 3 : p.tilt}deg)` }}>
    {p.view === "front" ? <Front {...p} /> : p.view === "threeQuarter" ? <ThreeQuarter {...p} /> : (
      <div style={{ translate: "-50% -50%" }}><Watch src={p.clip} d={Math.round(p.glassPx * 0.86)} /></div>
    )}
  </div>
);
```

Se questa versione di Remotion non conosce `trimBefore`, usare `startFrom` (stesso significato): lo dice `tsc`.

- [ ] **Step 2:** `npx tsc --noEmit` → nessun errore. (La prova visiva è nel Task 10.)

- [ ] **Step 3: commit**

```bash
git add tools/promo/remotion/src/film/PhotoWatch.tsx
git commit -m "feat(promo): photographic watch — synthetic glass on the front view, true reflections and real perspective on the three-quarter"
```

### Task 9: Il film e il confronto

**Files:**
- Create: `tools/promo/remotion/src/film/{Film.tsx,Compare.tsx}`
- Modify: `tools/promo/remotion/src/Root.tsx`

**Interfaces:**
- Consumes: tutto il Task 5-8.
- Produces: composizioni `Film` (durata dalla scaletta) e `Compare` (90 fotogrammi); `GRID`, `TIMELINE` esportati da `Film.tsx`;
  `<SceneView scene />`; gli effetti si agganciano nel Task 12 tramite `overlay`/`around`.

- [ ] **Step 1: `Film.tsx`**

```tsx
import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame, useVideoConfig } from "remotion";
import raw from "./timeline.json";
import { beatToFrame, spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { MOVE_BEATS, poseAt } from "./moves.ts";
import { totalBeats, validateTimeline } from "./timeline.ts";
import type { Scene } from "./timeline.ts";
import { Backdrop } from "./Backdrop.tsx";
import { PhotoWatch } from "./PhotoWatch.tsx";
import { WordMask } from "./WordMask.tsx";
import { THEME } from "./theme.ts";
import { useFilmFonts } from "./fonts.ts";

/** Scaletta sbagliata = il film non parte: l'errore elenca tutti i problemi. */
export const TIMELINE = validateTimeline(raw);
export const GRID: Grid = { bpm: TIMELINE.bpm, fps: TIMELINE.fps, offsetSeconds: TIMELINE.offsetSeconds };
export const filmFrames = (): number => beatToFrame(GRID, totalBeats(TIMELINE)) - beatToFrame(GRID, 0);

export const SceneView: React.FC<{ scene: Scene; overlay?: React.ReactNode; around?: React.ReactNode }> = ({ scene, overlay, around }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const total = spanFrames(GRID, scene.at, scene.len);
  const beat = spanFrames(GRID, scene.at, 1);
  const w = scene.watch;
  const pose = w ? poseAt(frame, total, beat * MOVE_BEATS, w.enter, w.exit) : null;
  const cx = (scene.text ? THEME.watchX : 0.5) * width;
  const textAt = spanFrames(GRID, scene.at, scene.text?.at ?? 0);
  return (
    <AbsoluteFill>
      <Backdrop act={scene.act} glowX={scene.text ? THEME.watchX : 0.5} />
      {w && pose ? (
        <div style={{ position: "absolute", width: 0, height: 0, left: cx + pose.x * width, top: height / 2 + pose.y * height, transformOrigin: "0 0", scale: String(pose.scale) }}>
          <PhotoWatch view={w.view} clip={w.clip} clipStart={w.clipStart} tilt={pose.tilt} overlay={overlay} around={around}
            glassPx={w.view === "threeQuarter" ? THEME.q34GlassPx : THEME.frontGlassPx} />
        </div>
      ) : null}
      {scene.text ? (
        <Sequence from={textAt} layout="none">
          <div style={{ position: "absolute", left: w ? THEME.leftMargin : 0, right: w ? undefined : 0, top: 0, bottom: 0, display: "flex", alignItems: "center", justifyContent: w ? "flex-start" : "center" }}>
            <WordMask lines={scene.text.lines} accent={scene.text.accent} size={scene.text.size} sub={scene.text.sub}
              perWordFrames={w ? Math.round(beat / 2) : beat} exitAt={total - textAt - 8} align={w ? "left" : "center"} />
          </div>
        </Sequence>
      ) : null}
    </AbsoluteFill>
  );
};

export const Film: React.FC = () => {
  useFilmFonts();
  const zero = beatToFrame(GRID, 0);
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      {TIMELINE.scenes.map((s) => (
        <Sequence key={s.id} name={s.id} from={beatToFrame(GRID, s.at) - zero} durationInFrames={spanFrames(GRID, s.at, s.len)}>
          <SceneView scene={s} />
        </Sequence>
      ))}
    </AbsoluteFill>
  );
};
```

In apertura (scene senza orologio) una parola per battito; con l'orologio, mezza battuta per parola.

- [ ] **Step 2: `Compare.tsx`** — stessa clip, cassa disegnata a sinistra e foto a destra

```tsx
import React from "react";
import { AbsoluteFill } from "remotion";
import { Backdrop } from "./Backdrop.tsx";
import { PhotoWatch } from "./PhotoWatch.tsx";

export const Compare: React.FC = () => (
  <AbsoluteFill>
    <Backdrop act="know" glowX={0.5} />
    {(["drawn", "front"] as const).map((view, i) => (
      <div key={view} style={{ position: "absolute", left: 1920 * (0.27 + 0.46 * i), top: 540 }}>
        <PhotoWatch view={view} clip="scenes/s1_list.mp4" glassPx={700} tilt={0} />
      </div>
    ))}
  </AbsoluteFill>
);
```

- [ ] **Step 3: `Root.tsx`** — accanto a `Release`, che resta:

```tsx
import { Compare } from "./film/Compare.tsx";
import { Film, filmFrames } from "./film/Film.tsx";
// dentro il frammento, dopo <Composition id="Release" … />:
<Composition id="Film" component={Film} fps={30} width={1920} height={1080} durationInFrames={filmFrames()} />
<Composition id="Compare" component={Compare} fps={30} width={1920} height={1080} durationInFrames={90} />
```
(`RemotionRoot` oggi restituisce una sola `Composition`: avvolgere le tre in `<>…</>`.)

- [ ] **Step 4: prova che la scaletta sbagliata ferma tutto**

```bash
cd tools/promo/remotion && cp src/film/timeline.json /tmp/tl.bak
python3 - <<'E'
import json; p='src/film/timeline.json'; t=json.load(open(p)); t['scenes'][3]['at']=14; json.dump(t,open(p,'w'))
E
npx remotion still Film out/no.png --frame=10 2>&1 | grep -m1 "Scaletta sbagliata"; cp /tmp/tl.bak src/film/timeline.json; npm run check
```
Expected: la riga `Scaletta sbagliata:` e nessun `out/no.png`; poi `scaletta valida: 6 scene, 32 battiti, 19.2 s`.

- [ ] **Step 5: primo fotogramma vero**

Run: `npx remotion still Film out/f_rise.png --frame=195 && npx remotion still Film out/f_list.png --frame=315`
(a 100 battiti: `open-3` = fotogrammi 144-234, `list` 234-360, `asks` 360-468, `limits` 468-576.)
Expected: due PNG 1920×1080. Guardarli con Read: orologio presente, interfaccia dentro il vetro, nessun testo di ripiego.

- [ ] **Step 6: commit**

```bash
git add tools/promo/remotion/src/film/Film.tsx tools/promo/remotion/src/film/Compare.tsx tools/promo/remotion/src/Root.tsx
git commit -m "feat(promo): the Film composition driven by the cue sheet, plus a drawn-case vs photo comparison"
```

### Task 10: Rifinitura dell'aspetto sui fotogrammi

Lavoro visivo, in sessione principale. Nessun file nuovo: si toccano solo `PhotoWatch.tsx`, `theme.ts`, `WordMask.tsx`.

- [ ] **Step 1: tavola di confronto con i fotogrammi approvati.** Affiancare `out/f_list.png` a
  `tools/promo/out/prototipi-mockup/prova_frontale_v3.jpg` e un fotogramma di `open-3` (frame 195) a
  `prova_tre_quarti_v2.jpg` con `ffmpeg -i A -i B -filter_complex hstack out/cfr_front.png`. Guardare con Read.
- [ ] **Step 2: bordi a 4×.** `ffmpeg -v error -i out/f_list.png -vf "crop=480:270:X:Y,scale=1920:1080:flags=neighbor" out/zoom_N.png`
  su: lato sinistro della cassa, corona, ansa/cinturino in alto, bordo del vetro in alto a sinistra, bordo del display.
  È dove Franz ha trovato i difetti: smusso tagliato, alone di tessuto, gradini.
- [ ] **Step 3: correggere e ripetere** finché: riflessi del vetro leggibili ma l'interfaccia resta nitida; il disco nero non
  mangia il bordo vero della cupola; nel tre quarti l'interfaccia segue il vetro (ore 12 verso il cinturino, ore 3 verso
  la corona) e le estremità del cinturino non si vedono finire; la parola in colore è una sola. Se l'interfaccia del tre
  quarti risulta ruotata o specchiata, l'ordine degli angoli in `mockup.geometry.json` non è TL, TR, BR, BL: correggere in
  `export.py`, non nel componente.
- [ ] **Step 4:** `npm run check`, poi commit dei soli file toccati:
  `git commit -m "polish(promo): glass, edges and type tuned against the approved still frames"`.

### Task 11: PUNTO DI CONTROLLO 1 (Franz)

- [ ] **Step 1: render**

```bash
cd tools/promo/remotion && mkdir -p out/punto1
for f in 195:stile-1-apertura 315:stile-2-lista 440:stile-3-domanda 545:stile-4-limiti; do npx remotion still Film out/punto1/${f#*:}.png --frame=${f%%:*}; done
npx remotion still Compare out/punto1/confronto-cassa-disegnata-foto.png --frame=45
setsid nohup npx remotion render Film out/punto1/movimento-muto.mp4 --frames=0-329 --muted > out/punto1/render.log 2>&1 &
```
Attendere in primo piano: `until grep -qE "Rendered|rror" out/punto1/render.log; do sleep 20; done; tail -3 out/punto1/render.log`.
Expected: `movimento-muto.mp4` di 11 s (330 fotogrammi, ~3 minuti di render).

- [ ] **Step 2: tavola di controllo prima di mostrare.** `ffmpeg -v error -i out/punto1/movimento-muto.mp4 -vf fps=2,scale=480:-1,tile=6x4 out/punto1/tavola.png`
  e guardarla: parole che entrano sul battito, orologio che sale, nessun fotogramma nero non voluto, nessun salto.
- [ ] **Step 3: mandare a Franz** i quattro fotogrammi, il confronto e il video con `SendUserFile`, con tre domande sole:
  foto o cassa disegnata; bianco caldo `#F4F2EC` o il bianco freddo dei provini; i tre colori d'atto. **Fermarsi.**
- [ ] **Step 4: dopo la risposta**, annotare la decisione in coda al documento di progetto (una riga datata) e correggere
  `theme.ts` / `PhotoWatch.tsx`. Se Franz sceglie la cassa disegnata, nella scaletta `view` diventa `"drawn"` e il resto
  del piano non cambia.

---

# Parte II — il motore completo (dopo il punto 1)

### Task 12: Gesti resi visibili

**Files:**
- Create: `tools/promo/remotion/src/film/Fx.tsx`
- Modify: `tools/promo/remotion/src/film/Film.tsx`

**Interfaces:**
- Produces: `<TapDot x y />` (dentro `overlay`, spazio 480), `<LongPressArc frames />` e `<HapticRings />` (dentro `around`,
  quadrato di lato 3 × caseR, quindi la cassa ha raggio 1/3 del lato), `fxLayers(scene, grid): { overlay, around }`.

- [ ] **Step 1: `Fx.tsx`**

```tsx
import React from "react";
import { Easing, Sequence, interpolate, useCurrentFrame } from "remotion";
import { spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import type { Scene } from "./timeline.ts";
import { THEME } from "./theme.ts";

const clampBoth = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** Il punto del tocco: compare pieno, si allarga e svanisce in 12 fotogrammi. */
export const TapDot: React.FC<{ x: number; y: number }> = ({ x, y }) => {
  const f = useCurrentFrame();
  const s = interpolate(f, [0, 12], [0.55, 1.25], { ...clampBoth, easing: Easing.out(Easing.cubic) });
  const o = interpolate(f, [0, 3, 12], [0, 0.55, 0], clampBoth);
  return <div style={{ position: "absolute", left: x - 34, top: y - 34, width: 68, height: 68, borderRadius: "50%", background: "#fff", opacity: o, scale: String(s) }} />;
};

/** L'arco della pressione lunga si chiude attorno alla cassa, partendo dalle ore 12. */
export const LongPressArc: React.FC<{ frames: number }> = ({ frames }) => {
  const f = useCurrentFrame();
  const p = interpolate(f, [0, frames], [0, 1], clampBoth);
  const o = interpolate(f, [frames, frames + 8], [1, 0], clampBoth);
  const r = 100 / 3 + 2.4, c = 2 * Math.PI * r;
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0, opacity: o, rotate: "-90deg" }}>
      <circle cx="50" cy="50" r={r} fill="none" stroke={THEME.accent} strokeWidth="0.9" strokeLinecap="round" strokeDasharray={`${c * p} ${c}`} />
    </svg>
  );
};

/** Due anelli di vibrazione dalla cassa, a cinque fotogrammi l'uno dall'altro. */
export const HapticRings: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0 }}>
      {[0, 5].map((d) => {
        const r = interpolate(f - d, [0, 18], [100 / 3, 100 / 3 + 9], { ...clampBoth, easing: Easing.out(Easing.quad) });
        const o = interpolate(f - d, [0, 2, 18], [0, 0.5, 0], clampBoth);
        return <circle key={d} cx="50" cy="50" r={r} fill="none" stroke="#fff" strokeWidth="0.5" opacity={o} />;
      })}
    </svg>
  );
};

export const fxLayers = (scene: Scene, g: Grid): { overlay: React.ReactNode; around: React.ReactNode } => {
  const at = (b: number) => spanFrames(g, scene.at, b);
  const fx = scene.fx ?? [];
  return {
    overlay: fx.map((e, i) => (e.kind === "tap" ? <Sequence key={i} from={at(e.at)} durationInFrames={14} layout="none"><TapDot x={e.x} y={e.y} /></Sequence> : null)),
    around: fx.map((e, i) =>
      e.kind === "longPress" ? <Sequence key={i} from={at(e.at)} durationInFrames={at(e.at + e.len) - at(e.at) + 8} layout="none"><LongPressArc frames={at(e.at + e.len) - at(e.at)} /></Sequence>
      : e.kind === "haptic" ? <Sequence key={i} from={at(e.at)} durationInFrames={26} layout="none"><HapticRings /></Sequence> : null),
  };
};
```

- [ ] **Step 2: agganciare in `Film.tsx`**: in `Film`, `const L = fxLayers(s, GRID)` e `<SceneView scene={s} overlay={L.overlay} around={L.around} />`.
- [ ] **Step 3: provare** aggiungendo alla scena `asks` `{ "kind": "longPress", "at": 3, "len": 2 }` e `{ "kind": "tap", "at": 2.5, "x": 240, "y": 330 }`;
  `npm run check`; tre fotogrammi (`--frame` a metà dell'arco, sul tocco, sugli anelli) guardati con Read.
- [ ] **Step 4: commit** `feat(promo): visible gestures — tap dot, long-press arc around the case, haptic rings`.

### Task 13: Testi che escono dall'orologio

**Files:**
- Create: `tools/promo/remotion/src/film/WatchText.tsx`
- Modify: `tools/promo/remotion/src/film/Film.tsx`

**Interfaces:**
- Produces: `<Counter to suffix frames />`, `<TypedLine text frames />`, `<TerminalLines lines everyFrames />`,
  `<SpokenWords file fps />` (legge `public/audio/<file>`: `[{ "start": s, "end": s, "word": "…" }]`), e
  `watchTextFor(scene, grid): React.ReactNode` che `SceneView` disegna nella colonna del testo, sotto il titolo.

- [ ] **Step 1: `WatchText.tsx`**

```tsx
import React, { useEffect, useState } from "react";
import { Easing, Sequence, continueRender, delayRender, interpolate, staticFile, useCurrentFrame } from "remotion";
import { spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import type { Scene } from "./timeline.ts";
import { THEME } from "./theme.ts";

const clampBoth = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const big: React.CSSProperties = { fontFamily: "Inter", fontWeight: 600, color: THEME.white, letterSpacing: "-0.02em" };

/** «11 %» enorme che conta da zero. */
export const Counter: React.FC<{ to: number; suffix: string; frames: number }> = ({ to, suffix, frames }) => {
  const f = useCurrentFrame();
  const v = Math.round(interpolate(f, [0, frames], [0, to], { ...clampBoth, easing: Easing.out(Easing.cubic) }));
  return <div style={{ ...big, fontSize: 300, lineHeight: 1, fontVariantNumeric: "tabular-nums" }}>{v}<span style={{ color: THEME.accent }}>{suffix}</span></div>;
};

/** La frase dettata si compone in grande, con cursore. */
export const TypedLine: React.FC<{ text: string; frames: number }> = ({ text, frames }) => {
  const f = useCurrentFrame();
  const n = Math.round(interpolate(f, [0, frames], [0, text.length], clampBoth));
  return <div style={{ ...big, fontSize: 64, lineHeight: 1.2, maxWidth: 760 }}>{text.slice(0, n)}<span style={{ color: THEME.accent, opacity: f % 16 < 8 ? 1 : 0 }}>|</span></div>;
};

/** Le righe nuove del terminale escono dallo schermo, grandi, monospazio: nascono a destra (verso l'orologio) e si impilano. */
export const TerminalLines: React.FC<{ lines: string[]; everyFrames: number }> = ({ lines, everyFrames }) => {
  const f = useCurrentFrame();
  return (
    <div style={{ fontFamily: "Noto Sans Mono", fontSize: 38, lineHeight: 1.5, color: THEME.white, whiteSpace: "pre" }}>
      {lines.map((l, i) => {
        const e = interpolate(f, [i * everyFrames, i * everyFrames + 10], [0, 1], { ...clampBoth, easing: Easing.out(Easing.cubic) });
        return <div key={i} style={{ opacity: e, translate: `${(1 - e) * 420}px 0`, scale: String(0.6 + 0.4 * e), transformOrigin: "100% 50%" }}>{l}</div>;
      })}
    </div>
  );
};

type Word = { start: number; end: number; word: string };

/** Le parole compaiono mentre l'orologio le dice (tempi da whisper); quella in corso è in colore. */
export const SpokenWords: React.FC<{ file: string; fps: number }> = ({ file, fps }) => {
  const f = useCurrentFrame();
  const [words, setWords] = useState<Word[]>([]);
  const [handle] = useState(() => delayRender(`parole ${file}`));
  useEffect(() => { fetch(staticFile(`audio/${file}`)).then((r) => r.json()).then((w) => { setWords(w); continueRender(handle); }); }, [file, handle]);
  const t = f / fps;
  return (
    <div style={{ ...big, fontSize: 54, lineHeight: 1.25, maxWidth: 780 }}>
      {words.filter((w) => w.start <= t).map((w, i) => <span key={i} style={{ color: t < w.end ? THEME.accent : THEME.white }}>{w.word} </span>)}
    </div>
  );
};

export const watchTextFor = (scene: Scene, g: Grid): React.ReactNode => {
  const at = (b: number) => spanFrames(g, scene.at, b);
  return (scene.fx ?? []).map((e, i) => {
    const node = e.kind === "counter" ? <Counter to={e.to} suffix={e.suffix} frames={at(e.at + e.len) - at(e.at)} />
      : e.kind === "typed" ? <TypedLine text={e.text} frames={at(e.at + e.len) - at(e.at)} />
      : e.kind === "terminal" ? <TerminalLines lines={e.lines} everyFrames={at(e.every)} />
      : e.kind === "spoken" ? <SpokenWords file={e.words} fps={g.fps} /> : null;
    return node ? <Sequence key={i} from={at(e.at)} layout="none">{node}</Sequence> : null;
  });
};
```

- [ ] **Step 2: in `SceneView`**, dentro la colonna del testo, sotto `<WordMask>`: `<div style={{ marginTop: 40 }}>{watchTextFor(scene, GRID)}</div>`
  (la colonna diventa `flexDirection: "column"`, `alignItems: "flex-start"`, `justifyContent: "center"`).
- [ ] **Step 3: provare** con tre scene provvisorie (`counter` su `limits`, `typed`, `terminal` con quattro righe vere prese da
  `s5_terminal.mp4`), `npm run check`, un fotogramma a metà di ciascuna guardato con Read. `SpokenWords` si prova nel Task 19.
- [ ] **Step 4: commit** `feat(promo): text that leaves the watch — counter, dictated line with cursor, terminal lines, spoken words`.

### Task 14: Cartello finale e scaletta completa a segnaposto

**Files:**
- Create: `tools/promo/remotion/src/film/EndCard.tsx`
- Modify: `tools/promo/remotion/src/film/{Film.tsx,timeline.json}`

- [ ] **Step 1: `EndCard.tsx`**

```tsx
import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { WordMask } from "./WordMask.tsx";
import { THEME } from "./theme.ts";

export const REPO = "github.com/frsorrentino/claude-master-watch";

export const EndCard: React.FC<{ beat: number }> = ({ beat }) => {
  const f = useCurrentFrame();
  const o = (from: number) => interpolate(f, [from, from + 12], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const small: React.CSSProperties = { fontFamily: "Inter", fontWeight: 500, color: THEME.dim, textAlign: "center" };
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", gap: 22 }}>
      <WordMask lines={["Claude Master"]} perWordFrames={beat} align="center" />
      <div style={{ ...small, fontSize: THEME.service, color: THEME.white, opacity: o(beat * 3) }}>Free. <span style={{ color: THEME.accent }}>Open source.</span></div>
      <div style={{ ...small, fontSize: 34, opacity: o(beat * 5) }}>{REPO}</div>
      <div style={{ ...small, position: "absolute", bottom: 64, left: 0, right: 0, fontSize: 22, lineHeight: 1.5, opacity: o(beat * 6) }}>
        An independent project, not affiliated with Anthropic.<br />Wear OS by Google and Pixel Watch are trademarks of Google LLC. Synthetic voice.
      </div>
    </AbsoluteFill>
  );
};
```

Prima di scriverlo, verificare l'indirizzo: `git remote get-url origin`. Se differisce, vale quello del remote.

- [ ] **Step 2: in `SceneView`**: se `scene.endCard`, dopo l'orologio disegnare `<Sequence from={beat * MOVE_BEATS} layout="none"><EndCard beat={beat} /></Sequence>`;
  in quella scena l'orologio ha `exit: "pullOut"` e resta piccolo al centro in alto (aggiungere `y: -0.18 * inOut(t)` a `pullOut` in `moves.ts` e rilanciare i test).
- [ ] **Step 3: la scaletta di 110 battiti (66 s)**, con le clip di stamattina come segnaposto: `open-1` 0-4, `open-2` 4-8,
  `open-3` 8-12 (tre quarti), `list` 12-20, `asks` 20-26 (haptic a 1), `speaks` 26-40 (tap su ▶, `spoken`; clip ferma: stessa
  `s2_question.mp4`), `answer` 40-46 (longPress), `follow` 46-52, `done` 52-58 (`s3_done.mp4`), `say` 58-64 (`typed`,
  `s4_write.mp4`), `watch` 64-72 (`terminal`, `s5_terminal.mp4`), `limits` 72-82 (`counter` a 11 %, `s6_quota.mp4`, atto
  `control`), `new` 82-96 (segnaposto `s1_list.mp4`), `close` 96-110 (tre quarti, `endCard`). Testi: quelli della tabella
  del copione, parola per parola. `follow` e `new` non hanno ancora una clip vera: usano `s1_list.mp4` e lo si dice nel
  messaggio di commit.
- [ ] **Step 4:** `npm run check` → `scaletta valida: 14 scene, 110 battiti, 66.0 s`. Tavola di fotogrammi chiave, uno per scena
  (`remotion still` al battito centrale di ogni scena, poi `ffmpeg … tile=5x3`), guardata con Read.
- [ ] **Step 5: commit** `feat(promo): end card with credits and the full 110-beat cue sheet on placeholder clips`.

---

# Parte III — Audio

Prima di ogni chiamata a Gemini: aprire un budget (`fd-telemetry.py budget-open`, vedi `fable-director:delega-efficiente`).
La chiave sta in `~/.claude/fable-director/cross-family.json` → `providers.gemini.api_key` (o nella variabile indicata da
`api_key_env`): si legge a tempo di esecuzione, non si stampa, non entra in nessun file del repo.

### Task 15: Misure audio

**Files:**
- Create: `tools/promo/audio/measure.py`
- Test: `tools/promo/audio/test_measure.py`

**Interfaces:**
- Produces: `read_wav(path) -> (sr, x)`, `to_wav(src, dst, sr=44100)`, `rms_db(x)`, `crest_db(x)`, `band_db(x, sr, lo, hi)`,
  `onset_env(x, sr) -> (env, times)`, `tempo(env, times) -> (bpm, first_beat_s, [(bpm, punteggio) × 3])`,
  `onsets(env, times) -> [s]`, `bar_energy_db(x, sr, bpm, first_beat_s) -> [dB]`, `loudness(path) -> (lufs, peak_dbfs)`.

- [ ] **Step 1: test che falliscono** — `test_measure.py`

```python
"""Segnali a risposta nota: se la misura sbaglia qui, non vale niente sulla musica vera."""
import subprocess, tempfile, unittest, wave
from pathlib import Path
import numpy as np
import measure as M

SR = 44100
def click_times(bpm, seconds, first=0.30):
    return [first + i * 60 / bpm for i in range(int((seconds - 0.2 - first) * bpm / 60) + 1)]      # per indice: sommando 0,6 sedici volte si resta sotto 9,9 e nasce un clic in più
def click_track(bpm, seconds, first=0.30):
    x = np.zeros(int(SR * seconds), np.float32)
    for t in click_times(bpm, seconds, first):
        i = int(t * SR); x[i:i + 220] += np.hanning(220) * np.sin(2 * np.pi * 1800 * np.arange(220) / SR) * 0.8
    return x + np.random.default_rng(1).normal(0, 0.002, len(x)).astype(np.float32)
def write(x, path):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())

class Tempo(unittest.TestCase):
    def test_cento_battiti(self):
        env, times = M.onset_env(click_track(100, 40), SR); bpm, first, _ = M.tempo(env, times)
        self.assertAlmostEqual(bpm, 100, delta=0.1)
        self.assertAlmostEqual(first % 0.6, 0.30, delta=0.005, msg=f"scarto {first % 0.6 - 0.30:+.4f} s: correggere ENV_BIAS_S di questo valore")
    def test_novantasette(self):
        self.assertAlmostEqual(M.tempo(*M.onset_env(click_track(97, 40), SR))[0], 97, delta=0.1)
    def test_attacchi(self):
        env, times = M.onset_env(click_track(100, 10), SR); on = M.onsets(env, times)
        want = click_times(100, 10); self.assertEqual(len(on), len(want)); self.assertLess(max(abs(o - w) for o, w in zip(on, want)), 0.010)

class Livelli(unittest.TestCase):
    def test_cresta(self):
        s = np.sin(2 * np.pi * 440 * np.arange(SR) / SR).astype(np.float32)
        self.assertAlmostEqual(M.crest_db(s), 3.01, delta=0.05)
        self.assertLess(M.crest_db(np.clip(s * 4, -1, 1)), 1.5)            # la distorsione schiaccia la cresta
    def test_banda(self):
        s = np.sin(2 * np.pi * 300 * np.arange(SR) / SR)
        self.assertGreater(M.band_db(s, SR, 200, 400), -0.1); self.assertLess(M.band_db(s, SR, 2000, 4000), -60)
    def test_lufs(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "s.wav"; write(0.1 * np.sin(2 * np.pi * 1000 * np.arange(SR * 5) / SR), p)
            lufs, peak = M.loudness(p)
            self.assertAlmostEqual(peak, -20.0, delta=0.3); self.assertAlmostEqual(lufs, -23.0, delta=1.0)
    def test_energia_per_battuta(self):
        x = click_track(100, 24); x[int(SR * 12):] *= 4
        e = M.bar_energy_db(x, SR, 100, 0.30)
        self.assertEqual(len(e), 9); self.assertAlmostEqual(e[7] - e[1], 12.0, delta=1.0)

if __name__ == "__main__":
    unittest.main()
```

Run: `cd tools/promo/audio && python3 -m unittest test_measure -v` → FAIL (`No module named 'measure'`).

- [ ] **Step 2: `measure.py`**

```python
"""Misure audio con numpy e wave soltanto (lo scipy di sistema non funziona con numpy 2.4.6). Claude non sente: misura."""
import re, subprocess, wave
import numpy as np

HOP_S = 0.005          # passo NOMINALE dell'inviluppo: quello vero è un numero intero di campioni, e lo dicono i tempi restituiti
ENV_BIAS_S = -0.0057   # l'inviluppo anticipa l'attacco vero di 5,7 ms (misurato su clic a 97, 100 e 108 battiti: −5,6/−5,8/−5,7): costante dello strumento, non della traccia

def read_wav(path):
    with wave.open(str(path), "rb") as w:
        sr, ch, sw, n = w.getframerate(), w.getnchannels(), w.getsampwidth(), w.getnframes()
        if sw != 2: raise ValueError("solo 16 bit: passare da to_wav()")
        x = np.frombuffer(w.readframes(n), dtype="<i2").astype(np.float32) / 32768.0
    return sr, x.reshape(-1, ch).mean(axis=1)

def to_wav(src, dst, sr=44100):
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(src), "-ac", "1", "-ar", str(sr), "-c:a", "pcm_s16le", str(dst)], check=True); return dst

def rms_db(x): return 20 * np.log10(max(float(np.sqrt(np.mean(np.square(x, dtype=np.float64)))), 1e-9))
def crest_db(x): return 20 * np.log10(max(float(np.max(np.abs(x))), 1e-9)) - rms_db(x)

def band_db(x, sr, lo, hi):
    X = np.abs(np.fft.rfft(x)) ** 2; f = np.fft.rfftfreq(len(x), 1 / sr); sel = (f >= lo) & (f < hi)
    return 10 * np.log10(max(float(X[sel].sum() / max(X.sum(), 1e-20)), 1e-12))

def onset_env(x, sr, n=1024, block=2000):
    """Flusso spettrale positivo su spettro compresso; i tempi sono il centro della finestra, corretti di ENV_BIAS_S."""
    hop = int(sr * HOP_S); w = np.hanning(n).astype(np.float32); k = 1 + (len(x) - n) // hop; prev = None; flux = []
    for s in range(0, k, block):                                  # a blocchi: una traccia di 3 minuti non deve occupare 1 GB
        idx = np.arange(n)[None, :] + hop * np.arange(s, min(k, s + block))[:, None]
        S = np.log1p(50 * np.abs(np.fft.rfft(x[idx] * w, axis=1)))
        if prev is not None: S = np.vstack([prev, S])
        flux.append(np.maximum(S[1:] - S[:-1], 0).sum(axis=1)); prev = S[-1:]
    env = np.r_[0.0, np.concatenate(flux)]
    return env, (np.arange(len(env)) * hop + n / 2) / sr - ENV_BIAS_S

def tempo(env, times, lo=80.0, hi=130.0, step=0.05):
    """Per ogni tempo candidato, la fase che raccoglie più attacchi. Restituisce bpm, primo battito (s), i tre migliori."""
    e = env - env.mean(); t = np.arange(len(e)); hop_s = float(times[1] - times[0]); res = []      # 220 campioni a 44,1 kHz sono 4,989 ms, non 5: con 5 tondi il tempo esce sbagliato dello 0,23 %
    for bpm in np.arange(lo, hi, step):
        period = 60 / bpm / hop_s; ph = np.arange(0, period, 0.5)
        beats = ph[:, None] + period * np.arange(int(len(e) / period) - 1)[None, :]
        sc = np.interp(beats, t, e).mean(axis=1); i = int(sc.argmax()); res.append((float(sc[i]), float(bpm), float(ph[i])))
    res.sort(reverse=True); _, bpm, ph = res[0]
    first = float(np.interp(ph, t, times))
    top = []
    for s, b, _ in res:
        if all(abs(b - q) > 1 for q, _ in top): top.append((round(b, 2), round(s, 3)))
        if len(top) == 3: break
    return round(bpm, 2), first, top

def onsets(env, times, k=2.5, gap_s=0.12):
    thr = np.median(env) + k * env.std(); out = []
    for i in range(1, len(env) - 1):
        if env[i] > thr and env[i] >= env[i - 1] and env[i] > env[i + 1] and (not out or times[i] - out[-1] > gap_s): out.append(float(times[i]))
    return out

def bar_energy_db(x, sr, bpm, first_beat_s, beats_per_bar=4):
    bar = int(sr * 60 / bpm * beats_per_bar); s = int(first_beat_s * sr)
    return [round(rms_db(x[i:i + bar]), 1) for i in range(s, len(x) - bar + 1, bar)]

def loudness(path):
    err = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(path), "-af", "ebur128=peak=true", "-f", "null", "-"], capture_output=True, text=True).stderr
    tail = err[err.rfind("Summary:"):]
    return float(re.search(r"I:\s+(-?[\d.]+) LUFS", tail).group(1)), float(re.search(r"Peak:\s+(-?[\d.]+) dBFS", tail).group(1))
```

`tempo()` lavora sui tempi veri restituiti da `onset_env` (il passo è un numero intero di campioni: 4,989 ms, non 5).

- [ ] **Step 3: tarare il ritardo.** Fatto il 17/09: su clic a 97, 100 e 108 battiti l'inviluppo anticipava di 5,6-5,8 ms; con
  `ENV_BIAS_S = -0.0057` gli attacchi cadono entro 2,7 ms e il primo battito entro 1,7 ms. È una costante dello strumento
  (finestra e compressione), non della traccia: se si cambia finestra o passo va rimisurata con lo stesso script.
- [ ] **Step 4:** `python3 -m unittest test_measure -v` → 7 test OK.
- [ ] **Step 5: commit** `feat(promo): audio measures with numpy only — tempo and first beat, onsets, crest factor, band energy, bar energy, LUFS via ffmpeg`.

### Task 16: Musica candidata e PUNTO DI CONTROLLO 2 (Franz)

**Files:**
- Create: `tools/promo/audio/track_card.py`, `docs/promo/licenses.md`

- [ ] **Step 1: `track_card.py`**

```python
"""Scheda misurata di una traccia candidata: tempo, primo battito, energia per battuta, dove cade la salita.
Uso: python3 track_card.py <file audio> → <file>.card.json e una tabella a schermo."""
import json, sys, tempfile
from pathlib import Path
import numpy as np
import measure as M

src = Path(sys.argv[1])
with tempfile.TemporaryDirectory() as d:
    sr, x = M.read_wav(M.to_wav(src, Path(d) / "t.wav"))
env, times = M.onset_env(x, sr); bpm, first, top = M.tempo(env, times); bars = M.bar_energy_db(x, sr, bpm, first)
e = np.array(bars); rise = int(np.argmax(np.convolve(np.diff(e), np.ones(2), "valid"))) + 1      # la battuta dove l'energia sale di più
lufs, peak = M.loudness(src)
card = {"file": src.name, "seconds": round(len(x) / sr, 1), "bpm": bpm, "bpm_candidates": top, "first_beat_s": round(first, 3),
        "bars": len(bars), "bar_seconds": round(240 / bpm, 3), "bar_energy_db": bars, "biggest_rise_at_bar": rise,
        "intro_bars_below_body": int(np.argmax(e > np.median(e) - 3)), "lufs": lufs, "peak_dbfs": peak,
        "film_bars": round(66 / (240 / bpm), 1)}
Path(str(src) + ".card.json").write_text(json.dumps(card, indent=1))
print(f"{src.name}: {bpm} bpm (alternative {top[1:]}), primo battito a {first:.3f} s, {len(bars)} battute, salita alla battuta {rise}, introduzione rada per {card['intro_bars_below_body']} battute")
print(" ".join(f"{v:.0f}" for v in bars))
```

- [ ] **Step 2: cercare 3-4 candidate su Pixabay** con chrome-bridge (`mcp__chrome-bridge__*`; se non è carico: avvisare Franz, non
  ripiegare su claude-in-chrome). Ricerca: musica elettronica calda e minimale, 95-110 battiti, introduzione rada, una
  salita. Scaricare in `tools/promo/materiali/audio/musica/`. Per ognuna salvare in `docs/promo/licenses.md`: titolo, autore,
  indirizzo della pagina, data e ora dello scaricamento, e il testo del riepilogo della licenza come appare quel giorno nella
  pagina della licenza di Pixabay (copiato, non riassunto a memoria). La traccia non entra nel repo.
- [ ] **Step 3: schede**: `for f in ../materiali/audio/musica/*.mp3; do python3 track_card.py "$f"; done`. Scartare con i numeri: tempo
  fuori da 95-110, nessuna introduzione rada (`intro_bars_below_body` = 0), nessuna salita nei primi 8 battute.
- [ ] **Step 4: mappa degli atti per ogni candidata**: con `bar_seconds` e la curva, scrivere in tre righe dove cadrebbero apertura
  (battute 0-3), comparsa dell'orologio (la salita), atti e finale vero della traccia.
- [ ] **Step 5: mandare a Franz** (`SendUserFile`) le tracce con la loro scheda in parole semplici. **Fermarsi**: la scelta è sua
  (è il primo dei tre ascolti).
- [ ] **Step 6: commit** di `track_card.py` e `docs/promo/licenses.md`: `feat(promo): measured cards for candidate tracks; licence evidence`.

### Task 17: Taglio sulle battute e prova della griglia

**Files:**
- Create: `tools/promo/audio/cut_track.py`, `tools/promo/remotion/src/film/GridTest.tsx`
- Modify: `tools/promo/audio/test_measure.py`, `tools/promo/remotion/src/Root.tsx`, `src/film/timeline.json` (`bpm`, `offsetSeconds`, `music`)

**Interfaces:**
- Produces: `cut_track.cut(x, sr, bpm, first_beat_s, segments, fade_ms=15) -> x` dove `segments = [(battuta_inizio, battuta_fine), …]`;
  da riga di comando scrive `public/audio/music.wav` e stampa `offsetSeconds` (il primo battito nel file tagliato).

- [ ] **Step 1: test che fallisce** — in `test_measure.py`

```python
class Taglio(unittest.TestCase):
    def test_la_giunta_resta_sulla_griglia(self):
        import cut_track
        x = click_track(100, 60); y = cut_track.cut(x, SR, 100, 0.30, [(0, 4), (10, 16), (20, 24)])
        self.assertAlmostEqual(len(y) / SR, 14 * 2.4 + 0.30, delta=0.05)
        env, times = M.onset_env(y, SR); on = M.onsets(env, times)
        self.assertLess(max(abs(o - (0.30 + 0.6 * i)) for i, o in enumerate(on)), 0.010)      # giunte comprese
```

- [ ] **Step 2: `cut_track.py`**

```python
"""Taglia la traccia sulle battute (introduzione, corpo, salto al finale vero) con dissolvenze a pari potenza di 15 ms
centrate sul battito: la griglia resta uniforme attraverso le giunte e la musica finisce con il film, non sfuma.
Uso: python3 cut_track.py <traccia> <scheda.card.json> 0-4 10-24 40-47"""
import json, sys, tempfile, wave
from pathlib import Path
import numpy as np
import measure as M

def cut(x, sr, bpm, first_beat_s, segments, fade_ms=15):
    bar = 240 / bpm; h = int(sr * fade_ms / 2000); out = None
    for a, b in segments:
        i0 = int((first_beat_s + a * bar) * sr); i1 = int((first_beat_s + b * bar) * sr)
        if out is None: out = x[:i1 + h].copy() if a == 0 else x[max(0, i0 - h):i1 + h].copy(); continue
        seg = x[i0 - h:i1 + h]; t = np.linspace(0, np.pi / 2, 2 * h)
        out[-2 * h:] = out[-2 * h:] * np.cos(t) + seg[:2 * h] * np.sin(t); out = np.r_[out, seg[2 * h:]]
    return out[:len(out) - h]

if __name__ == "__main__":
    src, card = Path(sys.argv[1]), json.loads(Path(sys.argv[2]).read_text()); segs = [tuple(int(v) for v in s.split("-")) for s in sys.argv[3:]]
    with tempfile.TemporaryDirectory() as d: sr, x = M.read_wav(M.to_wav(src, Path(d) / "t.wav"))
    y = cut(x, sr, card["bpm"], card["first_beat_s"], segs)
    dst = Path(__file__).resolve().parent.parent / "remotion/public/audio/music.wav"; dst.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(dst), "wb") as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes((np.clip(y, -1, 1) * 32767).astype("<i2").tobytes())
    print(f"{dst}: {len(y) / sr:.2f} s · bpm {card['bpm']} · offsetSeconds {card['first_beat_s']}")
```

Il taglio lavora in mono per semplicità di misura. Per il film si rifà lo stesso taglio in stereo con ffmpeg agli stessi
istanti (`atrim` + `acrossfade=d=0.015:c1=qsin:c2=qsin`), e si rimisura: gli attacchi devono coincidere entro 5 ms.

- [ ] **Step 3:** `python3 -m unittest test_measure -v` → 8 test OK.
- [ ] **Step 4: scaletta sulla traccia scelta**: in `timeline.json` mettere `bpm` e `offsetSeconds` della scheda e `"music": "audio/music.wav"`;
  scegliere i segmenti in modo che il totale valga le battute del film (110 battiti = 27,5 battute: arrotondare la scaletta a
  112 battiti = 28 battute allungando `close`). `npm run check`.
- [ ] **Step 5: `GridTest.tsx`** — un lampo bianco a ogni battito sopra la musica

```tsx
import React from "react";
import { AbsoluteFill, Audio, staticFile, useCurrentFrame } from "remotion";
import { beatToFrame } from "./beats.ts";
import { GRID, TIMELINE } from "./Film.tsx";
import { totalBeats } from "./timeline.ts";

export const GridTest: React.FC = () => {
  const f = useCurrentFrame();
  const flashes = new Set(Array.from({ length: totalBeats(TIMELINE) }, (_, b) => beatToFrame(GRID, b)));
  return (
    <AbsoluteFill style={{ background: flashes.has(f) ? "#fff" : "#000" }}>
      {TIMELINE.music ? <Audio src={staticFile(TIMELINE.music)} /> : null}
    </AbsoluteFill>
  );
};
```
Attenzione: qui i fotogrammi sono assoluti (la musica parte dal fotogramma 0 e il primo battito cade a `offsetSeconds`); `Film`
invece sottrae `beatToFrame(GRID, 0)`. Nel Task 20 `Film` smette di sottrarre e i primi fotogrammi prima del battito 0 restano neri.

- [ ] **Step 6: la misura** (script nello scratchpad, non nel repo): renderizzare `GridTest`, estrarre la luminosità per fotogramma
  (`ffmpeg -i grid.mp4 -vf signalstats,metadata=print:key=lavfi.signalstats.YAVG -f null - 2>&1`), prendere i fotogrammi con
  YAVG > 200, e confrontare `fotogramma / 30` con gli attacchi misurati su `music.wav` più vicini.
  Expected: scarto massimo < 20 ms su tutti i battiti che hanno un attacco entro 60 ms (giunte comprese). L'arrotondamento al
  fotogramma pesa fino a 16,7 ms: se lo scarto supera i 20 ms è un errore di `offsetSeconds` o di `bpm`, non di arrotondamento.
- [ ] **Step 7: commit** `feat(promo): bar-accurate music cut with equal-power splices; beat-grid flash test`.

### Task 18: Suoni d'interfaccia

**Files:**
- Create: `tools/promo/remotion/src/film/{sound.ts,sound.test.ts}`
- Modify: `docs/promo/licenses.md`

**Interfaces:**
- Produces:

```ts
export type SfxName = "notify" | "thump" | "tick" | "pressRise" | "whoosh";
export type SfxCue = { beat: number; name: SfxName; gainDb: number };        // beat assoluto nel film
export function sfxCues(t: Timeline): SfxCue[];                              // al massimo un suono per battito
export function duckGain(frame: number, windows: [number, number][], depthDb: number, rampFrames: number): number;
export const dbToGain: (db: number) => number;
```

- [ ] **Step 1: scaricare i suoni Material Design di Google** (pagina «Sound resources» di Material Design: trovarla con una
  ricerca, non indovinare l'indirizzo dello zip), scegliere cinque file per i cinque nomi, convertirli in
  `public/audio/sfx/<nome>.wav`, e scrivere in `docs/promo/licenses.md`: indirizzo della pagina, data, licenza CC-BY 4.0 come
  dichiarata sulla pagina, e la riga di credito per pagina del sito e README.
- [ ] **Step 2: test che falliscono** — `sound.test.ts`

```ts
import test from "node:test";
import assert from "node:assert/strict";
import { dbToGain, duckGain, sfxCues } from "./sound.ts";
import { validateTimeline } from "./timeline.ts";

const t = validateTimeline({
  bpm: 100, fps: 30, offsetSeconds: 0,
  scenes: [
    { id: "a", at: 0, len: 8, act: "know", watch: { view: "front", clip: "scenes/x.mp4" }, text: { lines: ["It asks."] },
      fx: [{ kind: "haptic", at: 0 }, { kind: "tap", at: 0.5, x: 1, y: 1 }, { kind: "tap", at: 4, x: 1, y: 1 }, { kind: "longPress", at: 5, len: 2 }] },
  ],
} as unknown);

test("al massimo un suono per battito, e vince il più importante", () => {
  const cues = sfxCues(t);
  const perBeat = new Map<number, number>();
  cues.forEach((c) => perBeat.set(Math.floor(c.beat), (perBeat.get(Math.floor(c.beat)) ?? 0) + 1));
  assert.ok([...perBeat.values()].every((n) => n === 1));
  assert.equal(cues.find((c) => Math.floor(c.beat) === 0)?.name, "notify");
  assert.deepEqual(cues.map((c) => c.name), ["notify", "tick", "pressRise"]);
});

test("la musica scende di 12 dB sotto la voce con rampe morbide e torna su", () => {
  const w: [number, number][] = [[100, 200]];
  assert.equal(duckGain(50, w, -12, 9), 1);
  assert.ok(Math.abs(duckGain(150, w, -12, 9) - dbToGain(-12)) < 1e-9);
  const mid = duckGain(100 - 4, w, -12, 9);
  assert.ok(mid < 1 && mid > dbToGain(-12));
  assert.equal(duckGain(260, w, -12, 9), 1);
});
```
- [ ] **Step 3: `sound.ts`**

```ts
import type { Timeline } from "./timeline.ts";

export type SfxName = "notify" | "thump" | "tick" | "pressRise" | "whoosh";
export type SfxCue = { beat: number; name: SfxName; gainDb: number };

export const dbToGain = (db: number): number => Math.pow(10, db / 20);

/** Priorità quando due suoni cadono nello stesso battito: la notifica, poi la pressione, il tocco, il soffio. */
const RANK: SfxName[] = ["notify", "pressRise", "tick", "whoosh", "thump"];

export const sfxCues = (t: Timeline): SfxCue[] => {
  const all: SfxCue[] = [];
  for (const s of t.scenes) {
    if (s.text && (s.text.size ?? "title") === "title" && !s.watch) all.push({ beat: s.at + (s.text.at ?? 0), name: "whoosh", gainDb: -26 });
    for (const f of s.fx ?? []) {
      const beat = s.at + f.at;
      if (f.kind === "haptic") all.push({ beat, name: "notify", gainDb: -16 });
      if (f.kind === "tap") all.push({ beat, name: "tick", gainDb: -20 });
      if (f.kind === "longPress") all.push({ beat, name: "pressRise", gainDb: -18 });
      if (f.kind === "terminal") f.lines.forEach((_, i) => all.push({ beat: beat + i * f.every, name: "tick", gainDb: -24 }));
    }
  }
  const byBeat = new Map<number, SfxCue>();
  for (const c of all) {
    const k = Math.floor(c.beat);
    const had = byBeat.get(k);
    if (!had || RANK.indexOf(c.name) < RANK.indexOf(had.name)) byBeat.set(k, c);
  }
  return [...byBeat.values()].sort((a, b) => a.beat - b.beat);
};

export const duckGain = (frame: number, windows: [number, number][], depthDb: number, rampFrames: number): number => {
  let down = 0;
  for (const [a, b] of windows) {
    const inn = Math.min(1, Math.max(0, (frame - (a - rampFrames)) / rampFrames));
    const out = Math.min(1, Math.max(0, (b + rampFrames - frame) / rampFrames));
    down = Math.max(down, Math.min(inn, out));
  }
  const smooth = down * down * (3 - 2 * down);
  return dbToGain(depthDb * smooth);
};
```
Il colpo basso (`thump`) accompagna `notify` nello stesso istante come unico evento sonoro: in `Soundtrack.tsx` (Task 20) la
notifica suona i due file insieme; non conta come secondo suono del battito.

- [ ] **Step 4:** `npm test` verde. Commit `feat(promo): interface sound cues from the cue sheet, one per beat, and the ducking curve`.

### Task 19: La voce dell'orologio e PUNTO DI CONTROLLO 3 (Franz)

**Files:**
- Create: `tools/promo/audio/voice.py`, `tools/promo/audio/speaker_fx.sh`

- [ ] **Step 1: il testo esatto.** Prenderlo da `SpeechText.question` nel core (cercare `SpeechText` con Grep) applicato alla domanda
  della demo: deve finire con «…Deploy version 2.8.0 to production now? 1, yes. 2, no.». Salvare in
  `tools/promo/materiali/audio/voce/question.txt`. Se il testo generato dall'app differisce dal copione, vale l'app.
- [ ] **Step 2: `voice.py`**

```python
"""Sintesi con Gemini: python3 voice.py <testo.txt> <voce> "<stile a parole>" <uscita.wav>
La chiave viene da ~/.claude/fable-director/cross-family.json e non si stampa mai."""
import base64, json, os, sys, urllib.request, wave
from pathlib import Path

MODEL = os.environ.get("TTS_MODEL", "gemini-3.1-flash-tts-preview")
cfg = json.loads((Path.home() / ".claude/fable-director/cross-family.json").read_text())["providers"]["gemini"]
key = cfg.get("api_key") or os.environ[cfg["api_key_env"]]
text, voice, style, out = Path(sys.argv[1]).read_text().strip(), sys.argv[2], sys.argv[3], sys.argv[4]
body = {"contents": [{"parts": [{"text": f"{style}: {text}"}]}],
        "generationConfig": {"responseModalities": ["AUDIO"], "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": voice}}}}}
req = urllib.request.Request(f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent",
                             data=json.dumps(body).encode(), headers={"Content-Type": "application/json", "x-goog-api-key": key})
part = json.loads(urllib.request.urlopen(req, timeout=120).read())["candidates"][0]["content"]["parts"][0]["inlineData"]
rate = int(part["mimeType"].split("rate=")[1]) if "rate=" in part["mimeType"] else 24000
with wave.open(out, "wb") as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate); w.writeframes(base64.b64decode(part["data"]))
print(out, part["mimeType"])
```
Prima chiamata: elencare i modelli (`GET …/v1beta/models` con la stessa intestazione) e verificare che il nome del modello TTS
del progetto esista ancora (è in anteprima); se la risposta ha un'altra forma, leggere il messaggio d'errore e adeguare lo
script ai documenti correnti, non a memoria. Riserva: Kokoro in locale.

- [ ] **Step 3: tre varianti** (voci e stile diversi: neutra e svelta; calda; più «assistente»), tutte con la richiesta di un ritmo
  svelto. Per ognuna: durata, `crest_db` (pulita attesa ~14-15 dB), e trascrizione
  `python3 ~/.claude/plugins/cache/fsorrentino/fable-director/1.43.1/scripts/transcribe.py <wav> --json --words --lang en`:
  tutte le parole del testo ritrovate, «2.8.0» compreso. Una variante che dura più di 9 s si rifà con ritmo più svelto o, se non
  basta, senza le opzioni: si decide sull'audio vero.
- [ ] **Step 4: `speaker_fx.sh`** — il «piccolo altoparlante» miscelato con la voce pulita, in due intensità

```bash
#!/usr/bin/env bash
# uso: speaker_fx.sh voce.wav uscita_leggera.wav uscita_forte.wav
set -euo pipefail
fx="highpass=f=450,lowpass=f=3600,acompressor=threshold=-22dB:ratio=4:attack=4:release=60,volume=4dB"
ffmpeg -v error -y -i "$1" -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.65 0.35':normalize=0" "$2"
ffmpeg -v error -y -i "$1" -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.30 0.70':normalize=0" "$3"
```
Misura: `band_db(x, sr, 450, 3600)` sale rispetto alla voce pulita (atteso: più vicino a 0 dB nella versione forte) e
`crest_db` resta sopra 9 dB (sotto 6 è distorsione: 4,4 misurato sul campione distorto).

- [ ] **Step 5: tempi parola per parola** della variante e dell'intensità scelte → `public/audio/question.words.json`
  (lista piatta `[{start, end, word}]` estratta dai segmenti di `transcribe.py`), voce in `public/audio/question.wav`.
- [ ] **Step 6: mandare a Franz** le tre varianti × (pulita, leggera, forte) con le misure in una riga ciascuna. **Fermarsi**: la
  voce la sceglie lui (secondo ascolto).
- [ ] **Step 7: commit** di `voice.py` e `speaker_fx.sh`: `feat(promo): watch voice synthesis and the small-speaker treatment, with measures`.

### Task 20: Mix dentro Remotion, tracce separate, misure

**Files:**
- Create: `tools/promo/remotion/src/film/Soundtrack.tsx`
- Modify: `tools/promo/remotion/src/film/Film.tsx`, `src/Root.tsx`

**Interfaces:**
- Consumes: `sfxCues`, `duckGain`, `dbToGain`, `GRID`, `TIMELINE`.
- Produces: `<Soundtrack stems />` con `stems: "all" | "music" | "voice" | "sfx"`; `Film` accetta la prop `stems` (predefinita `"all"`).

- [ ] **Step 1: `Soundtrack.tsx`**

```tsx
import React from "react";
import { Audio, Sequence, staticFile } from "remotion";
import { beatToFrame } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { dbToGain, duckGain, sfxCues } from "./sound.ts";
import type { Timeline } from "./timeline.ts";

export type Stems = "all" | "music" | "voice" | "sfx";

export const Soundtrack: React.FC<{ t: Timeline; g: Grid; stems: Stems }> = ({ t, g, stems }) => {
  const on = (s: Stems) => stems === "all" || stems === s;
  const spoken = t.scenes.flatMap((s) => (s.fx ?? []).flatMap((f) => (f.kind === "spoken" ? [{ from: beatToFrame(g, s.at + f.at), to: beatToFrame(g, s.at + f.at + f.len), voice: f.voice }] : [])));
  const windows = spoken.map((v) => [v.from, v.to] as [number, number]);
  return (
    <>
      {on("music") && t.music ? <Audio src={staticFile(t.music)} volume={(f) => dbToGain(-6) * duckGain(f, windows, -12, 9)} /> : null}
      {on("voice") ? spoken.map((v, i) => <Sequence key={i} from={v.from} layout="none"><Audio src={staticFile(`audio/${v.voice}`)} volume={dbToGain(-3)} /></Sequence>) : null}
      {on("sfx") ? sfxCues(t).flatMap((c, i) => (c.name === "notify" ? [c, { ...c, name: "thump" as const, gainDb: c.gainDb - 4 }] : [c]).map((k, j) => (
        <Sequence key={`${i}-${j}`} from={beatToFrame(g, k.beat)} layout="none"><Audio src={staticFile(`audio/sfx/${k.name}.wav`)} volume={dbToGain(k.gainDb)} /></Sequence>
      ))) : null}
    </>
  );
};
```
La voce parte sul fotogramma in cui ▶ diventa ■: il `tap` su ▶ e lo `spoken` della scena `speaks` hanno lo stesso `at`.
Il −12 dB è l'abbassamento della musica; il distacco voce/musica che si misura è quello risultante.

- [ ] **Step 2: in `Film.tsx`**: `Film` diventa `React.FC<{ stems?: Stems }>`, disegna `<Soundtrack t={TIMELINE} g={GRID} stems={stems ?? "all"} />`, e le
  `Sequence` delle scene partono da `beatToFrame(GRID, s.at)` senza sottrarre lo zero (`filmFrames()` = `beatToFrame(GRID, totalBeats)`).
  In `Root.tsx`: `defaultProps={{ stems: "all" }}` sulla composizione `Film`.
- [ ] **Step 3: tracce separate** (solo audio, veloci):

```bash
for s in all music voice sfx; do npx remotion render Film out/stem-$s.wav --props="{\"stems\":\"$s\"}"; done
```
- [ ] **Step 4: misure** (script nello scratchpad con `measure.py`), tutte da riportare con il numero:
  - distacco voce/musica nelle finestre parlate: `rms_db(voice) − rms_db(music)` ≥ 10 dB;
  - ogni suono d'interfaccia almeno 8 dB sotto la musica nello stesso tratto;
  - trascrizione di `stem-all.wav`: tutte le parole della domanda ritrovate sopra la musica;
  - `crest_db` della voce > 9 dB; `loudness(stem-all.wav)`: se non è −14 ±0,5 LUFS con picco ≤ −1 dB, passata finale
    `ffmpeg -i film.mp4 -af loudnorm=I=-14:TP=-1:LRA=11:measured_I=…` in due tempi (prima misura, poi applica) e rimisura;
  - la voce comincia entro un fotogramma dal `tap` su ▶ (primo campione sopra −40 dB di `stem-voice.wav` contro `beatToFrame`).
- [ ] **Step 5: versione muta** dallo stesso render: `ffmpeg -i film.mp4 -an -c:v copy film-muto.mp4`.
- [ ] **Step 6:** `npm run check`; commit `feat(promo): mix inside Remotion on the same cue sheet — ducking, stems for measurement, mute derivative`.

---

## Fine del piano 1

Esito atteso: `Film` di 66-67 s con mockup fotografico approvato al punto 1, traccia scelta al punto 2 e tagliata sulle
battute, voce scelta al punto 3, mix misurato; clip ancora segnaposto. Il piano 2 (con l'orologio) porta: i quattro difetti
del blocco A con `superpowers:systematic-debugging`, la registrazione momento per momento con asserzioni sull'albero
dell'interfaccia, la scaletta con le clip vere, le verifiche del film finito (tavola a 1 fotogramma al secondo, lettura
ottica contro parole italiane e dati personali, bordi a 3-4×, dimensioni), le consegne e il taglio corto.
