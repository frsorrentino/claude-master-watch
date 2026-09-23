import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 72 battiti, con la musica fino all'ultimo fotogramma", () => assert.equal(totalBeats(short), 72));
test("la pressione del «yes» cade sull'attacco della parte forte, al battito 24", () => {
  const answer = short.scenes.find((s) => s.id === "answer")!;
  const press = (answer.fx ?? []).find((f) => f.kind === "longPress")!;
  assert.equal(answer.at + press.at, 24);
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
test("l'anello della pressione parte sull'attacco (battito 24) e la voce finisce prima, così la musica entra piena", () => {
  const answer = byId("answer");
  const ob = (answer.fx ?? []).find((f) => f.kind === "optionsBuild") as { at: number; pressAt: number };
  assert.equal(answer.at + ob.at + ob.pressAt, 24);
  const speaks = byId("speaks");
  const v = (speaks.fx ?? []).find((f) => f.kind === "spoken") as { at: number; len: number };
  assert.ok(speaks.at + v.at + v.len <= 23.5, `la voce finisce al battito ${speaks.at + v.at + v.len}`);
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
