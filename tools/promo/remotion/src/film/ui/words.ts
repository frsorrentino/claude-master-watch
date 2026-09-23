/** Quando parte ogni parola di una frase (WordMask), in fotogrammi dall'inizio della frase: una ogni `perWordFrames`; le prime
 *  `carry` arrivano già scritte dalla scena prima; con `pauseFrames` la seconda riga (e le dopo) aspetta tanto in più
 *  (Franz, 23/09 23:20: «una pausa più lunga dopo Claude Code»). */
export const wordStarts = (lines: string[], perWordFrames: number, carry = 0, pauseFrames = 0): number[] => {
  const out: number[] = [];
  let i = 0;
  lines.forEach((l, li) => { for (const _ of l.split(" ")) out.push((i++ - carry) * perWordFrames + (li >= 1 ? pauseFrames : 0)); });
  return out;
};
