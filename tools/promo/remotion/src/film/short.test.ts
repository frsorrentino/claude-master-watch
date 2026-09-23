import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";
import { TAKEOVER_CUT } from "./ui/takeover.ts";
import { HUSH, SLEEP_CUT } from "./ui/sleep.ts";
import { dollyAt } from "./dolly.ts";
import { BLIND_CUT } from "./ui/blinds.ts";
import { notesAt } from "./endCard.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 71 battiti (38,7 s), con la musica fino all'ultimo fotogramma", () => assert.equal(totalBeats(short), 71));
test("il colpo della musica cade sull'entrata della corsia, non sulla pressione: la card «Deployed» compare sul colpo (Franz, 23/09 11:28)", () => {
  // taglio 0-1 0-11 9-10 11-12 43-45 dal battito 7: cinque battute d'introduzione, il colpo (la battuta 4 della traccia) al 27.
  // Lo stacco (la musica si ferma a 2,25-2,75 dell'ultima battuta d'introduzione) cade mentre il «yes» riempie il quadro
  const answer = byId("answer"), loop = byId("loop");
  const fl = (loop.fx ?? []).find((f) => f.kind === "float") as { at: number };
  const drop = (short.musicDelayBeats ?? 0) + 5 * 4, stop = drop - 4 + 2.25;
  const burst = loop.at - TAKEOVER_CUT * answer.takeover!.len, filled = burst + 0.42 * answer.takeover!.len;   // takeoverAt: cresce in 0-0,42
  assert.equal(drop, 27);
  assert.equal(loop.at + fl.at, drop);
  assert.ok(loop.at < drop && drop - loop.at <= 1, `la corsia entra al battito ${loop.at}, il colpo è al ${drop}`);
  assert.ok(burst <= stop && stop <= filled, `il «yes» cresce da ${burst} a ${filled}, lo stacco è al ${stop}`);
});

const byId = (id: string) => short.scenes.find((s) => s.id === id)!;
test("il dito preme quando la voce ha finito", () => {
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
  assert.equal(loop.at + fl.at, 27);
  assert.equal(loop.at + fl.at + fl.len, 39);
  assert.equal(loop.at + say.at, 31);
  assert.equal(loop.at + fl.cards.find((c) => c.dictation)!.dictation!.tap, 30.5);
  assert.equal(loop.at + loop.takeover!.press!, 39.5);
  assert.equal(loop.at + loop.len, 40);
});
test("gli avvisi del cartello si leggono e «It asks.» non è già scritta al fotogramma 0", () => {
  const end = byId("end");
  assert.equal(end.endPace, "blinds");
  assert.equal(end.logoCutout, true);
  assert.equal(end.strapBleed, true);
  assert.ok(end.len - notesAt(end.endPace) >= 4, `gli avvisi restano ${end.len - notesAt(end.endPace)} battiti`);
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


test("l'apertura: il titolo sull'orologio in ambient a luce ferma, poi la notifica e «It asks.» (Franz, 23/09 14:23)", () => {
  const wake = byId("wake"), asks = byId("asks"), speaks = byId("speaks");
  assert.deepEqual(wake.text?.lines, ["Claude Code,", "on your wrist."]);
  assert.equal(wake.sleep?.breath, false);
  assert.equal(wake.sleep?.titleLead, 0);
  // già ad ambient al fotogramma 0: la finestra del sonno comincia prima del film e il display cala entro HUSH (ui/sleep.ts)
  const windowStart = wake.at + wake.len - SLEEP_CUT * wake.sleep!.len;
  assert.ok(windowStart + HUSH * wake.sleep!.len <= 0, `il display arriva ad ambient al battito ${windowStart + HUSH * wake.sleep!.len}`);
  assert.equal(asks.at, wake.at + wake.len);
  assert.ok(speaks.at + (speaks.text?.at ?? 0) - asks.at <= 5, "da «It asks.» a «It speaks.» al più 5 battiti");
  assert.equal(short.scenes.find((s) => s.id === "title"), undefined);
});
test("la camera passa da una scena all'altra senza scatti: apertura, «It asks.», «It speaks.»", () => {
  const [a, b, c] = ["wake", "asks", "speaks"].map(byId);
  assert.equal(dollyAt(a.watch!.dolly, 1), dollyAt(b.watch!.dolly, 0));
  assert.equal(dollyAt(b.watch!.dolly, 1), dollyAt(c.watch!.dolly, 0));
});
test("la carrellata: lista con l'esito, «Work», «Context», con i blink fra l'una e l'altra (Franz, 23/09 14:23)", () => {
  const list = byId("list"), work = byId("work"), ctx = byId("context");
  assert.equal(list.at, 50.5);
  assert.equal(list.at + list.len, work.at);
  assert.equal(work.at + work.len, ctx.at);
  assert.equal(list.out, "lids");                                    // la frase se ne va prima delle palpebre
  assert.equal(work.out, "blink");
  assert.equal(work.text, undefined);                                // senza frase il blink sono le sole palpebre
  for (const b of [work.at, ctx.at]) assert.ok(Number.isInteger(b), `il blink al battito ${b} non cade su un battito`);
  for (const s of [list, work, ctx]) assert.equal(watchColumn(s), watchColumn(list), `${s.id} sposta l'orologio`);
  const card = (list.fx ?? []).find((f) => f.kind === "doneCard") as { slot?: number; text: string; at: number };
  assert.equal(card.slot, 146);                                      // la riga di payments-api in n_list.mp4 a 11,6 s
  assert.equal(card.at, 0);
  assert.match(card.text, /^Released 2\.8\.0/);
  assert.equal(list.watch!.clip, "scenes/n_list.mp4");
  assert.equal(list.watch!.clipStart, 11.6);
  assert.equal(list.watch!.freeze, true);
  for (const s of [work, ctx]) assert.equal(s.watch!.clip, "scenes/n_overview_fit.mp4");
  assert.ok(Math.abs(ctx.watch!.clipStart! - 10.64) <= 0.2, "sul display la scheda Context, dove cade nel film lungo (10,64 s)");
  for (const id of ["title", "glance", "done", "shipped"]) assert.equal(short.scenes.find((s) => s.id === id), undefined, `c'è ancora «${id}»`);
});
test("la scheda Context si legge prima che parta la tapparella, e le sue 3 barre diventano i listelli", () => {
  const ctx = byId("context"), end = byId("end");
  const aside = (ctx.fx ?? []).find((f) => f.kind === "aside") as { at: number; panel: string; out?: string; rows: unknown[] };
  assert.equal(aside.panel, "context");
  assert.equal(aside.out, "bars");
  assert.equal(aside.rows.length, 3);
  assert.equal(ctx.blinds?.len, 6);
  const blindStart = ctx.at + ctx.len - BLIND_CUT * ctx.blinds!.len;
  assert.ok(blindStart - (ctx.at + aside.at) >= 2, `la scheda resta ${blindStart - (ctx.at + aside.at)} battiti prima della tapparella`);
  assert.equal(ctx.at + ctx.len, end.at);
});
test("il taglio sul cartello cade sull'inizio del finale della musica, e il film finisce con la musica", () => {
  // taglio 0-1 0-11 9-10 11-12 43-45 dal battito 7: 14 battute prima del finale (43-44), che dura 2 battute
  const end = byId("end");
  assert.equal((short.musicDelayBeats ?? 0) + (1 + 11 + 1 + 1) * 4, end.at);
  assert.equal((short.musicDelayBeats ?? 0) + (1 + 11 + 1 + 1 + 2) * 4, totalBeats(short));
});
