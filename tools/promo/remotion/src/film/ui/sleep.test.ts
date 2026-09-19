import test from "node:test";
import assert from "node:assert/strict";
import { AMBIENT, HALO, SLEEP_CUT, TITLE_AT, sleepAt, sleepP } from "./sleep.ts";

const about = (v: number, w: number) => Math.abs(v - w) < 1e-9;

test("il display cala ad ambient e ci resta fino al taglio", () => {
  assert.equal(sleepAt(0).light, 1, "all'inizio il display è acceso");
  assert.ok(sleepAt(0.15).light < 0.6, "a un sesto di corsa sta già calando");
  assert.ok(about(sleepAt(0.3).light, AMBIENT), "a meno di un terzo è addormentato");
  assert.ok(about(sleepAt(SLEEP_CUT - 1 / 65).light, AMBIENT), "e sull'ultimo fotogramma prima del taglio lo è ancora");
});

test("la camera si sposta a display addormentato, e la si vede: intorno resta luce", () => {
  assert.equal(sleepAt(0.3).move, 0, "prima che il display sia addormentato la camera è ferma");
  assert.ok(about(sleepAt(0.44).light, AMBIENT), "a metà spostamento il display è ad ambient");
  assert.ok(sleepAt(0.44).move > 0.2 && sleepAt(0.44).move < 0.9, "e lì la camera è a metà strada");
  assert.ok(about(sleepAt(0.44).halo, HALO), "la luce intorno non si spegne: la cassa si vede muovere");
  assert.equal(sleepAt(0.56).move, 1, "lo spostamento finisce prima del taglio");
  assert.equal(sleepAt(SLEEP_CUT).move, 1);
});

test("la notifica riaccende il display con un soprassalto di luce e poi si assesta", () => {
  assert.ok(sleepAt(SLEEP_CUT).light > 1.2, "sul taglio il display è già acceso, e dà più luce del normale");
  assert.ok(sleepAt(0.72).light < sleepAt(SLEEP_CUT).light, "e subito rientra");
  assert.ok(about(sleepAt(0.78).light, 1), "poi è luce normale");
  assert.ok(about(sleepAt(1).light, 1));
  assert.ok(about(sleepAt(1).halo, 1), "e anche intorno la luce è tornata");
});

test("il taglio cade tra la scena che dorme e quella che si risveglia", () => {
  assert.equal(sleepP(60, 65, true, 60), SLEEP_CUT, "l'ultimo fotogramma della scena che dorme è il taglio");
  assert.equal(sleepP(0, 65, false, 0), SLEEP_CUT, "il primo della scena dopo è lo stesso punto");
  assert.ok(Math.abs(sleepP(60 - Math.round(SLEEP_CUT * 65), 65, true, 60)) < 0.02, "la finestra comincia SLEEP_CUT·len prima del taglio");
  assert.ok(about(sleepAt(sleepP(200, 65, false, 0)).light, 1), "molto dopo il risveglio il display è normale");
});

test("il titolo di prima lascia il posto a quello nuovo, che si scrive durante lo spostamento", () => {
  assert.equal(sleepAt(0.2).title, 1, "finché il display cala, il titolo di prima è lì");
  assert.equal(sleepAt(TITLE_AT).title, 0, "quando il titolo nuovo comincia a scriversi, quello di prima se n'è andato");
  assert.ok(sleepAt(TITLE_AT).move > 0, "e la camera si sta già spostando: la frase entra sul movimento");
  assert.ok(sleepAt(TITLE_AT).move < 1, "prima che il movimento finisca");
  assert.equal(sleepAt(SLEEP_CUT).title, 0, "dopo il risveglio il titolo di prima non torna");
});
