import test from "node:test";
import assert from "node:assert/strict";
import { doneCardAt, doneCardBody, doneCardPlace } from "./doneCard.ts";
import { CARD_BODY, LIST_BODY } from "./UiTokens.ts";

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

test("nella lista la card sta nella sua riga, senza velo sul display (corto, 23/09 14:44)", () => {
  assert.deepEqual(doneCardPlace(0, 146), { y: 146, scrim: 0 });
  assert.deepEqual(doneCardPlace(1, 146), { y: 146, scrim: 0 });
});
test("senza riga la card sale da sotto e il display si scurisce, com'era", () => {
  assert.deepEqual(doneCardPlace(0), { y: 480, scrim: 0 });
  assert.deepEqual(doneCardPlace(1), { y: 134, scrim: 1 });
});
test("nella riga della lista la card scrive il testo come le righe vere, più piccolo e più stretto (revisione del 23/09)", () => {
  // misurato su n_list.mp4 a 11,6 s: linee di base a 66 e 108 unità dall'alto del badge, corpo di circa 34 unità
  assert.deepEqual(doneCardBody(146), LIST_BODY);
  assert.deepEqual([LIST_BODY.size, LIST_BODY.line], [34, 42]);
  assert.equal(doneCardBody(undefined), undefined);            // la card che sale sul display resta com'era
  assert.deepEqual(CARD_BODY, { size: 36, line: 46, shift: -9.5 });
});
