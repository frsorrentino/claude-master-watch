import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 72 battiti: 68 di racconto e musica, 4 di silenzio sul cartello", () => assert.equal(totalBeats(short), 72));
test("la pressione del «yes» cade sull'attacco della parte forte, al battito 24", () => {
  const answer = short.scenes.find((s) => s.id === "answer")!;
  const press = (answer.fx ?? []).find((f) => f.kind === "longPress")!;
  assert.equal(answer.at + press.at, 24);
  assert.equal((short.musicDelayBeats ?? 0) + 5 * 4, 24);   // 5 battute d'introduzione
});
test("la seconda vibrazione cade sulla battuta quieta (52) e «Shipped.» sulla ripresa (56)", () => {
  assert.equal(short.scenes.find((s) => s.id === "done")!.at, 52);
  assert.equal(short.scenes.find((s) => s.id === "shipped")!.at, 56);
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
