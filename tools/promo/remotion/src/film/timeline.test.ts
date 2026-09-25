import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { TimelineError, totalBeats, validateTimeline, watchColumn } from "./timeline.ts";

const base = (): any => ({
  bpm: 100, fps: 30, offsetSeconds: 0,
  scenes: [
    { id: "open", at: 0, len: 8, act: "open", text: { lines: ["Claude is working."], accent: "working." } },
    { id: "list", at: 8, len: 8, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4", enter: "slideIn" },
      text: { lines: ["Every session.", "One glance."], accent: "glance." }, fx: [{ kind: "tap", at: 3, x: 240, y: 300 }] },
  ],
});
const problems = (raw: unknown): string[] => {
  try { validateTimeline(raw); return []; } catch (e) { if (e instanceof TimelineError) return e.problems; throw e; }
};

test("una scaletta giusta passa e dice quanto dura", () => {
  assert.equal(totalBeats(validateTimeline(base())), 16);
});
test("una tavolozza con un colore che non è un colore è un errore", () => {
  const t = base(); t.palette = { act: ["#3A4468", "blu", "#14172A", "rgb(30,35,60)"] };
  assert.match(problems(t).join("\n"), /tavolozza: i colori di «act» non sono 4 colori validi/);
});
test("una tavolozza giusta passa", () => {
  const t = base(); t.palette = { act: ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"] };
  assert.deepEqual(problems(t), []);
});
test("un dolly fuori da 0,8-1,3 è un errore", () => {
  const t = base(); t.scenes[1].watch.dolly = { from: 1, to: 2 };
  assert.match(problems(t).join("\n"), /list: dolly da 1 a 2 fuori da 0,8-1,3/);
});
test("un cartello che finisce prima degli avvisi è un errore (revisione del 23/09: nel corto non comparivano mai)", () => {
  const t = base(); t.scenes[1].endCard = true;
  assert.match(problems(t).join("\n"), /list: gli avvisi del cartello arrivano al battito 13 della scena, che ne dura 8/);
  t.scenes[1].endPace = "compact";
  assert.deepEqual(problems(t), []);
});
test("la frase dopo il sonno non può cominciare prima dell'inizio del film", () => {
  const t = base();
  t.scenes = [
    { id: "wake", at: 0, len: 2, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, sleep: { len: 6 } },
    { id: "asks", at: 2, len: 6, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, text: { lines: ["It asks."], accent: "asks." } },
  ];
  assert.match(problems(t).join("\n"), /asks: la frase comincerebbe al battito -2, prima dell'inizio del film/);
  t.scenes[0].sleep.titleLead = 0;
  assert.deepEqual(problems(t), []);
});
test("un buco o una sovrapposizione tra scene è un errore", () => {
  const t = base(); t.scenes[1].at = 9;
  assert.match(problems(t).join("\n"), /list: inizia al battito 9, la scena prima finisce a 8/);
});
test("la parola in colore deve essere una parola della frase", () => {
  const t = base(); t.scenes[0].text.accent = "sleeping.";
  assert.match(problems(t).join("\n"), /open: «sleeping\.» non è tra le parole/);
});
test("un effetto fuori dalla sua scena è un errore", () => {
  const t = base(); t.scenes[1].fx[0].at = 8;
  assert.match(problems(t).join("\n"), /list: l'effetto tap al battito 8 esce dalla scena/);
});
test("tipo di effetto, vista, movimento e atto sconosciuti sono errori, tutti insieme", () => {
  const t = base(); t.scenes[1].fx[0].kind = "swipe"; t.scenes[1].watch.view = "top"; t.scenes[1].watch.enter = "spin"; t.scenes[0].act = "intro";
  assert.equal(problems(t).length, 4);
});
test("id doppi, battiti che non sono mezzi, tocchi fuori dallo schermo", () => {
  const t = base(); t.scenes[1].id = "open"; t.scenes[1].fx[0].at = 0.3; t.scenes[1].fx[0].x = 500;
  assert.equal(problems(t).length, 3);
});
test("tre righe al massimo, mai i puntini di sospensione", () => {
  const t = base(); t.scenes[0].text.lines = ["a", "b", "c", "d…"];
  assert.equal(problems(t).length, 3);      // quattro righe, i puntini, e «working.» che non c'è più
});
test("con l'orologio in scena una riga di titolo non supera i 14 caratteri: oltre, finisce sopra la cassa", () => {
  const t = base(); t.scenes[1].text.lines = ["Know your limits."]; t.scenes[1].text.accent = "limits.";
  assert.match(problems(t).join("\n"), /list: la riga «Know your limits\.» ha 17 caratteri, al massimo 14 accanto all'orologio/);
  const solo = base(); solo.scenes[0].text.lines = ["Claude is working hard."]; solo.scenes[0].text.accent = "working";
  assert.deepEqual(problems(solo), []);
});
test("le parole entrano a mezzo battito l'una e la frase resta ferma due battiti: se non ci stanno, la scaletta lo dice", () => {
  const t = base(); t.scenes[0].len = 3; t.scenes[0].text.lines = ["You’re not", "at your desk."]; t.scenes[0].text.accent = "desk."; t.scenes[1].at = 3;
  assert.match(problems(t).join("\n"), /open: 5 parole a mezzo battito l'una più due per leggerle fanno 4.5 battiti, la scena ne ha 3/);
  const ok = base(); ok.scenes[0].len = 5; ok.scenes[0].text.lines = ["You’re not", "at your desk."]; ok.scenes[0].text.accent = "desk."; ok.scenes[1].at = 5;
  assert.deepEqual(problems(ok), []);
});
test("al posto della clip una scena può mostrare un'immagine ferma, ma deve essere un PNG del progetto", () => {
  const t = base(); t.scenes[1].watch.still = "icon/close.png";
  assert.deepEqual(problems(t), []);
  t.scenes[1].watch.still = "https://example.com/x.jpg";
  assert.match(problems(t).join("\n"), /list: immagine «https:\/\/example.com\/x.jpg»: attesa un PNG dentro public/);
});

test("una clip accelerata oltre 1,25× è un errore: al polso si vede", () => {
  const t = base(); t.scenes[1].watch.rate = 1.4;
  assert.match(problems(t).join("\n"), /velocità della clip 1.4/);
  t.scenes[1].watch.rate = 1.25; assert.equal(problems(t).length, 0);
});

test("la risposta tiene l'orologio dove l'ha lasciato «It speaks»: senza testo non scivola al centro", () => {
  // 21/09 19:03, Franz: «all'inizio della scena è a destra, poi diventa centrale, deve rimanere al suo posto». Tolta la
  // riga «You answer.», la colonna (che segue il testo) era passata al centro.
  const t = JSON.parse(readFileSync(new URL("./timeline.json", import.meta.url), "utf8"));
  const by = (id: string) => t.scenes.find((s: { id: string }) => s.id === id);
  assert.equal(watchColumn(by("answer")), watchColumn(by("speaks")));
});

test("il respiro del sonno si spegne con false; altri valori sono un errore", () => {
  const t = base();
  t.scenes = [
    { id: "wake", at: 0, len: 5, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, text: { lines: ["Claude Code,", "on your wrist."], accent: "wrist.", at: 0.5 }, sleep: { len: 8, titleLead: 0, breath: "no" } },
    { id: "asks", at: 5, len: 4, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, text: { lines: ["It asks."], accent: "asks." } },
  ];
  assert.match(problems(t).join("\n"), /wake: il respiro del sonno è «no»: serve true o false/);
  t.scenes[0].sleep.breath = false;
  assert.deepEqual(problems(t), []);
});

test("le sole palpebre («lids») sono un passaggio valido anche con una frase, che se ne va prima", () => {
  const t = base(); t.scenes[0].out = "lids";
  assert.deepEqual(problems(t), []);
  t.scenes[0].out = "wink";
  assert.match(problems(t).join("\n"), /open: passaggio «wink» sconosciuto/);
});
test("la card ✓ in una riga della lista sta dentro il display", () => {
  const t = base(); t.scenes[1].fx.push({ kind: "doneCard", at: 0, name: "payments-api", age: "0 m", text: "Released 2.8.0", slot: 400 });
  assert.match(problems(t).join("\n"), /list: la riga della card ✓ è a 400: fra 0 e 310/);
  t.scenes[1].fx[1].slot = 146;
  assert.deepEqual(problems(t), []);
});

test("logo ritagliato e cinturino che sborda valgono solo sul cartello", () => {
  const t = base(); t.scenes[1].logoCutout = true; t.scenes[1].strapBleed = true;
  assert.match(problems(t).join("\n"), /list: logo ritagliato e cinturino che sborda valgono solo sul cartello/);
  t.scenes[1].endCard = true; t.scenes[1].endPace = "blinds"; t.scenes[1].watch.view = "threeQuarter";
  assert.deepEqual(problems(t), []);
});

test("«hush» del sonno vuole true o false", () => {
  const t = base();
  t.scenes = [
    { id: "wake", at: 0, len: 5, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, sleep: { len: 8, titleLead: 0, hush: "no" } },
    { id: "asks", at: 5, len: 4, act: "know", watch: { view: "front", clip: "scenes/s1_list.mp4" }, text: { lines: ["It asks."], accent: "asks." } },
  ];
  assert.match(problems(t).join("\n"), /wake: «hush» del sonno è «no»: serve true o false/);
  t.scenes[0].sleep.hush = false;
  assert.deepEqual(problems(t), []);
});

test("la tapparella dura almeno 5 battiti (nel corto 5: parte più tardi e la scheda Context si legge, scelta B del 23/09)", () => {
  const t = base(); t.scenes = [
    { id: "a", at: 0, len: 6, act: "control", watch: { view: "front", clip: "scenes/s1_list.mp4" }, blinds: { len: 4.5 } },
    { id: "b", at: 6, len: 8, act: "close" },
  ];
  assert.match(problems(t).join("\n"), /a: la tapparella dura 4.5 battiti: il minimo è 5/);
  t.scenes[0].blinds.len = 5;
  assert.deepEqual(problems(t), []);
});
test("logo ritagliato e cinturino che sborda vogliono true o false; il cinturino sborda solo di tre quarti (revisione del 23/09)", () => {
  const t = base(); t.scenes[1].endCard = true; t.scenes[1].endPace = "blinds"; t.scenes[1].logoCutout = "yes"; t.scenes[1].strapBleed = 1;
  const p = problems(t).join("\n");
  assert.match(p, /list: «logoCutout» è «yes»: serve true o false/);
  assert.match(p, /list: «strapBleed» è «1»: serve true o false/);
  t.scenes[1].logoCutout = true; t.scenes[1].strapBleed = true;
  assert.match(problems(t).join("\n"), /list: il cinturino sborda solo con l'orologio di tre quarti/);
  t.scenes[1].watch.view = "threeQuarter";
  assert.deepEqual(problems(t), []);
});
test("la riga della card ✓ è un numero (revisione del 23/09)", () => {
  const t = base(); t.scenes[1].fx.push({ kind: "doneCard", at: 0, name: "payments-api", age: "0 m", text: "Released 2.8.0", slot: "146" });
  assert.match(problems(t).join("\n"), /list: la riga della card ✓ è «146»: serve un numero fra 0 e 310/);
});
test("il volo parte dal terminale della scena prima e dura almeno 1,5 battiti (piano 4, 23/09)", () => {
  const t = base(); t.scenes[1].fx.push({ kind: "takeIn", at: 0, len: 1, slot: 146, name: "payments-api", age: "0 m", text: "Released" });
  const p = problems(t).join("\n");
  assert.match(p, /list: il volo parte dal terminale della scena prima/);
  assert.match(p, /list: il volo dura 1 battiti: almeno 1,5/);
  t.scenes[1].fx[1].slot = "146";
  assert.match(problems(t).join("\n"), /list: la riga del volo è «146»: serve un numero fra 0 e 310/);
});
test("lo schermo sfuma per un numero di battiti positivo, dentro la scena (piano 6)", () => {
  const t = base(); t.scenes[1].watch.screenFade = 0;
  assert.match(problems(t).join("\n"), /list: lo schermo sfuma per 0 battiti: serve un numero fra 0 e la durata della scena/);
  t.scenes[1].watch.screenFade = 9;
  assert.match(problems(t).join("\n"), /list: lo schermo sfuma per 9 battiti: serve un numero fra 0 e la durata della scena/);
  t.scenes[1].watch.screenFade = 1;
  assert.deepEqual(problems(t), []);
});
test("il volo entra solo in un orologio di fronte (revisione finale dei piani 4-6)", () => {
  const t = base(); t.scenes[0].fx = [{ kind: "terminalPlane", at: 0, len: 4, rect: [26, 163, 427, 150], header: "T", title: "t", lines: ["⏺ ok"], every: 2 }];
  t.scenes[1].watch.view = "side";
  t.scenes[1].fx.push({ kind: "takeIn", at: 0, len: 2, slot: 146, name: "payments-api", age: "0 m", text: "ok" });
  assert.match(problems(t).join("\n"), /list: il volo entra solo in un orologio di fronte/);
});
test("la pausa fra le righe conta nel tempo della frase (23/09 23:20)", () => {
  const t = base(); t.scenes[1].text.pause = 6;   // due righe: la pausa sta fra la prima e la seconda
  assert.match(problems(t).join("\n"), /list: 4 parole a mezzo battito l'una più la pausa di 6 e due per leggerle fanno 10 battiti, la scena ne ha 8/);
  t.scenes[1].text.pause = 1;
  assert.deepEqual(problems(t), []);
});

// revisione finale del piano 7 (24/09): le minori rimaste
const withFlight = (): any => {
  const t = base();
  t.scenes[0].fx = [{ kind: "terminalPlane", at: 0, len: 4, rect: [26, 163, 427, 150], header: "T", title: "t", lines: ["⏺ Released 2.8.0 and tagged v2.8.0"], every: 2 }];
  t.scenes[1].fx.push({ kind: "takeIn", at: 0, len: 2, slot: 146, name: "payments-api", age: "0 m", text: "Released 2.8.0 and tagged v2.8.0" });
  t.scenes[1].fx.push({ kind: "doneCard", at: 2, slot: 146, name: "payments-api", age: "0 m", text: "Released 2.8.0 and tagged v2.8.0" });
  return t;
};
test("lo schermo sfuma per mezzi battiti, non per un numero qualsiasi", () => {
  const t = base(); t.scenes[1].watch.screenFade = 0.01;
  assert.match(problems(t).join("\n"), /list: lo schermo sfuma per 0.01 battiti: serve un numero fra 0 e la durata della scena, in mezzi battiti/);
  t.scenes[1].watch.screenFade = "1";
  assert.match(problems(t).join("\n"), /list: lo schermo sfuma per 1 battiti/);
});
test("il volo scrive l'ultima riga del terminale e atterra sulla card ✓ della stessa riga, quando finisce", () => {
  assert.deepEqual(problems(withFlight()), []);
  const a = withFlight(); a.scenes[1].fx[1].text = "Released 2.8.0";
  assert.match(problems(a).join("\n"), /list: il volo scrive «Released 2.8.0» ma l'ultima riga del terminale è «Released 2.8.0 and tagged v2.8.0»/);
  const b = withFlight(); b.scenes[1].fx[2].at = 3;
  assert.match(problems(b).join("\n"), /list: il volo finisce al battito 2 ma lì non c'è la sua card ✓ nella riga 146/);
  const c = withFlight(); c.scenes[1].fx[2].slot = 200;
  assert.match(problems(c).join("\n"), /list: il volo finisce al battito 2 ma lì non c'è la sua card ✓ nella riga 146/);
  const d = withFlight(); d.scenes[1].fx[2].name = "storefront";
  assert.match(problems(d).join("\n"), /list: la card ✓ dopo il volo non è quella in volo \(nome, età o testo diversi\)/);
});
test("la pausa sta fra due righe, senza parole portate e non dopo un sonno del display", () => {
  const one = base(); one.scenes[0].text = { lines: ["Claude is working."], accent: "working.", pause: 1 };
  assert.match(problems(one).join("\n"), /open: la pausa sta fra le righe: una frase di una riga non ne ha/);
  const carry = base(); carry.scenes[0].text.keep = true;
  carry.scenes[1].text = { lines: ["Claude is working.", "Now."], accent: "Now.", carry: 3, pause: 1 };
  assert.match(problems(carry).join("\n"), /list: la pausa non va con le parole portate dalla scena prima/);
  const nap = base(); nap.scenes[0].sleep = { len: 4 }; nap.scenes[1].text.pause = 1;
  assert.match(problems(nap).join("\n"), /list: dopo un sonno del display la frase si scrive senza pausa/);
});
test("l'anello del logo che arriva dalla forma (`ringFrom`) vale solo sul cartello, dentro la scena", () => {
  const t = base(); t.scenes[1].ringFrom = 1;
  assert.match(problems(t).join("\n"), /list: «ringFrom» vale solo sul cartello/);
  t.scenes[1].endCard = true; t.scenes[1].endPace = "compact"; t.scenes[1].ringFrom = 9;
  assert.match(problems(t).join("\n"), /list: l'anello del logo arriva al battito 9 della scena/);
  t.scenes[1].ringFrom = 1.25;
  assert.match(problems(t).join("\n"), /list: l'anello del logo arriva al battito 1.25 della scena/);
  t.scenes[1].ringFrom = 1.5;
  assert.deepEqual(problems(t), []);
});
