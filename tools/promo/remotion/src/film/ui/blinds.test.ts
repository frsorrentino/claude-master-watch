import test from "node:test";
import assert from "node:assert/strict";
import { blindAt, blindDelay } from "./blinds.ts";

const N = 12, BARS = [4, 7] as const;

test("all'inizio ci sono solo le due barre: gli altri listelli non sono ancora nati", () => {
  const b0 = blindAt(0, 4, N, BARS), o0 = blindAt(0, 0, N, BARS);
  assert.equal(b0.born, 1, "la barra c'è già");
  assert.equal(b0.spread, 0, "e parte larga quanto la barra");
  assert.equal(o0.born, 0, "gli altri no");
});

test("la tapparella si chiude prima di voltarsi: a metà corsa è piena e ancora dritta", () => {
  for (let i = 0; i < N; i++) {
    const b = blindAt(0.4, i, N, BARS);
    assert.ok(b.born > 0.99, `listello ${i} non ancora nato a 0,4`);
    assert.ok(b.spread > 0.99, `listello ${i} non ancora largo a 0,4`);
    assert.ok(Math.abs(b.rot) < 1e-9, `listello ${i} già voltato a 0,4`);
  }
});

test("la voltata parte dalle barre e arriva ai capi, oltre il quarto di giro", () => {
  const bar = blindAt(0.62, 4, N, BARS), far = blindAt(0.62, 0, N, BARS);
  assert.ok(Math.abs(bar.rot) > Math.abs(far.rot), "la barra si volta prima del listello lontano");
  const end = blindAt(1, 0, N, BARS);
  assert.ok(Math.abs(end.rot) > Math.PI / 2, "a fine corsa ogni listello ha passato il quarto di giro");
  assert.equal(blindAt(1, 0, N, BARS).alpha, 0, "e non si vede più");
});

test("il ritardo cresce con la distanza dalle barre ed è nullo sulle barre", () => {
  assert.equal(blindDelay(4, N, BARS), 0);
  assert.equal(blindDelay(7, N, BARS), 0);
  assert.ok(blindDelay(0, N, BARS) > blindDelay(2, N, BARS));
});
