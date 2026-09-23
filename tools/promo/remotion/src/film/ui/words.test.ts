import test from "node:test";
import assert from "node:assert/strict";
import { wordStarts } from "./words.ts";

test("le parole partono a mezzo battito l'una; con una pausa la seconda riga aspetta (Franz, 23/09 23:20)", () => {
  assert.deepEqual(wordStarts(["Claude Code,", "on your wrist."], 8), [0, 8, 16, 24, 32]);
  assert.deepEqual(wordStarts(["Claude Code,", "on your wrist."], 8, 0, 16), [0, 8, 32, 40, 48]);
  assert.deepEqual(wordStarts(["Every session,", "at a glance."], 8, 2), [-16, -8, 0, 8, 16]);   // `carry`: le prime due arrivano già scritte
});
