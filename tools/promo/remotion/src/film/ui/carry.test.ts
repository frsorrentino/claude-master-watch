import test from "node:test";
import assert from "node:assert/strict";
import { carryAt, mixColor, radiusOf, strokeAt, strokePoints } from "./carry.ts";
import type { Key } from "./carry.ts";

const a: Key = { shape: "square", x: 100, y: 100, w: 30, h: 30, color: "#3C81F2", glyph: "check" };
const b: Key = { shape: "circle", x: 500, y: 300, w: 100, h: 100, color: "#23272E", glyph: "mic" };

test("il passaggio parte esatto dalla prima chiave e arriva esatto sulla seconda", () => {
  const s = carryAt(a, b, 0), e = carryAt(a, b, 1);
  assert.deepEqual([s.x, s.y, s.w, s.r], [100, 100, 30, radiusOf(a)]);
  assert.deepEqual([e.x, e.y, e.w, e.r], [500, 300, 100, 50]);
  assert.equal(s.color, "rgb(60,129,242)"); assert.equal(e.color, "rgb(35,39,46)");
});

test("i glifi si scambiano a metà, mai due a metà forza", () => {
  assert.ok(carryAt(a, b, 0.3).fromGlyph === 1 && carryAt(a, b, 0.3).toGlyph === 0);
  const m = carryAt(a, b, 0.5); assert.ok(Math.abs(m.fromGlyph + m.toGlyph - 1) < 1e-9);
  assert.ok(carryAt(a, b, 0.7).toGlyph === 1);
});

test("linea e arco hanno lo stesso numero di punti e l'interpolazione è per punti", () => {
  const line: Key = { shape: "line", x: 600, y: 700, w: 800, h: 0, color: "#D97757" };
  const arc: Key = { shape: "arc", x: 300, y: 300, w: 100, h: 100, color: "#8BB4F7" };
  assert.equal(strokePoints(line).length, strokePoints(arc).length);
  const mid = strokeAt(line, arc, 0.5);
  assert.equal(mid.length, 49);
  assert.ok(Math.abs(strokeAt(line, arc, 1)[0][0] - strokePoints(arc)[0][0]) < 1e-9);
});

test("mixColor a 0 e 1 è esattamente i due colori", () => {
  assert.equal(mixColor("#000000", "#ffffff", 0), "rgb(0,0,0)"); assert.equal(mixColor("#000000", "#ffffff", 1), "rgb(255,255,255)");
});
