import test from "node:test";
import assert from "node:assert/strict";
import { cardOutAt, floatAt, gaugeHeroAt, optionsBuildAt, terminalPlaneAt } from "./heroes.ts";

test("la card parte dal display (a 0 combacia), esce morbida, resta fuori e si consegna alla scena dopo senza tornare", () => {
  const a = cardOutAt(0);
  assert.ok(a.travel === 0 && a.alpha === 1 && a.patch === 0 && a.exit === 0);
  const left = 1 - cardOutAt(0.42 * 0.85).travel;
  assert.ok(left > 0 && left < 0.03, `uscita: resta ${left}`);
  const mid = cardOutAt(0.6);
  assert.ok(mid.travel === 1 && mid.patch === 1 && mid.alpha === 1 && Math.abs(mid.swing) < 1e-9);
  const end = cardOutAt(0.999);
  assert.ok(end.travel === 1 && end.exit > 0.99 && end.alpha < 0.01, "non rientra: si ritira verso la scena dopo");
});

test("dopo il battito di ciglia la card è già fuori dal primo fotogramma", () => {
  const a = cardOutAt(0, true);
  assert.ok(a.travel === 1 && a.swing === 0 && a.alpha === 1);
  assert.equal(cardOutAt(-0.01, true).alpha, 0);
});

test("il gauge parte pieno, si svuota in volo, si disegna fuori col numero, poi si consegna", () => {
  assert.equal(gaugeHeroAt(0).value, 1);
  assert.ok(gaugeHeroAt(0.31).value === 0 && gaugeHeroAt(0.31).label === 0);
  const mid = gaugeHeroAt(0.5);
  assert.ok(mid.value > 0.5 && mid.value < 1 && mid.label === 1 && mid.travel === 1);
  assert.ok(gaugeHeroAt(0.7).value === 1 && gaugeHeroAt(0.9).label === 0 && gaugeHeroAt(0.95).exit > 0.5);
});

test("i tasti nascono, l'anello corre, il tasto si gonfia e poi diventa lo sfondo", () => {
  assert.equal(optionsBuildAt(0.2).build, 1);
  assert.ok(optionsBuildAt(0.52).ring > 0.4 && optionsBuildAt(0.63).ring === 1);
  assert.ok(optionsBuildAt(0.675).pop > 0.9 && optionsBuildAt(0.75).pop < 0.01);
  assert.ok(optionsBuildAt(0.86).fill === 0 && optionsBuildAt(0.999).fill > 0.99);
});

test("il terminale del PC compare dietro e si ritira alla fine", () => {
  assert.equal(terminalPlaneAt(-0.1).show, 0);
  assert.equal(terminalPlaneAt(0.3).show, 1);
  assert.ok(terminalPlaneAt(0.5).exit === 0 && terminalPlaneAt(0.99).exit > 0.9);
});

test("le card galleggianti salgono una dopo l'altra e le precedenti arretrano", () => {
  assert.equal(floatAt(0, 0, 3).rise, 0);
  assert.equal(floatAt(0.2, 0, 3).rise, 1);
  assert.equal(floatAt(0.2, 1, 3).rise, 0);
  assert.equal(floatAt(0.5, 1, 3).rise, 1);
  assert.ok(floatAt(0.5, 0, 3).depth >= 1 && floatAt(0.5, 2, 3).depth === 0);
});
