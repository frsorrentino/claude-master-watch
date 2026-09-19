/**
 * Il sonno del display (Franz, 19/09 17:44). Il passaggio verso «It asks» non lo fa il quadro: lo fa l'orologio. Il display
 * cala ad ambient come uno schermo che si addormenta; con il display ad ambient la camera si sposta — e la si VEDE
 * spostarsi (Franz, 17:58) — dal centro alla colonna di destra; sul taglio la notifica riaccende il display, che si apre
 * già sulla domanda. Non esce nessun oggetto dal display e nessun campo copre il quadro: il buio è DENTRO il vetro.
 *  - `light`: luminosità del display, 1 = normale, `AMBIENT` = addormentato, `WAKE` nel lampo del risveglio;
 *  - `halo`: luce intorno (l'alone dietro l'orologio): cala con il display ma non si spegne, se no la camera si muove al buio;
 *  - `move` 0-1: avanzamento dello spostamento della camera, tutto dentro la finestra ad ambient.
 * Funzione pura dell'avanzamento 0-1.
 */
import { bezier } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
const dimmer = bezier(0.25, 0.5, 0.5, 1);  // cala come uno schermo vero: perde subito la maggior parte della luce e poi scivola
const camera = bezier(0.4, 0, 0.2, 1);     // il carrello: parte piano, prende velocità, frena a lungo
const smooth = (t: number) => t * t * (3 - 2 * t);

export type Sleep = { light: number; halo: number; move: number; title: number };

/** Il taglio con la scena dopo cade a display addormentato: il fotogramma dopo è il risveglio. */
export const SLEEP_CUT = 0.62;
/** Quanta luce resta quando il display dorme. */
export const AMBIENT = 0.08;
/** Quanta luce resta intorno: la cassa si deve vedere mentre la camera si sposta. */
export const HALO = 0.45;
/** Dove, nella finestra, il titolo della scena dopo comincia a scriversi: con la camera che si sposta, prima della
 *  notifica (Franz, 19/09 18:14). La frase è già lì quando il display si riaccende. */
export const TITLE_AT = 0.34;
/** Il lampo del risveglio: un display che si accende dà più luce nel primo decimo di secondo. */
export const WAKE = 1.3;

export const sleepAt = (p: number): Sleep => {
  // lo spostamento comincia quando il display è già ad ambient e finisce prima del taglio: mezzo secondo di carrello visibile
  const move = camera(ramp(p, 0.32, 0.56));
  // il titolo della scena che dorme se ne va appena prima che si scriva quello nuovo: due frasi insieme non si leggono
  const title = 1 - ramp(p, TITLE_AT - 0.1, TITLE_AT);
  if (p < SLEEP_CUT) {
    const off = dimmer(ramp(p, 0.06, 0.3));
    return { light: 1 - (1 - AMBIENT) * off, halo: 1 - (1 - HALO) * off, move, title };
  }
  // un display si accende in un fotogramma: sul taglio è già acceso, e il lampo si smorza dopo (non è una dissolvenza)
  const settle = smooth(ramp(p, SLEEP_CUT + 0.03, 0.78));
  const light = WAKE - (WAKE - 1) * settle;
  return { light, halo: light, move, title: 0 };
};

/** L'avanzamento del sonno per una scena: `own` = la scena che dorme (la finestra finisce sul suo ultimo fotogramma, che è
 *  il taglio), altrimenti la scena che si risveglia (riparte da `SLEEP_CUT`). `frames` = la finestra intera in fotogrammi. */
export const sleepP = (frame: number, frames: number, own: boolean, total: number): number =>
  own ? SLEEP_CUT - (total - frame) / frames : SLEEP_CUT + frame / frames;
