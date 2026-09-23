import test from "node:test";
import assert from "node:assert/strict";
import { AMBIENT, BREATH, DOTS_GAP, FIELD, HALO, HALO_R, HUSH, SLEEP_CUT, TITLE_AT, sleepAt, sleepP, titleOnAt } from "./sleep.ts";

const inBreath = (v: number) => v >= AMBIENT - 1e-9 && v <= AMBIENT + BREATH + 1e-9;

const about = (v: number, w: number) => Math.abs(v - w) < 1e-9;

test("il display cala ad ambient e ci resta fino al taglio", () => {
  assert.equal(sleepAt(0).light, 1, "all'inizio il display è acceso");
  assert.equal(sleepAt(0.0625).light, 1, "la chiusura comincia sul battito, non prima");
  assert.ok(sleepAt(0.095).light < 0.6, "e dura mezzo battito: a metà ha già perso la luce");
  assert.ok(about(sleepAt(HUSH).light, AMBIENT), "sul battito dopo è addormentato");
  assert.ok(sleepAt(SLEEP_CUT - 1 / 131).light < AMBIENT + 0.01, "e sull'ultimo fotogramma prima del taglio il respiro è chiuso");
});

test("la camera si sposta a display addormentato, e la si vede: intorno resta luce", () => {
  assert.equal(sleepAt(HUSH).move, 0, "mentre il display cala, la camera è ferma");
  assert.ok(inBreath(sleepAt(0.21).light), "a metà spostamento il display è ad ambient (che respira)");
  assert.ok(sleepAt(0.21).move > 0.2 && sleepAt(0.21).move < 0.9, "e lì la camera è a metà strada");
  assert.ok(sleepAt(0.21).halo >= HALO - 1e-9 && sleepAt(0.21).halo <= HALO + 0.05 + 1e-9, "la luce intorno non si spegne: la cassa si vede muovere");
  assert.equal(sleepAt(0.3).move, 1, "lo spostamento finisce con la sua mezza battuta, prima che si scriva il titolo");
  assert.equal(sleepAt(SLEEP_CUT).move, 1);
});

test("la notifica riaccende il display con un soprassalto di luce e poi si assesta", () => {
  assert.ok(sleepAt(SLEEP_CUT).light > 1.2, "sul taglio il display è già acceso, e dà più luce del normale");
  assert.ok(sleepAt(0.85).light < sleepAt(SLEEP_CUT).light, "e subito rientra");
  assert.ok(about(sleepAt(0.95).light, 1), "poi è luce normale");
  assert.ok(about(sleepAt(1).light, 1));
  assert.ok(about(sleepAt(1).halo, 1), "e anche intorno la luce è tornata");
});

test("il taglio cade tra la scena che dorme e quella che si risveglia", () => {
  assert.equal(sleepP(130, 131, true, 130), SLEEP_CUT, "l'ultimo fotogramma della scena che dorme è il taglio");
  assert.equal(sleepP(0, 131, false, 0), SLEEP_CUT, "il primo della scena dopo è lo stesso punto");
  assert.ok(Math.abs(sleepP(130 - Math.round(SLEEP_CUT * 131), 131, true, 130)) < 0.01, "la finestra comincia SLEEP_CUT·len prima del taglio");
  assert.ok(about(sleepAt(sleepP(300, 131, false, 0)).light, 1), "molto dopo il risveglio il display è normale");
});

test("il titolo di prima lascia il posto a quello nuovo, che si scrive sulla battuta dopo lo spostamento", () => {
  assert.equal(sleepAt(0.1).title, 1, "finché il display cala, il titolo di prima è lì");
  assert.equal(sleepAt(0.25).title, 1, "e resta per tutto lo spostamento");
  assert.equal(sleepAt(TITLE_AT).title, 0, "quando il titolo nuovo comincia a scriversi, quello di prima se n'è andato");
  assert.equal(sleepAt(TITLE_AT).move, 1, "la camera ha finito di spostarsi: la frase si scrive nella battuta dopo");
  assert.ok(TITLE_AT < SLEEP_CUT, "e comunque prima della notifica");
  assert.equal(sleepAt(SLEEP_CUT).title, 0, "dopo il risveglio il titolo di prima non torna");
});

test("la deriva si ferma nel buio e torna piano dopo la notifica", () => {
  assert.equal(sleepAt(0.1).still, 0, "mentre il display cala l'orologio respira ancora");
  assert.equal(sleepAt(0.3).still, 1, "dalla fine del carrello è fermo");
  assert.equal(sleepAt(0.5).still, 1, "e resta fermo per tutto il sonno");
  assert.equal(sleepAt(SLEEP_CUT).still, 1, "anche sul risveglio: qui non deve saltare di posto");
  assert.ok(sleepAt(SLEEP_CUT + 0.13).still < 1, "poi la deriva torna");
  assert.equal(sleepAt(SLEEP_CUT + 0.2).still, 0, "e a un battito dal risveglio è tornata del tutto");
});

test("nel sonno il quadro cala ma non si spegne, e l'alone si stringe verso la cassa", () => {
  assert.equal(sleepAt(0).field, 1, "prima della chiusura il campo dell'atto è pieno");
  assert.ok(about(sleepAt(HUSH).field, FIELD), "ad ambient resta il 18 per cento: la sagoma si deve leggere");
  assert.ok(about(sleepAt(0.4).field, FIELD), "e ci resta per tutto il sonno");
  assert.equal(sleepAt(SLEEP_CUT).field, 1, "sulla notifica il campo torna pieno in un fotogramma");
  assert.equal(sleepAt(0).haloR, 1, "l'alone parte largo");
  assert.ok(about(sleepAt(HUSH).haloR, HALO_R), "e nel sonno si stringe a un terzo");
  assert.equal(sleepAt(SLEEP_CUT).haloR, 1, "sul risveglio si riapre");
});

test("il display respira ad ambient e l'ultimo respiro si chiude sul battito della notifica", () => {
  const luci = [0.2, 0.3, 0.4, 0.5, 0.6, 0.7].map((p) => sleepAt(p).light);
  assert.ok(luci.every(inBreath), "la luce resta dentro la banda del respiro");
  assert.ok(Math.max(...luci) - Math.min(...luci) > 0.02, "e respira davvero, non è una luce ferma");
  assert.ok(sleepAt(SLEEP_CUT - 0.002).light < AMBIENT + 0.005, "l'ultimo respiro è chiuso sul battito");
});

test("il risveglio accende anche la ghiera, per pochi fotogrammi", () => {
  assert.equal(sleepAt(SLEEP_CUT - 0.001).rim, 0, "prima della notifica la ghiera è spenta");
  assert.equal(sleepAt(SLEEP_CUT).rim, 1, "sul battito si accende");
  assert.ok(sleepAt(SLEEP_CUT + 0.015).rim < 0.6, "e si spegne subito");
  assert.equal(sleepAt(SLEEP_CUT + 0.03).rim, 0, "in tre fotogrammi non c'è più");
});

test("con la lunghezza in battiti il display respira sui puntini, ogni volta più forte", () => {
  const L = 6.5, at = (b: number) => SLEEP_CUT + (b - (DOTS_GAP - 1)) / L;
  const l1 = sleepAt(at(-3), L).light, l2 = sleepAt(at(-2), L).light, l3 = sleepAt(at(-1), L).light;
  assert.ok(l1 > AMBIENT && l2 > l1 && l3 > l2, `i tre respiri crescono: ${l1.toFixed(3)} ${l2.toFixed(3)} ${l3.toFixed(3)}`);
  assert.ok(Math.abs(sleepAt(at(-2.5), L).light - AMBIENT) < 1e-9, "fra un respiro e l'altro torna ad ambient");
});

test("senza respiro l'ambient resta a luce ferma fino al taglio (corto, Franz 23/09 14:34)", () => {
  for (let p = HUSH; p < SLEEP_CUT; p += 0.01) {
    assert.ok(about(sleepAt(p, 8, false).light, AMBIENT), `a ${p.toFixed(2)} la luce è ${sleepAt(p, 8, false).light}`);
    assert.ok(about(sleepAt(p, 8, false).halo, HALO), `a ${p.toFixed(2)} l'alone è ${sleepAt(p, 8, false).halo}`);
  }
  assert.ok(sleepAt(SLEEP_CUT, 8, false).light > 1.2, "la notifica riaccende il display come sempre");
});
test("con il respiro, com'è nel film lungo, la luce dell'ambient sale sui puntini", () => {
  const top = Math.max(...Array.from({ length: 60 }, (_, k) => sleepAt(HUSH + (SLEEP_CUT - HUSH) * (k / 60), 8).light));
  assert.ok(top > AMBIENT + 0.01, `il respiro arriva a ${top}`);
});
test("la frase di chi dorme resta accesa fino al taglio solo quando quella dopo non la anticipa, titleLead 0 (revisione del 23/09)", () => {
  assert.equal(titleOnAt(0.3, true, undefined), 0.3);          // film lungo: si spegne col sonno
  assert.equal(titleOnAt(0.3, true, 0), 1);                    // corto: resta fino al taglio
  assert.equal(titleOnAt(0.3, true, 2), 0.3);                  // con un anticipo le due frasi non stanno insieme
  assert.equal(titleOnAt(0.3, false, 0), 1);                   // la scena dopo il sonno
});
