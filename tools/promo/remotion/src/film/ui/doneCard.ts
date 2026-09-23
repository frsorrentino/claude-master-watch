/**
 * La card ✓ della fine del lavoro (design del corto, 23/09): arriva sul display insieme alla seconda vibrazione, sale dal
 * basso come una notifica e il segno ✓ si disegna in un tratto. `rise` 0-1: quanto è salita (un terzo di battito, frenata
 * lunga); `draw` 0-1: quanto del ✓ è disegnato (da un decimo a mezzo battito dalla vibrazione). Con `rest` è già a posto e
 * disegnata: la scena dopo la tiene com'è. Funzione pura del fotogramma.
 */
import { LIST_BODY, type CardBody } from "./UiTokens.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
export const doneCardAt = (frame: number, beat: number, rest = false): { rise: number; draw: number } => {
  if (rest) return { rise: 1, draw: 1 };
  const r = clamp(frame / (beat / 3));
  const d = clamp((frame - 0.1 * beat) / (0.4 * beat));
  return { rise: 1 - (1 - r) ** 3, draw: d * d * (3 - 2 * d) };
};

/** Dove sta la card e quanto si scurisce il display. Senza `slot` sale da sotto il bordo (a riposo al centro, card alta
 *  213) e il display si scurisce con lei; con `slot` sta ferma nella sua riga della lista e il display resta com'è. */
export const doneCardPlace = (rise: number, slot?: number): { y: number; scrim: number } =>
  slot === undefined ? { y: 134 + (1 - rise) * 346, scrim: rise } : { y: slot, scrim: 0 };

/** Nella riga della lista la card scrive il testo come le righe vere dell'app (revisione del 23/09: stava 7 unità più in
 *  alto e con righe più larghe); quella che sale sul display resta com'era. */
export const doneCardBody = (slot?: number): CardBody | undefined => (slot === undefined ? undefined : LIST_BODY);
