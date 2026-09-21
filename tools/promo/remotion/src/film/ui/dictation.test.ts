import test from "node:test";
import assert from "node:assert/strict";
import { dictationAt } from "./dictation.ts";

const B = 60 / 110;                       // un battito in secondi
const W = [{ start: 0, end: 0.24, word: "Great." }, { start: 0.72, end: 0.98, word: "Now" }, { start: 0.98, end: 1.5, word: "update" }];
const TAP = 5, VOICE = 5.4, OK = 8;

test("prima del tocco ci sono i tre tasti, nessuna parola, niente dito", () => {
  const d = dictationAt(TAP - 0.3 * B, TAP, VOICE, W, B, OK);
  assert.ok(d.chooser === 1 && d.press === 0 && d.listen === 0 && d.words === 0 && d.pressOk === 0);
});

test("il dito preme il microfono sul tocco e si alza subito dopo; i tasti lasciano il posto all'ascolto", () => {
  assert.equal(dictationAt(TAP, TAP, VOICE, W, B, OK).press, 1);
  const after = dictationAt(TAP + 0.6 * B, TAP, VOICE, W, B, OK);
  assert.ok(after.press === 0 && after.chooser === 0 && after.listen === 1, JSON.stringify(after));
});

test("le parole compaiono quando la voce le dice, e si sa quale sta dicendo", () => {
  assert.equal(dictationAt(VOICE - 0.01, TAP, VOICE, W, B, OK).words, 0);
  const d = dictationAt(VOICE + 1.0, TAP, VOICE, W, B, OK);
  assert.equal(d.words, 3);
  assert.equal(d.speaking, 2);
  assert.equal(dictationAt(VOICE + 0.5, TAP, VOICE, W, B, OK).speaking, -1, "nella pausa dopo «Great.» nessuna parola è in corso");
});

test("in ascolto il tasto centrale è il microfono; appena arriva la prima parola diventa ✓", () => {
  assert.equal(dictationAt(VOICE - 0.05, TAP, VOICE, W, B, OK).ok, 0);
  assert.equal(dictationAt(VOICE + 0.2, TAP, VOICE, W, B, OK).ok, 1);
});

test("il dito conferma su ✓ e si alza prima che la schermata cresca", () => {
  assert.equal(dictationAt(OK - 0.3 * B, TAP, VOICE, W, B, OK).pressOk, 0);
  assert.equal(dictationAt(OK, TAP, VOICE, W, B, OK).pressOk, 1);
  assert.equal(dictationAt(OK + 0.45 * B, TAP, VOICE, W, B, OK).pressOk, 0);
});
