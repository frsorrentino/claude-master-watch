/**
 * La tapparella (revisione 3D, momento 1): le due barre del Context non si dissolvono, diventano i primi due listelli di una
 * tapparella che copre il quadro e si volta, scoprendo la scena dopo.
 *
 * Il collegamento fra il grafico e la tapparella si deve VEDERE (Franz, 18/09 22:27-22:28): perciò prima il quadro si
 * svuota — orologio, titolo e numeri se ne vanno in dissolvenza (`solo`) — e restano sole le due barre, che si allungano e
 * si ingrossano fino a essere listelli (`spread`); poi le due fanno un accenno di voltata (`wink`) dichiarando di essere
 * oggetti che gireranno; solo dopo nascono gli altri (`born`) e la tapparella si volta davvero (`rot`).
 * Tutto funzione dell'avanzamento 0-1 dell'effetto, testabile senza Three.
 */
const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ease = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);   // morbida ai due capi
const out = (t: number) => 1 - Math.pow(1 - t, 3);                                          // parte svelta, frena a lungo

export type Blind = { spread: number; born: number; rot: number; alpha: number; wink: number; settle: number };

/** Il ritardo del listello `i` su `n`, contato dai due che erano le barre (`bars`): chi è più lontano parte dopo. */
export const blindDelay = (i: number, n: number, bars: readonly [number, number]): number => {
  const d = Math.min(Math.abs(i - bars[0]), Math.abs(i - bars[1]));
  return d / Math.max(1, n - 1);
};

/** Quanto il resto del quadro ha lasciato il posto alle due barre: 0 tutto in scena, 1 restano solo loro. */
export const blindSoloAt = (p: number): number => ease(clamp(p / 0.16));

export const blindAt = (p: number, i: number, n: number, bars: readonly [number, number] = [6, 7]): Blind => {
  const isBar = i === bars[0] || i === bars[1];
  const del = blindDelay(i, n, bars);
  // le due barre restano sole in scena e diventano listelli: si allungano e si ingrossano mentre il resto sfuma
  const spread = isBar ? ease(clamp((p - 0.1) / 0.24)) : 1;
  // l'accenno di voltata delle sole barre, a tapparella ancora vuota: un quarto di secondo, avanti e indietro
  const wink = isBar ? Math.sin(Math.PI * clamp((p - 0.34) / 0.1)) : 0;
  // gli altri listelli nascono dopo l'accenno, a ventaglio dalle barre verso i capi
  const born = isBar ? 1 : ease(clamp((p - 0.4 - del * 0.22) / 0.13));
  // le due barre tengono la distanza che avevano sul quadro finché non nascono gli altri: due oggetti, non un blocco solo.
  // Solo allora scivolano al loro posto nella griglia (Franz, 22:37: si vedeva una fascia unica)
  const settle = ease(clamp((p - 0.4) / 0.16));
  // la voltata comincia a tapparella chiusa, sfalsata anch'essa dal centro ai capi
  const r = ease(clamp((p - 0.68 - del * 0.18) / 0.26));
  const rot = -r * (Math.PI / 2 + 0.22) - wink * 0.2;
  // il listello svanisce quando è quasi di taglio: l'uscita segue la voltata, non l'orologio
  const alpha = 1 - out(clamp((Math.abs(rot) - 1.35) / 0.2));
  return { spread, born, rot, alpha, wink, settle };
};

/** Il taglio con la scena dopo cade qui: la tapparella è chiusa e non ha ancora cominciato a voltarsi. */
export const BLIND_CUT = 0.66;
