/** Il battito di ciglia (piano 5): funzione pura del fotogramma. */
import { bezier, soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const move = bezier(0.3, 0, 0.1, 1);

/**
 * La parola in colore lascia il titolo e va a posarsi come titolo in alto (Franz, 18/09 18:22: «fermarsi come titolo centrato
 * in alto»), mentre la card esce dal display e si ferma grande al centro; quando tutte e due sono ferme le palpebre si
 * chiudono e si riaprono sulla scena dopo, dove la card è nello stesso posto e l'orologio compare attorno.
 * `f` fotogrammi dall'inizio, `cut` il fotogramma del taglio (la chiusura finisce lì).
 * `travel` 0-1: quanto la parola ha percorso verso il suo posto in alto; `lid` 0-1: palpebre chiuse.
 */
export const blinkAt = (f: number, cut: number): { travel: number; lid: number; word: number } => {
  const close = 7;                                   // le palpebre si chiudono negli ultimi 7 fotogrammi prima del taglio
  const travel = move(clamp(f / Math.max(1, cut - close - 2)));
  const lid = f < cut ? soft(clamp((f - (cut - close)) / close)) : 1 - soft(clamp((f - cut) / 9));
  return { travel, lid, word: f < cut ? 1 : 0 };
};
