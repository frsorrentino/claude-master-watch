/**
 * La scheda che si volta (Franz, 19/09 09:33: l'ingrandimento verso «It asks» va ripensato). Non cresce: **ruota su sé
 * stessa** e sul retro c'è la domanda. Stesso oggetto, altra faccia — è la trasformazione più corta che si possa fare, e
 * dice quello che la scena dopo deve dire.
 *  - `yaw`   radianti: mezzo giro sull'asse verticale;
 *  - `lift`  0-1: la scheda si stacca appena verso chi guarda mentre gira, e torna (dà peso alla rotazione);
 *  - `back`  true quando si vede la faccia posteriore (la domanda);
 *  - `alpha` 0-1: la scheda lascia il posto alla scena dopo, quando l'orologio è arrivato.
 * Funzione pura dell'avanzamento 0-1, testabile senza React.
 */
import { bezier } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
/** La rotazione parte decisa e frena a lungo: è il movimento di un oggetto con un peso, non di una diapositiva. */
const turn = bezier(0.45, 0, 0.15, 1);

export type Flip = { yaw: number; lift: number; back: boolean; alpha: number };

/** Il taglio con la scena dopo cade qui: la scheda ha già girato ed è la domanda a essere in quadro. */
export const FLIP_CUT = 0.62;

export const flipAt = (p: number): Flip => {
  const yaw = Math.PI * turn(ramp(p, 0.06, 0.58));
  return {
    yaw,
    lift: Math.sin(Math.PI * ramp(p, 0.06, 0.58)),
    back: yaw > Math.PI / 2,
    alpha: 1 - ramp(p, 0.78, 1),
  };
};
