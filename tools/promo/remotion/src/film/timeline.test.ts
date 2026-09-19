import test from "node:test";
import assert from "node:assert/strict";
import { TimelineError, totalBeats, validateTimeline } from "./timeline.ts";

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
