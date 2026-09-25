import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { BLEED_LIFT, MAX_TILT, closingAt, displayUnit, poseAt, screenAt, watchPlace } from "./moves.ts";
import { THEME } from "./theme.ts";
import type { Move } from "./moves.ts";

const MOVES: Move[] = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut", "settleSmall", "zoomLeft", "diveIn"];

test("l'inclinazione resta entro i 10 gradi in ogni momento di ogni movimento", () => {
  for (const enter of MOVES) for (const exit of MOVES) for (let f = 0; f <= 120; f++) {
    assert.ok(Math.abs(poseAt(f, 120, 36, enter, exit).tilt) <= 10, `${enter}/${exit} @${f}`);
  }
  assert.ok(MAX_TILT <= 10);
});

test("finita l'entrata l'orologio è al centro, a grandezza piena, con la sola deriva lenta", () => {
  const p = poseAt(60, 120, 36, "riseIn", undefined);
  assert.ok(Math.abs(p.x) < 0.02 && Math.abs(p.y) < 0.02 && Math.abs(p.scale - 1) < 0.04);
});

test("riseIn parte da sotto il quadro", () => {
  assert.ok(poseAt(0, 120, 36, "riseIn").y > 0.8);
});

test("settleSmall lascia l'orologio piccolo e in alto per tutto il resto della scena", () => {
  const p = poseAt(200, 252, 36, "settleSmall");
  assert.ok(p.scale < 0.6 && p.y < -0.15 && Math.abs(p.x) < 0.02);
});

test("zoomLeft ingrandisce verso la complication di sinistra e parte da fermo", () => {
  const a = poseAt(84, 120, 36, undefined, "zoomLeft"), b = poseAt(120, 120, 36, undefined, "zoomLeft");
  assert.ok(Math.abs(a.scale - 1) < 0.05 && b.scale > 2);
});

test("zoomLeft finisce centrato sul quadrante della complication: simmetrico, deriva compresa", () => {
  const p = poseAt(120, 120, 36, undefined, "zoomLeft");
  const cx = 0.69 * 1920, dx = (85 - 240) * 1.326, dy = (240.5 - 240) * 1.326;      // centro del quadrante misurato sul quadrante: (85, 240,5) su 480
  assert.ok(Math.abs(cx + p.x * 1920 + dx * p.scale - 960) < 2, "orizzontale");
  assert.ok(Math.abs(540 + p.y * 1080 + dy * p.scale - 540) < 2, "verticale");
  assert.ok(p.scale > 3.2);
});

test("chiusura: prima solo il logo grande, poi si inclina e compare l'orologio, poi si allontana fino all'inquadratura finale", () => {
  assert.equal(closingAt(0).logo, 0); assert.equal(closingAt(2).logo, 1);
  assert.equal(closingAt(2.2).tilt, 0); assert.equal(closingAt(2.2).body, 0); assert.equal(closingAt(3.6).tilt, 1); assert.equal(closingAt(3.9).body, 1);
  // i tre movimenti si sovrappongono: a metà rotazione l'allontanamento è già cominciato (Franz, 21/09)
  const m = closingAt(3);
  assert.ok(m.tilt > 0 && m.tilt < 1 && m.body > 0 && m.body < 1 && m.pose.scale < closingAt(2.5).pose.scale);
  const a = closingAt(1), z = closingAt(9);
  assert.ok(a.focus === 1 && a.pose.scale > 1.5 && a.pose.y === 0);
  assert.ok(z.focus === 0 && Math.abs(z.pose.scale - 0.5) < 1e-9 && Math.abs(z.pose.y + 0.2) < 1e-9);
});

test("diveIn entra nello schermo fino a riempire il quadro, con il display al centro", () => {
  const p = poseAt(120, 120, 36, undefined, "diveIn");
  assert.ok(p.scale > 6 && Math.abs(0.69 * 1920 + p.x * 1920 - 960) < 2 && Math.abs(p.y) < 0.02);
});

test("chiusura speculare: il logo compare mentre ci si allontana da vicino, e il suo arco si disegna da zero", () => {
  assert.ok(closingAt(0).pose.scale > closingAt(2.5).pose.scale + 0.5);
  assert.ok(Math.abs(closingAt(2.5).pose.scale - 1.7) < 1e-9);
  assert.equal(closingAt(0.5).draw, 0); assert.equal(closingAt(3).draw, 1);
});

test("i movimenti finali atterrano morbidi: nell'ultimo quinto del tempo resta meno del 3 % della strada (con la molla anche un filo oltre: scavalco sotto l'1 %)", () => {
  const scaleAt = (b: number) => closingAt(b).pose.scale;
  const left = (scaleAt(4.34) - scaleAt(4.8)) / (scaleAt(2.5) - scaleAt(4.8));   // allontanamento: battiti 2,5-4,8
  assert.ok(left > -0.01 && left < 0.03, `allontanamento: resta ${left}`);
  const tiltLeft = 1 - closingAt(3.32).tilt;                                       // inclinazione: battiti 2,2-3,6
  assert.ok(tiltLeft > -0.01 && tiltLeft < 0.03, `inclinazione: resta ${tiltLeft}`);
});

test("con il cinturino che sborda l'orologio del cartello sale di più: il bordo alto della foto resta sempre fuori quadro", () => {
  const Q = JSON.parse(readFileSync(new URL("./mockup.geometry.json", import.meta.url), "utf8")).q34;
  const k = THEME.q34GlassPx / (2 * Q.b);
  const top = (p: { y: number; scale: number }) => 540 + p.y * 1080 - Q.cy * k * p.scale;   // bordo alto della foto, in pixel del quadro
  for (let b = 0; b <= 8; b += 0.25) assert.ok(top(closingAt(b, BLEED_LIFT).pose) < -40, `al battito ${b} il bordo è a ${top(closingAt(b, BLEED_LIFT).pose).toFixed(0)} px`);
  assert.equal(closingAt(8).pose.y, -0.2);   // senza, com'è nel film lungo
});
test("col logo ritagliato il display nero compare con la cassa, non prima: il logo parte da solo sul blu", () => {
  assert.equal(screenAt(closingAt(0), true), 0);
  assert.equal(screenAt(closingAt(8), true), 1);
  assert.equal(screenAt(closingAt(0), false), 1);
});
test("dove sta l'orologio: posa, tremito, spostamento verticale e zoom in un solo conto, per l'orologio e per il volo (piano 7)", () => {
  const pose = { x: 0.01, y: -0.02, scale: 1.1, tilt: 0 };
  const p = watchPlace(1324.8, pose, 1920, 1080, 3, -12, 1.05);
  assert.equal(p.x, 1324.8 + 0.01 * 1920 + 3);
  assert.equal(p.y, 1080 / 2 + -0.02 * 1080 + -12);
  assert.equal(p.scale, 1.1 * 1.05);
  // un'unità del display in pixel del quadro a quella scala: 480 unità = il diametro del display
  assert.equal(displayUnit(740, 300, 258, p.scale), ((740 / (2 * 300)) * 2 * 258 * p.scale) / 480);
});
