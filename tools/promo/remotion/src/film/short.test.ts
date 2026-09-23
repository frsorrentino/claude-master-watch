import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";
import { TAKEOVER_CUT } from "./ui/takeover.ts";
import { HUSH, SLEEP_CUT } from "./ui/sleep.ts";
import { dollyAt } from "./dolly.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 75 battiti dopo l'apertura nuova; il piano 3 lo porta a 71 con la carrellata (Task 2)", () => assert.equal(totalBeats(short), 75));
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
test("sul polso le schermate vanno in fila: la card del rilascio sale sulla lista delle sessioni, senza tornare a «Deployed» (Franz, 23/09 13:01)", () => {
  // nella bozza 5 il display passava dalla lista alla card «Deployed» (notizia di prima del terminale) e poi a «Released»
  const glance = byId("glance"), done = byId("done"), shipped = byId("shipped");
  const listEnd = glance.watch!.clipStart! + (glance.len * 60) / short.bpm;     // dove la lista è arrivata quando il titolo esce
  for (const s of [done, shipped]) {
    assert.equal(s.watch!.clip, glance.watch!.clip, `${s.id} mostra ${s.watch!.clip}`);
    assert.equal(s.watch!.freeze, true);
    assert.ok(Math.abs(s.watch!.clipStart! - listEnd) <= 1 / 30, `${s.id} riparte dalla lista a ${s.watch!.clipStart} s, la lista era a ${listEnd.toFixed(3)} s`);
  }
});

test("la Panoramica torna nel corto: dopo il titolo, a sinistra, la scheda Context mentre il display mostra la stessa scheda (Franz, 23/09 13:25)", () => {
  // come nel film lungo (Franz, 18/09 20:33): la scheda non esce dal polso, compare ferma a sinistra quando sul display passa
  // la sua scheda, e lì le barre si riempiono. Il titolo ha prima i suoi 4,5 battiti (5 parole a mezzo battito, più 2).
  const glance = byId("glance");
  const aside = (glance.fx ?? []).find((f) => f.kind === "aside") as { at: number; len: number; panel: string; out?: string };
  assert.equal(aside.panel, "context");
  assert.equal(aside.out, undefined);                                        // nel corto non c'è la tapparella che la chiude
  assert.ok(aside.at >= 4.5 && aside.len >= 3.5 && aside.at + aside.len <= glance.len, `scheda a ${aside.at} per ${aside.len}, scena di ${glance.len}`);
  assert.equal(glance.watch!.clip, "scenes/n_overview_fit.mp4");
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  const limits = long.scenes.find((s) => s.id === "limits")!;
  const lf = (limits.fx ?? []).find((f) => f.kind === "aside" && (f as { panel: string }).panel === "context") as { at: number };
  const clipAt = (w: { clipStart?: number; hold?: number; rate?: number }, beat: number) => (w.clipStart ?? 0) + Math.max(0, beat - (w.hold ?? 0)) * (60 / short.bpm) * (w.rate ?? 1);
  const inLong = clipAt(limits.watch!, lf.at), inShort = clipAt(glance.watch!, aside.at);
  assert.ok(Math.abs(inShort - inLong) <= 0.5, `sul display la scheda Context è a ${inLong.toFixed(2)} s della clip, nel corto la scheda compare a ${inShort.toFixed(2)} s`);
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
