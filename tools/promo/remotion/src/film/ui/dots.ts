/**
 * I puntini dell'attesa (Franz, 20/09 08:57): sotto «It asks.», mentre il display dorme, tre puntini pulsano a turno —
 * la sessione ti sta scrivendo. Sulla notifica i tre collassano in un punto di luce che si allarga e sparisce.
 * Franz, 21/09 14:40: i tempi sono in BATTITI, non in frazioni della finestra del sonno — i culmini cadono 6, 4 e 2 battiti
 * prima della notifica — poi, alle 15:05, uno per battito: 33 · 34 · 35 con la notifica al 36, che fa da quarto tempo.
 * Funzione pura dell'avanzamento del sonno (ui/sleep.ts) e della sua lunghezza in battiti.
 */
import { DOTS_GAP, SLEEP_CUT } from "./sleep.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

export type Dots = { alpha: number; on: [number, number, number]; collapse: number };

/** Un puntino per battito (Franz, 21/09 15:05): puntino, puntino, puntino, notifica scandiscono l'unica battuta di silenzio. */
export const DOT_STEP = 1;
/** La frase della scena dopo si scrive tanti battiti prima della notifica: arriva sola, subito dopo il blink. */
export const TITLE_LEAD = 3 + DOTS_GAP;
/** I puntini entrano mentre la frase finisce di scriversi: il primo deve culminare già sul battito dopo il blink. */
export const DOTS_FROM = -2.3 - DOTS_GAP;
/** Quanto resta acceso un puntino dopo il suo turno: il crescendo si accumula. */
export const DOT_HOLD = 0.72;
/** Quanto resta acceso un puntino prima del suo turno: non si spengono, respirano. */
export const DOT_BASE = 0.28;

/** `p` avanzamento del sonno, `len` la sua lunghezza in battiti: da qui i battiti che mancano alla notifica. */
export const dotsAt = (p: number, len: number): Dots => {
  const b = (p - SLEEP_CUT) * len;                                   // battiti rispetto alla notifica (negativi prima)
  const alpha = ramp(b, DOTS_FROM, DOTS_FROM + 0.25) * (1 - ramp(b, 0, 0.07));
  const bell = (i: number): number => {
    const d = Math.abs(b - -(DOT_STEP * (3 - i) + DOTS_GAP - 1)) / (DOT_STEP * 0.5);   // mezzo passo per lato: uno per volta, non sfumati l'uno nell'altro
    return d >= 1 ? 0 : 0.5 + 0.5 * Math.cos(Math.PI * d);
  };
  // dopo il suo battito il puntino resta acceso: si riempiono uno dopo l'altro, e la notifica è il quarto passo (Franz, 21/09 15:40)
  const lit = (i: number): number => Math.max(bell(i), b >= -(DOT_STEP * (3 - i) + DOTS_GAP - 1) ? DOT_HOLD : 0);
  // sulla notifica i puntini spariscono e basta: il punto di luce che ne nasceva stava al posto del primo e si leggeva
  // come un quarto puntino (Franz, 21/09 16:18). La notifica la dicono gli anelli attorno all'orologio (Fx.tsx).
  return { alpha, on: [lit(0), lit(1), lit(2)], collapse: 0 };
};
