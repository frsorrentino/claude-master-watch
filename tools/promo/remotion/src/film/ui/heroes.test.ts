import test from "node:test";
import assert from "node:assert/strict";
import { cardOutAt, gaugeHeroAt, terminalPlaneAt } from "./heroes.ts";

test("la card parte dal display e ci torna: agli estremi combacia con la card vera", () => {
  assert.deepEqual(cardOutAt(0), { travel: 0, swing: 0, drift: 0, patch: 0, alpha: 1 });
  const end = cardOutAt(1);
  assert.ok(end.travel === 0 && end.alpha === 0 && end.patch === 0);
  const mid = cardOutAt(0.55);
  assert.ok(mid.travel === 1 && mid.alpha === 1 && mid.patch === 1 && Math.abs(mid.swing) < 1e-9, "fuori è ferma e frontale");
});

test("uscita e rientro atterrano morbidi: verso la fine del volo manca meno del 3 %", () => {
  const left = 1 - cardOutAt(0.42 * 0.85).travel;
  assert.ok(left > 0 && left < 0.03, `uscita: resta ${left}`);
  const home = cardOutAt(0.62 + 0.33 * 0.78).travel;
  assert.ok(home > 0 && home < 0.03, `rientro: resta ${home}`);
});

test("gira solo in volo, con un picco a metà, e si dissolve solo dopo essere atterrata", () => {
  assert.ok(cardOutAt(0.16).swing > 0.6 && cardOutAt(0.16).travel < 0.9);
  assert.ok(cardOutAt(3 / 57).travel < 0.08, `nei primi tre fotogrammi non salta: ${cardOutAt(3 / 57).travel}`);
  assert.equal(cardOutAt(0.94).swing < 0.2, true);
  assert.equal(cardOutAt(0.95).alpha, 1);
  assert.ok(cardOutAt(0.975).alpha > 0.4 && cardOutAt(0.975).alpha < 0.6);
  assert.equal(cardOutAt(0.95).travel, 0, "atterrata prima di dissolversi");
});

test("la toppa copre la card vera appena la card si stacca e se ne va con lei", () => {
  assert.ok(cardOutAt(0.01).patch > 0 && cardOutAt(0.01).travel < 0.02);
  assert.equal(cardOutAt(0.5).patch, 1);
  assert.equal(cardOutAt(0.975).patch, 0, "il fantasma se n'è già andato a metà dissolvenza");
});

test("il gauge parte pieno, si svuota in volo, si disegna fuori col numero, rientra pieno", () => {
  assert.equal(gaugeHeroAt(0).value, 1);
  assert.ok(gaugeHeroAt(0.31).value === 0 && gaugeHeroAt(0.31).label === 0, "arrivato vuoto, senza numero");
  const mid = gaugeHeroAt(0.5);
  assert.ok(mid.value > 0.5 && mid.value < 1 && mid.label === 1 && mid.travel === 1);
  assert.ok(gaugeHeroAt(0.65).value === 1 && gaugeHeroAt(0.75).label === 0, "pieno prima del rientro, numero già andato");
  assert.ok(gaugeHeroAt(0.96).value === 1 && gaugeHeroAt(0.96).travel === 0, "atterra pieno com'era sul display");
});

test("il terminale è com'è sul display a casa e finestra del PC quando è fuori", () => {
  assert.equal(terminalPlaneAt(0).morph, 0);
  assert.equal(terminalPlaneAt(0.5).morph, 1);
  assert.equal(terminalPlaneAt(0.96).morph, 0);
});
