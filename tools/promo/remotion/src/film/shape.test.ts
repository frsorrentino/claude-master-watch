import test from "node:test";
import assert from "node:assert/strict";
import { SHAPE_SHUTTER, SWAP_IN, SWAP_OUT, arcOf, blurSamples, boxStyle, contentAt, keyRect, shapeAt, shapeSpeed, shapeVisible, shutterCentre, toFrameRect, validateShape, inkOn } from "./shape.ts";
import type { Display, Rect, ShapeKey, ShapeState } from "./shape.ts";
import { beatToFrame, frameToBeat } from "./beats.ts";
import { springSettle, springs } from "./spring.ts";

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
    { at: 0.5, rect: [0, 0, 10, 10], anchor: "watch", r: -1, color: "red" },
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

// Revisione del 25/09: molle che si sovrappongono con durate diverse escono dall'inviluppo dei bersagli.
const field: Rect = [-100, -100, 2120, 1280];
test("aggancio sovrapposto: il peso resta fra 0 e 1, e dopo la chiave ancorata la forma È il display", () => {
  const a: ShapeKey[] = [
    { at: 0, anchor: "display", r: 318, color: "#000000" },
    { at: 8, rect: field, r: 0, color: "#000000", len: 2 },
    { at: 8.5, anchor: "display", r: 318, color: "#000000", len: 0.5 },
  ];
  for (let b = 0; b < 14; b += 0.01) {
    const s = shapeAt(a, b, drifting);
    assert.ok(s.anchor >= 0 && s.anchor <= 1, `battito ${b}: peso ${s.anchor}`);
    if (b >= 9) { const d = drifting(b); near(s.x, d[0]); near(s.y, d[1]); near(s.w, d[2]); near(s.h, d[3]); near(s.r, d[2] / 2); }
  }
  const b: ShapeKey[] = [
    { at: 0, rect: field, r: 0, color: "#000000" },
    { at: 2, anchor: "display", r: 318, color: "#000000", len: 4 },
    { at: 2.5, rect: field, r: 0, color: "#000000", len: 0.5 },
  ];
  for (let t = 0; t < 8; t += 0.01) {
    const s = shapeAt(b, t, still);
    assert.ok(s.anchor >= 0 && s.anchor <= 1 && s.r >= 0, `battito ${t}: peso ${s.anchor}, raggio ${s.r}`);
  }
});

test("mai negativa: da un campo grande a una linea sottile la molla non porta misure e raggio sotto zero", () => {
  const k: ShapeKey[] = [
    { at: 0, rect: field, r: 40, color: "#000000" },
    { at: 2, rect: [360, 700, 1200, 6], r: 0, color: "#000000" },
  ];
  const g = { bpm: 110, fps: 30, offsetSeconds: 0.01 };
  for (let f = 0; f <= beatToFrame(g, 6); f++) {
    const s = shapeAt(k, frameToBeat(g, f), still);
    assert.ok(s.w >= 1 && s.h >= 1 && s.r >= 0 && s.r <= Math.min(s.w, s.h) / 2 + 1e-9, `fotogramma ${f}: ${s.w}×${s.h} r ${s.r}`);
  }
});

test("blur centrato: i campioni della scatola cadono in media sul fotogramma, non davanti", () => {
  // CameraMotionBlur (4.0.490) disegna il campione i (1..n) congelato al fotogramma f − frazione·i/n + 1: la scatola
  // si valuta a quel fotogramma meno shutterCentre(n), e la media deve tornare f (se no la scatola anticipa il contenuto)
  const sf = SHAPE_SHUTTER / 360, f = 100;
  for (const n of [4, 8]) {
    const at = Array.from({ length: n }, (_, i) => f - sf * ((i + 1) / n) + 1 - shutterCentre(n));
    near(at.reduce((a, b) => a + b, 0) / n, f, 1e-9);
    assert.ok(Math.min(...at) >= f - sf / 2 - 1e-9 && Math.max(...at) <= f + sf / 2 + 1e-9, `${n}: ${at}`);
  }
});

test("validazione: una chiave nulla o con numeri scritti come testo è un errore, non un'eccezione", () => {
  const ok = { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000" };
  assert.ok(validateShape([null], 20).some((m) => m.includes("chiave")));
  assert.ok(validateShape([ok, null, { ...ok, at: 1 }], 20).length > 0);
  assert.ok(validateShape([{ ...ok, zoom: "1.2" }], 20).some((m) => m.includes("zoom")));
  assert.ok(validateShape([{ ...ok, r: Infinity }], 20).some((m) => m.includes("raggio")));
  assert.ok(validateShape([{ ...ok, len: Infinity }], 20).some((m) => m.includes("durata")));
});

test("validazione: una molla che finisce prima di quella della chiave prima fa uscire la forma dai bersagli", () => {
  const field: Rect = [-100, -100, 2120, 1280];
  const p = validateShape([
    { at: 0, rect: field, r: 0, color: "#000000" },
    { at: 2, anchor: "display", r: 0, color: "#000000", len: 4 },
    { at: 2.5, rect: field, r: 0, color: "#000000", len: 0.5 },
  ], 20);
  assert.ok(p.some((m) => m.includes("prima di quella della chiave prima")), p.join(" | "));
  assert.deepEqual(validateShape([
    { at: 0, rect: field, r: 0, color: "#000000" },
    { at: 2, rect: [0, 0, 10, 10], r: 0, color: "#000000", len: 1 },
    { at: 2.5, rect: field, r: 0, color: "#000000", len: 0.5 },
  ], 20), []);   // finiscono insieme: va bene
});

// Piano 2, Task 2: una chiave agganciata può avere il suo rect in unità del display (0-480), e fra due chiavi agganciate
// la molla corre in quelle unità: la forma segue il display anche durante la corsa, non una sua fotografia.
const breathing: Display = (b) => [1000 + 3 * Math.sin(b), 222 + 2 * Math.cos(b * 1.7), 636 + 4 * Math.sin(b / 2), 636 + 4 * Math.sin(b / 2)];
const card: ShapeKey[] = [
  { at: 0, anchor: "display", rect: [26, 146, 428, 120], r: 42, color: "#2a2f3a" },
  { at: 2, anchor: "display", rect: [40, 60, 400, 360], r: 20, color: "#2a2f3a", len: 2 },
  { at: 6, rect: [110, 330, 840, 420], r: 28, color: "#2a2f3a" },
];

test("agganciata con un rect in unità del display: la molla corre nel display, che intanto deriva e cambia misura", () => {
  for (let b = 0; b <= 4.5; b += 0.02) {
    const s = shapeAt(card, b, breathing), d = breathing(b);
    const unit = [0, 1, 2, 3].map((i) => springs(b, card[0].rect![i], [{ at: 2, to: card[1].rect![i], len: 2 }])) as Rect;
    const want = toFrameRect(unit, d);
    near(s.x, want[0]); near(s.y, want[1]); near(s.w, want[2]); near(s.h, want[3]);
    near(s.r, springs(b, 42, [{ at: 2, to: 20, len: 2 }]) * (d[2] / 480));
    assert.equal(s.anchor, 1);
  }
  assert.deepEqual(keyRect(card[1], breathing), toFrameRect([40, 60, 400, 360], breathing(2)));
});

test("una chiave libera dopo le agganciate riporta il peso a 0 con la molla critica", () => {
  let prev = 1;
  for (let b = 6; b <= 7.5; b += 0.02) {
    const a = shapeAt(card, b, breathing).anchor;
    near(a, 1 - springSettle(b - 6), 1e-9);
    assert.ok(a <= prev + 1e-12, `battito ${b}: il peso risale`);
    prev = a;
  }
  assert.equal(shapeAt(card, 7, breathing).anchor, 0);
});

test("validazione: rect e aggancio insieme vanno bene, senza nessuno dei due no", () => {
  assert.deepEqual(validateShape(card, 20), []);
  assert.ok(validateShape([{ at: 0, r: 0, color: "#000000" }], 20).some((m) => m.includes("rect") && m.includes("anchor")));
});

// Piano 2, Task 3: il tratto. Con `bend` > 0 il rect è una linea che si piega in un arco (fino ai 280° del logo), e la
// parte `split` dal capo sinistro è nel colore, il resto in `track`.
test("tratto: appena piegato combacia con la scatola dritta (niente scatto a bend 0)", () => {
  const box: ShapeState = { x: 575, y: 655, w: 770, h: 8, r: 4, color: "#d97757", anchor: 0, bend: 1.0001e-3, split: 1, track: "#3a404c", show: 1 };
  const a = arcOf(box)!;
  const end = (deg: number) => [a.cx + a.R * Math.cos((deg * Math.PI) / 180), a.cy + a.R * Math.sin((deg * Math.PI) / 180)];
  const [l, r] = [end(a.start), end(a.start + a.sweep)];
  // i capi della linea mediana, che gli estremi arrotondati allungano di mezzo spessore: come la scatola col raggio h/2
  for (const [p, want] of [[l, [box.x + box.h / 2, box.y + box.h / 2]], [r, [box.x + box.w - box.h / 2, box.y + box.h / 2]]])
    assert.ok(Math.hypot(p[0] - want[0], p[1] - want[1]) < 0.5, `capo ${p} contro ${want}`);
  assert.equal(a.stroke, box.h);
  assert.equal(arcOf({ ...box, bend: 0 }), null);
});

test("tratto: a bend 1 è l'arco del logo (LogoMark) portato nel quadro dal display", () => {
  const r = (40.2 * 300) / 92, aw = (5.6 * 300) / 92, L = (r * 280 * Math.PI) / 180;
  const logo: ShapeKey[] = [{ at: 0, anchor: "display", rect: [240 - L / 2, 240 - r - aw / 2, L, aw], r: 0, color: "#d97757", bend: 1, split: 0.7 }];
  for (const b of [0, 1.3, 7]) {
    const d = breathing(b), u = d[2] / 480;
    const s = shapeAt(logo, b, breathing), a = arcOf(s)!;
    const eq = (x: number, y: number) => assert.ok(Math.abs(x - y) < 1e-6, `${x} ≠ ${y}`);
    eq(a.cx, d[0] + 240 * u); eq(a.cy, d[1] + 240 * u); eq(a.R, r * u); eq(a.start, 130); eq(a.sweep, 280); eq(a.stroke, aw * u);
    assert.deepEqual([s.split, s.track], [0.7, "#3a404c"]);
  }
});

test("tratto: la piega va a molla critica e non supera mai 1 (oltre 280° si chiuderebbe il varco del logo)", () => {
  const k: ShapeKey[] = [
    { at: 0, rect: [575, 655, 770, 8], r: 4, color: "#d97757" },
    { at: 1, rect: [800, 300, 500, 20], r: 10, color: "#d97757", bend: 1, len: 0.5 },
    { at: 2, rect: [800, 300, 500, 20], r: 10, color: "#d97757", bend: 1, split: 0.7, track: "#1B2B4F" },
  ];
  let prev = 0;
  for (let b = 0; b <= 4; b += 0.01) {
    const s = shapeAt(k, b, still);
    assert.ok(s.bend >= prev - 1e-12 && s.bend <= 1 && s.split >= 0 && s.split <= 1, `battito ${b}: bend ${s.bend}, split ${s.split}`);
    assert.match(s.track, /^#[0-9a-f]{6}$/);
    prev = s.bend;
  }
  assert.deepEqual([shapeAt(k, 4, still).split, shapeAt(k, 4, still).track], [0.7, "#1b2b4f"]);
});

test("validazione: piega e divisione fra 0 e 1, colore del tratto esadecimale", () => {
  const ok = { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000" };
  assert.deepEqual(validateShape([{ ...ok, bend: 1, split: 0.7, track: "#1B2B4F" }], 20), []);
  const bad = validateShape([{ ...ok, bend: 1.2, split: -0.1, track: "grey" }], 20);
  for (const piece of ["bend", "split", "tratto"]) assert.ok(bad.some((m) => m.includes(piece)), `manca «${piece}»: ${bad.join(" | ")}`);
});

test("passaggio al logo: dall'arrivo della chiave `handoff` la forma non si disegna più (l'arco lo fa la scena)", () => {
  const k: ShapeKey[] = [
    { at: 0, rect: [0, 0, 10, 10], r: 0, color: "#000000" },
    { at: 4, anchor: "display", r: 0, color: "#d97757", len: 1.5, handoff: true },
  ];
  assert.equal(shapeVisible(k, 5.49), true);
  assert.equal(shapeVisible(k, 5.5), false);
  assert.equal(shapeVisible(k.slice(0, 1), 1e6), true);
  assert.deepEqual(validateShape(k, 20), []);
  assert.ok(validateShape([{ ...k[0], handoff: true }, k[1]], 20).some((m) => m.includes("handoff") && m.includes("ultima")));
  assert.ok(validateShape([k[0], { ...k[1], handoff: "yes" }], 20).some((m) => m.includes("handoff")));
});

// Piano 3, Task 1: `show`. La forma c'è (misura, posto, camera) anche quando non copre: a 0 il display vero, o il fondo
// della scena, È la forma.
test("show: molla critica fra 0 e 1, di norma 1; a 0 la scatola non copre", () => {
  const k: ShapeKey[] = [
    { at: 0, anchor: "display", r: 0, color: "#000000", show: 0 },
    { at: 2, rect: [150, 690, 720, 170], r: 40, color: "#f4f2ec", len: 1.5 },
    { at: 6, rect: [150, 690, 720, 170], r: 40, color: "#f4f2ec", show: 0, len: 0.5 },
  ];
  let prev = 0;
  for (let b = 0; b <= 6; b += 0.01) {
    const s = shapeAt(k, b, still);
    assert.ok(s.show >= 0 && s.show <= 1 && s.show >= prev - 1e-12, `battito ${b}: show ${s.show}`);
    prev = s.show;
  }
  assert.equal(shapeAt(k, 3.5, still).show, 1);
  assert.equal(shapeAt(k, 7, still).show, 0);
  assert.equal(boxStyle(shapeAt(k, 1, still)).opacity, 0);
  assert.equal(boxStyle(shapeAt(k, 4, still)).opacity, 1);
  assert.equal(shapeAt(keys, 4, still).show, 1);
  assert.ok(validateShape([{ ...k[0], show: 1.5 }], 20).some((m) => m.includes("show")));
});

import { RIPPLE_BEATS, ambientOf, rippleAt, shadowOf } from "./shape.ts";
test("effetti: ombra zero da agganciata o invisibile, piena da libera; onda solo sui gesti e per RIPPLE_BEATS", () => {
  const base = shapeAt(keys, 0.5, still);
  assert.equal(shadowOf(base).alpha, 0);                            // agganciata al display
  const free = shapeAt(keys, 3.9, still);
  assert.ok(shadowOf(free).alpha > 0.4 && shadowOf(free).y > 20);
  assert.equal(shadowOf({ ...free, show: 0 }).alpha, 0);
  assert.equal(rippleAt(keys, 1), null);
  const r = rippleAt(keys, 2 + RIPPLE_BEATS / 2)!;
  assert.equal(r.key.at, 2); assert.ok(Math.abs(r.p - 0.5) < 1e-9);
  assert.equal(rippleAt(keys, 2 + RIPPLE_BEATS + 0.01), null);
  assert.equal(rippleAt(keys, 5), null);                             // la chiave 4 non ha gesto
  const a = ambientOf(free);
  assert.ok(a.alpha > 0 && a.x > 0 && a.x < 1 && a.color === free.color);
});
