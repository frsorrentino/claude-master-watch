import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { framesOf, gridOf } from "./cut.ts";
import { validateTimeline } from "./timeline.ts";

const film = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));

test("la griglia di una scaletta viene dalla scaletta stessa", () => {
  assert.deepEqual(gridOf(film), { bpm: 110, fps: 30, offsetSeconds: film.offsetSeconds });
});
test("il film lungo dura 2455 fotogrammi, come la resa consegnata il 23/09", () => {
  assert.equal(framesOf(film), 2455);
});
