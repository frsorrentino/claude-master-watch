import test from "node:test";
import assert from "node:assert/strict";
import { DOTS_FROM, dotsAt } from "./dots.ts";
import { SLEEP_CUT, TITLE_AT } from "./sleep.ts";

test("i puntini entrano dopo la frase e se ne vanno sulla notifica", () => {
  assert.equal(dotsAt(TITLE_AT).alpha, 0, "mentre la frase si scrive non ci sono");
  assert.ok(dotsAt(DOTS_FROM + 0.02).alpha > 0.99, "subito dopo sono in quadro");
  assert.ok(dotsAt(SLEEP_CUT - 0.01).alpha > 0.99, "e restano per tutta l'attesa");
  assert.equal(dotsAt(SLEEP_CUT + 0.01).alpha, 0, "dopo la notifica non ci sono più");
});

test("pulsano a turno, e il terzo culmina sul battito della notifica", () => {
  const period = (SLEEP_CUT - DOTS_FROM) / 3;
  for (const i of [0, 1, 2]) {
    const top = dotsAt(DOTS_FROM + (i + 1) * period).on;
    assert.ok(top[i] > 0.99, `il puntino ${i + 1} è al culmine nel suo turno`);
    for (const j of [0, 1, 2]) if (j !== i) assert.ok(top[j] < top[i], `e gli altri sono più spenti`);
  }
  assert.ok(dotsAt(SLEEP_CUT).on[2] > 0.99, "il terzo culmina esattamente sul taglio");
});

test("sulla notifica i tre diventano un punto di luce che si allarga e sparisce", () => {
  assert.equal(dotsAt(SLEEP_CUT - 0.001).collapse, 0, "prima del battito non c'è");
  assert.equal(dotsAt(SLEEP_CUT).collapse, 1, "sul battito è pieno");
  assert.ok(dotsAt(SLEEP_CUT + 0.02).collapse < 0.6, "e si spegne subito");
  assert.equal(dotsAt(SLEEP_CUT + 0.04).collapse, 0, "in pochi fotogrammi non c'è più");
});
