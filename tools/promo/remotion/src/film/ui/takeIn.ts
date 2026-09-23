/**
 * Il volo del terminale (piano 4, primo pezzo; Franz, 23/09 20:15: «versione completa»), il takeover al contrario: il quadro
 * intero, che è il terminale del PC, si rimpicciolisce ed entra nel display come la card ✓ di payments-api in cima alla lista.
 * Funzioni pure dell'avanzamento `p` 0-1 lungo il volo.
 */
import { soft } from "../moves.ts";
import { LIST_BODY } from "./UiTokens.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
const lerp = (a: number, b: number, t: number) => a * (1 - t) + b * t;   // esatto agli estremi: all'arrivo coincide con la card

/** Un rettangolo nel quadro: angolo in alto a sinistra, misura, raggio degli angoli, in pixel. */
export type Rect = { x: number; y: number; w: number; h: number; r: number };

/** La card della riga `slot` della lista (x 26, larga 428, raggio 42, alta quanto UiCard con `lines` righe di LIST_BODY) nel
 *  quadro, col display centrato in (`dx`, `dy`) e `u` pixel per unità: lo stesso conto dei Heroes e di `toFrame`. */
export const slotRect = (slot: number, dx: number, dy: number, u: number, lines: number): Rect => ({
  x: dx + (26 - 240) * u,
  y: dy + (slot - 240) * u,
  w: 428 * u,
  h: (24 + 36 + LIST_BODY.shift + lines * LIST_BODY.line + 24.5) * u,
  r: 42 * u,
});

/** `shrink`: quanto la finestra è arrivata; parte decisa, così il bordo sinistro libera subito la colonna della frase, e si
 *  posa lunga. `terminal`: il testo del terminale, che se ne va nel primo 40 %. `line`: la riga dell'esito in volo.
 *  `card`: intestazione e testo della card, fra 0,55 e 0,9. */
export const takeInAt = (p: number): { shrink: number; terminal: number; line: number; card: number } => ({
  shrink: soft(clamp(p)),
  terminal: 1 - soft(ramp(p, 0, 0.4)),
  line: soft(ramp(p, 0.05, 0.85)),
  card: soft(ramp(p, 0.55, 0.9)),
});

/** La finestra a `p`: dal quadro intero (`frame`, angoli vivi) al rettangolo della card. I quattro bordi si muovono
 *  ciascuno verso il suo, quindi la card d'arrivo sta sempre dentro la finestra. */
export const takeInRect = (to: Rect, p: number, frame: { w: number; h: number }): Rect => {
  const s = takeInAt(p).shrink;
  return { x: lerp(0, to.x, s), y: lerp(0, to.y, s), w: lerp(frame.w, to.w, s), h: lerp(frame.h, to.h, s), r: lerp(0, to.r, s) };
};
