/**
 * Il muro degli account (revisione 3D, momento 2): la finestra del terminale che riempie il quadro resta lì, la **camera
 * arretra** e si scopre che è una lastra fra tante, disposte su una parete leggermente curva — un account per lastra.
 * Poi la camera rientra in una sola lastra e da lì comincia la Panoramica.
 *  - `back`   0-1: quanto la camera è arretrata (0 = dentro la lastra centrale, 1 = tutto il muro nel quadro);
 *  - `others` 0-1: quanto le altre lastre sono comparse, a ondate dal centro verso i bordi;
 *  - `drift`  radianti: il lento scorrimento laterale del muro, che dà il parallasse;
 *  - `inAgain` 0-1: il rientro dentro una lastra, alla fine.
 * Funzione pura dell'avanzamento 0-1, testabile senza Three.
 */
const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ease = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

export type Wall = { back: number; others: number; drift: number; inAgain: number };

/** Il ritardo di comparsa della lastra `i` su `n`: dal centro verso i bordi. */
export const wallDelay = (i: number, n: number): number => Math.abs(i - (n - 1) / 2) / Math.max(1, (n - 1) / 2);

export const wallAt = (p: number): Wall => ({
  back: ease(clamp((p - 0.04) / 0.46)),
  others: ease(clamp((p - 0.16) / 0.4)),
  drift: (p - 0.5) * 0.22,
  inAgain: ease(clamp((p - 0.74) / 0.26)),
});

/** Quanto è comparsa la lastra `i`: le centrali per prime, le esterne dopo; quella dell'account in primo piano c'è sempre. */
export const wallPlateAt = (p: number, i: number, n: number, hero: number): number => {
  if (i === hero) return 1;
  const w = wallAt(p);
  return clamp((w.others - wallDelay(i, n) * 0.55) / 0.45);
};
