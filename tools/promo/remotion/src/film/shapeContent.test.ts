import test from "node:test";
import assert from "node:assert/strict";
import { overshootOf } from "./spring.ts";
import { OPTION_STAGGER, OPTION_ZETA, optionsEnterAt } from "./shapeContent.ts";
import { SWAP_OUT } from "./shape.ts";

test("i tasti entrano con la molla ζ 0,7: uno scavalco solo del 4,6 %, niente oscillazione (§8.4)", () => {
  assert.ok(Math.abs(overshootOf(OPTION_ZETA) - 0.046) < 0.001);
  const xs = Array.from({ length: 801 }, (_, i) => optionsEnterAt(SWAP_OUT + (i / 800) * 2, 0));
  const max = Math.max(...xs), top = xs.indexOf(max);
  assert.ok(max > 1.04 && max < 1.05, `scavalco ${max}`);
  for (let i = top; i < xs.length; i++) assert.ok(xs[i] >= 1 - 0.003, `ricade sotto 1: ${xs[i]}`);   // non rimbalza
  assert.equal(optionsEnterAt(SWAP_OUT, 0), 0);
  assert.equal(optionsEnterAt(SWAP_OUT + 5, 0), 1);
});

test("il secondo tasto entra un quarto di battito dopo il primo, dopo l'uscita del contenuto di prima", () => {
  assert.equal(OPTION_STAGGER, 0.25);
  for (const b of [0, 0.3, 0.6, 0.9, 1.4]) assert.ok(Math.abs(optionsEnterAt(b + OPTION_STAGGER, 1) - optionsEnterAt(b, 0)) < 1e-12);
  assert.equal(optionsEnterAt(SWAP_OUT - 0.01, 0), 0);
});

test("le schede della corsia nella forma: card1 e card2 sono la prima e la seconda scheda (non le scritte), dict quella che detta", async () => {
  const { readFileSync } = await import("node:fs");
  const { laneCard } = await import("./shapeContent.ts");
  const short = JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8"));
  const cards = short.scenes.find((s: { id: string }) => s.id === "loop").fx.find((f: { kind: string }) => f.kind === "float").cards;
  assert.equal(laneCard(cards, "card1")?.text, "Running the staging checks for 2.8.0");
  assert.equal(laneCard(cards, "card2")?.text, "Deployed 2.8.0, smoke tests green");
  assert.ok(laneCard(cards, "dict")?.dictation);
  assert.equal(laneCard(cards, "card9"), undefined);
  assert.equal(laneCard(cards, "notify"), undefined);
});
