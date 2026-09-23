import test from "node:test";
import assert from "node:assert/strict";
import { screenFadeAt, slotRect, takeInAt, takeInRect, toDisplay } from "./takeIn.ts";
import { LIST_BODY } from "./UiTokens.ts";

const F = { w: 1920, h: 1080 };
test("la card della riga 146 nel quadro: stesso conto di toFrame, alta quanto la card con due righe", () => {
  const r = slotRect(146, 1325, 540, 1.344, 2);
  assert.equal(r.x, 1325 + (26 - 240) * 1.344);
  assert.equal(r.y, 540 + (146 - 240) * 1.344);
  assert.equal(r.w, 428 * 1.344);
  assert.equal(r.h, (24 + 36 + LIST_BODY.shift + 2 * LIST_BODY.line + 24.5) * 1.344);
  assert.equal(r.r, 42 * 1.344);
});
test("il volo parte dal quadro intero e arriva esattamente sulla card", () => {
  const to = slotRect(146, 1325, 540, 1.344, 2);
  assert.deepEqual(takeInRect(to, 0, F), { x: 0, y: 0, w: 1920, h: 1080, r: 0 });
  assert.deepEqual(takeInRect(to, 1, F), to);
});
test("in volo la card resta sempre dentro la finestra: la riga vera sotto non si vede mai", () => {
  const to = slotRect(146, 1325, 540, 1.344, 2);
  for (let i = 0; i <= 40; i++) {
    const r = takeInRect(to, i / 40, F);
    assert.ok(r.x <= to.x + 1e-9 && r.y <= to.y + 1e-9 && r.x + r.w >= to.x + to.w - 1e-9 && r.y + r.h >= to.y + to.h - 1e-9, `p ${i / 40}`);
  }
});
test("il terminale se ne va mentre la card arriva, senza un attimo di finestra vuota (fotogramma 843 della prima prova)", () => {
  assert.equal(takeInAt(0).terminal, 1);
  assert.equal(takeInAt(0.5).terminal, 0);
  assert.equal(takeInAt(0.2).card, 0);
  assert.equal(takeInAt(0.6).card, 1);
  for (let i = 0; i <= 20; i++) { const k = takeInAt(i / 20); assert.ok(k.terminal + k.card > 0.2 || i / 20 < 0.05, `p ${i / 20}: finestra vuota`); }
  assert.equal(takeInAt(0).line, 0);
  assert.equal(takeInAt(1).line, 1);
  assert.equal(takeInAt(1).shrink, 1);
});
test("la riga dell'esito si scioglie nel testo della card fra 0,6 e 0,85: mai due testi pieni insieme", () => {
  for (const p of [0, 0.5, 0.6, 0.7, 0.8, 0.85, 1]) assert.ok(Math.abs(takeInAt(p).body + takeInAt(p).lineAlpha - 1) < 1e-12, `p ${p}`);
  assert.equal(takeInAt(0.5).lineAlpha, 1);
  assert.ok(takeInAt(0.7).lineAlpha > 0 && takeInAt(0.7).lineAlpha < 1);
  assert.equal(takeInAt(0.85).lineAlpha, 0);
});
test("nell'ultimo battito del terminale lo schermo sfuma nel grigio del terminale: pieno sull'ultimo fotogramma (piano 6)", () => {
  const total = 200, frames = 16;
  assert.equal(screenFadeAt(total - frames - 1, total, frames), 0);
  assert.equal(screenFadeAt(total - frames, total, frames), 0);
  assert.equal(screenFadeAt(total - 1, total, frames), 1);
  let last = 0;
  for (let f = total - frames; f < total; f++) { const v = screenFadeAt(f, total, frames); assert.ok(v >= last, `fotogramma ${f}`); last = v; }
});
test("un rettangolo del quadro nelle unità del display: la card della riga torna x 26, y della riga, 428 di larghezza (piano 6)", () => {
  const dx = 1325.4, dy = 531.7, u = 1.3441;
  const r = toDisplay(slotRect(146, dx, dy, u, 2), dx, dy, u);
  const near = (a: number, b: number) => Math.abs(a - b) < 1e-9;
  assert.ok(near(r.x, 26) && near(r.y, 146) && near(r.w, 428) && near(r.r, 42), JSON.stringify(r));
  assert.ok(near(r.h, 24 + 36 + LIST_BODY.shift + 2 * LIST_BODY.line + 24.5));
});
test("la finestra nel display è la stessa finestra del quadro: interpolare e poi convertire è convertire e poi interpolare (piano 6)", () => {
  const dx = 1325.4, dy = 531.7, u = 1.3441, to = slotRect(146, dx, dy, u, 2);
  const a = toDisplay({ x: 0, y: 0, w: 1920, h: 1080, r: 0 }, dx, dy, u), b = toDisplay(to, dx, dy, u);
  for (let i = 0; i <= 10; i++) {
    const p = i / 10, s = takeInAt(p).shrink, r = toDisplay(takeInRect(to, p, F), dx, dy, u);
    const lerp = (x: number, y: number) => x * (1 - s) + y * s;
    for (const k of ["x", "y", "w", "h", "r"] as const) assert.ok(Math.abs(r[k] - lerp(a[k], b[k])) < 1e-9, `p ${p} ${k}`);
  }
});
