import test from "node:test";
import assert from "node:assert/strict";
import { CAMERA_MARGIN, CAMERA_MAX, cameraAt, cameraTarget } from "./camera.ts";
import type { Rect, ShapeKey } from "./shape.ts";

const W = 1920, H = 1080;
const onScreen = (r: Rect, c: { cx: number; cy: number; s: number }) =>
  [(r[0] - c.cx) * c.s + W / 2, (r[1] - c.cy) * c.s + H / 2, r[2] * c.s, r[3] * c.s];

test("un rect piccolo si avvicina fino a 1,3 e resta dentro il margine", () => {
  // a destra quanto basta perché la camera si sposti; più in là (bordo destro oltre ~1743 px) margine e «mai fuori dalla
  // scena» non stanno insieme a nessuna scala, e vince la scena (test sotto)
  const r: Rect = [1400, 650, 300, 120];
  const c = cameraTarget(r);
  assert.equal(c.s, CAMERA_MAX);
  assert.ok(c.cx > W / 2, "la camera si sposta verso il rect");
  const [x, y, w, h] = onScreen(r, c);
  assert.ok(x >= W * CAMERA_MARGIN - 0.5 && x + w <= W * (1 - CAMERA_MARGIN) + 0.5, `x ${x}..${x + w}`);
  assert.ok(y >= H * CAMERA_MARGIN - 0.5 && y + h <= H * (1 - CAMERA_MARGIN) + 0.5, `y ${y}..${y + h}`);
});

test("un rect più grande del quadro: scala 1, camera ferma al centro, niente bordo della scena", () => {
  const c = cameraTarget([-100, -100, 2120, 1280]);
  assert.deepEqual(c, { cx: W / 2, cy: H / 2, s: 1 });
});

test("la camera non mostra mai fuori dalla scena, a nessuna scala", () => {
  for (const r of [[0, 0, 200, 200], [1720, 880, 200, 200], [900, 0, 100, 50]] as Rect[]) {
    const c = cameraTarget(r);
    assert.ok(c.cx - W / (2 * c.s) >= -1e-6 && c.cx + W / (2 * c.s) <= W + 1e-6);
    assert.ok(c.cy - H / (2 * c.s) >= -1e-6 && c.cy + H / (2 * c.s) <= H + 1e-6);
  }
});

test("zoom esplicito: vince sulla misura (1 = camera ferma sul display del quadrante)", () => {
  assert.deepEqual(cameraTarget([1004, 222, 636, 636], 1), { cx: W / 2, cy: H / 2, s: 1 });
});

test("cameraAt va a molla critica: scala mai oltre il bersaglio, continua", () => {
  const keys: ShapeKey[] = [
    { at: 0, rect: [1004, 222, 636, 636], r: 318, color: "#000000", zoom: 1 },
    { at: 2, rect: [1500, 700, 300, 120], r: 40, color: "#ffffff" },
  ];
  let prev = cameraAt(keys, 0, () => [0, 0, 1, 1]);
  for (let b = 0; b <= 4; b += 1 / 16) {
    const c = cameraAt(keys, b, () => [0, 0, 1, 1]);
    assert.ok(c.s <= CAMERA_MAX + 1e-9 && c.s >= 1 - 1e-9);
    assert.ok(Math.abs(c.s - prev.s) < 0.1 && Math.abs(c.cx - prev.cx) < 80);
    prev = c;
  }
});
