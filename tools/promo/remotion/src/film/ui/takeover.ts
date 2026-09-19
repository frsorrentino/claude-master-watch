/**
 * Il takeover (piano 5 §2): un componente della UI prende tutto il quadro e si trasforma nella scena dopo. Quattro fasi su un
 * arco di `len` battiti a cavallo del taglio: grow (dal suo posto cresce fino a coprire il quadro, 0-0,42, `soft`), hold
 * (0,42-0,46, appena un respiro), become (0,46-0,72: si trasforma nella cosa della scena dopo), settle (0,72-1: la scena
 * dopo si compone sopra). Il fermo è corto di proposito: con il campo pieno che durava mezzo secondo si vedeva un vuoto. Funzione pura dell'avanzamento 0-1.
 * Regola dei colori (master, 16:10): il colore del componente vive nel become; nel settle lo sfondo torna alla palette.
 */
import { bezier, soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
/** Crescita che accelera e poi frena a lungo: la cosa ti viene addosso e si ferma piena. */
const growEase = bezier(0.55, 0, 0.2, 1);

export type Takeover = {
  grow: number;     // 0-1: quanto ha coperto il quadro
  hold: number;     // 1 nel fermo pieno
  become: number;   // 0-1: trasformazione nella cosa della scena dopo
  settle: number;   // 0-1: la scena dopo si compone, il colore torna alla palette
  cover: number;    // 0-1: quanto il quadro è coperto dal colore del componente (sale con grow, scende con settle)
};
export const takeoverAt = (p: number): Takeover => {
  const grow = growEase(ramp(p, 0, 0.42));
  const become = soft(ramp(p, 0.44, 0.54));
  // il campo pieno si dissolve SOPRA la scena dopo, che a quel punto è già composta: prima virava a un colore piatto e
  // restava lì un secondo e mezzo prima che comparisse qualcosa (Franz, 19/09 11:25)
  const settle = soft(ramp(p, 0.46, 0.62));
  return { grow, hold: p >= 0.42 && p < 0.46 ? 1 : 0, become, settle, cover: grow * (1 - settle) };
};
/** Il taglio della scaletta cade a 0,58 dell'arco (fine del hold): così la scena dopo nasce dentro il become. */
export const TAKEOVER_CUT = 0.46;
