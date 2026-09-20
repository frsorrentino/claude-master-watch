/**
 * I puntini dell'attesa (Franz, 20/09 08:57): sotto «It asks.», mentre il display dorme, tre puntini pulsano a turno —
 * la sessione ti sta scrivendo. Il terzo impulso ha il culmine ESATTAMENTE sul battito della notifica, e lì i tre
 * collassano in un punto di luce che si allarga e sparisce. Funzione pura dell'avanzamento del sonno (ui/sleep.ts).
 */
import { SLEEP_CUT, TITLE_AT } from "./sleep.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

export type Dots = { alpha: number; on: [number, number, number]; collapse: number };

/** I puntini entrano appena la frase è scritta, non prima: la frase deve arrivare sola. */
export const DOTS_FROM = TITLE_AT + 0.03;
/** Quanto resta acceso un puntino fuori dal suo turno: non si spengono, respirano. */
export const DOT_BASE = 0.28;

export const dotsAt = (p: number): Dots => {
  const alpha = ramp(p, DOTS_FROM, DOTS_FROM + 0.02) * (1 - ramp(p, SLEEP_CUT, SLEEP_CUT + 0.005));
  const period = (SLEEP_CUT - DOTS_FROM) / 3;
  // il culmine del puntino i cade a `(i+1)` periodi: il terzo cade sul taglio, insieme alla campanella
  const bell = (i: number): number => {
    const d = Math.abs(p - (DOTS_FROM + (i + 1) * period)) / (period * 0.8);
    return d >= 1 ? 0 : 0.5 + 0.5 * Math.cos(Math.PI * d);
  };
  return { alpha, on: [bell(0), bell(1), bell(2)], collapse: p < SLEEP_CUT ? 0 : 1 - ramp(p, SLEEP_CUT, SLEEP_CUT + 0.04) };
};
