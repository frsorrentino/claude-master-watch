import test from "node:test";
import assert from "node:assert/strict";
import { FLIP_CUT, flipAt } from "./flip.ts";

test("la scheda parte ferma di faccia e finisce girata di mezzo giro, mostrando il retro", () => {
  const a = flipAt(0);
  assert.equal(a.yaw, 0, "all'inizio non ha ancora girato");
  assert.equal(a.back, false, "e si vede la scheda, non la domanda");
  assert.equal(a.alpha, 1);
  const z = flipAt(0.7);
  assert.ok(Math.abs(z.yaw - Math.PI) < 1e-9, "a 0,7 il mezzo giro è compiuto");
  assert.equal(z.back, true, "e si vede la domanda");
});

test("al taglio con la scena dopo la domanda è già in quadro, e la scheda si ritira solo alla fine", () => {
  const c = flipAt(FLIP_CUT);
  assert.equal(c.back, true, "al taglio si vede già il retro");
  assert.equal(c.alpha, 1, "e la scheda è ancora piena: se ne va dopo, quando l'orologio è arrivato");
  assert.equal(flipAt(1).alpha, 0);
});

test("lo stacco verso chi guarda accompagna la rotazione e torna a zero ai due capi", () => {
  assert.ok(flipAt(0).lift < 1e-9 && flipAt(0.58).lift < 1e-9, "ai capi della rotazione la scheda è a riposo");
  assert.ok(flipAt(0.32).lift > 0.9, "a metà giro è al massimo");
});
