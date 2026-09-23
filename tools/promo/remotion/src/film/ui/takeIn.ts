/**
 * Il volo del terminale (piano 4, primo pezzo; Franz, 23/09 20:15: «versione completa»), il takeover al contrario: il quadro
 * intero, che è il terminale del PC, si rimpicciolisce ed entra nel display come la card ✓ di payments-api in cima alla lista.
 * Funzioni pure dell'avanzamento `p` 0-1 lungo il volo.
 */
import { inOut, soft } from "../moves.ts";
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
 *  posa lunga. `terminal`: il testo del terminale, che sfuma fra 0,1 e 0,5 mentre arriva `card`, la card senza il suo
 *  testo (fra 0,2 e 0,6), tutte e due lente agli estremi e con i centri vicini: nella prima prova il terminale spariva quasi subito e per cinque
 *  fotogrammi restava una finestra vuota. `line`:
 *  la riga dell'esito in volo, che si scioglie (`lineAlpha`) nel testo della card (`body`) fra 0,6 e 0,85: mai due testi
 *  pieni insieme. */
export const takeInAt = (p: number): { shrink: number; terminal: number; line: number; lineAlpha: number; card: number; body: number } => {
  const melt = soft(ramp(p, 0.6, 0.85));
  return {
    shrink: soft(clamp(p)),
    terminal: 1 - inOut(ramp(p, 0.1, 0.5)),
    line: soft(ramp(p, 0.05, 0.85)),
    lineAlpha: 1 - melt,
    card: inOut(ramp(p, 0.2, 0.6)),
    body: melt,
  };
};

/** La finestra a `p`: dal quadro intero (`frame`, angoli vivi) al rettangolo della card. I quattro bordi si muovono
 *  ciascuno verso il suo, quindi la card d'arrivo sta sempre dentro la finestra. */
export const takeInRect = (to: Rect, p: number, frame: { w: number; h: number }): Rect => {
  const s = takeInAt(p).shrink;
  return { x: lerp(0, to.x, s), y: lerp(0, to.y, s), w: lerp(frame.w, to.w, s), h: lerp(frame.h, to.h, s), r: lerp(0, to.r, s) };
};

/** Lo schermo della scena che finisce sfuma nel colore del suo fondo, negli ultimi `frames` fotogrammi, pieno sull'ultimo
 *  (piano 6): nel corto lo schermo del terminale diventa il grigio del terminale, e al taglio la finestra che vola lo copre
 *  già. Al posto della dissolvenza di tutto l'orologio, che lo faceva sembrare un altro (Franz, 23/09 21:13). */
export const screenFadeAt = (frame: number, total: number, frames: number): number =>
  inOut(clamp((frame - (total - frames)) / Math.max(1, frames - 1)));
