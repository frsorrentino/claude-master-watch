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
