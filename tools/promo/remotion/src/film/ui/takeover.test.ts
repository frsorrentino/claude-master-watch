import test from "node:test";
import assert from "node:assert/strict";
import { TAKEOVER_CUT, takeoverAt } from "./takeover.ts";

test("il takeover cresce, tiene, diventa, si posa: le fasi non si sovrappongono e il quadro è coperto tra grow e settle", () => {
  assert.deepEqual(takeoverAt(0), { grow: 0, hold: 0, become: 0, settle: 0, cover: 0 });
  const g = takeoverAt(0.3); assert.ok(g.grow > 0.3 && g.grow < 1 && g.become === 0);
  const h = takeoverAt(0.5); assert.ok(h.grow === 1 && h.hold === 1 && h.become === 0 && h.cover === 1);
  const b = takeoverAt(0.7); assert.ok(b.become > 0.3 && b.become < 1 && b.settle === 0 && b.cover === 1);
  const s = takeoverAt(0.95); assert.ok(s.become === 1 && s.settle > 0.5 && s.cover < 0.5);
  const e = takeoverAt(1); assert.ok(e.settle === 1 && e.cover === 0);
  assert.ok(TAKEOVER_CUT > 0.45 && TAKEOVER_CUT < 0.85, "il taglio cade dentro il fermo o il become");
});
