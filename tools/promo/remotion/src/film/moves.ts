/** L'orologio è un'immagine piatta nello spazio: entra, deriva lentamente, esce. Oltre i 10 gradi sembra finto. */
export type Move = "riseIn" | "slideIn" | "slideOut" | "pushIn" | "pullOut" | "settleSmall" | "zoomLeft";
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
    // entrata della chiusura: l'orologio rimpicciolisce e sale, e lì RESTA (a fine entrata non torna a riposo) per lasciare il posto al cartello
    case "settleSmall": return { x: 0, y: -0.2 * inOut(t), scale: 1 - 0.5 * inOut(t), tilt: 0 };
    // uscita dell'apertura: dentro la complication di sinistra del quadrante (nel tre quarti sta 183 px a sinistra e 18 sotto il centro del vetro)
    case "zoomLeft": return { x: 0.039 * inOut(t), y: -0.04 * inOut(t), scale: 1 + 1.4 * inOut(t), tilt: 0 };
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
