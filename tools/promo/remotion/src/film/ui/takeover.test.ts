import test from "node:test";
import assert from "node:assert/strict";
import { TAKEOVER_CUT, takeoverAt } from "./takeover.ts";

test("il takeover cresce, tiene un respiro, diventa la scena dopo e si posa: il quadro non resta mai vuoto", () => {
  assert.deepEqual(takeoverAt(0), { grow: 0, hold: 0, become: 0, settle: 0, cover: 0 });
  const g = takeoverAt(0.3); assert.ok(g.grow > 0.3 && g.grow < 1 && g.become === 0);
  const h = takeoverAt(0.44); assert.ok(h.grow === 1 && h.hold === 1 && h.become === 0 && h.cover === 1);
  const b = takeoverAt(0.52); assert.ok(b.become > 0.3 && b.become < 1, "a metà arco il campo sta già diventando la scena dopo");
  const s = takeoverAt(0.58); assert.ok(s.become === 1 && s.settle > 0.5 && s.cover < 0.5, "e si dissolve sopra la scena");
  const e = takeoverAt(1); assert.ok(e.settle === 1 && e.cover === 0);
  assert.ok(TAKEOVER_CUT >= 0.42 && TAKEOVER_CUT <= 0.5, "il taglio cade nel respiro, non dopo mezzo secondo di pieno");
  // il fermo a campo pieno non supera un quinto dell'arco: è lì che si vedeva il vuoto
  const held = [0.42, 0.44, 0.45].filter((p) => takeoverAt(p).hold === 1).length;
  assert.ok(held <= 3 && takeoverAt(0.5).hold === 0);
  // e quando il campo è ancora pieno la scena dopo ha già cominciato a vedersi: mai un colore piatto da solo
  assert.ok(takeoverAt(0.55).settle > 0.3 && takeoverAt(0.65).settle > 0.95, "il campo scopre la scena in fretta");
});
