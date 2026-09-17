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
  const t = base(); t.scenes[1].fx[0].kind = "swipe"; t.scenes[1].watch.view = "side"; t.scenes[1].watch.enter = "spin"; t.scenes[0].act = "intro";
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
