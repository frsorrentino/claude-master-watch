/**
 * La camera che insegue la forma (specifica §4): ogni stato riempie il quadro con lo stesso margine, e il cambio di scala
 * È la transizione. Come la forma, è una somma di molle sui bersagli delle chiavi: funzione pura del battito. Critica
 * (ζ 1): una camera che scavalca si sente come un rimbalzo del mondo intero, non della forma.
 */
import { springs } from "./spring.ts";
import { KEY_LEN, SHAPE_SHUTTER, ZOOM_MAX, keyRect, shapeAt } from "./shape.ts";
import type { Display, Rect, ShapeKey } from "./shape.ts";

export const CAMERA_MARGIN = 0.12, CAMERA_MAX = ZOOM_MAX;
/** Il punto del quadro che sta al centro dello schermo, e la scala. */
export type Cam = { cx: number; cy: number; s: number };

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

/** Il centro su un asse: il minimo spostamento dal centro che tiene il rect nel margine, poi mai oltre il bordo della scena. */
const axis = (pos: number, len: number, size: number, s: number) => {
  const half = (size / 2 - CAMERA_MARGIN * size) / s;
  const lo = pos + len - half, hi = pos + half;
  const c = lo <= hi ? clamp(size / 2, lo, hi) : pos + len / 2;
  // la scena vince sul margine: fuori c'è il vuoto, e un rect che sta sul bordo resta un po' più vicino al bordo
  return clamp(c, size / (2 * s), size - size / (2 * s));
};

export const cameraTarget = (rect: Rect, zoom?: number, W = 1920, H = 1080): Cam => {
  const [x, y, w, h] = rect;
  const s = zoom ?? clamp(Math.min((W * (1 - 2 * CAMERA_MARGIN)) / w, (H * (1 - 2 * CAMERA_MARGIN)) / h), 1, CAMERA_MAX);
  return { cx: axis(x, w, W, s), cy: axis(y, h, H, s), s };
};

export const cameraAt = (keys: readonly ShapeKey[], beat: number, display: Display, W = 1920, H = 1080): Cam => {
  const targets = keys.map((k) => cameraTarget(keyRect(k, display), k.zoom, W, H));
  const along = (p: keyof Cam) =>
    springs(beat, targets[0][p], keys.slice(1).map((k, i) => ({ at: k.at, to: targets[i + 1][p], len: k.len ?? KEY_LEN })), 1);
  // anche critiche, molle sovrapposte con durate diverse escono dall'inviluppo dei bersagli (scala 0,93, 130 px fuori
  // dalla scena): i limiti dei bersagli valgono anche fra un bersaglio e l'altro, prima la scala e poi il centro con quella
  const s = clamp(along("s"), 1, CAMERA_MAX);
  return { cx: clamp(along("cx"), W / (2 * s), W - W / (2 * s)), cy: clamp(along("cy"), H / (2 * s), H - H / (2 * s)), s };
};

/**
 * Quanto corre la forma SULLO SCHERMO mentre l'otturatore è aperto (mezzo fotogramma centrato sul fotogramma, come i
 * campioni della scatola), in px per fotogramma: decide i campioni di blur. Nel quadro non basta: una forma ferma sotto
 * una camera che si avvicina corre sullo schermo; e misurata sul fotogramma prima perde l'avvio delle molle.
 */
export const screenSpeed = (keys: readonly ShapeKey[], beat: number, display: Display, beatsPerFrame: number, W = 1920, H = 1080): number => {
  const shutter = SHAPE_SHUTTER / 360;
  const corners = (b: number) => {
    const s = shapeAt(keys, b, display), c = cameraAt(keys, b, display, W, H);
    return [[s.x, s.y], [s.x + s.w, s.y], [s.x, s.y + s.h], [s.x + s.w, s.y + s.h]].map(([x, y]) => [(x - c.cx) * c.s + W / 2, (y - c.cy) * c.s + H / 2]);
  };
  const a = corners(beat - (beatsPerFrame * shutter) / 2), b = corners(beat + (beatsPerFrame * shutter) / 2);
  return Math.max(...a.map(([x, y], i) => Math.hypot(b[i][0] - x, b[i][1] - y))) / shutter;
};

/** Da applicare con `transform-origin: 0 0`, e senza `will-change`: lo strato scalato resterebbe una bitmap sgranata. */
export const cameraCss = (c: Cam, W = 1920, H = 1080): string =>
  `translate(${W / 2}px, ${H / 2}px) scale(${c.s}) translate(${-c.cx}px, ${-c.cy}px)`;
