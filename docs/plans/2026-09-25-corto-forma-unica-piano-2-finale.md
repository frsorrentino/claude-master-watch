# Corto «forma unica», piano 2: il tratto finale (list → end, battiti 47,5-73,5) — piano di esecuzione

> **Per chi esegue:** `superpowers:executing-plans`, caselle `- [ ]`, TDD. Task 1-5 sono motore (esecutore); Task 6-8 sono
> regia (sessione).

**Obiettivo:** dalla card ✓ ferma nella lista (49,5) alla fine, la forma è una sola: card ✓ → pannelli della Panoramica
a sinistra → linea sotto lo slogan → arco corallo del logo dentro il display di tre quarti. Via le tre palpebre e i neri.

**Architettura:** il display vero diventa una funzione pura (`displayPlace.ts`) estratta da `SceneView`, usata dalla scena
e dalla forma: niente copie che divergono. Le chiavi agganciate hanno un `rect` in unità del display (0-480). Una
primitiva a tratto (`bend`, `split`, `track`) fa linea e arco. Il film accende la forma da un battito (`shape.from`).
All'arrivo nel logo la forma passa la mano al `LogoMark` della scena (stessa geometria, un solo arco alla volta).

**Specifica:** `docs/plans/2026-09-25-corto-forma-unica-design.md` §2 (righe 47,5-73,5), §3, §4, §8. Piano 1:
`2026-09-25-corto-forma-unica-piano-1-motore.md` (interfacce di `shape.ts`, `camera.ts`, `Shape.tsx`).

Cartella: `tools/promo/remotion/`. Test con Node 24: `PATH=~/.local/share/fnm/node-versions/v24.13.1/installation/bin:$PATH`.

## Vincoli globali

- Tutti quelli del piano 1 (molle, scambio del testo 0,25 + 0,25 battiti, controscala, blur solo sulla forma, commit
  per nome, commenti in italiano).
- Le registrazioni vere del display restano visibili dove sono vere: la forma copre il display solo come card ✓ nella
  riga della lista (è già così oggi) e come logo nel cartello.
- L'arco del logo è quello di `LogoMark`: centro (240, 240) del display, raggio `40.2·300/92`, spessore `5.6·300/92`,
  ampiezza 280° da 130° in senso orario (varco in basso), corallo `#D97757` sul 70 % dal capo sinistro, resto `#3A404C`.
- `SceneView` estratta deve rendere IDENTICO: guardia a pixel su più fotogrammi di `Short` prima/dopo.
- Nessuna resa completa negli esecutori: solo `remotion still` a 0,25× per le guardie, e solo col carico sotto 12.

## Da tenere d'occhio in revisione

1. Display che si muove (deriva, dolly, tremito, zoom della scena): la card ✓ agganciata deve seguirlo al centesimo di
   pixel, come oggi la `DoneCard` disegnata dentro il display. Test in Task 1-2.
2. Più chiavi agganciate con `rect` diversi (card ✓ → … → arco): fra di esse la molla corre in unità del display, non nel
   quadro, così segue il display anche durante la corsa. Test in Task 2.
3. `bend` che scavalca oltre 280° chiuderebbe il varco del logo: molla critica, pinza 0-1. Test in Task 3.
4. Fuori da `shape.from`/`shape.to` il film è identico a oggi (guardia a pixel su un fotogramma prima di `from`). Task 4.
5. Dopo il passaggio al `LogoMark` un solo arco corallo in quadro: la forma non si disegna più, il logo sì. Task 5.

---

### Task 1: `displayPlace.ts` — il display vero come funzione pura

**File:** crea `src/film/displayPlace.ts`, `src/film/displayPlace.test.ts`; modifica `src/film/Film.tsx` (`SceneView`).

**Interfacce prodotte:**
```ts
export type Place = { x: number; y: number; scale: number };             // centro del vetro nel quadro e scala dell'orologio
export type WatchState = { place: Place | null; pose: Pose | null; closing: Closing | null; zoom: number; focus: number; watchIn: number; mv: number; sleep: ReturnType<typeof sleepAt> | null; aroundDy: number; shake: number; cx: number };
export const watchStateAt: (t: Timeline, scene: Scene, frame: number, W: number, H: number) => WatchState;   // frame relativo alla scena
export const displayRectAt: (t: Timeline, frame: number, W?: number, H?: number) => Rect | null;         // frame assoluto; il quadrato 0-480 del display nel quadro
```
- `watchStateAt` è il blocco di `SceneView` che oggi calcola `sleep`, `mv`, `drift`, `closing`, `pose`, `cx`, `zoom`,
  `aroundDy`, `shake` e `place` (righe ~74-170 di `Film.tsx`): spostato così com'è, e `SceneView` lo chiama. Nessuna
  formula cambia. Ciò che serve solo al disegno resta in `SceneView`.
- `displayRectAt`: trova la scena del fotogramma assoluto (`beatToFrame` di `at` ≤ frame < fine), chiama `watchStateAt`
  con il fotogramma relativo; `null` senza orologio o con la vista laterale.
  - Vista frontale: `u = displayUnit(THEME.frontGlassPx, geo.front.glassR, geo.front.displayR, place.scale)`,
    `[place.x − 240u, place.y − 240u, 480u, 480u]`.
  - Tre quarti: vale solo col display piatto (`closing.tilt` = 0, cioè `reveal` 0 in `PhotoWatch`): la matrice `flat` di
    `ThreeQuarter` (lato medio del quadrilatero, centro `applyH(H, [240,240])`) con `k = THEME.q34GlassPx / (2·Q.b)`,
    l'origine `ox, oy` di `focus`, poi `place` e `scale`. Con `tilt > 0` restituisce il piatto (approssimazione) e la
    forma non deve esserci agganciata (lo controlla il test del Task 5).
  - Attenzione alla prospettiva di `PhotoWatch` (`rotateY(tilt/3)` sul tre quarti, `tilt` della posa sul frontale): con
    tilt 0 è l'identità. Il test lo dichiara.
- Test: (a) su `SHORT_TIMELINE`, per face, asks, list, work, end (battiti 0,5, 3, 50, 53, 64,5) il centro del rect
  coincide con `place` e la misura con `displayUnit`; (b) in list il rect si muove di meno di 2 px per fotogramma (deriva)
  e non è costante (deriva viva); (c) nel loop (`side`) è `null`.
- Guardia: `npx remotion still Short out/review/_g-<f>.png --frame=<f> --scale=0.25` per f in 100, 400, 700, 900, 1100
  prima e dopo (prima = `git stash push -- src/film/Film.tsx`), bbox `None` per tutti.
- Commit `refactor(promo): the watch placement as a pure function shared by the scene and the shape`.

### Task 2: chiavi agganciate con un `rect` in unità del display

**File:** `src/film/shape.ts`, `src/film/shape.test.ts`, `src/film/shapeDisplay.ts`.

- `ShapeKey` con `anchor: "display"` accetta `rect` in unità del display (0-480, anche fuori dal display) e `r` in unità
  del display. Senza `rect`, è il display intero `[0, 0, 480, 480]` con raggio 240 (come oggi).
- Nuovo: `displayUnits(d: Rect) => { x0, y0, u }` (`u = d[2] / 480`); `toFrameRect(r: Rect, d: Rect): Rect`.
- `shapeAt`: tre tracce di molle.
  - libera (quadro): le chiavi libere col loro `rect`, le agganciate col loro rect portato nel quadro dal display al loro
    battito (fotografia, come oggi);
  - agganciata (unità del display): solo sulle chiavi agganciate, rect e raggio in unità del display; prima della prima
    agganciata vale la prima agganciata;
  - peso (0/1, critico, pinza 0-1, come oggi).
  Risultato = libera + (toFrameRect(agganciata, display(beat)) − libera)·peso; raggio uguale con `r·u`.
- `keyRect(k, display)`: per le agganciate col `rect`, `toFrameRect(k.rect, display(k.at))`.
- `validateShape`: `rect` ammesso con `anchor` (quattro numeri finiti, w e h > 0); il resto invariato.
- `shapeDisplay.ts`: `realDisplay(t: Timeline): Display` = `(beat) => displayRectAt(t, frameOf(beat)) ?? frontDisplayRect(watchColumn(sceneAt(t, beat))·1920)`,
  con `frameOf` continuo (`(offsetSeconds + beat·60/bpm)·fps`, non arrotondato: la deriva è continua).
- Test: display che deriva, due chiavi agganciate con rect diversi in unità del display e `len` 2: a ogni battito della
  corsa il rect è `toFrameRect(molla in unità, display(beat))` al centesimo; peso 1 costante; una chiave libera dopo
  riporta il peso a 0 con la molla critica.
- Commit `feat(promo): anchored shape keys with a rect in display units, springing in display space`.

### Task 3: la primitiva a tratto (linea e arco)

**File:** `src/film/shape.ts`, `src/film/shape.test.ts`, `src/film/Shape.tsx`.

- `ShapeKey`: `bend?: number` (0-1, default 0), `split?: number` (0-1, default 1), `track?: string` (#rrggbb, default
  `#3a404c`). `ShapeState` guadagna `bend`, `split`, `track`. `bend` e `split` sono molle critiche con pinza 0-1; `track`
  come il colore.
- Semantica con `bend > 0`: il rect è il tratto. Centro del rect = punto più alto dell'arco (il centro del tratto); `w` =
  lunghezza del tratto; `h` = spessore; ampiezza `θ = bend·280°`; raggio `R = w/θ`; centro del cerchio `(cx, cy + R)`;
  l'arco va da `270° − θ/2` a `270° + θ/2` (angoli SVG, y in giù; con θ = 280° da 130° a 410°, cioè il varco del logo
  in basso). Estremi arrotondati. I primi `split·w` del tratto dal capo sinistro sono nel colore, il resto in `track`.
  A `bend` 0 il tratto è la scatola `w × h` con raggio `h/2`: si disegna la scatola (continuità esatta).
- Esporta `arcOf(s: ShapeState): { cx: number; cy: number; R: number; start: number; sweep: number; stroke: number } | null`
  (null a bend < 1e-3) e usala in `Shape.tsx`: con `arcOf` non nullo la scatola diventa un `<svg>` a tutto quadro dentro
  la stessa camera e lo stesso `CameraMotionBlur` (il blur vale anche per il tratto), due `<path>` (colore e track).
- Test: (a) continuità a bend → 0 (a bend 1e-3 gli estremi del tratto distano da quelli della scatola meno di 0,5 px);
  (b) identità col logo: una chiave agganciata `{ rect: [240 − L/2, 240 − r − aw/2, L, aw], bend: 1, split: 0.7 }` con
  `r = 40.2·300/92`, `aw = 5.6·300/92`, `L = r·280·π/180`, su un display qualsiasi, dà `arcOf` con centro =
  `toFrame(240, 240)`, `R = r·u`, `start = 130°`, `sweep = 280°`, spessore `aw·u` (tolleranza 1e-6); (c) `bend` con
  chiavi 0 → 1 non supera mai 1.
- Commit `feat(promo): stroke primitive — the shape bends from a line into the logo arc`.

### Task 4: la forma accesa da un battito

**File:** `src/film/Film.tsx`, `src/film/Shape.tsx`, `src/Root.tsx`.

- `Film` prende `shape?: { from: number; to?: number }` (battiti) al posto del booleano. Fuori dall'intervallo né forma né
  camera (camera identità: `CameraFrame` non trasforma). Dentro: `CameraFrame` attorno alle scene, `Shape` sopra, con
  `display = realDisplay(timeline)`.
- `ShortFilm` riceve la prop dalla composizione: `defaultProps` di `Short` `{ stems: "all", blur: true, shape: null }`
  per ora (la accende la regia al Task 8).
- Guardia: con `shape: { from: 49.5 }` il fotogramma 700 (prima di `from`) è identico a oggi.
- Commit `feat(promo): the film switches the shape on from a given beat`.

### Task 5: il passaggio al logo

**File:** `src/film/shape.ts`, `src/film/Shape.tsx`, `src/film/LogoMark.tsx`, `src/film/Film.tsx`, `src/film/moves.ts`,
`src/film/timeline.ts`, test.

- `ShapeKey.handoff?: true` (solo sull'ultima chiave, validato): da `at + len` la forma non si disegna più
  (`shapeVisible(keys, beat)`); la scena ha il suo arco identico.
- `LogoMark` prende `ring?: boolean` (default true): a false disegna solo `>_`.
- `Scene.ringFrom?: number` (battito della scena; validato: solo con `endCard`): prima, `LogoMark` senza anello; da lì
  anello pieno al 70 % (`draw` 1), senza la crescita di `closingAt().draw` (la crescita la fa la forma con `split`).
  Senza `ringFrom` tutto com'è oggi (il film lungo non cambia).
- Test: con la traccia del corto (che la regia completa al Task 7), per ogni fotogramma di end dopo `ringFrom` la forma
  è invisibile e prima è visibile; il peso dell'aggancio vale 1 in ogni fotogramma con `closing.tilt > 0` finché la forma è
  visibile (di fatto: il passaggio avviene prima del battito 2,2 della scena).
- Commit `feat(promo): the shape hands over to the logo arc inside the display`.

---

### Task 6 (regia): contenuti della forma nel tratto

- `src/film/ShapeContent.tsx`: `done` (UiCard ✓ in scala `kw/428`, `checkDraw` 1, `LIST_BODY`), `work`, `questions`,
  `context` (il markup dei pannelli di `Aside`, spostato in componenti riusati da `Aside` e dalla forma: stesso
  aspetto, animazioni sul tempo della chiave: barre e conteggi che nascono dal bordo come oggi con `asideAt`).
- Scene: in list via la `doneCard` a riposo (la fa la forma dal 49,5); work/questions/context senza `aside` e senza
  `out: "blink"`; slogan con il fondo che viene dal colore dell'atto control (`bgFrom`, `bgFadeBeats`); end senza
  `blur` (§8.3: mai sulle scene) e con `ringFrom`.

### Task 7 (regia): la traccia del tratto

Perché i pannelli stanno a sinistra (verifica chiesta da master, confermata dall'advisor il 25/09 23:00): è un solo
oggetto che viaggia — al 52 la card ✓ si stacca dalla riga della lista e diventa Work, poi Questions, poi Context senza
spostarsi, e al 58 la stessa scatola si assottiglia nella linea. Il display resta alla sua registrazione vera. Pannelli
che escono dal display e vi rientrano farebbero due oggetti in quadro, o coprirebbero la Panoramica vera. Condizioni
controllate sulla tavola: mai due pannelli (Aside spento dove c'è la forma), volo continuo al 52.

I tagli delle clip senza palpebre (le palpebre nascondevano i salti di `clipStart`):
- 47,5 → 52: `n_list` ferma → `n_overview_fit`: registrazioni diverse, nessun passaggio vero. Al 51,5 la card si gonfia
  sul display (chiave agganciata `[0, 0, 480, 480]`, raggio 240, `len` 0,5): lo copre al 52, sul taglio; poi vola a
  sinistra e scopre la Panoramica.
- work `clipStart` 7,16 (pagina Work ferma da 6,25): alla fine del battito 54 la clip è a 8,25, dove comincia lo
  scorrimento vero verso Open questions, che è la scena dopo (`clipStart` 8,25): continuità esatta. Lo scorrimento sul
  display è il gesto della chiave 54.
- context `clipStart` 10,0 invece di 10,25: la scena prima finisce a 9,34, e fra 8,5 e 10,0 la pagina Open questions è
  ferma, quindi il salto non si vede; lo scorrimento vero verso Context parte al 56, sotto la chiave 56.
- 58 (context → slogan, fondo e orologio che spariscono): al 57,5 il pannello si allarga a tutto quadro in nero
  (`len` 0,5) e copre il taglio; al 58 si ritira nella linea sotto la prima parola.


- 47,5 card ✓ agganciata `{ anchor, rect: [26, 146, 428, h], r: 42 }` (h da `slotRect`), `len` 2 come il volo.
- 52/54/56 pannelli a sinistra (già), 58-61,5 linea, 64 arco agganciato con `bend` 1 e `split` 1, 65 `split` 0,7 con
  `handoff` (passaggio prima del battito 66,2).
- Test di `short.test.ts` aggiornati (§8.5, niente battiti morti; eccezioni dichiarate).

### Task 8 (regia): tavola, resa, confronto

- `shape: { from: 49.5 }` nella composizione `Short`.
- Tavola per battito del tratto: resa 0,25× del solo tratto (`--frames=<47,5>-<fine>`), `battiti.py`, revisione con
  l'advisor.
- Resa completa del tratto a piena misura (una sola, `--concurrency=2`, riparte a 1 se il log è fermo 15 minuti), prima
  = `prima-end.mp4` e una resa del motore attuale per 47,5-64 fatta PRIMA di accendere la forma; `affianca.sh`.
