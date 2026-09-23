/** I tempi delle schede della Panoramica accanto all'orologio (ui/Aside.tsx), funzioni pure del fotogramma. */
import { soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

/** `t` 0-1 lungo la scheda; `enter` quanto è entrata (dissolvenza); `d` quanto il dato è disegnato. Senza `fadeIn`/`draw`
 *  (fotogrammi) i tempi sono frazioni della scheda, come nel film lungo, con le stesse espressioni: il film lungo resta
 *  identico al pixel. Con tempi propri la scheda del corto entra e si disegna prima che parta la tapparella (23/09). */
export const asideAt = (frame: number, from: number, len: number, fadeIn?: number, draw?: number): { t: number; enter: number; d: number } => {
  const t = (frame - from) / len;
  if (fadeIn === undefined && draw === undefined) return { t, enter: soft(clamp(t / 0.12)), d: soft(clamp((t - 0.12) / 0.45)) };
  const fi = fadeIn ?? 0.12 * len, dr = draw ?? 0.45 * len;
  return { t, enter: soft(clamp((frame - from) / fi)), d: soft(clamp((frame - from - fi) / dr)) };
};
/** Quanto è piena la riga `k` del Context a disegno `d`: le righe partono una dopo l'altra. */
export const contextFill = (d: number, k: number): number => clamp(d * 1.6 - k * 0.35);
/** Il testo del Context (nomi e percentuali) sfuma in 12 fotogrammi a partire da 14 prima della tapparella. */
export const contextTextAt = (frame: number, blindStart: number): number => 1 - clamp((frame - (blindStart - 14)) / 12);
/** Quanto resta di una scheda che se ne va: sfuma negli 8 fotogrammi prima di `gone`. */
export const fadeOutAt = (frame: number, gone: number): number => 1 - soft(clamp((frame - (gone - 8)) / 8));
/** Il numero della scheda Work: sale da 0 e arriva a `n` a metà disegno. */
export const workCount = (n: number, d: number): number => Math.round(n * clamp(d * 2));
/** Quanto è lunga la barra `k` della scheda Work a disegno `d`: partono una dopo l'altra. */
export const workBar = (d: number, k: number): number => clamp(d * 1.7 - k * 0.3);
