/** Coreografie dei momenti forti (piano 3): funzioni pure dell'avanzamento 0-1, come `moves.ts`. */
import { soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

/** La card esce dal display (0-0,35, atterraggio morbido), resta fuori, gira lo stato (0,45-0,65), rientra (0,82-1). */
export type CardOut = { travel: number; flip: number; alpha: number };
export const cardOutAt = (p: number): CardOut => {
  const back = Math.pow(ramp(p, 0.82, 1), 3);
  return { travel: soft(ramp(p, 0, 0.35)) * (1 - back), flip: soft(ramp(p, 0.45, 0.65)), alpha: clamp(ramp(p, 0, 0.06) - ramp(p, 0.94, 1)) };
};
