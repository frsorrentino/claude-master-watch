import test from "node:test";
import assert from "node:assert/strict";
import { cardOutAt } from "./heroes.ts";

test("la card esce morbida, resta fuori, gira lo stato a metà e rientra prima della fine", () => {
  assert.deepEqual(cardOutAt(0), { travel: 0, flip: 0, alpha: 0 });
  const mid = cardOutAt(0.5);
  assert.ok(mid.travel === 1 && mid.alpha === 1 && mid.flip > 0 && mid.flip < 1);
  assert.equal(cardOutAt(0.7).flip, 1);
  const end = cardOutAt(1);
  assert.ok(end.travel === 0 && end.alpha === 0);
  const left = 1 - cardOutAt(0.28).travel;                       // atterraggio morbido dell'uscita (0 → 0,35)
  assert.ok(left > 0 && left < 0.03, `resta ${left}`);
});
