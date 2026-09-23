import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { totalBeats, validateTimeline, watchColumn } from "./timeline.ts";
import { TAKEOVER_CUT } from "./ui/takeover.ts";
import { dollyAt } from "./dolly.ts";
import { BLIND_CUT } from "./ui/blinds.ts";
import { asideAt, contextFill, contextTextAt, fadeOutAt, workBar, workCount } from "./ui/aside.ts";
import { spanFrames } from "./beats.ts";
import { laneArrivals } from "./ui/heroes.ts";
import { BLEED_LIFT, closingAt } from "./moves.ts";
import { END_PACE, contrast, endColors, logoTrack, notesAt } from "./endCard.ts";
import { sfxCues } from "./sound.ts";

const short = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.short.json", import.meta.url), "utf8")));

test("il corto dura 77,5 battiti (42,3 s); la musica parte col primo fotogramma", () => {
  assert.equal(totalBeats(short), 77.5);   // 73,5 fino alla bozza 15: la corsia riprende le sue prime due card (Franz, 23/09 22:17)
  assert.equal(short.musicDelayBeats ?? 0, 0);                         // Franz, 23/09 18:15: la musica parte da subito
});
test("il colpo della musica cade su «Deployed», e lo stop sul «yes» (Franz, 23/09 11:28 e 22:17)", () => {
  // taglio 0-1 0-4 3-11 40-45 (23/09 22:17): una battuta d'attacco, l'introduzione 0-3, la battuta 3 ancora (quella che nel
  // brano precede il colpo), il colpo (la battuta 4) al 24. Due stop, a 2,25-2,75 di ogni battuta 3: il primo mentre il «yes»
  // riempie il quadro, il secondo 1,75 battiti prima del colpo, mentre nella corsia c'è la card dei controlli
  const answer = byId("answer"), loop = byId("loop");
  const fl = (loop.fx ?? []).find((f) => f.kind === "float") as { at: number; len: number; width?: number; cards: { text?: string; lines?: string[]; kind?: string; hold?: number }[] };
  const drop = (short.musicDelayBeats ?? 0) + 4 + 5 * 4, stop = 4 + 3 * 4 + 2.25;
  const burst = loop.at - TAKEOVER_CUT * answer.takeover!.len, filled = burst + 0.42 * answer.takeover!.len;   // takeoverAt: cresce in 0-0,42
  assert.equal(drop, 24);
  assert.ok(burst <= stop && stop <= filled, `il «yes» cresce da ${burst} a ${filled}, lo stacco è al ${stop}`);
  const k = fl.cards.findIndex((c) => c.text?.startsWith("Deployed"));
  const arrive = loop.at + fl.at + laneArrivals(fl.cards, fl.width ?? 560)[k] * fl.len;
  assert.ok(Math.abs(arrive - drop) <= 0.05, `«Deployed» arriva in evidenza al battito ${arrive.toFixed(2)}, il colpo è al ${drop}`);
});
const byId = (id: string) => short.scenes.find((s) => s.id === id)!;
test("il dito preme quando la voce ha finito", () => {
  const answer = byId("answer");
  const ob = (answer.fx ?? []).find((f) => f.kind === "optionsBuild") as { at: number; len: number; pressAt: number };
  const squash = answer.at + ob.at + ob.pressAt - 0.04 * ob.len;                              // optionsBuildAt: il tasto si schiaccia 0,04 prima
  const speaks = byId("speaks");
  const v = (speaks.fx ?? []).find((f) => f.kind === "spoken") as { at: number; len: number };
  const voiceEnd = speaks.at + v.at + v.len;
  assert.ok(voiceEnd <= answer.at + 4.5, `la voce finisce al battito ${voiceEnd}`);
  assert.ok(squash >= voiceEnd, `il dito preme al battito ${squash}, la voce finisce al ${voiceEnd}`);
});
test("la corsia riprende le card del film lungo, e dettatura, voce e invio restano agli stessi intervalli dal colpo (Franz, 23/09 22:17)", () => {
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  type F = { kind: string; at: number; len: number; cards: { kind?: string; text?: string; lines?: string[]; hold?: number; dictation?: { tap: number } }[] };
  const lf = (long.scenes.find((s) => s.id === "loop")!.fx ?? []).find((f) => f.kind === "float") as F;
  const loop = byId("loop"), drop = 24;
  const fl = (loop.fx ?? []).find((f) => f.kind === "float") as F;
  const say = (loop.fx ?? []).find((f) => f.kind === "spoken") as { at: number };
  const strip = (c: F["cards"][number]) => ({ ...c, hold: undefined });
  assert.deepEqual(fl.cards.slice(0, 2).map(strip), lf.cards.slice(0, 2).map(strip));   // «It keeps you in the loop.» e i controlli
  assert.equal(loop.at + fl.at + fl.len, drop + 12);
  assert.equal(loop.at + say.at, drop + 4);
  assert.equal(loop.at + fl.cards.find((c) => c.dictation)!.dictation!.tap, drop + 3.5);
  assert.equal(loop.at + loop.takeover!.press!, drop + 12.5);
  assert.equal(loop.at + loop.len, drop + 13);
});
test("gli avvisi del cartello si leggono, e al fotogramma 0 c'è il quadrante senza testo", () => {
  const end = byId("end");
  assert.equal(end.endPace, "blinds");
  assert.equal(end.logoCutout, true);
  assert.equal(end.strapBleed, true);
  // gli avvisi restano 4 battiti, 2,2 s: col nome dopo il logo erano scesi a 1,4 s (Franz, 23/09 19:58)
  assert.ok(end.len - notesAt(end.endPace) >= 4, `gli avvisi restano ${end.len - notesAt(end.endPace)} battiti`);
  // il nome arriva quando il logo si è già posato sull'orologio, non sopra l'arco grande (Franz, 23/09 18:55)
  const scale = closingAt(END_PACE[end.endPace!].start, BLEED_LIFT).pose.scale;
  assert.ok(scale <= 0.7, `quando arriva il nome l'orologio è a scala ${scale.toFixed(2)}`);
  assert.equal(short.scenes[0].id, "face");
  assert.equal(short.scenes[0].text, undefined);
});

test("il terminale del corto è quello del film lungo, per intero: stesse righe agli stessi battiti (Franz, 23/09 07:10)", () => {
  // nella bozza 2 il terminale durava 6 battiti e le righe arrivavano al doppio della velocità: metà non si vedeva
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  const a = long.scenes.find((s) => s.id === "watch")!, b = byId("watch");
  const { at: _a, len: _la, ...lungo } = a, { at: _b, len: _lb, ...corto } = b;
  // la clip sì: nel corto il tasto Write resta fermo in fondo invece di sobbalzare a ogni riga nuova (Franz, 23/09 18:58);
  // n_watch_pinned.mp4 è n_watch_fit.mp4 passata da tools/promo/pin_button.py, stessi fotogrammi e stessa durata
  // e acceso dal primo fotogramma, senza righe finché il PC non le scrive (Franz, 23/09 22:17): n_watch_on.mp4 è
  // n_watch_pinned.mp4 passata da tools/promo/screen_on.py
  assert.equal(corto.watch!.clip, "scenes/n_watch_on.mp4");
  assert.equal(lungo.watch!.clip, "scenes/n_watch_fit.mp4");
  // e l'esito: il corto chiude il lavoro con la riga che poi vola nell'orologio (piano 4, volo del terminale, 23/09)
  type Term = { kind: string; lines: string[]; times: number[] };
  const tc = (corto.fx ?? []).find((f) => f.kind === "terminalPlane") as Term, tl = (lungo.fx ?? []).find((f) => f.kind === "terminalPlane") as Term;
  assert.deepEqual(tc.lines, [...tl.lines, "⏺ Released 2.8.0 and tagged v2.8.0"]);
  assert.deepEqual(tc.times, [...tl.times, 9.5]);
  // e l'orologio: nel corto non sfuma, sfuma il suo schermo nel grigio del terminale (piano 6, Franz 23/09 21:13)
  assert.equal(corto.watch!.fadeOut, undefined);
  assert.equal(corto.watch!.screenFade, 1);
  assert.equal(lungo.watch!.fadeOut, 1.5);
  const senza = (s: typeof corto) => ({ ...s, watch: { ...s.watch!, clip: "", fadeOut: undefined, screenFade: undefined }, fx: (s.fx ?? []).map((f) => (f.kind === "terminalPlane" ? { ...f, lines: [], times: [] } : f)) });
  assert.deepEqual(senza(corto), senza(lungo));
  // la scena finisce con il terminale (5 + 5,5 battiti): il film lungo lo tiene fermo 1,5 battiti in più, che nel corto
  // servono a «Every session, at a glance.» (5 parole, 4,5 battiti)
  const term = (b.fx ?? []).find((f) => f.kind === "terminalPlane") as { at: number; len: number };
  assert.ok(b.len >= term.at + term.len, `il terminale finisce al battito ${term.at + term.len}, la scena ne dura ${b.len}`);
});


test("l'apertura: il quadrante con la complication, poi la notifica, «Claude has a question.» e «Hear it out.» (Franz, 23/09 21:13-21:24)", () => {
  const face = byId("face"), asks = byId("asks"), speaks = byId("speaks");
  assert.equal(face.at, 0);
  assert.equal(face.watch!.clip, "scenes/n_face.mp4");                  // il quadrante pulito, senza «payments-api» (21:24)
  assert.equal(face.sleep, undefined);                                   // acceso, non in ambient
  assert.equal(asks.at, face.at + face.len);
  assert.deepEqual(asks.text?.lines, ["Claude has", "a question."]);
  assert.equal(asks.text?.accent, "question.");
  assert.deepEqual(speaks.text?.lines, ["Hear it out."]);
  const tap = (speaks.fx ?? []).find((f) => f.kind === "tap") as { at: number };
  // il tocco su ▶ arriva presto: erano 8 battiti dalla notifica (Franz, 21:13: «ridurre il tempo tra notifica e play»)
  assert.ok(speaks.at + tap.at - asks.at <= 5, `il tocco su ▶ arriva ${speaks.at + tap.at - asks.at} battiti dopo la notifica`);
  for (const id of ["wake", "title"]) assert.equal(short.scenes.find((s) => s.id === id), undefined, `c'è ancora «${id}»`);
});
test("la camera passa da una scena all'altra senza scatti: quadrante, notifica, tocco su ▶", () => {
  const [a, b, c] = ["face", "asks", "speaks"].map(byId);
  // lo stesso orologio: il quadrante sta già dove arriverà la notifica, non al centro (Franz, 23/09 22:08)
  for (const s of [a, b, c]) assert.equal(watchColumn(s), watchColumn(b), `${s.id} sposta l'orologio`);
  for (const s of [a, b]) assert.equal(s.watch!.exit, undefined, `${s.id} fa uscire l'orologio`);
  for (const s of [b, c]) assert.equal(s.watch!.enter, undefined, `${s.id} fa entrare l'orologio`);
  assert.equal(dollyAt(a.watch!.dolly, 1), dollyAt(b.watch!.dolly, 0));
  assert.equal(dollyAt(b.watch!.dolly, 1), dollyAt(c.watch!.dolly, 0));
});
test("la carrellata: lista con l'esito, «Work», «Open questions», «Context», con tre blink allo stesso passo (Franz, 23/09 14:23 e 21:13)", () => {
  const list = byId("list"), work = byId("work"), q = byId("questions"), ctx = byId("context");
  assert.equal(list.at, 47.5);
  assert.equal(list.at + list.len, work.at);
  assert.equal(work.at + work.len, q.at);
  assert.equal(q.at + q.len, ctx.at);
  assert.equal(q.at - work.at, ctx.at - q.at, "i blink allo stesso passo");
  assert.equal(list.out, "lids");                                    // la frase se ne va prima delle palpebre
  for (const s of [work, q]) {
    assert.equal(s.out, "blink");
    assert.equal(s.text, undefined);                                 // senza frase il blink sono le sole palpebre
  }
  for (const b of [work.at, q.at, ctx.at]) assert.ok(Number.isInteger(b), `il blink al battito ${b} non cade su un battito`);
  for (const s of [list, work, q, ctx]) assert.equal(watchColumn(s), watchColumn(list), `${s.id} sposta l'orologio`);
  const card = (list.fx ?? []).find((f) => f.kind === "doneCard") as { slot?: number; text: string; at: number };
  assert.equal(card.slot, 146);                                      // la riga di payments-api in n_list.mp4 a 11,6 s
  // l'esito c'è dal primo fotogramma della lista: prima col volo del terminale, poi con la card ✓ (piano 4, 23/09)
  const volo = (list.fx ?? []).find((f) => f.kind === "takeIn") as { at: number; len: number } | undefined;
  assert.equal(volo ? volo.at : card.at, 0);
  assert.match(card.text, /^Released 2\.8\.0/);
  assert.equal(list.watch!.clip, "scenes/n_list.mp4");
  assert.equal(list.watch!.clipStart, 11.6);
  assert.equal(list.watch!.freeze, true);
  for (const s of [work, q, ctx]) assert.equal(s.watch!.clip, "scenes/n_overview_fit.mp4");
  // la Panoramica scorre com'è nella registrazione (Franz, 23/09 18:32): «Work» scatta in vista a 6,0 s, «Open questions» a
  // 8,5 s, «Context» a 10,5 s; ciascuna arriva nel primo mezzo battito della sua scena, mentre le palpebre si riaprono
  const half = 0.5 * 60 / short.bpm;
  for (const [s, snap] of [[work, 6.0], [q, 8.5], [ctx, 10.5]] as const) {
    assert.notEqual(s.watch!.freeze, true, `${s.id} è ferma`);
    assert.ok(snap - s.watch!.clipStart! > 0 && snap - s.watch!.clipStart! <= half, `${s.id}: la scheda arriva ${(snap - s.watch!.clipStart!).toFixed(2)} s dopo il taglio`);
  }
  for (const id of ["title", "glance", "done", "shipped"]) assert.equal(short.scenes.find((s) => s.id === id), undefined, `c'è ancora «${id}»`);
});
test("il terzo blink porta «Open questions» con la sua scheda a sinistra, come nel film lungo (Franz, 23/09 21:13)", () => {
  const long = validateTimeline(JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8")));
  type Q = { kind: string; at: number; len: number; panel: string; n?: number; note?: string; quote?: string; fadeIn?: number; draw?: number };
  const lq = long.scenes.flatMap((s) => (s.fx ?? []) as Q[]).find((f) => f.kind === "aside" && f.panel === "questions")!;
  const q = byId("questions");
  const a = ((q.fx ?? []) as Q[]).find((f) => f.kind === "aside");
  assert.ok(a, "la scena Open questions ha la sua scheda");
  assert.deepEqual([a.panel, a.n, a.note, a.quote], [lq.panel, lq.n, lq.note, lq.quote]);
  assert.deepEqual([a.at, a.len, a.fadeIn, a.draw], [0, q.len, 0.2, 0.5]);
});
test("la scheda Context si legge con i valori finali per almeno 0,9 battiti prima che sfumi (revisione del 23/09, scelta B di Franz)", () => {
  // si contano i fotogrammi in cui i tre valori sono già quelli finali e scheda e testo sono pieni, con le stesse funzioni
  // che usa il componente (ui/aside.ts): il test di prima misurava dall'ingresso alla tapparella e passava anche quando
  // i valori finali non si vedevano mai
  const g = { bpm: short.bpm, fps: short.fps, offsetSeconds: short.offsetSeconds };
  const ctx = byId("context"), after = byId("slogan");   // dopo il Context lo slogan su nero (23/09 21:13), prima era il cartello
  const aside = (ctx.fx ?? []).find((f) => f.kind === "aside") as { at: number; len: number; panel: string; out?: string; rows: { pct: number }[]; fadeIn?: number; draw?: number };
  assert.equal(aside.panel, "context");
  assert.equal(aside.out, "bars");
  assert.equal(aside.rows.length, 3);
  assert.equal(ctx.at + ctx.len, after.at);
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
test("l'accordo finale cade quando nasce il logo, dopo lo slogan, e si spegne quando arriva il nome (Franz, 23/09 19:43 e 21:13)", () => {
  // taglio 0-1 0-4 3-11 40-45: 4 battiti d'attacco, le battute 0-3, ancora la 3 e le 4-10, le 40-42 che portano al finale, il finale piano
  // (43) sotto lo slogan e l'accordo (44), che si spegne in una battuta. Il taglio sta nel comando di cut_track.py: qui si
  // tiene il conto delle battute, la traccia vera si misura sulla resa (accordo al 68, silenzio dal 72)
  const slogan = byId("slogan"), end = byId("end");
  const phrase = (short.musicDelayBeats ?? 0) + 4 + (4 + 8 + 3) * 4;
  assert.equal(phrase, 64);
  assert.ok(phrase >= slogan.at && phrase + 4 <= slogan.at + slogan.len, "il finale piano sta sotto lo slogan");
  assert.equal(phrase + 4, end.at, "l'accordo sul logo");
  assert.equal(phrase + 8, end.at + END_PACE[end.endPace!].start, "si spegne quando arriva il nome");
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
test("il finale su nero: la tapparella va nel nero, lo slogan, poi il cartello su nero come nel film lungo (Franz, 23/09 21:13)", () => {
  const ctx = byId("context"), slogan = byId("slogan"), end = byId("end");
  assert.equal(ctx.blinds?.to, "black");
  assert.equal(slogan.at, ctx.at + ctx.len);
  assert.equal(slogan.act, "close");
  assert.equal(slogan.watch, undefined);
  assert.deepEqual(slogan.text?.lines, ["Claude Code,", "on your wrist."]);
  assert.equal(slogan.text?.accent, "wrist.");
  assert.equal(end.at, slogan.at + slogan.len);
  assert.deepEqual(short.palette?.close, ["#000000", "#000000", "#000000", "rgb(0,0,0)"]);
  assert.equal(end.endTone, undefined);
  const c = endColors(end.endTone);
  for (const [name, col, min] of [["titolo", c.title, 7], ["Open source.", c.accent, 4.5]] as const)
    assert.ok(contrast(col, "#000000") >= min, `${name} ${col}: contrasto ${contrast(col, "#000000").toFixed(1)}:1`);
  assert.equal(logoTrack(end.endTone), logoTrack());
});
test("anche il blink di sole palpebre, dalla lista a «Work», ha lo scatto sul taglio (revisione del 23/09)", () => {
  const list = byId("list");
  assert.equal(list.out, "lids");
  assert.ok(sfxCues(short).some((c) => c.name === "shutter" && c.beat === list.at + list.len), "scatto al battito " + (list.at + list.len));
});
test("fra il terminale e la lista il terminale entra nell'orologio e diventa la card ✓ (piano 4, volo; Franz, 23/09 20:15)", () => {
  type TakeIn = { kind: "takeIn"; at: number; len: number; slot: number; name: string; age: string; text: string; badge?: string };
  type Done = { kind: "doneCard"; at: number; slot?: number; name: string; age: string; text: string; badge?: string };
  const list = byId("list"), watch = byId("watch");
  const t = (list.fx ?? []).find((f) => f.kind === "takeIn") as TakeIn | undefined;
  assert.ok(t, "la lista si apre col volo");
  assert.equal(t.at, 0);
  assert.equal(t.len, 2);
  const d = (list.fx ?? []).find((f) => f.kind === "doneCard") as Done;
  assert.equal(d.at, t.at + t.len, "la card ✓ prende il posto del volo quando arriva");
  assert.deepEqual([d.slot, d.name, d.age, d.text, d.badge], [t.slot, t.name, t.age, t.text, t.badge]);
  const lines = ((watch.fx ?? []).find((f) => f.kind === "terminalPlane") as { lines: string[] }).lines;
  assert.equal("⏺ " + t.text, lines[lines.length - 1], "vola l'ultima riga del terminale");
});
