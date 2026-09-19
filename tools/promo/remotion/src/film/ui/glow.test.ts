import test from "node:test";
import assert from "node:assert/strict";
import { GLOW_CUT, glowAt } from "./glow.ts";

test("la luce parte dal display, copre il quadro e si ritira", () => {
  const a = glowAt(0);
  assert.equal(a.r, 0, "all'inizio non c'è alone");
  assert.equal(a.cover, 0);
  assert.ok(glowAt(0.42).r > 0.99, "a poco meno di metà corsa ha raggiunto tutto il quadro");
  assert.equal(glowAt(0.45).cover, 1, "e lì il quadro è pieno");
  assert.ok(glowAt(1).r < 0.2 && glowAt(1).cover === 0, "alla fine se n'è andata");
});

test("c'è una finestra in cui la scena sotto è nascosta: lì l'orologio cambia posto senza che si veda", () => {
  assert.equal(glowAt(0.45).hide, 1);
  assert.equal(glowAt(GLOW_CUT).hide, 1, "il taglio cade dentro quella finestra");
  assert.ok(glowAt(0.3).hide < 1 && glowAt(0.75).hide === 0, "e fuori dalla finestra la scena si vede");
});
