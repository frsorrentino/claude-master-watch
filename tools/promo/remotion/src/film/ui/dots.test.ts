import test from "node:test";
import assert from "node:assert/strict";
import { DOTS_FROM, DOT_STEP, TITLE_LEAD, askDotsAt, dotsAt } from "./dots.ts";
import { DOTS_GAP, SLEEP_CUT } from "./sleep.ts";

const L = 6.5;                                                 // il sonno del film: 6,5 battiti
const at = (beatsToCut: number) => SLEEP_CUT + beatsToCut / L; // p a tanti battiti dalla notifica

test("i puntini entrano dopo la frase e se ne vanno sulla notifica", () => {
  assert.equal(dotsAt(at(-TITLE_LEAD), L).alpha, 0, "quando la frase comincia a scriversi non ci sono");
  assert.ok(dotsAt(at(DOTS_FROM + 0.3), L).alpha > 0.99, "subito dopo sono in quadro");
  assert.ok(dotsAt(at(-0.05), L).alpha > 0.99, "e restano per tutta l'attesa");
  assert.equal(dotsAt(at(0.1), L).alpha, 0, "dopo la notifica non ci sono più");
});

test("culminano uno per battito: 3, 2 e 1 prima della notifica (33 · 34 · 35 con la notifica al 36)", () => {
  for (const i of [0, 1, 2]) {
    const top = dotsAt(at(-DOT_STEP * (3 - i) - (DOTS_GAP - 1)), L).on;
    assert.ok(top[i] > 0.99, `il puntino ${i + 1} è al culmine nel suo turno`);
    for (const j of [0, 1, 2]) if (j !== i) assert.ok(top[j] < top[i], "e gli altri sono più spenti");
  }
});

test("sulla notifica i puntini spariscono e non lasciano nessun punto al posto del primo", () => {
  assert.ok(dotsAt(at(-0.05), L).alpha > 0.99, "fino alla notifica ci sono");
  assert.ok(dotsAt(at(0.1), L).alpha < 1e-9, "subito dopo non ci sono più");
  for (const b of [0, 0.2, 0.5]) assert.equal(dotsAt(at(b), L).collapse, 0, `a ${b} battiti dalla notifica nessun punto di luce`);
});

test("crescendo: dopo il suo battito ogni puntino resta acceso, e prima del quarto tempo sono accesi tutti e tre", async () => {
  const { DOT_HOLD } = await import("./dots.ts");
  const mid = dotsAt(at(-1.5 - (DOTS_GAP - 1)), L).on;                   // fra il secondo e il terzo
  assert.ok(mid[0] >= DOT_HOLD && mid[1] >= DOT_HOLD && mid[2] < DOT_HOLD, "●●○");
  const last = dotsAt(at(-0.4), L).on;                  // subito prima della notifica
  assert.ok(last.every((v) => v >= DOT_HOLD), "●●●");
});
test("se la frase non anticipa la notifica (titleLead 0) i puntini non si accendono, nemmeno sul fotogramma della notifica (revisione del 23/09)", () => {
  for (const f of [0, 1, 2, 10]) assert.equal(askDotsAt(f, 0, 131, 8).alpha, 0);
  // con l'anticipo del film lungo sono quelli di sempre
  for (const f of [0, 20, 49, 60, 90]) assert.deepEqual(askDotsAt(f, 49, 106, 6.5), dotsAt(SLEEP_CUT + (f - 49) / 106, 6.5));
});
