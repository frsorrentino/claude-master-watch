/** L'orologio è un'immagine piatta nello spazio: entra, deriva lentamente, esce. Oltre i 10 gradi sembra finto. */
import { springEase, springSettle } from "./spring.ts";

export type Move = "riseIn" | "slideIn" | "slideOut" | "pushIn" | "pullOut" | "settleSmall" | "zoomLeft" | "diveIn";
export type Pose = { x: number; y: number; scale: number; tilt: number };
export const MAX_TILT = 9;
export const MOVE_BEATS = 2;

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const out3 = (t: number) => 1 - Math.pow(1 - t, 3);
const in3 = (t: number) => t * t * t;
export const inOut = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

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

/** Arrivo con un filo di rimbalzo (Franz, 19/09 09:59: «un po' di ease e un po' di bump»): l'oggetto supera il punto di
 *  arrivo di `over` e ci torna sopra. 0,06 = sei per cento, quanto basta perché si senta il peso senza sembrare un gioco. */
export const bump = (over = 0.06) => (t: number): number => {
  const c = over * 10.5;
  const u = t - 1;
  return 1 + (c + 1) * u * u * u + c * u * u;
};

const REST: Pose = { x: 0, y: 0, scale: 1, tilt: 0 };

const moveAt = (m: Move, t: number): Pose => {
  switch (m) {
    // molla (direttive di motion, 25/09): l'orologio sale e si posa con uno scavalco di un centesimo, senza rimbalzo
    case "riseIn": { const s = springEase(t); return { x: 0, y: (1 - s) * 0.95, scale: 0.9 + 0.1 * s, tilt: -6 * (1 - s) }; }
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

/** `steady`: niente deriva. Serve quando un componente deve uscire ESATTAMENTE dal suo posto sul display: se l'orologio
 *  respira, il punto di partenza si sposta e l'incastro si perde (Franz, 19/09 11:03). */
export const poseAt = (frame: number, total: number, moveFrames: number, enter?: Move, exit?: Move, absFrame = frame, steady = false, driftScale = 1, exitHold = 0): Pose => {
  const a = enter ? moveAt(enter, clamp(frame / moveFrames)) : REST;
  // `exitHold`: fotogrammi in cui il movimento è GIÀ finito e l'inquadratura resta al culmine, prima del taglio. Senza,
  // lo zoom arriva in fondo nell'ultimo fotogramma e l'orologio sparisce sul più bello (Franz, 21/09 12:05).
  const run = Math.max(1, moveFrames - exitHold);
  const b = exit ? moveAt(exit, clamp((frame - (total - moveFrames)) / run)) : REST;
  // durante un'uscita la deriva si spegne: l'inquadratura finale del movimento è esatta, senza il respiro sopra
  const bw = exit ? 1 - clamp((frame - (total - moveFrames)) / moveFrames) : 1;
  // `driftScale` 0-1: quanto vale la deriva adesso. Serve a fermarla per un tratto senza farla saltare quando torna
  //  (ui/sleep.ts, `still`): a 0 l'orologio è immobile, e il ritorno si fa scalando piano, non riaccendendola di colpo.
  const d0raw = steady ? REST : driftAt(absFrame);
  const d0: Pose = { x: d0raw.x * driftScale, y: d0raw.y * driftScale, scale: 1 + (d0raw.scale - 1) * driftScale, tilt: d0raw.tilt * driftScale };
  const drift: Pose = { x: d0.x * bw, y: d0.y * bw, scale: 1 + (d0.scale - 1) * bw, tilt: d0.tilt * bw };
  const tilt = Math.max(-10, Math.min(10, a.tilt + b.tilt + drift.tilt));
  return { x: a.x + b.x + drift.x, y: a.y + b.y + drift.y, scale: a.scale * b.scale * drift.scale, tilt };
};

/** La chiusura (Franz, 17/09 23:52): solo il logo grande in dissolvenza, poi si inclina e sotto compare l'orologio, poi ci si
 *  allontana fino all'inquadratura finale con l'orologio piccolo in alto. `beats` = battiti dall'inizio della scena.
 *  Franz, 21/09 02:18: i tre movimenti finali NON sono più in fila — inclinazione, corpo e allontanamento partono a 2,2, 2,35
 *  e 2,5 e finiscono insieme: uno sfalsamento di poco, non tre annunci. Sono la stessa rivelazione (quel logo sta sul display
 *  di un orologio, e l'orologio è lontano), quindi si dicono insieme. L'arco resta solo fino al battito 3: è la firma. */
export type Closing = { pose: Pose; logo: number; draw: number; tilt: number; body: number; focus: number };
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
/** Di quanto sale l'orologio del cartello quando il cinturino deve uscire intero dal bordo in alto (corto, 23/09): con
 *  0,2 il bordo della foto finiva a 63 px dall'alto e la sfumatura lo nascondeva; con 0,3 resta 45 px fuori quadro. */
export const BLEED_LIFT = 0.3;
export const closingAt = (beats: number, lift = 0.2): Closing => {
  // tutti i movimenti del cartello sono molle (direttive di motion, 25/09): stessi tempi di prima, curva della molla
  const out = springEase(ramp(beats, 2.5, 4.8));
  const near = 1 - springEase(ramp(beats, 0, 2.5));      // speculare all'ingresso nello schermo: si parte da vicino e ci si allontana mentre il logo compare
  return {
    logo: ramp(beats, 0, 2),
    draw: springEase(ramp(beats, 0.5, 3)),              // l'arco del logo si disegna da zero al suo 70 %, come un gauge che si riempie
    tilt: springEase(ramp(beats, 2.2, 3.6)),
    body: ramp(beats, 2.35, 3.9) >= 1 ? 1 : springSettle(ramp(beats, 2.35, 3.9)) * (beats <= 2.2 ? 0 : 1),   // opacità della cassa: senza scavalco
    focus: 1 - out,
    pose: { x: 0, y: -lift * out, scale: 1.7 + 0.9 * near + (0.5 - 1.7) * out, tilt: 0 },
  };
};

/** Il display nero del cartello: col logo ritagliato compare con la cassa (`body`), così il logo nasce da solo sul blu. */
export const screenAt = (c: Closing, cutout: boolean): number => (cutout ? c.body : 1);

/** Dove sta l'orologio nel quadro (centro del display in pixel, scala): posa, tremito della notifica, spostamento verticale
 *  e zoom della camera, in un solo conto. Lo usano il transform dell'orologio e il volo del terminale (piano 7: la revisione
 *  dei piani 4-6 trovò il volo senza zoom né tremito; con la camera ferma non si vedeva, con una dolly le copie divergevano). */
export const watchPlace = (cx: number, pose: Pose, width: number, height: number, shake: number, aroundDy: number, zoom: number): { x: number; y: number; scale: number } =>
  ({ x: cx + pose.x * width + shake, y: height / 2 + pose.y * height + aroundDy, scale: pose.scale * zoom });
/** Un'unità del display (0-480) in pixel del quadro, per un vetro largo `glassPx` e la geometria della foto (`glassR`, `displayR`). */
export const displayUnit = (glassPx: number, glassR: number, displayR: number, scale: number): number => ((glassPx / (2 * glassR)) * 2 * displayR * scale) / 480;
