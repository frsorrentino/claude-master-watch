/** Coreografie dei momenti forti (piano 3): funzioni pure dell'avanzamento 0-1, come `moves.ts`. */
import { bezier, soft } from "../moves.ts";

/** Strappo: un oggetto che si stacca prende velocità per un attimo e poi frena a lungo (la curva `soft` parte troppo secca: un
 *  quarto della strada nei primi tre fotogrammi, e senza sfocatura di movimento sembra un taglio). */
const pull = bezier(0.35, 0, 0.15, 1);

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

/**
 * Il volo comune ai componenti che lasciano il display e ci tornano: strappo e atterraggio morbido nel posto da protagonista
 * (0-`outEnd`), sosta con deriva lenta, rientro morbido sul proprio rettangolo (`backFrom`-`backTo`), dissolvenza sull'originale.
 * - `travel` 0-1: dov'è tra il display (0) e il posto da protagonista (1); a 0 combacia con l'originale sul fotogramma.
 * - `swing` 0-1: quanto è girato attorno all'asse verticale: solo in volo, all'arrivo è quasi frontale.
 * - `drift` −1…1: respiro lento mentre è fuori (posizione e inclinazione di pochi pixel e gradi).
 * - `patch` 0-1: quanto è coperto l'originale sul display (un fantasma della superficie che resta al suo posto); il fantasma
 *   sparisce prima del componente (0,95-0,97), così sotto la dissolvenza c'è già l'originale.
 * - `alpha` 0-1: visibilità del componente ricostruito: a 0 è già disegnato, combaciante (il confronto di fedeltà si fa lì);
 *   si dissolve solo dopo essere atterrato.
 */
export type Flight = { travel: number; swing: number; drift: number; patch: number; alpha: number };
export const flightAt = (p: number, outEnd: number, backFrom: number, backTo = 0.95): Flight => {
  const out = pull(ramp(p, 0, outEnd)), back = soft(ramp(p, backFrom, backTo));
  const travel = out * (1 - back);
  const fade = ramp(p, backTo, 1);
  return {
    travel,
    swing: Math.sin(Math.PI * travel),
    drift: Math.sin(2 * Math.PI * ramp(p, outEnd * 0.7, backFrom + 0.1)),
    patch: ramp(p, 0, 0.03) * (1 - ramp(p, backTo, backTo + 0.02)),
    alpha: p < 0 || p >= 1 ? 0 : 1 - fade,
  };
};

/** La card lascia il display (0-0,42), resta fuori, rientra (0,62-0,95) e si dissolve sulla card vera (0,95-1). Su 3,5
 *  battiti (57 fotogrammi) uscita e rientro durano 800 e 630 ms. */
export type CardOut = Flight;
export const cardOutAt = (p: number): CardOut => flightAt(p, 0.42, 0.62);

/**
 * Il gauge della quota lascia la card (0-0,3) svuotandosi mentre vola, fuori si disegna da zero al suo valore (0,34-0,62,
 * molla lenta come nell'app) con il numero che conta, resta, e rientra pieno (0,72-0,95) com'era sul display.
 * `value` 0-1: frazione del valore vero mostrata dagli archi e dal contatore; agli estremi è 1, cioè il display.
 * `label` 0-1: numero e frase accanto al gauge, solo mentre è fermo fuori (compaiono con il disegno, se ne vanno prima del rientro).
 */
export type GaugeHero = Flight & { value: number; label: number };
export const gaugeHeroAt = (p: number): GaugeHero => {
  const f = flightAt(p, 0.3, 0.72);
  const value = p < 0.32 ? 1 - pull(ramp(p, 0, 0.3)) : soft(ramp(p, 0.34, 0.62));
  const label = ramp(p, 0.33, 0.38) * (1 - ramp(p, 0.7, 0.74));
  return { ...f, value, label };
};

/**
 * I tasti della domanda nascono come contorno fuori dal display (0-0,2), si riempiono (0,2-0,3: «1 · yes» pieno, «2 · no»
 * scuro), l'anello corallo della pressione lunga corre attorno a «1 · yes» in tempo con la pressione vera (0,3-0,68), poi
 * i tasti restano un attimo e si dissolvono (0,86-1) mentre sul display arriva «Sent». Non lasciano il display: sono l'eco.
 */
export type OptionsBuild = { build: number; ring: number; alpha: number; travel: number };
export const optionsBuildAt = (p: number): OptionsBuild => ({
  build: soft(ramp(p, 0, 0.3)),
  ring: ramp(p, 0.3, 0.68),
  alpha: p < 0 || p >= 1 ? 0 : 1 - ramp(p, 0.86, 1),
  travel: p < 0 || p >= 1 ? 0 : Math.min(soft(ramp(p, 0, 0.2)), 1 - ramp(p, 0.86, 1)),
});
