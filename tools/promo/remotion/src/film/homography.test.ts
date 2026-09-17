import test from "node:test";
import assert from "node:assert/strict";
import { applyH, homography, toMatrix3d } from "./homography.ts";
import type { Quad } from "./homography.ts";

const QUAD: Quad = [[441.4, 366.2], [1342.5, 298.9], [1309.4, 1453.1], [403.9, 1404.1]];

test("gli angoli del quadrato cadono sugli angoli misurati del display", () => {
  const h = homography(480, QUAD);
  ([[0, 0], [480, 0], [480, 480], [0, 480]] as const).forEach((p, i) => {
    const [x, y] = applyH(h, [p[0], p[1]]);
    assert.ok(Math.abs(x - QUAD[i][0]) < 1e-6 && Math.abs(y - QUAD[i][1]) < 1e-6);
  });
});

test("il centro del cerchio proiettato NON è il centro del quadrilatero: sta verso il lato lontano", () => {
  const [x] = applyH(homography(480, QUAD), [240, 240]);
  const media = (QUAD[0][0] + QUAD[1][0] + QUAD[2][0] + QUAD[3][0]) / 4;
  assert.ok(x < media - 5);
});

test("la matrice CSS è per colonne e ha sedici valori", () => {
  const m = toMatrix3d([1, 0, 10, 0, 1, 20, 0, 0, 1]);
  assert.equal(m, "matrix3d(1,0,0,0,0,1,0,0,0,0,1,0,10,20,0,1)");
});
