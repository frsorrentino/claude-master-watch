/**
 * La card ✓ della fine del lavoro (design del corto, 23/09): arriva sul display insieme alla seconda vibrazione, sale dal
 * basso come una notifica e il segno ✓ si disegna in un tratto. `rise` 0-1: quanto è salita (un terzo di battito, frenata
 * lunga); `draw` 0-1: quanto del ✓ è disegnato (da un decimo a mezzo battito dalla vibrazione). Con `rest` è già a posto e
 * disegnata: la scena dopo la tiene com'è. Funzione pura del fotogramma.
 */
const clamp = (t: number) => Math.min(1, Math.max(0, t));
export const doneCardAt = (frame: number, beat: number, rest = false): { rise: number; draw: number } => {
  if (rest) return { rise: 1, draw: 1 };
  const r = clamp(frame / (beat / 3));
  const d = clamp((frame - 0.1 * beat) / (0.4 * beat));
  return { rise: 1 - (1 - r) ** 3, draw: d * d * (3 - 2 * d) };
};
