import test from "node:test";
import assert from "node:assert/strict";
import { blinkAt } from "./blink.ts";

test("la parola arriva al suo posto prima che le palpebre si chiudano, e si riaprono dopo il taglio", () => {
  assert.deepEqual(blinkAt(0, 30), { travel: 0, lid: 0, word: 1 });
  assert.ok(blinkAt(22, 30).travel === 1, "ferma prima della chiusura");
  assert.equal(blinkAt(24, 30).lid > 0, true);
  assert.ok(Math.abs(blinkAt(30, 30).lid - 1) < 1e-9 && blinkAt(30, 30).word === 0, "al taglio buio");
  assert.ok(blinkAt(35, 30).lid < 0.6 && blinkAt(39, 30).lid === 0, "riaperte in nove fotogrammi");
});
