/**
 * La dettatura sulla card (Franz, 21/09 18:55): i tre tasti che Wear OS propone per scrivere (emoji, microfono, tastiera), il
 * dito sul microfono, le parole che compaiono mentre la voce le dice, il dito su ✓; da lì cresce il takeover. Funzione
 * pura del tempo: `t`, `tap`, `voice` e `confirm` in secondi dall'inizio della scena, `beat` in secondi.
 */
import { soft } from "../moves.ts";

export type Word = { start: number; end: number; word: string };
export type Dictation = {
  chooser: number;    // 1 = i tre tasti in vista, 0 = se ne sono andati
  press: number;      // il dito sul microfono
  listen: number;     // 0-1: la card in ascolto (intestazione con il microfono, riga delle parole)
  words: number;      // quante parole si vedono
  speaking: number;   // l'indice della parola che la voce sta dicendo, −1 fra una parola e l'altra
  ok: number;         // 0-1: il tasto centrale è ✓ (c'è del testo) invece del microfono
  pressOk: number;    // il dito su ✓ (poi è la schermata stessa a crescere nel takeover: Franz, 21/09 19:33)
};

const clamp = (v: number) => Math.min(1, Math.max(0, v));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

export const dictationAt = (t: number, tap: number, voice: number, words: Word[], beat: number, confirm: number): Dictation => {
  const tv = t - voice;
  const first = words.length ? words[0].start : 0;
  return {
    // i tasti se ne vanno prima che arrivi la schermata di dettatura: sovrapposte si leggevano tutte e due (21/09 19:30)
    chooser: 1 - soft(ramp(t, tap + 0.12 * beat, tap + 0.3 * beat)),
    press: ramp(t, tap - 0.12 * beat, tap) * (1 - ramp(t, tap + 0.3 * beat, tap + 0.42 * beat)),
    listen: soft(ramp(t, tap + 0.28 * beat, tap + 0.5 * beat)),
    words: words.filter((w) => tv >= w.start).length,
    speaking: words.findIndex((w) => tv >= w.start && tv < w.end),
    // come sull'orologio: finché non c'è testo il tasto è il microfono, con la prima parola diventa ✓
    ok: ramp(tv, first, first + 0.1),
    pressOk: ramp(t, confirm - 0.12 * beat, confirm) * (1 - ramp(t, confirm + 0.3 * beat, confirm + 0.42 * beat)),
  };
};
