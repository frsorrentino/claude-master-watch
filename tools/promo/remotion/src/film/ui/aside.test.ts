import test from "node:test";
import assert from "node:assert/strict";
import { asideAt, contextFill, contextTextAt } from "./aside.ts";
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
