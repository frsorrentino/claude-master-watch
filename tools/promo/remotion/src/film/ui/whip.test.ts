import test from "node:test";
import assert from "node:assert/strict";
import { whipAt } from "./whip.ts";

test("la frustata parte da destra, arriva morbida a sinistra e la sua luce è piena a metà corsa", () => {
  assert.deepEqual(whipAt(0, 8), { x: 1, light: 0 });
  const mid = whipAt(4, 8);
  assert.ok(mid.x < 0.3 && Math.abs(mid.light - 1) < 1e-9, "a metà è già oltre due terzi della strada");
  const end = whipAt(8, 8);
  assert.ok(end.x === 0 && Math.abs(end.light) < 1e-9);
  assert.ok(1 - whipAt(6, 8).x > 0.97, "atterraggio morbido: negli ultimi due fotogrammi resta meno del 3 %");
});
