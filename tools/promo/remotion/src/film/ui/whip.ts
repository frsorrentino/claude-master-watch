/** La frustata tra gli atti: funzione pura del fotogramma. */
import { soft } from "../moves.ts";

/** Dove sta la frustata in un fotogramma 0-`frames`: attraversa il quadro da destra a sinistra con atterraggio morbido, e la sua
 *  luce sale e scende (piena a metà corsa). Funzione pura. */
export const whipAt = (f: number, frames: number): { x: number; light: number } => {
  const p = Math.min(1, Math.max(0, f / frames));
  return { x: 1 - soft(p), light: Math.sin(Math.PI * p) };
};

