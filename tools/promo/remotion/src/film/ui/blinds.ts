/**
 * La tapparella (revisione 3D, momento 1): le due barre del Context non si dissolvono, diventano i primi due listelli di una
 * tapparella che copre il quadro e si volta, scoprendo la scena dopo. Coreografia pura, un listello alla volta:
 *  - `spread`  0-1: il listello nasce largo come la barra (760 px, a sinistra) e si allarga fino a coprire il quadro;
 *  - `born`    0-1: i listelli che NON sono le due barre entrano dopo, sfalsati dal centro verso i capi;
 *  - `rot`     radianti: la voltata sul proprio asse, sfalsata anch'essa, oltre il quarto di giro così l'apertura è netta;
 *  - `alpha`   il listello di taglio non si vede più: sparisce quando ha finito di voltarsi.
 * Tutto funzione dell'avanzamento 0-1 dell'effetto, testabile senza Three.
 */
const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ease = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);   // morbida ai due capi
const out = (t: number) => 1 - Math.pow(1 - t, 3);                                          // parte svelta, frena a lungo

export type Blind = { spread: number; born: number; rot: number; alpha: number };

/** Il ritardo del listello `i` su `n`, contato dai due che erano le barre (`bars`): chi è più lontano parte dopo. */
export const blindDelay = (i: number, n: number, bars: readonly [number, number]): number => {
  const d = Math.min(Math.abs(i - bars[0]), Math.abs(i - bars[1]));
  return d / Math.max(1, n - 1);
};

export const blindAt = (p: number, i: number, n: number, bars: readonly [number, number] = [4, 7]): Blind => {
  const isBar = i === bars[0] || i === bars[1];
  const del = blindDelay(i, n, bars);
  // le due barre ci sono già e si allargano subito; gli altri nascono dopo, a ventaglio
  const born = isBar ? 1 : ease(clamp((p - 0.04 - del * 0.45) / 0.18));
  const spread = isBar ? ease(clamp((p - 0.02) / 0.3)) : 1;
  // la voltata comincia quando la tapparella è chiusa, sfalsata dal centro ai capi
  const r = ease(clamp((p - 0.4 - del * 0.42) / 0.32));
  const rot = -r * (Math.PI / 2 + 0.22);
  // il listello svanisce quando è quasi di taglio: l'uscita segue la voltata, non l'orologio
  const alpha = 1 - out(clamp((Math.abs(rot) - 1.35) / 0.2));
  return { spread, born, rot, alpha };
};

/** Il taglio con la scena dopo cade qui: la tapparella è chiusa e non ha ancora cominciato a voltarsi. */
export const BLIND_CUT = 0.42;
