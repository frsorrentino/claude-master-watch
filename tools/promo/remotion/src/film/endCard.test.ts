import test from "node:test";
import assert from "node:assert/strict";
import { notesAt } from "./endCard.ts";

test("nel film lungo gli avvisi del cartello arrivano al battito 13 della scena, come sempre", () => assert.equal(notesAt(), 13));
test("nel cartello stretto del corto arrivano al battito 4,5", () => assert.equal(notesAt("compact"), 4.5));
