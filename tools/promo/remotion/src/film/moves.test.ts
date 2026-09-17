import test from "node:test";
import assert from "node:assert/strict";
import { MAX_TILT, poseAt } from "./moves.ts";
import type { Move } from "./moves.ts";

const MOVES: Move[] = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut"];

test("l'inclinazione resta entro i 10 gradi in ogni momento di ogni movimento", () => {
  for (const enter of MOVES) for (const exit of MOVES) for (let f = 0; f <= 120; f++) {
    assert.ok(Math.abs(poseAt(f, 120, 36, enter, exit).tilt) <= 10, `${enter}/${exit} @${f}`);
  }
  assert.ok(MAX_TILT <= 10);
});

test("finita l'entrata l'orologio è al centro, a grandezza piena, con la sola deriva lenta", () => {
  const p = poseAt(60, 120, 36, "riseIn", undefined);
  assert.ok(Math.abs(p.x) < 0.02 && Math.abs(p.y) < 0.02 && Math.abs(p.scale - 1) < 0.04);
});

test("riseIn parte da sotto il quadro", () => {
  assert.ok(poseAt(0, 120, 36, "riseIn").y > 0.8);
});
