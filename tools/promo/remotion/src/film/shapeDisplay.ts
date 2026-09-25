/**
 * Il display dell'orologio come rettangolo del quadro, per la forma agganciata. Qui e non in `shape.ts` perché legge la
 * scaletta: la forma resta pura e senza cicli di import. `realDisplay` è quello vero della scena (`displayPlace.ts`);
 * `restDisplay` quello a riposo, senza pose né deriva, per la tavola della sola forma.
 */
import geo from "./mockup.geometry.json" with { type: "json" };
import { THEME } from "./theme.ts";
import { watchColumn } from "./timeline.ts";
import { displayRectAt } from "./displayPlace.ts";
import type { Scene, Timeline } from "./timeline.ts";
import type { Display, Rect } from "./shape.ts";

/** Lo stesso conto di `toFrame` in `Film.tsx`: il vetro frontale è largo `frontGlassPx`, il display ne è la parte displayR/glassR. */
export const frontDisplayRect = (cx: number, cy = 540): Rect => {
  const d = (2 * geo.front.displayR * THEME.frontGlassPx) / (2 * geo.front.glassR);
  return [cx - d / 2, cy - d / 2, d, d];
};

/** La scena che contiene il battito; oltre la fine l'ultima (la forma c'è anche dopo l'ultimo fotogramma). */
export const sceneAt = (t: Timeline, beat: number): Scene => t.scenes.find((s) => beat < s.at + s.len) ?? t.scenes[t.scenes.length - 1];

export const restDisplay = (t: Timeline): Display => (beat) => frontDisplayRect(watchColumn(sceneAt(t, beat)) * 1920);

/** Il display vero della scena a quel battito; dove non c'è (vista laterale, niente orologio) quello a riposo della scena.
 *  Il fotogramma è continuo, non arrotondato: la deriva è continua e la forma va letta anche fra due fotogrammi (il blur). */
export const realDisplay = (t: Timeline): Display => (beat) =>
  displayRectAt(t, (t.offsetSeconds + (beat * 60) / t.bpm) * t.fps) ?? frontDisplayRect(watchColumn(sceneAt(t, beat)) * 1920);
