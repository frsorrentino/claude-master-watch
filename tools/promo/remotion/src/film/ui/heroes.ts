/** Coreografie dei momenti forti (piano 3): funzioni pure dell'avanzamento 0-1, come `moves.ts`. */
import { bezier, soft } from "../moves.ts";

/** Strappo: un oggetto che si stacca prende velocità per un attimo e poi frena a lungo (la curva `soft` parte troppo secca: un
 *  quarto della strada nei primi tre fotogrammi, e senza sfocatura di movimento sembra un taglio). */
const pull = bezier(0.35, 0, 0.15, 1);

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

/**
 * La card lascia il display (0-0,42, strappo e atterraggio morbido nel posto da protagonista), resta fuori con una deriva
 * lenta, rientra (0,62-0,95, atterraggio morbido sul suo rettangolo) e si dissolve sulla card vera (0,95-1). Su 3,5 battiti
 * (57 fotogrammi) uscita e rientro durano 800 e 630 ms.
 * - `travel` 0-1: dov'è tra il display (0) e il posto da protagonista (1); a 0 combacia con la card vera.
 * - `swing` 0-1: quanto è girata attorno all'asse verticale: solo in volo, all'arrivo è quasi frontale.
 * - `drift` −1…1: respiro lento mentre è fuori (posizione e inclinazione di pochi pixel e gradi).
 * - `patch` 0-1: quanto è coperta la card vera sul display (un fantasma della superficie che resta al suo posto).
 * - `alpha` 0-1: visibilità della card ricostruita (si dissolve solo alla fine, dopo essere atterrata sul display).
 */
export type CardOut = { travel: number; swing: number; drift: number; patch: number; alpha: number };
export const cardOutAt = (p: number): CardOut => {
  const out = pull(ramp(p, 0, 0.42)), back = soft(ramp(p, 0.62, 0.95));
  const travel = out * (1 - back);
  const fade = ramp(p, 0.95, 1);
  return {
    travel,
    swing: Math.sin(Math.PI * travel),
    drift: Math.sin(2 * Math.PI * ramp(p, 0.3, 0.8)),
    patch: ramp(p, 0, 0.03) * (1 - ramp(p, 0.95, 0.97)),   // il fantasma sparisce prima della card: sotto la dissolvenza c'è già la card vera
    alpha: p < 0 || p >= 1 ? 0 : 1 - fade,          // a 0 è già disegnata, combaciante con la card vera: il confronto si fa lì
  };
};
