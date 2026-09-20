/**
 * Le due bande degli account (Franz, 20/09 18:48): il campo della scena si apre in due — lavoro a sinistra, personale a
 * destra — con un orologio solo in quadro, e alla fine **una vince**: si allarga e diventa il fondo della sezione dopo,
 * dove i pannelli seguono quell'account solo. Funzione pura dell'avanzamento 0-1 della scena.
 *  - `split` 0-1: quanto è aperta la separazione (0 = campo unico, 1 = due bande);
 *  - `win` 0-1: quanto la banda di sinistra si è ripresa il quadro.
 */
import { soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

export type Bands = { split: number; win: number };

export const bandsAt = (p: number, openBeats: number, winBeats: number, len: number): Bands => ({
  split: soft(ramp(p, 0, openBeats / len)),
  win: soft(ramp(p, 1 - winBeats / len, 1)),
});
