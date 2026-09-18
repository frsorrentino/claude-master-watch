import test from "node:test";
import assert from "node:assert/strict";
import { stopGain, dbToGain, duckGain, sfxCues } from "./sound.ts";
import { validateTimeline } from "./timeline.ts";

const t = validateTimeline({
  bpm: 100, fps: 30, offsetSeconds: 0,
  scenes: [
    { id: "a", at: 0, len: 8, act: "know", watch: { view: "front", clip: "scenes/x.mp4" }, text: { lines: ["It asks."] },
      fx: [{ kind: "haptic", at: 0 }, { kind: "tap", at: 0.5, x: 1, y: 1 }, { kind: "tap", at: 4, x: 1, y: 1 }, { kind: "longPress", at: 5, len: 2 }] },
  ],
} as unknown);

test("al massimo un suono per battito, e vince il più importante", () => {
  const cues = sfxCues(t);
  const perBeat = new Map<number, number>();
  cues.forEach((c) => perBeat.set(Math.floor(c.beat), (perBeat.get(Math.floor(c.beat)) ?? 0) + 1));
  assert.ok([...perBeat.values()].every((n) => n === 1));
  assert.equal(cues.find((c) => Math.floor(c.beat) === 0)?.name, "notify");
  assert.deepEqual(cues.map((c) => c.name), ["notify", "tick", "pressRise"]);
});

test("la musica scende di 12 dB sotto la voce con rampe morbide e torna su", () => {
  const w: [number, number][] = [[100, 200]];
  assert.equal(duckGain(50, w, -12, 9), 1);
  assert.ok(Math.abs(duckGain(150, w, -12, 9) - dbToGain(-12)) < 1e-9);
  const mid = duckGain(100 - 4, w, -12, 9);
  assert.ok(mid < 1 && mid > dbToGain(-12));
  assert.equal(duckGain(260, w, -12, 9), 1);
});

test("stop and go: la musica tace di colpo sul battito e rientra in tre fotogrammi", () => {
  const w: [number, number][] = [[100, 160]];
  assert.equal(stopGain(99, w), 1);
  assert.equal(stopGain(100, w), 0.5);
  assert.equal(stopGain(101, w), 0);
  assert.equal(stopGain(130, w), 0);
  assert.ok(stopGain(160, w) > 0 && stopGain(161, w) < 1 && stopGain(163, w) === 1);
});
