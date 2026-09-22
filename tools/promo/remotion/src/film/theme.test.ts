import test from "node:test";
import assert from "node:assert/strict";
import { ACT_BG, actColors } from "./theme.ts";

const NAVY: [string, string, string, string] = ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"];

test("senza tavolozza ogni atto ha i suoi colori di sempre", () => {
  for (const a of ["open", "know", "act", "control", "close"] as const) assert.deepEqual(actColors(a), ACT_BG[a]);
});
test("la tavolozza della scaletta sostituisce solo gli atti che nomina", () => {
  assert.deepEqual(actColors("act", { act: NAVY }), NAVY);
  assert.deepEqual(actColors("control", { act: NAVY }), ACT_BG.control);
});
