import test from "node:test";
import assert from "node:assert/strict";
import { asideAt, contextFill, contextTextAt, fadeOutAt, workBar, workCount } from "./aside.ts";
import { soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

test("senza tempi propri la scheda entra e si disegna come nel film lungo, identica", () => {
  for (const [frame, from, len] of [[10, 0, 100], [37, 12, 147], [120, 12, 147], [400, 300, 90]]) {
    const t = (frame - from) / len;
    const s = asideAt(frame, from, len);
    assert.equal(s.t, t);
    assert.equal(s.enter, soft(clamp(t / 0.12)));
    assert.equal(s.d, soft(clamp((t - 0.12) / 0.45)));
  }
});
test("con tempi propri entra e si disegna in quei fotogrammi (corto, 23/09: la scheda si deve leggere prima della tapparella)", () => {
  assert.equal(asideAt(100, 100, 98, 5, 13).enter, 0);
  assert.equal(asideAt(105, 100, 98, 5, 13).enter, 1);
  assert.equal(asideAt(105, 100, 98, 5, 13).d, 0);
  assert.equal(asideAt(118, 100, 98, 5, 13).d, 1);
});
test("le righe del Context si riempiono una dopo l'altra, il testo sfuma 14 fotogrammi prima della tapparella", () => {
  assert.equal(contextFill(1, 0), 1);
  assert.ok(contextFill(0.5, 2) < contextFill(0.5, 0));
  assert.equal(contextTextAt(50, 100), 1);
  assert.equal(contextTextAt(92, 100), 0.5);
  assert.equal(contextTextAt(98, 100), 0);
});
test("la scheda se ne va in 8 fotogrammi prima di «gone», con le espressioni di prima (il film lungo resta identico)", () => {
  for (const [frame, gone] of [[10, 100], [92, 100], [95, 100], [100, 100], [130, 100]]) {
    assert.equal(fadeOutAt(frame, gone), 1 - soft(clamp((frame - (gone - 8)) / 8)));
  }
  assert.equal(fadeOutAt(92, 100), 1);
  assert.equal(fadeOutAt(100, 100), 0);
});
test("la scheda Work: il numero sale a metà disegno, le tre barre partono una dopo l'altra", () => {
  for (const d of [0, 0.2, 0.49, 0.5, 0.8, 1]) {
    assert.equal(workCount(1, d), Math.round(1 * clamp(d * 2)));
    for (const k of [0, 1, 2]) assert.equal(workBar(d, k), clamp(d * 1.7 - k * 0.3));
  }
  assert.equal(workCount(3, 0.25), 2);
  assert.equal(workBar(1, 2), 1);
  assert.ok(workBar(0.5, 2) < workBar(0.5, 0));
});
