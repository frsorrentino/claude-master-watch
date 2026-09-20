import test from "node:test";
import assert from "node:assert/strict";
import { splitAt } from "./split.ts";

test("il terminale si stringe fino a metà, tiene, e poi il lato chiude verso sinistra", () => {
  const s = (p: number) => splitAt(p, 1.5, 2, 1.5, 5);
  assert.equal(s(0).edge, 1, "all'inizio c'è un terminale solo, quello della scena prima");
  assert.ok(Math.abs(s(0.3).edge - 0.5) < 0.001, "dopo un battito e mezzo il lato è a metà quadro");
  assert.ok(s(0.26).edge < 0.5, "e ci arriva scavalcando di poco, non frenando di colpo");
  assert.equal(s(0.6).edge, 0.5, "e lì resta per la sosta");
  assert.ok(s(0.85).edge < 0.5 && s(0.85).edge > 0, "poi continua verso sinistra");
  assert.equal(s(1).edge, 0, "e si chiude: resta la schermata della scena dopo");
});

test("le schede entrano mentre il lato scorre, e la frase si legge nella sosta", () => {
  const s = (p: number) => splitAt(p, 1.5, 2, 1.5, 5);
  assert.equal(s(0.05).cards, 0, "sul primo scorcio non ci sono ancora");
  assert.ok(s(0.2).cards > 0 && s(0.2).cards < 1, "entrano mentre il lato va verso il centro");
  assert.equal(s(0.35).cards, 1, "e ci sono tutte quando il lato è arrivato");
  assert.equal(s(0.1).mid, 0, "la frase non è ancora in quadro");
  assert.equal(s(0.5).mid, 1, "si legge nella sosta");
  assert.equal(s(0.95).mid, 0, "e se ne va quando il lato riparte");
});
