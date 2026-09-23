import test from "node:test";
import assert from "node:assert/strict";
import { doneCardAt } from "./doneCard.ts";

const B = 16.36;   // un battito a 110 bpm, in fotogrammi
test("al primo fotogramma la card è ancora sotto il display e il ✓ non è disegnato", () => {
  assert.deepEqual(doneCardAt(0, B), { rise: 0, draw: 0 });
});
test("la card è salita entro un terzo di battito e il ✓ è disegnato entro mezzo battito dalla vibrazione", () => {
  assert.equal(doneCardAt(Math.ceil(B * 0.35), B).rise, 1);
  assert.equal(doneCardAt(Math.ceil(B * 0.5), B).draw, 1);
  const m = doneCardAt(Math.round(B * 0.3), B); assert.ok(m.draw > 0 && m.draw < 1, `a 0,3 battiti ${m.draw}`);
});
test("salita e tratto non tornano mai indietro", () => {
  for (let f = 0; f < 3 * B; f++) {
    const a = doneCardAt(f, B), b = doneCardAt(f + 1, B);
    assert.ok(b.rise >= a.rise && b.draw >= a.draw);
  }
});
test("«a riposo» la card è già al suo posto con il ✓ disegnato: la scena dopo la riprende così com'è", () => {
  assert.deepEqual(doneCardAt(0, B, true), { rise: 1, draw: 1 });
});
