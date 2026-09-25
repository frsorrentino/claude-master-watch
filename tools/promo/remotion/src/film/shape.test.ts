import test from "node:test";
import assert from "node:assert/strict";
import { SWAP_IN, SWAP_OUT, blurSamples, contentAt, shapeAt, shapeSpeed, validateShape, inkOn } from "./shape.ts";
import type { Display, Rect, ShapeKey } from "./shape.ts";
import { beatToFrame, frameToBeat } from "./beats.ts";

const still: Display = () => [1000, 222, 636, 636];
const drifting: Display = (b) => [1000 + 3 * Math.sin(b), 222 + 2 * Math.cos(b * 1.7), 636, 636];
const keys: ShapeKey[] = [
  { at: 0, anchor: "display", r: 318, color: "#000000" },
  { at: 2, rect: [150, 300, 700, 160], r: 40, color: "#F4F2EC", content: "notify", gesture: "tap" },
  { at: 4, rect: [150, 300, 700, 420], r: 40, color: "#f4f2ec", content: "voice" },
  { at: 8, rect: [-100, -100, 2120, 1280], r: 0, color: "#FFFFFF", len: 2 },
  { at: 12, anchor: "display", r: 318, color: "#000000", ease: "settle" },
];
const near = (a: number, b: number, eps = 0.01) => assert.ok(Math.abs(a - b) < eps, `${a} ≠ ${b}`);

test("agganciata al display è IDENTICA al display anche mentre si muove", () => {
  for (let b = 0; b < 2; b += 0.05) {
    const s = shapeAt(keys, b, drifting), d = drifting(b);
    near(s.x, d[0]); near(s.y, d[1]); near(s.w, d[2]); near(s.h, d[3]); near(s.r, d[2] / 2);
  }
  const s = shapeAt(keys, 20, drifting), d = drifting(20);
  near(s.x, d[0]); near(s.w, d[2]);
});

test("fra due chiavi la forma va a molla e a riposo è l'ultima chiave", () => {
  const mid = shapeAt(keys, 2.5, still);
  assert.ok(mid.w > 636 * 0.5 && mid.h < 636, `a metà corsa ${mid.w}×${mid.h}`);
  const rest = shapeAt(keys, 3.99, still);
  near(rest.x, 150, 1); near(rest.h, 160, 1);
});

test("c'è sempre: misura positiva prima della prima chiave, dopo l'ultima e in ogni fotogramma", () => {
  const g = { bpm: 110, fps: 30, offsetSeconds: 0.01 };
  for (let f = -5; f <= beatToFrame(g, 16); f++) {
    const s = shapeAt(keys, frameToBeat(g, f), drifting);
    assert.ok(s.w >= 1 && s.h >= 1, `fotogramma ${f}: ${s.w}×${s.h}`);
  }
});

test("colori: esadecimale minuscolo valido, mai fuori da 0-255, anche fra nero e bianco", () => {
  for (let b = 0; b < 16; b += 0.03) assert.match(shapeAt(keys, b, still).color, /^#[0-9a-f]{6}$/);
  assert.equal(shapeAt(keys, 3.99, still).color, "#f4f2ec");
});

test("contenuto: il vecchio esce, poi entra il nuovo, mai due insieme", () => {
  assert.equal(contentAt(keys, 1), null);
  const out = contentAt(keys, 4 + SWAP_OUT / 2)!;
  assert.equal(out.id, "notify"); assert.ok(out.opacity < 1 && out.dy < 0 && out.blur > 0);
  assert.equal(contentAt(keys, 4 + SWAP_OUT + 0.001)?.id ?? "voice", "voice");
  const inn = contentAt(keys, 4 + SWAP_OUT + SWAP_IN / 2)!;
  assert.equal(inn.id, "voice"); assert.ok(inn.dy > 0 && inn.blur > 0);
  const hold = contentAt(keys, 6)!;
  assert.deepEqual([hold.id, hold.opacity, hold.blur, hold.dy], ["voice", 1, 0, 0]);
  assert.deepEqual(hold.key.rect, [150, 300, 700, 420] as Rect);
});

test("motion blur: 8 campioni solo sopra 24 px per fotogramma", () => {
  assert.equal(blurSamples(24), 4); assert.equal(blurSamples(24.1), 8);
  const bpf = 110 / 60 / 30;
  assert.equal(shapeSpeed(keys, 6, still, bpf), 0);                   // ferma
  assert.ok(shapeSpeed(keys, 8.3, still, bpf) > 24);                   // il campo che esplode
});

test("frameToBeat è l'inverso di beatToFrame", () => {
  const g = { bpm: 110, fps: 30, offsetSeconds: 0.01 };
  for (const b of [0, 0.5, 12, 73.5]) assert.ok(Math.abs(frameToBeat(g, beatToFrame(g, b)) - b) < 1 / 30 * 110 / 60);
});

test("validazione: una traccia buona passa, gli errori si elencano in italiano", () => {
  assert.deepEqual(validateShape(keys, 20), []);
  assert.deepEqual(validateShape(undefined, 20), []);
  const bad = validateShape([
    { at: 0.5, rect: [0, 0, 10, 10], anchor: "display", r: -1, color: "red" },
    { at: 0.5, rect: [0, 0, 0, 10], r: 0, color: "#000000", zoom: 2, gesture: "crown" },
    { at: 0.75, rect: [0, 0, 10, 10], r: 0, color: "#000000" },
  ], 20);
  for (const piece of ["prima chiave", "rect", "anchor", "colore", "raggio", "crescent", "zoom", "gesto", "mezzi battiti"])
    assert.ok(bad.some((m) => m.includes(piece)), `manca un errore su «${piece}»: ${bad.join(" | ")}`);
});
test("validazione: due scambi di contenuto a meno di mezzo battito sono testi sovrapposti", () => {
  const p = validateShape([
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "a" },
    { at: 0.5, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "b" },
    { at: 1, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "c" },
  ], 20);
  assert.deepEqual(p, []);   // 0,5 battiti bastano: con chiavi su mezzi battiti crescenti la sovrapposizione è impossibile,
  // ma la regola resta esplicita perché SWAP_OUT/SWAP_IN possono cambiare
  assert.ok(validateShape([
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "a" },
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000", content: "b" },
  ], 20).some((m) => m.includes("sovrappo")));
});

test("inchiostro: scuro sulle forme chiare, bianco sulle scure", () => {
  assert.equal(inkOn("#f4f2ec"), "#14203A");
  assert.equal(inkOn("#F4F2EC"), "#14203A");
  assert.equal(inkOn("#000000"), "#ffffff");
  assert.equal(inkOn("#ffffff"), "#14203A");
});
