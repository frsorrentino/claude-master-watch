import test from "node:test";
import assert from "node:assert/strict";
import { LOGO_REST, logoSpinAt } from "./logo3d.ts";

test("il segno nasce di taglio e finisce quasi di faccia, con un filo di inclinazione", () => {
  const a = logoSpinAt(0);
  assert.ok(Math.abs(a.yaw - Math.PI / 2) < 1e-9, "all'inizio è di taglio");
  assert.equal(a.draw, 0);
  assert.equal(a.name, 0);
  const z = logoSpinAt(1);
  assert.ok(Math.abs(z.yaw - LOGO_REST) < 1e-9, "alla fine tiene i 4° di inclinazione");
  assert.equal(z.draw, 1);
  assert.equal(z.name, 1);
});

test("prima gira, poi si disegna l'arco, poi entra il nome: mai tutto insieme", () => {
  const mid = logoSpinAt(0.4);
  assert.ok(mid.yaw < Math.PI / 4, "a 0,4 ha già girato oltre metà");
  assert.ok(mid.draw > 0 && mid.draw < 1, "e l'arco si sta disegnando");
  assert.equal(mid.name, 0, "il nome non è ancora entrato");
  assert.ok(logoSpinAt(0.47).glow > 0.9, "la luce passa quando si presenta di faccia");
  assert.ok(logoSpinAt(0.05).glow === 0 && logoSpinAt(1).glow < 1e-9, "e non prima né dopo");
});
