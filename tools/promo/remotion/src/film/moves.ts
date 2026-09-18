/** L'orologio è un'immagine piatta nello spazio: entra, deriva lentamente, esce. Oltre i 10 gradi sembra finto. */
export type Move = "riseIn" | "slideIn" | "slideOut" | "pushIn" | "pullOut" | "settleSmall" | "zoomLeft" | "diveIn";
export type Pose = { x: number; y: number; scale: number; tilt: number };
export const MAX_TILT = 9;
export const MOVE_BEATS = 2;

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const out3 = (t: number) => 1 - Math.pow(1 - t, 3);
const in3 = (t: number) => t * t * t;
const inOut = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

/** Curva di Bézier cubica come in CSS: dato il tempo 0-1 restituisce l'avanzamento. */
export const bezier = (x1: number, y1: number, x2: number, y2: number) => (t: number): number => {
  const f = (a: number, b: number, s: number) => 3 * a * s * (1 - s) * (1 - s) + 3 * b * s * s * (1 - s) + s * s * s;
  if (t <= 0) return 0;
  if (t >= 1) return 1;          // agli estremi esatto: la bisezione si fermerebbe a 0,9999999
  let lo = 0, hi = 1;
  for (let i = 0; i < 24; i++) { const mid = (lo + hi) / 2; if (f(x1, x2, mid) < t) lo = mid; else hi = mid; }
  return f(y1, y2, (lo + hi) / 2);
};
/** Atterraggio morbido (Franz, 18/09): parte deciso e frena a lungo, come le entrate del testo. */
export const soft = bezier(0.2, 0, 0, 1);

const REST: Pose = { x: 0, y: 0, scale: 1, tilt: 0 };

const moveAt = (m: Move, t: number): Pose => {
  switch (m) {
    case "riseIn": return { x: 0, y: (1 - out3(t)) * 0.95, scale: 0.9 + 0.1 * out3(t), tilt: -6 * (1 - out3(t)) };
    case "slideIn": return { x: (1 - out3(t)) * 0.75, y: 0, scale: 1, tilt: MAX_TILT * (1 - out3(t)) };
    case "slideOut": return { x: -in3(t) * 0.75, y: 0, scale: 1, tilt: -MAX_TILT * in3(t) };
    case "pushIn": return { x: 0, y: 0, scale: 1 + 0.35 * inOut(t), tilt: 0 };
    // entrata della chiusura: l'orologio rimpicciolisce e sale, e lì RESTA (a fine entrata non torna a riposo) per lasciare il posto al cartello
    case "settleSmall": return { x: 0, y: -0.2 * inOut(t), scale: 1 - 0.5 * inOut(t), tilt: 0 };
    // uscita dell'apertura: dentro il quadrante della complication di sinistra, centro (85, 240,5) e raggio 63 su 480 (misurati sul
    // fotogramma). Inquadratura finale simmetrica: il quadrante al centro del quadro a 3,4× (la deriva si spegne durante l'uscita).
    case "zoomLeft": return { x: 0.1739 * inOut(t), y: -0.0022 * inOut(t), scale: 1 + 2.4 * inOut(t), tilt: 0 };
    // uscita della penultima scena: dentro lo schermo fino a riempire il quadro (il display al centro), verso il nero da cui nasce il logo
    case "diveIn": return { x: -0.19 * in3(t), y: 0, scale: 1 + 6.52 * in3(t), tilt: 0 };
    case "pullOut": return { x: 0, y: 0, scale: 1 - 0.45 * inOut(t), tilt: 4 * inOut(t) };
  }
};

/** La deriva lenta è una funzione del fotogramma ASSOLUTO del film, non della scena: ripartendo da zero a ogni scena faceva un
 *  micro-scatto sui tagli (Franz, 18/09 13:13). Un respiro di 14 s, di pochi millesimi del quadro e un grado e mezzo. */
export const driftAt = (absFrame: number): Pose => {
  const t = (absFrame / 30) / 14 * 2 * Math.PI;
  return { x: 0.006 * Math.sin(t), y: -0.005 + 0.005 * Math.cos(t * 0.7), scale: 1.015 + 0.015 * Math.sin(t * 0.5), tilt: 0.9 * Math.sin(t) };
};

export const poseAt = (frame: number, total: number, moveFrames: number, enter?: Move, exit?: Move, absFrame = frame): Pose => {
  const a = enter ? moveAt(enter, clamp(frame / moveFrames)) : REST;
  const b = exit ? moveAt(exit, clamp((frame - (total - moveFrames)) / moveFrames)) : REST;
  // durante un'uscita la deriva si spegne: l'inquadratura finale del movimento è esatta, senza il respiro sopra
  const bw = exit ? 1 - clamp((frame - (total - moveFrames)) / moveFrames) : 1;
  const d0 = driftAt(absFrame);
  const drift: Pose = { x: d0.x * bw, y: d0.y * bw, scale: 1 + (d0.scale - 1) * bw, tilt: d0.tilt * bw };
  const tilt = Math.max(-10, Math.min(10, a.tilt + b.tilt + drift.tilt));
  return { x: a.x + b.x + drift.x, y: a.y + b.y + drift.y, scale: a.scale * b.scale * drift.scale, tilt };
};

/** La chiusura (Franz, 17/09 23:52): solo il logo grande in dissolvenza, poi si inclina e sotto compare l'orologio, poi ci si
 *  allontana fino all'inquadratura finale con l'orologio piccolo in alto. `beats` = battiti dall'inizio della scena. */
export type Closing = { pose: Pose; logo: number; draw: number; tilt: number; body: number; focus: number };
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
export const closingAt = (beats: number): Closing => {
  const out = soft(ramp(beats, 5, 8));
  const near = 1 - out3(ramp(beats, 0, 2.5));      // speculare all'ingresso nello schermo: si parte da vicino e ci si allontana mentre il logo compare
  return {
    logo: ramp(beats, 0, 2),
    draw: inOut(ramp(beats, 0.5, 3)),              // l'arco del logo si disegna da zero al suo 70 %, come un gauge che si riempie
    tilt: soft(ramp(beats, 3, 5)),
    body: ramp(beats, 3.5, 5) >= 1 ? 1 : inOut(ramp(beats, 3.5, 5)) * (beats <= 3 ? 0 : 1),
    focus: 1 - out,
    pose: { x: 0, y: -0.2 * out, scale: 1.7 + 0.9 * near + (0.5 - 1.7) * out, tilt: 0 },
  };
};
