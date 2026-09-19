/**
 * La luce della notifica che diventa la scena (Franz, 19/09 16:53). La domanda arriva: il display si accende, l'alone
 * ambra esce dalla cassa e riempie il quadro; mentre copre, l'inquadratura si rifà (l'orologio passa dal centro alla
 * colonna di destra, il titolo entra a sinistra); poi l'alone si ritira e la scena nuova è già a posto.
 * Non esce nessun oggetto dal display — è luce, e non contraddice l'orologio appena materializzato.
 *  - `r`     0-1: quanto è cresciuto l'alone, da un cerchio sul display a tutto il quadro;
 *  - `cover` 0-1: quanto il quadro è coperto (tiene un istante al culmine);
 *  - `hide`  0-1: quanto la scena sotto è nascosta: è la finestra in cui si può spostare tutto senza che si veda.
 * Funzione pura dell'avanzamento 0-1.
 */
import { bezier } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));
const open = bezier(0.3, 0, 0.1, 1);      // la luce si apre svelta e frena
const close = bezier(0.5, 0, 0.3, 1);     // e si ritira più morbida

export type Glow = { r: number; cover: number; hide: number };

/** Il taglio con la scena dopo cade a quadro coperto: lì l'orologio cambia posto senza che si veda. */
export const GLOW_CUT = 0.5;

export const glowAt = (p: number): Glow => {
  const r = open(ramp(p, 0, 0.42)) * (1 - 0.85 * close(ramp(p, 0.58, 1)));
  const cover = clamp(ramp(p, 0.3, 0.44) - ramp(p, 0.56, 0.78));
  return { r, cover, hide: clamp(ramp(p, 0.34, 0.45) - ramp(p, 0.55, 0.7)) };
};
