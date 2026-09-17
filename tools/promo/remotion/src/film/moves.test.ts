import test from "node:test";
import assert from "node:assert/strict";
import { MAX_TILT, closingAt, poseAt } from "./moves.ts";
import type { Move } from "./moves.ts";

const MOVES: Move[] = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut", "settleSmall", "zoomLeft", "diveIn"];

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

test("settleSmall lascia l'orologio piccolo e in alto per tutto il resto della scena", () => {
  const p = poseAt(200, 252, 36, "settleSmall");
  assert.ok(p.scale < 0.6 && p.y < -0.15 && Math.abs(p.x) < 0.02);
});

test("zoomLeft ingrandisce verso la complication di sinistra e parte da fermo", () => {
  const a = poseAt(84, 120, 36, undefined, "zoomLeft"), b = poseAt(120, 120, 36, undefined, "zoomLeft");
  assert.ok(Math.abs(a.scale - 1) < 0.05 && b.scale > 2);
});

test("zoomLeft finisce centrato sul quadrante della complication: simmetrico, deriva compresa", () => {
  const p = poseAt(120, 120, 36, undefined, "zoomLeft");
  const cx = 0.69 * 1920, dx = (85 - 240) * 1.326, dy = (240.5 - 240) * 1.326;      // centro del quadrante misurato sul quadrante: (85, 240,5) su 480
  assert.ok(Math.abs(cx + p.x * 1920 + dx * p.scale - 960) < 2, "orizzontale");
  assert.ok(Math.abs(540 + p.y * 1080 + dy * p.scale - 540) < 2, "verticale");
  assert.ok(p.scale > 3.2);
});

test("chiusura: prima solo il logo grande, poi si inclina e compare l'orologio, poi si allontana fino all'inquadratura finale", () => {
  assert.equal(closingAt(0).logo, 0); assert.equal(closingAt(2).logo, 1);
  assert.equal(closingAt(3).tilt, 0); assert.equal(closingAt(3).body, 0); assert.equal(closingAt(5).tilt, 1); assert.equal(closingAt(5).body, 1);
  const a = closingAt(1), z = closingAt(9);
  assert.ok(a.focus === 1 && a.pose.scale > 1.5 && a.pose.y === 0);
  assert.ok(z.focus === 0 && Math.abs(z.pose.scale - 0.5) < 1e-9 && Math.abs(z.pose.y + 0.2) < 1e-9);
});

test("diveIn entra nello schermo fino a riempire il quadro, con il display al centro", () => {
  const p = poseAt(120, 120, 36, undefined, "diveIn");
  assert.ok(p.scale > 6 && Math.abs(0.69 * 1920 + p.x * 1920 - 960) < 2 && Math.abs(p.y) < 0.02);
});

test("chiusura speculare: il logo compare mentre ci si allontana da vicino, e il suo arco si disegna da zero", () => {
  assert.ok(closingAt(0).pose.scale > closingAt(2.5).pose.scale + 0.5);
  assert.ok(Math.abs(closingAt(2.5).pose.scale - 1.7) < 1e-9);
  assert.equal(closingAt(0.5).draw, 0); assert.equal(closingAt(3).draw, 1);
});
