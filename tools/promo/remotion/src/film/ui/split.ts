/**
 * Lo sdoppiamento degli account (Franz, 20/09 19:06): il terminale della scena prima si STRINGE da destra fino a metà
 * quadro — e in quella metà che si libera compare il secondo terminale, con le sue sessioni — poi il lato divisorio
 * continua verso sinistra fino a chiudersi, e resta una schermata sola: quella della scena dopo.
 *  - `edge` 1→0,5→0: dove sta il lato divisorio, in frazione di quadro (1 = tutto il primo, 0 = tutto il secondo);
 *  - `cards` 0-1: quanto sono entrate le schede dei due terminali;
 *  - `mid` 0-1: quanto siamo dentro la sosta a metà, dove la frase si legge.
 * Funzione pura dell'avanzamento 0-1 della scena.
 */
import { bump, soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

export type Split = { edge: number; cards: number; mid: number };

/** L'arrivo a metà quadro: parte piano, prende velocità, scavalca di poco e si assesta (Franz, 20/09 19:27: «ease e
 *  bounce, non lineare»). La chiusura invece frena e basta: se rimbalzasse sembrerebbe un errore. */
const morbido = (t: number) => t * t * (3 - 2 * t);
const back = bump(0.09);
const arrivo = (t: number) => back(morbido(t));

export const splitAt = (p: number, openBeats: number, holdBeats: number, closeBeats: number, len: number): Split => {
  const a = openBeats / len, b = (openBeats + holdBeats) / len;
  const edge = p < a ? 1 - 0.5 * arrivo(clamp(p / a)) : p < b ? 0.5 : 0.5 * (1 - soft(clamp((p - b) / Math.max(1e-6, 1 - b))));
  return { edge, cards: ramp(p, a * 0.35, a * 1.05), mid: ramp(p, a, a + 0.04) * (1 - ramp(p, b, b + 0.04)) };
};
