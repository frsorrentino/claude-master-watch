import test from "node:test";
import assert from "node:assert/strict";
import { LIDS_CLOSE, beforeLids, blinkAt } from "./blink.ts";

test("la parola arriva al suo posto prima che le palpebre si chiudano, e si riaprono dopo il taglio", () => {
  const a = blinkAt(0, 30);
  assert.ok(Math.abs(a.travel) < 1e-9, "la parola parte ferma");
  assert.ok(blinkAt(8, 49).travel > 0.3 && blinkAt(16, 49).travel === 1, "sale insieme alla card e arriva in 16 fotogrammi, prima di lei");
  assert.equal(a.lid, 0);
  assert.equal(a.word, 1);
  assert.ok(blinkAt(22, 30).travel === 1, "ferma prima della chiusura");
  assert.equal(blinkAt(26, 30).lid > 0, true);
  assert.ok(Math.abs(blinkAt(30, 30).lid - 1) < 1e-9 && blinkAt(30, 30).word === 0, "al taglio buio");
  assert.ok(blinkAt(35, 30).lid < 0.6 && blinkAt(37, 30).lid === 0, "riaperte in sette fotogrammi");
});

test("la parola resta piena per tutta la corsa: si ferma in alto al centro, non copre la scheda e non sfuma", () => {
  const cut = 78;
  for (const f of [0, 40, cut - 8]) assert.equal(blinkAt(f, cut).fade, 1, `a ${f} la parola si sta spegnendo`);
  assert.equal(blinkAt(cut - 10, cut).travel, 1, "e ha finito di muoversi prima che le palpebre si chiudano");
});
test("con le sole palpebre la frase è già uscita quando cominciano a chiudersi (revisione del 23/09)", () => {
  assert.equal(LIDS_CLOSE, 5);
  assert.equal(blinkAt(94, 100).lid, 0);                      // le palpebre partono 5 fotogrammi prima del taglio
  assert.ok(blinkAt(96, 100).lid > 0);
  assert.equal(beforeLids(92, 100), 100 - 5 - 8);             // le parole escono in 8 fotogrammi (WordMask) e finiscono lì
  assert.equal(beforeLids(60, 100), 60);                      // se se ne andavano già prima, resta com'era
});
