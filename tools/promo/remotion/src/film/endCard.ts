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
