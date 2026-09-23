import test from "node:test";
import assert from "node:assert/strict";
import { END_PACE, contrast, endColors, logoTrack, notesAt } from "./endCard.ts";

test("nel film lungo gli avvisi del cartello arrivano al battito 13 della scena, come sempre", () => assert.equal(notesAt(), 13));
test("nel cartello stretto del corto arrivano al battito 4,5", () => assert.equal(notesAt("compact"), 4.5));
test("dopo la tapparella il nome arriva a 4 battiti dal taglio, quando il logo è sull'orologio, e gli avvisi a 5,5 (corto, 23/09)", () => {
  assert.deepEqual(END_PACE.blinds, { start: 4, sub: 0.5, repo: 1, notes: 1.5 });
  assert.equal(notesAt("blinds"), 5.5);
});
test("il contrasto si calcola come nelle WCAG", () => {
  assert.equal(contrast("#000000", "#FFFFFF").toFixed(1), "21.0");
  assert.equal(contrast("#777777", "#777777").toFixed(1), "1.0");
});
test("i colori del cartello del film lungo non cambiano; sull'azzurro del corto gli avvisi schiariscono", () => {
  assert.deepEqual(endColors(), { title: "#F4F2EC", accent: "#9EBEFF", dim: "#AAB2CD" });
  assert.notEqual(endColors("blue").dim, "#AAB2CD");
});
test("la parte vuota dell'arco del logo resta quella del film lungo, e schiarisce solo sul blu del corto", () => {
  assert.equal(logoTrack(), "#3A404C");
  assert.equal(logoTrack("blue"), "rgba(244,242,236,0.25)");
});
