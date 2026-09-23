/** La camera che si avvicina o arretra dentro una scena (analisi del 23/09, priorità 1): lo zoom va da `from` a `to`
 *  con una curva morbida; «out» parte veloce e frena, per un movimento che si ferma su un evento. */
export type Dolly = { from: number; to: number; ease?: "inOut" | "out" };
const clamp = (t: number) => Math.min(1, Math.max(0, t));
export const dollyAt = (d: Dolly | undefined, p: number): number => {
  if (!d) return 1;
  const t = clamp(p);
  const e = d.ease === "out" ? 1 - (1 - t) ** 3 : t * t * (3 - 2 * t);
  return d.from + (d.to - d.from) * e;
};
