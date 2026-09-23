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

export type Sleep = { light: number; halo: number; haloR: number; field: number; rim: number; move: number; title: number; still: number };

/**
 * Il blocco sta su DUE battute (Franz, 19/09 18:51: «serve più rapido»), dal battito 32 al 40: cala ad ambient (32,5-33),
 * la camera si sposta (33-34,4), il titolo si scrive (34,5), la notifica sveglia il display (38) e l'attacco del giro
 * principale arriva DUE battiti dopo (40): un battito solo era troppo attaccato, quattro troppo lontani (Franz, 20:14).
 */
export const SLEEP_CUT = 0.75;
/** Quanta luce resta quando il display dorme. */
export const AMBIENT = 0.08;
/** Dove, nella finestra, il display è spento e la musica è a zero: da qui comincia il silenzio. */
export const HUSH = 0.125;
/** Quanta luce resta intorno: la cassa si deve vedere mentre la camera si sposta. */
export const HALO = 0.45;
/** Quanto resta acceso il campo dell'atto nel sonno: non nero pieno, o il quadro diventa un buco (Franz, 19/09 22:40). */
export const FIELD = 0.18;
/** Quanto si stringe l'alone dietro l'orologio mentre la luce se ne va: da tutto il quadro a un terzo, verso la cassa. */
export const HALO_R = 0.32;
/** Il respiro dell'ambient: quanto sale la luce del display fra un respiro e l'altro (0,08 → 0,13). */
export const BREATH = 0.05;
/** Dove, nella finestra, il titolo della scena dopo comincia a scriversi: con la camera che si sposta, prima della
 *  notifica (Franz, 19/09 18:14). La frase è già lì quando il display si riaccende. */
export const TITLE_AT = 0.3125;   // battito 34,5 di un blocco 32-40: il titolo si scrive dopo il carrello, prima della notifica
/** Il lampo del risveglio: un display che si accende dà più luce nel primo decimo di secondo. */
export const WAKE = 1.3;

/** Tre respiri fra il silenzio e la notifica, e l'ultimo si chiude ESATTAMENTE sul battito della campanella. */
const BREATHS = 3;
const breathAt = (p: number): number => {
  if (p < HUSH || p > SLEEP_CUT) return 0;
  const cycle = (SLEEP_CUT - HUSH) / BREATHS;
  return 0.5 - 0.5 * Math.cos((2 * Math.PI * (p - HUSH)) / cycle);
};

/** Con la lunghezza del sonno in battiti il respiro si aggancia ai puntini (uno per battito, 3, 2 e 1 prima della
 *  notifica) e cresce a ogni passo: mezzo, uno, uno e otto decimi del respiro normale (Franz, 21/09 15:40). */
const RISE = [0.5, 1, 1.8];
/** Battiti fra l'ultimo puntino e la notifica: 1 = la notifica è il quarto tempo; 2 = in mezzo entra la musica (prova 21/09 16:42). */
export const DOTS_GAP = 1;
const breathOnDots = (b: number): number => {
  let v = 0;
  for (let i = 0; i < 3; i++) {
    const d = Math.abs(b + (3 - i) + (DOTS_GAP - 1)) / 0.5;
    if (d < 1) v = Math.max(v, RISE[i] * (0.5 + 0.5 * Math.cos(Math.PI * d)));
  }
  return v;
};

export const sleepAt = (p: number, len?: number, breathe = true): Sleep => {
  // lo spostamento comincia quando il display è già ad ambient e finisce prima del taglio: mezzo secondo di carrello visibile
  const move = camera(ramp(p, 0.125, 0.3));
  // il titolo della scena che dorme se ne va appena prima che si scriva quello nuovo: due frasi insieme non si leggono
  const title = 1 - ramp(p, TITLE_AT - 0.05, TITLE_AT);
  // Ferma la deriva lenta dal carrello fino a dopo il risveglio (Franz, 19/09 22:20: «tra ambient e notifica l'orologio
  // cambia posizione»): al buio la deriva è l'unico movimento in quadro, e in cinque battiti sposta l'orologio di dieci
  // pixel. Si spegne con il carrello, resta spenta per tutto il sonno, e torna piano dopo la notifica.
  const still = smooth(ramp(p, 0.125, 0.3)) * (1 - smooth(ramp(p, SLEEP_CUT + 0.06, SLEEP_CUT + 0.2)));
  // `breathe` false: l'ambient resta a luce ferma. Il respiro serviva allo stop and go della musica del film lungo; nel
  // corto non c'è (Franz, 23/09 14:34)
  const breath = !breathe ? 0 : len ? breathOnDots((p - SLEEP_CUT) * len) : breathAt(p);
  if (p < SLEEP_CUT) {
    const off = dimmer(ramp(p, 0.0625, HUSH));   // mezzo battito, e finisce esatta sul battito: lo stacco è secco (Franz, 19/09 19:41)
    return {
      light: 1 - (1 - AMBIENT) * off + BREATH * breath,   // ad ambient il display respira, come il quadrante vero
      halo: 1 - (1 - HALO) * off + 0.05 * breath,
      haloR: 1 - (1 - HALO_R) * off,                      // l'alone non cala soltanto: si stringe verso la cassa
      field: 1 - (1 - FIELD) * off,
      rim: 0, move, title, still,
    };
  }
  // un display si accende in un fotogramma: sul taglio è già acceso, e il lampo si smorza dopo (non è una dissolvenza)
  const settle = smooth(ramp(p, SLEEP_CUT + 0.02, 0.95));
  const light = WAKE - (WAKE - 1) * settle;
  // il risveglio si vede anche fuori dal vetro: tre fotogrammi di luce sulla ghiera, e il campo torna pieno di colpo
  const rim = 1 - clamp((p - SLEEP_CUT) / 0.03);
  return { light, halo: light, haloR: 1, field: 1, rim, move, title: 0, still };
};

/** L'avanzamento del sonno per una scena: `own` = la scena che dorme (la finestra finisce sul suo ultimo fotogramma, che è
 *  il taglio), altrimenti la scena che si risveglia (riparte da `SLEEP_CUT`). `frames` = la finestra intera in fotogrammi. */
export const sleepP = (frame: number, frames: number, own: boolean, total: number): number =>
  own ? SLEEP_CUT - (total - frame) / frames : SLEEP_CUT + frame / frames;
