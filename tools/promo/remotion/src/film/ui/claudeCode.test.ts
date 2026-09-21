import test from "node:test";
import assert from "node:assert/strict";
import { CC, ccBoxHeight, ccKind, ccLines, ccPromptTop, ccPromptY, ccRows, ccStackBottom, ccTool } from "./claudeCode.ts";

test("una riga è una frase di Claude (⏺), un risultato (⎿) o una chiamata a uno strumento", () => {
  assert.equal(ccKind("⏺ Reading the checklist"), "says");
  assert.equal(ccKind("⎿ Read 38 lines"), "result");
  assert.equal(ccKind("Read(RELEASE.md)"), "tool");
});

test("la chiamata si divide in nome dello strumento (in grassetto) e argomenti", () => {
  assert.deepEqual(ccTool("Bash(git tag v2.8.0)"), { name: "Bash", args: "(git tag v2.8.0)" });
  assert.deepEqual(ccTool("qualcosa senza parentesi"), { name: "qualcosa senza parentesi", args: "" });
});

test("il prompt va a capo come il terminale: caratteri monospazio da 0,6 em, con i due del «> » davanti", () => {
  const perRow = Math.floor(883 / (0.6 * CC.font));
  assert.equal(ccRows("Great. Now update the changelog and tag the release", 883), Math.ceil((2 + 51) / perRow));
  assert.equal(ccRows("hi", 883), 1);
});

test("il prompt sta sotto l'intestazione di tre righe, con mezza riga d'aria", () => {
  assert.equal(ccPromptY(100), 100 + 3.5 * CC.row);
});

test("la casella in fondo è alta tre righe e una linea; la pila delle righe si appoggia mezza riga sopra", () => {
  assert.equal(ccBoxHeight(), 3 * CC.row + 1.5);
  assert.equal(ccStackBottom(1080), 1080 - CC.bottom - ccBoxHeight() - CC.row / 2);
});

test("finché Claude non ha risposto il prompt è l'ultima cosa della pila: sta subito sopra la casella", () => {
  const w = 768, rows = ccRows("Great. Now update the changelog and tag the release", w);
  assert.equal(ccPromptTop(1080, "Great. Now update the changelog and tag the release", w), ccStackBottom(1080) - rows * CC.row);
});

test("a capo per parole come il terminale: le nostre righe stanno in una riga, il risultato lungo in due", () => {
  const w = 768;
  assert.equal(ccLines("⏺ Reading the checklist", w), 1);
  assert.equal(ccLines("Bash(git tag v2.8.0)", w), 1);
  assert.equal(ccLines("⎿ Updated CHANGELOG.md with 9 additions", w), 2);
  assert.equal(ccRows("Great. Now update the changelog and tag the release", w), 2);
});
