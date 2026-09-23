import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";
import { TAKEOVER_CUT } from "./ui/takeover.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 72 battiti, con la musica fino all'ultimo fotogramma", () => assert.equal(totalBeats(short), 72));
test("il «yes» esplode sul culmine della musica, al battito 24: lì l'anello si chiude e parte l'espansione (Franz, 23/09 10:42)", () => {
  // prima il dito cadeva sull'attacco e l'espansione arrivava due battiti dopo, a musica già partita
  const answer = short.scenes.find((s) => s.id === "answer")!, next = short.scenes.find((s) => s.id === "loop")!;
  const ob = (answer.fx ?? []).find((f) => f.kind === "optionsBuild") as { at: number; len: number; pressAt: number };
  const press = (answer.fx ?? []).find((f) => f.kind === "longPress")!;
  const ringStart = answer.at + ob.at + ob.pressAt, ringEnd = ringStart + 0.12 * ob.len;   // optionsBuildAt: l'anello corre 0,12 dell'arco
  const burst = next.at - TAKEOVER_CUT * answer.takeover!.len;                                // Film.tsx: dove parte l'espansione
  assert.ok(ringEnd >= 24 && ringEnd <= 24.1, `l'anello si chiude al battito ${ringEnd}`);
  // l'espansione parte fino a 0,2 battiti prima: la crescita comincia piano (growEase), sul 24 il tasto è ancora al 3 % e si
  // gonfia davvero subito dopo, sul colpo; e il takeover non può durare meno di 2,5 battiti
  assert.ok(burst >= 23.8 && burst <= 24.1, `l'espansione parte al battito ${burst}`);
  assert.equal(answer.at + press.at, ringStart);                                              // il suono della pressione parte con l'anello
  assert.equal((short.musicDelayBeats ?? 0) + 5 * 4, 24);   // 5 battute d'introduzione
});
test("la seconda vibrazione cade sulla battuta quieta (56) e «Shipped.» sulla ripresa (60)", () => {
  // taglio della musica 0-1 0-11 9-10 11-13 43-45: 13 battute prima di quella quieta (la 11 della traccia), poi la ripresa
  // (la 12) e il finale (43-44) sotto il cartello, fino all'ultimo battito
  assert.equal(short.scenes.find((s) => s.id === "done")!.at, 56);
  assert.equal(short.scenes.find((s) => s.id === "shipped")!.at, 60);
  assert.equal((short.musicDelayBeats ?? 0) + (1 + 11 + 1) * 4, 56);
  assert.equal((short.musicDelayBeats ?? 0) + (1 + 11 + 1 + 2 + 2) * 4, totalBeats(short));
});

const byId = (id: string) => short.scenes.find((s) => s.id === id)!;
test("il dito preme quando la voce ha finito, e la musica entra piena al 24", () => {
  const answer = byId("answer");
  const ob = (answer.fx ?? []).find((f) => f.kind === "optionsBuild") as { at: number; len: number; pressAt: number };
  const squash = answer.at + ob.at + ob.pressAt - 0.04 * ob.len;                              // optionsBuildAt: il tasto si schiaccia 0,04 prima
  const speaks = byId("speaks");
  const v = (speaks.fx ?? []).find((f) => f.kind === "spoken") as { at: number; len: number };
  const voiceEnd = speaks.at + v.at + v.len;
  assert.ok(voiceEnd <= 23.5, `la voce finisce al battito ${voiceEnd}`);
  assert.ok(squash >= voiceEnd, `il dito preme al battito ${squash}, la voce finisce al ${voiceEnd}`);
});
test("la corsia comincia dopo l'espansione, ma card, voce, dettatura e invio restano ai loro battiti", () => {
  const loop = byId("loop");
  const fl = (loop.fx ?? []).find((f) => f.kind === "float") as { at: number; len: number; cards: { dictation?: { tap: number } }[] };
  const say = (loop.fx ?? []).find((f) => f.kind === "spoken") as { at: number };
  assert.equal(loop.at + fl.at, 28);
  assert.equal(loop.at + fl.at + fl.len, 40);
  assert.equal(loop.at + say.at, 32);
  assert.equal(loop.at + fl.cards.find((c) => c.dictation)!.dictation!.tap, 31.5);
  assert.equal(loop.at + loop.takeover!.press!, 40.5);
  assert.equal(loop.at + loop.len, 41);
});
test("attorno alla card ✓ l'orologio resta nella stessa colonna: niente salti ai tagli", () => {
  assert.equal(watchColumn(byId("done")), watchColumn(byId("glance")));
  assert.equal(watchColumn(byId("done")), watchColumn(byId("shipped")));
});
test("gli avvisi del cartello si leggono e «It asks.» non è già scritta al fotogramma 0", () => {
  assert.equal(byId("end").endPace, "compact");
  assert.equal(byId("wake").sleep?.titleLead, 0);
});

test("il terminale del corto è quello del film lungo, per intero: stesse righe agli stessi battiti (Franz, 23/09 07:10)", () => {
  // nella bozza 2 il terminale durava 6 battiti e le righe arrivavano al doppio della velocità: metà non si vedeva
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  const a = long.scenes.find((s) => s.id === "watch")!, b = byId("watch");
  const { at: _a, len: _la, ...lungo } = a, { at: _b, len: _lb, ...corto } = b;
  assert.deepEqual(corto, lungo);
  // la scena finisce con il terminale (5 + 5,5 battiti): il film lungo lo tiene fermo 1,5 battiti in più, che nel corto
  // servono a «Every session, at a glance.» (5 parole, 4,5 battiti)
  const term = (b.fx ?? []).find((f) => f.kind === "terminalPlane") as { at: number; len: number };
  assert.ok(b.len >= term.at + term.len, `il terminale finisce al battito ${term.at + term.len}, la scena ne dura ${b.len}`);
});
