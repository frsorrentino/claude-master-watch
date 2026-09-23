import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline } from "./timeline.ts";

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
