import test from "node:test";
import assert from "node:assert/strict";
import { blinkAt } from "./blink.ts";

test("la parola arriva al suo posto prima che le palpebre si chiudano, e si riaprono dopo il taglio", () => {
  const a = blinkAt(0, 30);
  assert.equal(a.travel, 0);
  assert.equal(a.lid, 0);
  assert.equal(a.word, 1);
  assert.ok(blinkAt(22, 30).travel === 1, "ferma prima della chiusura");
  assert.equal(blinkAt(24, 30).lid > 0, true);
  assert.ok(Math.abs(blinkAt(30, 30).lid - 1) < 1e-9 && blinkAt(30, 30).word === 0, "al taglio buio");
  assert.ok(blinkAt(35, 30).lid < 0.6 && blinkAt(39, 30).lid === 0, "riaperte in nove fotogrammi");
});

test("la parola che cresce si spegne prima della chiusura, così non copre la scheda al centro", () => {
  const cut = 78;
  assert.equal(blinkAt(0, cut).fade, 1, "all'inizio la parola è piena");
  assert.ok(blinkAt(50, cut).fade > 0 && blinkAt(50, cut).fade < 1, "a metà corsa si sta spegnendo");
  assert.equal(blinkAt(cut - 7, cut).fade, 0, "quando le palpebre partono la parola non c'è più");
});
