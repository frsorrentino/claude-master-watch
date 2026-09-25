import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { validateTimeline } from "./timeline.ts";
import { beatToFrame } from "./beats.ts";
import { gridOf } from "./cut.ts";
import { displayUnit } from "./moves.ts";
import { THEME } from "./theme.ts";
import { displayRectAt, watchStateAt } from "./displayPlace.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));
const GEO = JSON.parse(readFileSync(new URL("./mockup.geometry.json", import.meta.url), "utf8"));
const G = gridOf(short);
const sceneOf = (b: number) => short.scenes.find((s) => b < s.at + s.len)!;
const near = (a: number, b: number, eps = 1e-6) => assert.ok(Math.abs(a - b) < eps, `${a} ≠ ${b}`);

test("il rect del display sta dove la scena mette l'orologio, grande quanto il suo display", () => {
  for (const b of [0.5, 3, 50, 53, 64.5]) {
    const f = beatToFrame(G, b), sc = sceneOf(b);
    const st = watchStateAt(short, sc, f - beatToFrame(G, sc.at), 1920, 1080);
    const r = displayRectAt(short, f)!;
    assert.ok(st.place && st.pose && r, `battito ${b}: orologio in scena`);
    // la prospettiva di PhotoWatch (rotateY della posa) è l'identità solo a inclinazione 0: la deriva inclina fino a 0,9°,
    // e lì il display vero è un trapezio che sborda dal rect di meno di un pixel ai bordi. Il rect la ignora
    assert.ok(Math.abs(st.pose.tilt) <= 0.9 + 1e-9, `battito ${b}: posa inclinata di ${st.pose.tilt}°`);
    near(r[0] + r[2] / 2, st.place.x); near(r[1] + r[3] / 2, st.place.y);
    if (sc.watch!.view === "front") {
      near(r[2], 480 * displayUnit(THEME.frontGlassPx, GEO.front.glassR, GEO.front.displayR, st.place.scale));
    } else {
      // tre quarti col display piatto (all'inizio del cartello): il lato medio del quadrilatero della foto, alla scala del vetro
      const q: number[][] = GEO.q34.quad;
      const side = [[0, 1], [3, 2], [0, 3], [1, 2]].reduce((a, [i, j]) => a + Math.hypot(q[j][0] - q[i][0], q[j][1] - q[i][1]), 0) / 4;
      near(r[2], side * (THEME.q34GlassPx / (2 * GEO.q34.b)) * st.place.scale);
    }
    near(r[2], r[3]);
  }
});

test("in list il display deriva: si muove, ma di meno di 2 px per fotogramma", () => {
  const from = beatToFrame(G, 47.5), to = beatToFrame(G, 52) - 1;
  let prev = displayRectAt(short, from)!, moved = 0;
  for (let f = from + 1; f <= to; f++) {
    const r = displayRectAt(short, f)!;
    const d = Math.max(...r.map((v, i) => Math.abs(v - prev[i])));
    assert.ok(d < 2, `fotogramma ${f}: ${d.toFixed(2)} px`);
    moved = Math.max(moved, d);
    prev = r;
  }
  assert.ok(moved > 0.01, "la deriva è viva");
});

test("senza display frontale o di tre quarti non c'è rect: la vista laterale del loop, lo slogan senza orologio", () => {
  assert.equal(displayRectAt(short, beatToFrame(G, 25)), null);
  assert.equal(displayRectAt(short, beatToFrame(G, 60)), null);
});
