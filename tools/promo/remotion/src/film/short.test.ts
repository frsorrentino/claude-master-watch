import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";
import { TAKEOVER_CUT } from "./ui/takeover.ts";
import { HUSH, SLEEP_CUT } from "./ui/sleep.ts";
import { dollyAt } from "./dolly.ts";
import { BLIND_CUT } from "./ui/blinds.ts";
import { asideAt, contextFill, contextTextAt, fadeOutAt, workBar, workCount } from "./ui/aside.ts";
import { spanFrames } from "./beats.ts";
import { BLEED_LIFT, closingAt } from "./moves.ts";
import { END_PACE, contrast, endColors, logoTrack, notesAt } from "./endCard.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 71 battiti (38,7 s); la musica parte col primo fotogramma", () => {
  assert.equal(totalBeats(short), 71);
  assert.equal(short.musicDelayBeats ?? 0, 0);                         // Franz, 23/09 18:15: la musica parte da subito
  assert.equal(byId("wake").sleep?.hush, false);                         // e il sonno del display non la zittisce
});
test("il colpo della musica cade sull'entrata della corsia, non sulla pressione: la card «Deployed» compare sul colpo (Franz, 23/09 11:28)", () => {
  // taglio 0.25-1 0-1 0-1 0-11 43-45 dal battito 0: tre battiti d'attacco che salgono dal silenzio, sei battute
  // d'introduzione (la 0 tre volte, poi 1-3), il colpo (la battuta 4 della traccia) al 27. Lo stacco (la musica si ferma a
  // 2,25-2,75 dell'ultima battuta d'introduzione) cade mentre il «yes» riempie il quadro
  const answer = byId("answer"), loop = byId("loop");
  const fl = (loop.fx ?? []).find((f) => f.kind === "float") as { at: number };
  const drop = (short.musicDelayBeats ?? 0) + 3 + 6 * 4, stop = drop - 4 + 2.25;
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
  assert.ok(end.len - notesAt(end.endPace) >= 2.5, `gli avvisi restano ${end.len - notesAt(end.endPace)} battiti`);
  // il nome arriva quando il logo si è già posato sull'orologio, non sopra l'arco grande (Franz, 23/09 18:55)
  const scale = closingAt(END_PACE[end.endPace!].start, BLEED_LIFT).pose.scale;
  assert.ok(scale <= 0.7, `quando arriva il nome l'orologio è a scala ${scale.toFixed(2)}`);
  assert.equal(byId("wake").sleep?.titleLead, 0);
});

test("il terminale del corto è quello del film lungo, per intero: stesse righe agli stessi battiti (Franz, 23/09 07:10)", () => {
  // nella bozza 2 il terminale durava 6 battiti e le righe arrivavano al doppio della velocità: metà non si vedeva
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  const a = long.scenes.find((s) => s.id === "watch")!, b = byId("watch");
  const { at: _a, len: _la, ...lungo } = a, { at: _b, len: _lb, ...corto } = b;
  // la clip sì: nel corto il tasto Write resta fermo in fondo invece di sobbalzare a ogni riga nuova (Franz, 23/09 18:58);
  // n_watch_pinned.mp4 è n_watch_fit.mp4 passata da tools/promo/pin_button.py, stessi fotogrammi e stessa durata
  assert.equal(corto.watch!.clip, "scenes/n_watch_pinned.mp4");
  assert.equal(lungo.watch!.clip, "scenes/n_watch_fit.mp4");
  assert.deepEqual({ ...corto, watch: { ...corto.watch!, clip: lungo.watch!.clip } }, lungo);
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
  // la Panoramica scorre com'è nella registrazione (Franz, 23/09 18:32): «Work» scatta in vista a 6,0 s, «Context» a
  // 10,5 s; ciascuna arriva nel primo mezzo battito della sua scena, mentre le palpebre si riaprono
  const half = 0.5 * 60 / short.bpm;
  for (const [s, snap] of [[work, 6.0], [ctx, 10.5]] as const) {
    assert.notEqual(s.watch!.freeze, true, `${s.id} è ferma`);
    assert.ok(snap - s.watch!.clipStart! > 0 && snap - s.watch!.clipStart! <= half, `${s.id}: la scheda arriva ${(snap - s.watch!.clipStart!).toFixed(2)} s dopo il taglio`);
  }
  for (const id of ["title", "glance", "done", "shipped"]) assert.equal(short.scenes.find((s) => s.id === id), undefined, `c'è ancora «${id}»`);
});
test("la scheda Context si legge con i valori finali per almeno 0,9 battiti prima che sfumi (revisione del 23/09, scelta B di Franz)", () => {
  // si contano i fotogrammi in cui i tre valori sono già quelli finali e scheda e testo sono pieni, con le stesse funzioni
  // che usa il componente (ui/aside.ts): il test di prima misurava dall'ingresso alla tapparella e passava anche quando
  // i valori finali non si vedevano mai
  const g = { bpm: short.bpm, fps: short.fps, offsetSeconds: short.offsetSeconds };
  const ctx = byId("context"), end = byId("end");
  const aside = (ctx.fx ?? []).find((f) => f.kind === "aside") as { at: number; len: number; panel: string; out?: string; rows: { pct: number }[]; fadeIn?: number; draw?: number };
  assert.equal(aside.panel, "context");
  assert.equal(aside.out, "bars");
  assert.equal(aside.rows.length, 3);
  assert.equal(ctx.at + ctx.len, end.at);
  const total = spanFrames(g, ctx.at, ctx.len), blindStart = total - Math.round(spanFrames(g, ctx.at, ctx.blinds!.len) * BLIND_CUT);
  const from = spanFrames(g, ctx.at, aside.at), len = spanFrames(g, ctx.at + aside.at, aside.len);
  const fi = aside.fadeIn === undefined ? undefined : spanFrames(g, ctx.at + aside.at, aside.fadeIn);
  const dr = aside.draw === undefined ? undefined : spanFrames(g, ctx.at + aside.at, aside.draw);
  let readable = 0;
  for (let f = 0; f < total; f++) {
    const s = asideAt(f, from, len, fi, dr);
    const final = aside.rows.every((r, k) => Math.round(r.pct * contextFill(s.d, k)) === r.pct);
    if (final && s.enter >= 0.999 && f < blindStart && contextTextAt(f, blindStart) >= 0.999) readable++;
  }
  const beats = readable / (g.fps * 60 / g.bpm);
  assert.ok(beats >= 0.9, `i valori finali si leggono per ${beats.toFixed(2)} battiti`);
});
test("l'ultimo colpo del brano cade quando la tapparella si apre e nasce il logo, e si spegne quando arriva il nome (Franz, 23/09 19:43)", () => {
  // taglio 0.25-1 0-1 0-1 0-11 42-45: 3 battiti d'attacco, 14 battute piene fino alla 42 (quella che nel brano precede il
  // finale), il finale piano (43) sotto la tapparella e il colpo (44), che si spegne in una battuta. Nella bozza 9 il colpo
  // cadeva al 59 e quando nasceva il logo c'era già silenzio. Il taglio sta nel comando di cut_track.py, non nel codice:
  // qui si tiene il conto delle battute, la traccia vera si misura sulla resa (colpo al 63, silenzio dal 67)
  const ctx = byId("context"), end = byId("end");
  const phrase = (short.musicDelayBeats ?? 0) + 3 + (1 + 1 + 11 + 1) * 4;
  assert.equal(phrase + 4, end.at);
  assert.ok(phrase <= ctx.at + ctx.len - ctx.blinds!.len * BLIND_CUT, "il finale piano parte prima della tapparella");
  assert.equal(phrase + 8, end.at + END_PACE[end.endPace!].start, "il colpo si spegne quando arriva il nome");
  assert.ok(totalBeats(short) > phrase + 8, "dopo la musica il cartello resta in silenzio");
});
test("al primo blink c'è la scheda Work a sinistra, come nel film lungo, e si legge prima che se ne vada (Franz, 23/09 19:45)", () => {
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  type Work = { kind: string; at: number; len: number; panel: string; n?: number; note?: string; bars?: number[]; fadeIn?: number; draw?: number };
  const lw = long.scenes.flatMap((s) => (s.fx ?? []) as Work[]).find((f) => f.kind === "aside" && f.panel === "work")!;
  const work = byId("work");
  const a = ((work.fx ?? []) as Work[]).find((f) => f.kind === "aside");
  assert.ok(a, "la scena Work ha la sua scheda");
  assert.equal(a.panel, "work");
  assert.deepEqual([a.n, a.note, a.bars], [lw.n, lw.note, lw.bars]);
  assert.equal(a.at, 0);
  assert.equal(a.at + a.len, work.len, "se ne va col blink");
  // si contano i fotogrammi con numero e barre al valore finale e la scheda piena, con le funzioni del componente
  const g = { bpm: short.bpm, fps: short.fps, offsetSeconds: short.offsetSeconds };
  const from = spanFrames(g, work.at, a.at), len = spanFrames(g, work.at + a.at, a.len);
  const fi = a.fadeIn === undefined ? undefined : spanFrames(g, work.at + a.at, a.fadeIn);
  const dr = a.draw === undefined ? undefined : spanFrames(g, work.at + a.at, a.draw);
  let readable = 0;
  for (let f = 0; f < spanFrames(g, work.at, work.len); f++) {
    const s = asideAt(f, from, len, fi, dr);
    const full = workCount(a.n ?? 1, s.d) === (a.n ?? 1) && (a.bars ?? [1, 1, 1]).every((_, k) => workBar(s.d, k) >= 1);
    if (full && s.enter >= 0.999 && fadeOutAt(f, from + len) >= 0.999) readable++;
  }
  const beats = readable / (g.fps * 60 / g.bpm);
  assert.ok(beats >= 0.8, `la scheda Work si legge per ${beats.toFixed(2)} battiti`);
});
test("il cartello sta su un blu profondo: logo corallo e testi chiari su scuro (Franz, 23/09 18:46)", () => {
  // sull'azzurro della tapparella corallo e scritte stonavano (chiaro su chiaro); sul blu più profondo della prova 2 no
  const end = byId("end");
  assert.deepEqual(short.palette?.close, ["#34568C", "#2A4472", "#223861", "rgb(52,86,140)"]);
  assert.equal(end.endTone, "blue");
  const c = endColors(end.endTone);
  for (const [name, col, min] of [["titolo", c.title, 7], ["avvisi", c.dim, 7], ["Open source.", c.accent, 4.5]] as const)
    assert.ok(contrast(col, "#2A4472") >= min, `${name} ${col}: contrasto ${contrast(col, "#2A4472").toFixed(1)}:1`);
  assert.notEqual(logoTrack(end.endTone), logoTrack());          // la parte vuota dell'arco schiarisce, sul blu non sparisce
});
