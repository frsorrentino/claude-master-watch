import test from "node:test";
import assert from "node:assert/strict";
import { blinkAt } from "./blink.ts";

test("il battito: la parola cresce fino al taglio, le palpebre si chiudono prima e si riaprono dopo", () => {
  assert.deepEqual(blinkAt(0, 16), { scale: 1, lid: 0, word: 1 });
  assert.ok(blinkAt(11, 16).scale > 6 && blinkAt(11, 16).lid === 0, "a cinque fotogrammi dal taglio la parola è grande e le palpebre aperte");
  assert.ok(Math.abs(blinkAt(16, 16).lid - 1) < 1e-9 && blinkAt(16, 16).word === 0, "al taglio buio, parola sparita");
  assert.ok(blinkAt(20, 16).lid < 0.3 && blinkAt(24, 16).lid === 0, "riaperte in otto fotogrammi");
});
