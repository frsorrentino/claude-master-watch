import test from "node:test";
import assert from "node:assert/strict";
import { waveAmplitudeAt, wavePath } from "./wave.ts";

test("l'ampiezza segue l'inviluppo con un po' di memoria e resta tra 0 e 1", () => {
  const env = [0, 0.2, 1, 0.1, 0];
  assert.equal(waveAmplitudeAt(env, -3), 0);
  assert.equal(waveAmplitudeAt(env, 2), 1);
  assert.equal(waveAmplitudeAt(env, 3), 0.6, "il fotogramma dopo il picco ne conserva il 60 %");
  assert.ok(waveAmplitudeAt(env, 99) < 0.1, "oltre la fine resta quasi spenta");
  assert.equal(waveAmplitudeAt([], 5), 0);
});

test("il tracciato parte e finisce sulla linea di mezzo: ai capi l'onda è spenta", () => {
  const d = wavePath(800, 60, 40, 1.3);
  const pts = d.split(/[ML] /).filter(Boolean).map((p) => p.trim().split(" ").map(Number));
  assert.equal(pts.length, 49);
  assert.ok(Math.abs(pts[0][1] - 60) < 1e-6 && Math.abs(pts[48][1] - 60) < 1e-6);
  assert.ok(pts.some((p) => Math.abs(p[1] - 60) > 20), "in mezzo ondeggia");
});
