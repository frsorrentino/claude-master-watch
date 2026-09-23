import test from "node:test";
import assert from "node:assert/strict";
import { stopGain, dbToGain, duckGain, sfxCues, sleepGain, napsOf, MUSIC_FALL, MUSIC_EARLY } from "./sound.ts";
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

test("il battito di ciglia ha il suo scatto, sul taglio con la scena dopo", () => {
  const b = validateTimeline({
    bpm: 100, fps: 30, offsetSeconds: 0,
    scenes: [
      { id: "a", at: 0, len: 8, act: "know", watch: { view: "front", clip: "scenes/x.mp4" }, text: { lines: ["One glance."], accent: "glance." }, out: "blink" },
      { id: "b", at: 8, len: 4, act: "know", watch: { view: "front", clip: "scenes/x.mp4" } },
    ],
  } as unknown);
  const s = sfxCues(b).find((c) => c.name === "shutter");
  assert.ok(s, "lo scatto manca");
  assert.equal(s!.beat, 8, "lo scatto cade sul taglio, non all'inizio della scena");
});

test("la musica si azzera sul battito e rientra dopo la notifica", () => {
  const cut = 300, back = 333, naps = [{ cut, frames: 131, back }];   // due battute a 110 bpm
  const at = (f: number) => sleepGain(f, naps);
  const hush = cut - Math.round(131 * (0.75 - 0.125)) - MUSIC_EARLY;  // il fotogramma in cui deve esserci silenzio
  assert.equal(at(hush - MUSIC_FALL), 1, "fino a tre fotogrammi prima la musica è piena");
  assert.ok(at(hush - 2) < 0.7 && at(hush - 2) > 0, "e cade in tre fotogrammi, non in otto");
  assert.equal(at(hush), 0, "un soffio prima del battito è già silenzio");
  assert.equal(at(cut), 0, "sul risveglio si sente solo la notifica");
  assert.equal(at(back - 1), 0, "tace fino al battito del rientro");
  assert.ok(at(back) > 0 && at(back + 1) === 1, "e rientra lì, in due fotogrammi");
  assert.equal(at(600), 1, "fuori dalla finestra non tocca niente");
});

test("il sonno del display zittisce la musica, salvo con «hush» false: nel corto la musica parte col video (Franz, 23/09 18:24)", () => {
  const g = { bpm: 110, fps: 30, offsetSeconds: 0.01 };
  const t = (hush?: boolean): any => ({ bpm: 110, fps: 30, offsetSeconds: 0.01, scenes: [
    { id: "wake", at: 0, len: 5, act: "know", watch: { view: "front", clip: "x.mp4" }, sleep: { len: 8, musicBackBeats: 2, ...(hush === undefined ? {} : { hush }) } },
    { id: "asks", at: 5, len: 4, act: "know", watch: { view: "front", clip: "x.mp4" } },
  ] });
  assert.equal(napsOf(t(), g).length, 1);
  assert.equal(napsOf(t(true), g).length, 1);
  assert.deepEqual(napsOf(t(false), g), []);
});
