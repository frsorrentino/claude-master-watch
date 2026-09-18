/** Il battito di ciglia (piano 4 §2 bis): funzione pura del fotogramma attorno al taglio. */
import { bezier, soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const grow = bezier(0.5, 0, 0.9, 0.6);   // la parola prende velocità: alla fine riempie il quadro di colpo, come una cosa che ti viene addosso

/** `f` fotogrammi dall'inizio, `cut` il fotogramma del taglio. `scale`: la parola in colore da 1 (titolo) a `max`; `lid` 0-1:
 *  quanto le palpebre nere sono chiuse (si chiudono negli ultimi 5 fotogrammi prima del taglio, si riaprono in 8 dopo). */
export const blinkAt = (f: number, cut: number, max = 14): { scale: number; lid: number; word: number } => {
  const g = grow(clamp(f / (cut - 4)));
  const scale = 1 + (max - 1) * g;
  const lid = f < cut ? soft(clamp((f - (cut - 5)) / 5)) : 1 - soft(clamp((f - cut) / 8));
  return { scale, lid, word: f < cut ? 1 : 0 };
};
