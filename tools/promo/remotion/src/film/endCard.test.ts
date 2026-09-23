import test from "node:test";
import assert from "node:assert/strict";
import { END_PACE, contrast, endColors, notesAt } from "./endCard.ts";

test("nel film lungo gli avvisi del cartello arrivano al battito 13 della scena, come sempre", () => assert.equal(notesAt(), 13));
test("nel cartello stretto del corto arrivano al battito 4,5", () => assert.equal(notesAt("compact"), 4.5));
test("dopo la tapparella il cartello parte a 2 battiti dal taglio e gli avvisi a 4 (corto, 23/09)", () => {
  assert.deepEqual(END_PACE.blinds, { start: 2, sub: 1, repo: 1.5, notes: 2 });
  assert.equal(notesAt("blinds"), 4);
});
test("il contrasto si calcola come nelle WCAG", () => {
  assert.equal(contrast("#000000", "#FFFFFF").toFixed(1), "21.0");
  assert.equal(contrast("#777777", "#777777").toFixed(1), "1.0");
});
test("i colori del cartello del film lungo non cambiano; sull'azzurro del corto gli avvisi schiariscono", () => {
  assert.deepEqual(endColors(), { title: "#F4F2EC", accent: "#9EBEFF", dim: "#AAB2CD" });
  assert.notEqual(endColors("blue").dim, "#AAB2CD");
});
