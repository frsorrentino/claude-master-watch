import test from "node:test";
import assert from "node:assert/strict";
import { wallAt, wallDelay, wallPlateAt } from "./wall.ts";

test("si parte dentro la lastra e la camera arretra fino a mostrare il muro", () => {
  const a = wallAt(0);
  assert.equal(a.back, 0, "all'inizio la camera è dentro la lastra");
  assert.equal(a.others, 0, "e non c'è nessun'altra lastra");
  assert.equal(a.inAgain, 0);
  const z = wallAt(1);
  assert.equal(z.back, 1);
  assert.equal(z.others, 1);
  assert.equal(z.inAgain, 1, "alla fine si rientra in una lastra sola");
});

test("le altre lastre arrivano dopo che la camera ha cominciato ad arretrare, dal centro ai bordi", () => {
  assert.equal(wallAt(0.1).others, 0, "nessuna lastra prima che la camera si muova");
  const centro = wallPlateAt(0.4, 2, 5, 2), medio = wallPlateAt(0.4, 1, 5, 2), bordo = wallPlateAt(0.4, 0, 5, 2);
  assert.equal(centro, 1, "la lastra dell'account in primo piano c'è sempre");
  assert.ok(medio > bordo, "le vicine compaiono prima delle esterne");
  assert.ok(bordo >= 0 && bordo < 1);
});

test("il ritardo è zero al centro e uno ai capi, e lo scorrimento attraversa lo zero a metà corsa", () => {
  assert.equal(wallDelay(2, 5), 0);
  assert.equal(wallDelay(0, 5), 1);
  assert.equal(wallDelay(4, 5), 1);
  assert.ok(wallAt(0.5).drift === 0 && wallAt(0).drift < 0 && wallAt(1).drift > 0);
});
