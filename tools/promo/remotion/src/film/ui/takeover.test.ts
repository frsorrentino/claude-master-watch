import test from "node:test";
import assert from "node:assert/strict";
import { TAKEOVER_CUT, sendAt, takeoverAt } from "./takeover.ts";

test("il takeover cresce, tiene un respiro, diventa la scena dopo e si posa: il quadro non resta mai vuoto", () => {
  assert.deepEqual(takeoverAt(0), { grow: 0, hold: 0, become: 0, settle: 0, cover: 0 });
  const g = takeoverAt(0.3); assert.ok(g.grow > 0.3 && g.grow < 1 && g.become === 0);
  const h = takeoverAt(0.44); assert.ok(h.grow === 1 && h.hold === 1 && h.become === 0 && h.cover === 1);
  const b = takeoverAt(0.52); assert.ok(b.become > 0.3 && b.become < 1, "a metà arco il campo sta già diventando la scena dopo");
  const s = takeoverAt(0.58); assert.ok(s.become === 1 && s.settle > 0.5 && s.cover < 0.5, "e si dissolve sopra la scena");
  const e = takeoverAt(1); assert.ok(e.settle === 1 && e.cover === 0);
  assert.ok(TAKEOVER_CUT >= 0.42 && TAKEOVER_CUT <= 0.5, "il taglio cade nel respiro, non dopo mezzo secondo di pieno");
  // il fermo a campo pieno non supera un quinto dell'arco: è lì che si vedeva il vuoto
  const held = [0.42, 0.44, 0.45].filter((p) => takeoverAt(p).hold === 1).length;
  assert.ok(held <= 3 && takeoverAt(0.5).hold === 0);
  // e quando il campo è ancora pieno la scena dopo ha già cominciato a vedersi: mai un colore piatto da solo
  assert.ok(takeoverAt(0.55).settle > 0.3 && takeoverAt(0.65).settle > 0.95, "il campo scopre la scena in fretta");
});

test("l'invio: il dito sul ✓, il tasto si schiaccia e scatta; al taglio il cerchio si apre in mezzo battito e il testo vola sul prompt", () => {
  const B = 16.36, P = 40, C = 50;
  const a = sendAt(P - 0.3 * B, P, C, B);
  assert.ok(a.finger === 0 && a.squash === 0 && a.pop === 0 && a.reveal === 0 && a.fly === 0);
  assert.equal(sendAt(P, P, C, B).finger, 1);
  assert.ok(sendAt(P + 0.1 * B, P, C, B).squash === 1, "sotto il dito il tasto è schiacciato");
  assert.ok(sendAt(P + 0.42 * B, P, C, B).pop > 0.9, "al rilascio scatta");
  assert.equal(sendAt(C, P, C, B).reveal, 0);
  assert.equal(sendAt(C + 0.75 * B, P, C, B).reveal, 1, "il cerchio si apre in tre quarti di battito (21/09 21:16: era troppo rapido)");
  assert.equal(sendAt(P, P, C, B).ripple, 0);
  assert.equal(sendAt(P + 0.3 * B, P, C, B).ripple, 1, "l'onda della pressione riempie il ✓ in un terzo di battito");
  assert.equal(sendAt(C + 0.1 * B, P, C, B).reformat, 0);
  assert.equal(sendAt(C + 0.45 * B, P, C, B).reformat, 1, "la frase diventa testo del terminale in un terzo di battito, e si legge");
  assert.equal(sendAt(C + 0.45 * B, P, C, B).fly, 0, "solo dopo si muove");
  assert.equal(sendAt(C + 1.1 * B, P, C, B).fly, 1, "e si posa sul prompt in due terzi di battito");
});
