/**
 * Il display dell'orologio come rettangolo del quadro, per la forma agganciata. Qui e non in `shape.ts` perché legge la
 * scaletta: la forma resta pura e senza cicli di import. È il display a riposo (posa senza movimenti né deriva), per la
 * tavola e i test; la geometria vera (pose, deriva, tre quarti) arriva con l'omografia ai passi 3-4 della specifica.
 */
import geo from "./mockup.geometry.json";
import { THEME } from "./theme.ts";
import { watchColumn } from "./timeline.ts";
import type { Timeline } from "./timeline.ts";
import type { Display, Rect } from "./shape.ts";

/** Lo stesso conto di `toFrame` in `Film.tsx`: il vetro frontale è largo `frontGlassPx`, il display ne è la parte displayR/glassR. */
export const frontDisplayRect = (cx: number, cy = 540): Rect => {
  const d = (2 * geo.front.displayR * THEME.frontGlassPx) / (2 * geo.front.glassR);
  return [cx - d / 2, cy - d / 2, d, d];
};

export const restDisplay = (t: Timeline): Display => (beat) => {
  const scene = t.scenes.find((s) => beat < s.at + s.len) ?? t.scenes[t.scenes.length - 1];
  return frontDisplayRect(watchColumn(scene) * 1920);
};
