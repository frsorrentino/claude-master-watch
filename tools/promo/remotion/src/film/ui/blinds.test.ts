import test from "node:test";
import assert from "node:assert/strict";
import { BLIND_CUT, blindAt, blindDelay, blindSoloAt } from "./blinds.ts";

const N = 12, BARS = [6, 7] as const;

test("all'inizio ci sono solo le due barre, e il resto del quadro se ne va subito", () => {
  const b0 = blindAt(0, 6, N, BARS), o0 = blindAt(0, 0, N, BARS);
  assert.equal(b0.born, 1, "la barra c'è già");
  assert.equal(b0.spread, 0, "e parte larga quanto la barra");
  assert.equal(o0.born, 0, "gli altri no");
  assert.equal(blindSoloAt(0), 0);
  assert.ok(blindSoloAt(0.16) > 0.99, "a un sesto della corsa il quadro ha già lasciato il posto alle barre");
});

test("le barre diventano listelli PRIMA che nascano gli altri: il collegamento si vede", () => {
  const bar = blindAt(0.28, 6, N, BARS);
  assert.ok(bar.spread > 0.99, "la barra è già un listello a 0,28");
  assert.ok(bar.wink > 0.5, "e sta facendo l'accenno di voltata");
  for (let i = 0; i < N; i++) if (i !== BARS[0] && i !== BARS[1]) assert.equal(blindAt(0.28, i, N, BARS).born, 0, `listello ${i} nato troppo presto`);
});

test("a larghezza piena il quadro non resta fermo: l'accenno è già in corso e gli altri nascono subito dopo", () => {
  // Franz, 21/09 12:46: fra le barre larghe quanto lo schermo e il resto dell'animazione c'era una stasi
  assert.ok(blindAt(0.27, 6, N, BARS).wink > 0.3, "l'accenno parte prima che le barre finiscano di allargarsi");
  assert.ok(blindAt(0.4, 5, N, BARS).born > 0.3, "subito dopo l'accenno i vicini stanno già nascendo (prima a 0,4 non erano ancora partiti)");
});

test("al taglio la tapparella è piena e ancora dritta", () => {
  for (let i = 0; i < N; i++) {
    const b = blindAt(BLIND_CUT, i, N, BARS);
    assert.ok(b.born > 0.99, `listello ${i} non ancora nato al taglio`);
    assert.ok(b.spread > 0.99, `listello ${i} non ancora largo al taglio`);
    assert.ok(Math.abs(b.rot) < 1e-9, `listello ${i} già voltato al taglio`);
  }
});

test("la voltata parte dalle barre e arriva ai capi, oltre il quarto di giro", () => {
  const bar = blindAt(0.82, 6, N, BARS), far = blindAt(0.82, 0, N, BARS);
  assert.ok(Math.abs(bar.rot) > Math.abs(far.rot), "la barra si volta prima del listello lontano");
  const end = blindAt(1, 0, N, BARS);
  assert.ok(Math.abs(end.rot) > Math.PI / 2, "a fine corsa ogni listello ha passato il quarto di giro");
  assert.equal(end.alpha, 0, "e non si vede più");
});

test("il ritardo cresce con la distanza dalle barre ed è nullo sulle barre", () => {
  assert.equal(blindDelay(6, N, BARS), 0);
  assert.equal(blindDelay(7, N, BARS), 0);
  assert.ok(blindDelay(0, N, BARS) > blindDelay(4, N, BARS));
});

test("i listelli che nascono dalle barre sono tanti quante le righe del Context, attorno al centro della griglia", async () => {
  const { blindBars } = await import("./blinds.ts");
  assert.deepEqual(blindBars(2), [6, 7]);
  assert.deepEqual(blindBars(3), [5, 6, 7]);
  // con tre barre la terza nasce insieme alle altre due, e i listelli vicini partono dopo di lei, non prima
  assert.equal(blindAt(0.5, 5, 12, [5, 6, 7]).born, 1);
  assert.ok(blindDelay(4, 12, [5, 6, 7]) < blindDelay(4, 12, [6, 7]));
});
