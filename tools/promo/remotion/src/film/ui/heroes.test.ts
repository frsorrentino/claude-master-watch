import test from "node:test";
import assert from "node:assert/strict";
import { RAIL_MOVE, cardOutAt, cardUnits, gaugeHeroAt, laneY, optionsBuildAt, panelHeroAt, railAt, railScroll, railScrollVar, railStackPx, stackAt, terminalPlaneAt } from "./heroes.ts";

test("la card parte dal display (a 0 combacia), esce morbida, resta fuori e si consegna alla scena dopo senza tornare", () => {
  const a = cardOutAt(0);
  assert.ok(a.travel === 0 && a.alpha === 1 && a.patch === 0 && a.exit === 0);
  const left = 1 - cardOutAt(0.42 * 0.85).travel;
  assert.ok(left > 0 && left < 0.03, `uscita: resta ${left}`);
  const mid = cardOutAt(0.6);
  assert.ok(mid.travel === 1 && mid.patch === 1 && mid.alpha === 1 && Math.abs(mid.swing) < 1e-9);
  const end = cardOutAt(0.999);
  assert.ok(end.travel === 1 && end.exit > 0.99 && end.alpha < 0.01, "non rientra: si ritira verso la scena dopo");
});

test("dopo il battito di ciglia la card è già fuori dal primo fotogramma", () => {
  const a = cardOutAt(0, true);
  assert.ok(a.travel === 1 && a.swing === 0 && a.alpha === 1);
  assert.equal(cardOutAt(-0.01, true).alpha, 0);
});

test("il gauge parte pieno, si svuota in volo, si disegna fuori col numero, poi si consegna", () => {
  assert.equal(gaugeHeroAt(0).value, 1);
  assert.ok(gaugeHeroAt(0.31).value === 0 && gaugeHeroAt(0.31).label === 0);
  const mid = gaugeHeroAt(0.5);
  assert.ok(mid.value > 0.5 && mid.value < 1 && mid.label === 1 && mid.travel === 1);
  assert.ok(gaugeHeroAt(0.7).value === 1 && gaugeHeroAt(0.9).label === 0 && gaugeHeroAt(0.95).exit > 0.5);
});

test("i tasti escono dall'orologio, l'anello corre svelto, il tasto si gonfia e si riempie", () => {
  assert.equal(optionsBuildAt(0.12).build, 1, "a un ottavo della scena i tasti ci sono già");
  assert.ok(optionsBuildAt(0.22).travel > 0.99, "e si sono posati al centro");
  assert.ok(optionsBuildAt(0.4).ring > 0.4 && optionsBuildAt(0.46).ring === 1, "l'anello si chiude entro metà scena");
  assert.ok(optionsBuildAt(0.495).pop > 0.9 && optionsBuildAt(0.56).pop < 0.01);
  assert.ok(optionsBuildAt(0.53).fill === 0 && optionsBuildAt(0.6).fill > 0.99, "il celeste riempie subito dopo");
  // e da lì in poi resta pieno: è quel campo che diventa lo sfondo della scena dopo
  assert.equal(optionsBuildAt(0.9).fill, 1);
});

test("il terminale del PC compare dietro e si ritira alla fine", () => {
  assert.equal(terminalPlaneAt(-0.1).show, 0);
  assert.equal(terminalPlaneAt(0.3).show, 1);
  assert.ok(terminalPlaneAt(0.5).exit === 0 && terminalPlaneAt(0.99).exit > 0.9);
});

test("la lista scorre di un passo alla volta e si sofferma su ogni scheda", () => {
  assert.equal(railScroll(0), 0);
  assert.ok(railScroll(0.2) > 0.2 && railScroll(0.42) === 1, "il passo si compie nel primo 42 % del tempo");
  assert.equal(railScroll(0.8), 1, "poi resta ferma sulla scheda");
  assert.equal(railScroll(1), 1);
  assert.ok(railScroll(2.42) === 3, "ogni passo uguale");
});

test("la corsia deforma come le liste di Wear OS: piccola sotto, larga al centro, piccola in cima", () => {
  assert.ok(Math.abs(railAt(0).scale - 0.62) < 1e-9 && Math.abs(railAt(0).alpha - 0.45) < 1e-9, "ai capi resta piccola e visibile");
  assert.ok(Math.abs(railAt(0.5).scale - 1) < 1e-9 && railAt(0.5).alpha === 1);
  assert.ok(Math.abs(railAt(1).scale - 0.62) < 1e-9 && railAt(1).alpha > 0.4, "non sparisce mai");
  assert.ok(railAt(0.25).scale > 0.85 && railAt(0.25).scale < 1);
});

test("la sosta è diversa per scheda, e cade quando la scheda è al centro della corsia", () => {
  const holds = [0.2, 1.2];
  const total = holds.reduce((a, h) => a + RAIL_MOVE + h, 0);
  assert.ok(Math.abs(railScrollVar(0, holds) - (-0.2)) < 1e-9, "la prima parte da sotto il centro");
  assert.ok(Math.abs(railScrollVar(RAIL_MOVE / total, holds) - 0.8) < 1e-9, "ferma con la prima al centro");
  assert.ok(Math.abs(railScrollVar((RAIL_MOVE + 0.2) / total, holds) - 0.8) < 1e-9, "resta ferma per tutta la sosta");
  assert.ok(Math.abs(railScrollVar(1, holds) - 1.8) < 1e-9);
  assert.ok(railScrollVar(0.95, holds) === railScrollVar(0.85, holds), "sosta lunga sulla seconda: non si muove");
});

test("le schede si impilano a stacco costante: fra due consecutive restano sempre i pixel dello stacco vero", () => {
  const H = 279, gap = 12, span = 2.2;
  for (const v of [0.2, 0.8, 1.4]) {
    const yA = railStackPx(v, H, gap, span), yB = railStackPx(v + 1, H, gap, span);
    const hA = H * railAt(v / span).scale, hB = H * railAt((v + 1) / span).scale;
    assert.ok(Math.abs((yB - yA) - (hA + hB) / 2 - gap) < 22, `a ${v} lo stacco è ${((yB - yA) - (hA + hB) / 2).toFixed(1)} invece di ${gap}`);
  }
});

test("il pannello della Panoramica esce vuoto, si disegna fuori e si consegna", () => {
  assert.equal(panelHeroAt(0).draw, 0);
  assert.equal(panelHeroAt(0.31).draw, 0);
  assert.ok(panelHeroAt(0.5).draw > 0.3 && panelHeroAt(0.5).draw < 1 && panelHeroAt(0.5).travel === 1);
  assert.equal(panelHeroAt(0.7).draw, 1);
  assert.ok(panelHeroAt(0.95).exit > 0.5);
});

test("la scheda che resta protagonista non se ne va: con la sosta l'uscita è zero e resta visibile fino alla fine", () => {
  for (const p of [0.5, 0.86, 0.99]) {
    const c = cardOutAt(p, false, true);
    assert.equal(c.exit, 0, `a ${p} la card sta già uscendo`);
    assert.ok(c.alpha > 0.99, `a ${p} la card è sparita (alpha ${c.alpha})`);
    assert.ok(c.travel > 0.99, `a ${p} la card non è ancora arrivata`);
  }
  assert.ok(cardOutAt(0.99, false, false).exit > 0.5, "senza sosta la card se ne va davvero");
});

test("l'altezza della scheda viene dal suo testo, come sul display", () => {
  assert.equal(cardUnits("Run the checkout test suite after the Stripe webhook refactor"), 213, "tre righe: 213 unità, come il rettangolo misurato sul fotogramma");
  assert.equal(cardUnits("Deployed 2.8.0, smoke tests green"), 167, "due righe: 167");
  assert.ok(cardUnits("Short one") < cardUnits("Deployed 2.8.0, smoke tests green"), "una riga sta in meno");
});

test("nella corsia fra due voci vicine resta sempre lo stacco, a qualunque punto dello scorrimento", () => {
  const h = [219, 108, 279, 167], flat = [false, true, false, false], gap = 12, yB = 600;
  for (const offset of [0, 0.3, 0.8, 1.2, 1.9, 2.5, 3]) {
    const lane = laneY(offset, h, gap, yB, flat);
    for (let i = 0; i + 1 < h.length; i++) {
      const a = lane.y[i], b = lane.y[i + 1];
      if (a === null || b === null) continue;
      const vuoto = b - a - (h[i] * lane.scale[i]) / 2 - (h[i + 1] * lane.scale[i + 1]) / 2;
      assert.ok(Math.abs(vuoto - gap) < 0.001, `offset ${offset}, voci ${i}-${i + 1}: vuoto ${vuoto.toFixed(1)} invece di ${gap}`);
    }
  }
});

test("in corsia ci sono al massimo due voci: quella davanti piena e una sopra, più piccola e trasparente", () => {
  assert.equal(stackAt(0).scale, 1, "chi è davanti è pieno");
  assert.equal(stackAt(0).alpha, 1, "e opaco");
  assert.ok(stackAt(1).scale < 1, "quella sopra è più piccola");
  assert.ok(stackAt(1).alpha > 0.75 && stackAt(1).alpha < 0.95, `e appena più trasparente (${stackAt(1).alpha.toFixed(2)}), non un fantasma`);
  assert.ok(stackAt(1.8).alpha < 0.5, "e si spegne solo in fondo alla salita");
  assert.equal(stackAt(2).alpha, 0, "quella ancora prima è sparita");
  const h = [219, 219, 219, 219], gap = 12, yB = 600;
  for (const offset of [1.2, 2.0, 2.7, 3.4]) {
    const lane = laneY(offset, h, gap, yB);
    const vive = lane.y.filter((v, i) => v !== null && lane.alpha[i] > 0.02).length;
    assert.ok(vive <= 2, `a offset ${offset} ci sono ${vive} voci in quadro`);
  }
});

test("la corsia non salta quando il turno passa alla voce dopo", () => {
  const h = [219, 108, 279, 167, 219], flat = [false, true, false, false, false], gap = 12, yB = 600;
  let prev: number | null = null, max = 0;
  for (let k = 0; k <= 400; k++) {
    const y = laneY((k * 4) / 400, h, gap, yB, flat).y[1];
    if (y !== null && prev !== null) max = Math.max(max, Math.abs(y - prev));
    prev = y;
  }
  assert.ok(max < 6, `il passo più grande fra due campioni è ${max.toFixed(1)} px: nessuno scatto`);
});
