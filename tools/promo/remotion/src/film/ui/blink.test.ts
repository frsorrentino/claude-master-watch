import test from "node:test";
import assert from "node:assert/strict";
import { blinkAt } from "./blink.ts";

test("la parola arriva al suo posto prima che le palpebre si chiudano, e si riaprono dopo il taglio", () => {
  const a = blinkAt(0, 30);
  assert.ok(Math.abs(a.travel) < 1e-9, "la parola parte ferma");
  assert.equal(a.lid, 0);
  assert.equal(a.word, 1);
  assert.ok(blinkAt(22, 30).travel === 1, "ferma prima della chiusura");
  assert.equal(blinkAt(24, 30).lid > 0, true);
  assert.ok(Math.abs(blinkAt(30, 30).lid - 1) < 1e-9 && blinkAt(30, 30).word === 0, "al taglio buio");
  assert.ok(blinkAt(35, 30).lid < 0.6 && blinkAt(39, 30).lid === 0, "riaperte in nove fotogrammi");
});

test("la parola resta piena per tutta la corsa: si ferma in alto al centro, non copre la scheda e non sfuma", () => {
  const cut = 78;
  for (const f of [0, 40, cut - 8]) assert.equal(blinkAt(f, cut).fade, 1, `a ${f} la parola si sta spegnendo`);
  assert.equal(blinkAt(cut - 10, cut).travel, 1, "e ha finito di muoversi prima che le palpebre si chiudano");
});
