import test from "node:test";
import assert from "node:assert/strict";
import { RAIL_MOVE, cardOutAt, gaugeHeroAt, optionsBuildAt, railAt, railScroll, railScrollVar, terminalPlaneAt } from "./heroes.ts";

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

test("la lista scorre di un passo alla volta e si sofferma su ogni scheda", () => {
  assert.equal(railScroll(0), 0);
  assert.ok(railScroll(0.2) > 0.2 && railScroll(0.42) === 1, "il passo si compie nel primo 42 % del tempo");
  assert.equal(railScroll(0.8), 1, "poi resta ferma sulla scheda");
  assert.equal(railScroll(1), 1);
  assert.ok(railScroll(2.42) === 3, "ogni passo uguale");
});

test("la corsia deforma come le liste di Wear OS: piccola sotto, larga al centro, piccola in cima", () => {
  assert.ok(Math.abs(railAt(0).scale - 0.62) < 1e-9 && Math.abs(railAt(0).alpha - 0.45) < 1e-9, "ai capi resta piccola e visibile");
  assert.ok(Math.abs(railAt(0.5).scale - 1) < 1e-9 && railAt(0.5).alpha === 1);
  assert.ok(Math.abs(railAt(1).scale - 0.62) < 1e-9 && railAt(1).alpha > 0.4, "non sparisce mai");
  assert.ok(railAt(0.25).scale > 0.85 && railAt(0.25).scale < 1);
});

test("la sosta è diversa per scheda, e cade quando la scheda è al centro della corsia", () => {
  const holds = [0.2, 1.2];
  const total = holds.reduce((a, h) => a + RAIL_MOVE + h, 0);
  assert.ok(Math.abs(railScrollVar(0, holds) - (-0.2)) < 1e-9, "la prima parte da sotto il centro");
  assert.ok(Math.abs(railScrollVar(RAIL_MOVE / total, holds) - 0.8) < 1e-9, "ferma con la prima al centro");
  assert.ok(Math.abs(railScrollVar((RAIL_MOVE + 0.2) / total, holds) - 0.8) < 1e-9, "resta ferma per tutta la sosta");
  assert.ok(Math.abs(railScrollVar(1, holds) - 1.8) < 1e-9);
  assert.ok(railScrollVar(0.95, holds) === railScrollVar(0.85, holds), "sosta lunga sulla seconda: non si muove");
});
