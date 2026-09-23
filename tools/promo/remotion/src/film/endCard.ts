import { THEME } from "./theme.ts";

/**
 * I tempi del cartello, in battiti: `start` dall'inizio della scena, gli altri dall'inizio del cartello. «normal» è quello
 * del film lungo (22 battiti di scena); «compact» è quello del corto, dove il cartello dura 12 battiti e con i tempi lunghi
 * gli avvisi (Anthropic, marchi Google, voce sintetica) non comparivano mai (revisione del 23/09).
 */
export type EndPace = "normal" | "compact" | "blinds";
export const END_PACE: Record<EndPace, { start: number; sub: number; repo: number; notes: number }> = {
  normal: { start: 4, sub: 4, repo: 7, notes: 9 },
  compact: { start: 1, sub: 1.5, repo: 2.5, notes: 3.5 },
  // dopo la tapparella (corto, 23/09 pomeriggio): i listelli si voltano per 2,5 battiti dal taglio; il nome arriva a
  // tapparella quasi aperta e gli avvisi restano 4 battiti su 8
  blinds: { start: 2, sub: 1, repo: 1.5, notes: 2 },
};
/** Il battito della scena in cui compaiono gli avvisi. */
export const notesAt = (pace: EndPace = "normal"): number => END_PACE[pace].start + END_PACE[pace].notes;

/** Il contrasto fra due colori esadecimali, come lo calcolano le WCAG (1-21). */
export const contrast = (a: string, b: string): number => {
  const lum = (hex: string) => {
    const [r, g, bl] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255).map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
    return 0.2126 * r + 0.7152 * g + 0.0722 * bl;
  };
  const [hi, lo] = [lum(a), lum(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
};
/** I colori dei testi del cartello. «blue»: il cartello del corto resta sull'azzurro della tapparella chiusa (Franz, 23/09
 *  18:15); titolo e accento reggono (5,3 e 3,2:1), il grigio degli avvisi no (2,8:1) e schiarisce. */
export type EndTone = "blue";
export const endColors = (tone?: EndTone): { title: string; accent: string; dim: string } =>
  tone === "blue" ? { title: THEME.white, accent: THEME.accent, dim: "#E4E9F4" } : { title: THEME.white, accent: THEME.accent, dim: THEME.dim };
