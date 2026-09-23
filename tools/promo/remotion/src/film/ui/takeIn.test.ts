import test from "node:test";
import assert from "node:assert/strict";
import { slotRect, takeInAt, takeInRect } from "./takeIn.ts";
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
test("il terminale sparisce nella prima metà, la card compare nella seconda, la riga dell'esito vola in mezzo", () => {
  assert.equal(takeInAt(0).terminal, 1);
  assert.equal(takeInAt(0.4).terminal, 0);
  assert.equal(takeInAt(0.5).card, 0);
  assert.equal(takeInAt(0.9).card, 1);
  assert.equal(takeInAt(0).line, 0);
  assert.equal(takeInAt(1).line, 1);
  assert.equal(takeInAt(1).shrink, 1);
});
