import test from "node:test";
import assert from "node:assert/strict";
import { beatToFrame, spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";

const g100: Grid = { bpm: 100, fps: 30, offsetSeconds: 0 };

test("a 100 battiti al minuto un battito vale 18 fotogrammi e 110 battiti fanno 66 secondi", () => {
  assert.equal(beatToFrame(g100, 1), 18);
  assert.equal(beatToFrame(g100, 0.5), 9);
  assert.equal(beatToFrame(g100, 110), 1980);
});

test("con un tempo non intero le scene in fila non accumulano deriva", () => {
  const g: Grid = { bpm: 97, fps: 30, offsetSeconds: 0.35 };
  let end = beatToFrame(g, 0);
  for (let b = 0; b < 110; b++) end += spanFrames(g, b, 1);
  assert.equal(end, beatToFrame(g, 110));
});
