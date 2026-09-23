/** Il battito di ciglia (piano 5): funzione pura del fotogramma. */
import { bump, soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const move = bump(0.055);   // la parola arriva in alto e si assesta con un filo di rimbalzo
/** Le palpebre si chiudono negli ultimi 5 fotogrammi prima del taglio. */
export const LIDS_CLOSE = 5;
const WORDS_OUT = 8;        // le parole di una frase escono in 8 fotogrammi (WordMask)

/**
 * La parola in colore lascia il titolo e va a posarsi come titolo in alto (Franz, 18/09 18:22: «fermarsi come titolo centrato
 * in alto»), mentre la card esce dal display e si ferma grande al centro; quando tutte e due sono ferme le palpebre si
 * chiudono e si riaprono sulla scena dopo, dove la card è nello stesso posto e l'orologio compare attorno.
 * `f` fotogrammi dall'inizio, `cut` il fotogramma del taglio (la chiusura finisce lì).
 * `travel` 0-1: quanto la parola ha percorso verso il suo posto in alto; `lid` 0-1: palpebre chiuse.
 */
export const blinkAt = (f: number, cut: number): { travel: number; lid: number; word: number; fade: number } => {
  const close = LIDS_CLOSE;
  // la parola sale INSIEME alla card che esce dal display e arriva al suo posto un filo prima di lei (Franz, 21/09 14:25):
  // il blink comincia sul battito in cui parte la card, e il viaggio della card al centro dura il 42 % dei suoi 3 battiti
  // (≈ 20 fotogrammi): la parola ne impiega 16. La frase intera è già rimasta in quadro fino a qui.
  const TRAVEL = 16;
  const travel = move(clamp(f / Math.min(TRAVEL, Math.max(1, cut - close - 4))));
  const lid = f < cut ? soft(clamp((f - (cut - close)) / close)) : 1 - soft(clamp((f - cut) / 7));
  // la parola NON si spegne: cresce e si ferma in alto al centro, dove non copre la card grande (Franz, 19/09 09:33)
  const fade = 1;
  return { travel, lid, word: f < cut ? 1 : 0, fade };
};

/** Con le sole palpebre (`out: "lids"`) la frase deve essere già uscita quando cominciano a chiudersi: prima usciva negli
 *  ultimi 8 fotogrammi, insieme a loro (revisione del 23/09). `leave` = il fotogramma in cui la frase comincia a uscire. */
export const beforeLids = (leave: number, total: number): number => Math.min(leave, total - LIDS_CLOSE - WORDS_OUT);
