import test from "node:test";
import assert from "node:assert/strict";
import { bandsAt } from "./bands.ts";

test("il campo si apre in due e alla fine una banda si riprende il quadro", () => {
  const b = (p: number) => bandsAt(p, 1.5, 1.5, 5);
  assert.equal(b(0).split, 0, "all'inizio è un campo solo");
  assert.equal(b(0.3).split, 1, "dopo un battito e mezzo le bande sono aperte");
  assert.equal(b(0.5).win, 0, "a metà scena nessuna ha ancora vinto");
  assert.ok(b(0.85).win > 0 && b(0.85).win < 1, "verso la fine una comincia a prendersi il quadro");
  assert.equal(b(1).win, 1, "e sul taglio l'ha preso tutto");
});
