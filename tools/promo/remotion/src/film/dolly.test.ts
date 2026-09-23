import test from "node:test";
import assert from "node:assert/strict";
import { dollyAt } from "./dolly.ts";

test("senza dolly la camera non si muove", () => {
  for (const p of [0, 0.5, 1]) assert.equal(dollyAt(undefined, p), 1);
});
test("il dolly va da «from» a «to» nella scena, fermo prima e dopo", () => {
  const d = { from: 1, to: 1.08 };
  assert.equal(dollyAt(d, -0.2), 1);
  assert.equal(dollyAt(d, 0), 1);
  assert.equal(dollyAt(d, 1), 1.08);
  assert.equal(dollyAt(d, 1.3), 1.08);
  const m = dollyAt(d, 0.5); assert.ok(m > 1.03 && m < 1.05, `a metà ${m}`);
});
test("«out» parte veloce e frena: a metà ha già fatto più di metà strada", () => {
  const d = { from: 1.08, to: 1.12, ease: "out" as const };
  assert.ok(dollyAt(d, 0.5) > 1.1);
});
test("continuità: due scene che si passano la camera non fanno scatti al taglio", () => {
  const a = { from: 1, to: 1.08 }, b = { from: 1.08, to: 1.12, ease: "out" as const };
  assert.equal(dollyAt(a, 1), dollyAt(b, 0));
});
