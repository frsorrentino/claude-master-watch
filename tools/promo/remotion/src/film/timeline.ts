import type { Grid } from "./beats.ts";
import type { Move } from "./moves.ts";

/** La scaletta: scene in fila, in battiti. Dentro una scena i tempi (text.at, fx[].at) partono dall'inizio della scena; mezzi battiti ammessi. */
export type Act = "open" | "know" | "act" | "control" | "close";
export type Fx =
  | { kind: "tap"; at: number; x: number; y: number }               // x, y nello schermo dell'orologio, 0-480
  | { kind: "longPress"; at: number; len: number }
  | { kind: "haptic"; at: number }
  | { kind: "counter"; at: number; len: number; to: number; suffix: string }
  | { kind: "typed"; at: number; len: number; text: string }
  | { kind: "terminal"; at: number; every: number; lines: string[] }
  | { kind: "cardOut"; at: number; len: number; rect: [number, number, number, number]; name: string; age: string; text: string; badge?: string; icon?: "check" | "play" }   // la card ferma sul display (rettangolo 0-480) esce e torna (piano 3); badge: colore dell'account, icona di stato
  | { kind: "gaugeHero"; at: number; len: number; cx: number; cy: number; size: number; value: number; week: number; suffix: string; phrase: string }   // il gauge della quota (centro e lato nel display) esce, si disegna col contatore, torna
  | { kind: "optionsBuild"; at: number; len: number; yes: [number, number, number, number]; no: [number, number, number, number]; yesLabel: string; noLabel: string }   // i tasti della domanda nascono da contorno fuori dal display, l'anello corre su «yes»
  | { kind: "spoken"; at: number; len: number; voice: string; words: string };   // file in public/audio/
export type WatchCue = { view: "front" | "threeQuarter" | "drawn"; clip: string; clipStart?: number; rate?: number; freeze?: boolean; still?: string; enter?: Move; exit?: Move; camera?: Camera };   // freeze: la clip resta ferma su clipStart (schermo fermo durante la lettura)
/** Camera della scena (piano 4): `close` = ci si avvicina mentre il momento forte è fuori (default); `release` = si parte
 *  vicini (dopo un battito di ciglia) e la camera torna indietro mentre il componente è fuori, che atterra sull'orologio piccolo. */
export type Camera = "close" | "release";
export type TextCue = { lines: string[]; accent?: string; size?: "title" | "service"; at?: number; sub?: string };
/** Passaggio alla scena dopo (piano 4 §2 bis): `blink` = la parola in colore cresce fino a riempire il quadro e il suo nero è un battito di ciglia. */
export type Scene = { id: string; at: number; len: number; act: Act; watch?: WatchCue; text?: TextCue; fx?: Fx[]; endCard?: boolean; out?: "blink" };
export type Timeline = Grid & { music?: string; scenes: Scene[] };

export class TimelineError extends Error {
  problems: string[];
  constructor(problems: string[]) {
    super(`Scaletta sbagliata:\n- ${problems.join("\n- ")}`);
    this.problems = problems;
  }
}

const ACTS = ["open", "know", "act", "control", "close"];
const VIEWS = ["front", "threeQuarter", "drawn"];
const MOVES = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut", "settleSmall", "zoomLeft", "diveIn"];
const FX = ["tap", "longPress", "haptic", "counter", "typed", "terminal", "spoken", "cardOut", "gaugeHero", "optionsBuild"];
const half = (v: unknown): v is number => typeof v === "number" && v >= 0 && Number.isInteger(v * 2);

export const totalBeats = (t: Timeline): number => (t.scenes.length ? t.scenes[t.scenes.length - 1].at + t.scenes[t.scenes.length - 1].len : 0);

export const validateTimeline = (raw: unknown): Timeline => {
  const t = raw as Timeline;
  const bad: string[] = [];
  if (!(t.bpm >= 60 && t.bpm <= 160)) bad.push(`bpm ${t.bpm} fuori da 60-160`);
  if (t.fps !== 30) bad.push(`fps ${t.fps}: il film è a 30`);
  if (typeof t.offsetSeconds !== "number") bad.push("offsetSeconds manca");
  const ids = new Set<string>();
  let end = 0;
  for (const s of t.scenes ?? []) {
    const say = (m: string) => bad.push(`${s.id}: ${m}`);
    if (ids.has(s.id)) say("id doppio");
    ids.add(s.id);
    if (!half(s.at) || !half(s.len) || s.len <= 0) say(`at/len devono essere battiti o mezzi battiti (at ${s.at}, len ${s.len})`);
    if (s.at !== end) say(`inizia al battito ${s.at}, la scena prima finisce a ${end}`);
    end = s.at + s.len;
    if (!ACTS.includes(s.act)) say(`atto «${s.act}» sconosciuto`);
    if (s.watch) {
      if (!VIEWS.includes(s.watch.view)) say(`vista «${s.watch.view}» sconosciuta`);
      for (const m of [s.watch.enter, s.watch.exit]) if (m !== undefined && !MOVES.includes(m)) say(`movimento «${m}» sconosciuto`);
      if (!/^scenes\/[\w.-]+\.mp4$/.test(s.watch.clip)) say(`clip «${s.watch.clip}»: attesa scenes/<nome>.mp4`);
      // Un'interfaccia da polso accelerata si vede (piano 4): le clip vanno a tempo reale, al massimo 1,25×.
      if (s.watch.rate !== undefined && !(s.watch.rate > 0 && s.watch.rate <= 1.25)) say(`velocità della clip ${s.watch.rate}: al massimo 1,25×`);
      if (s.watch.still !== undefined && !/^[\w-]+(\/[\w.-]+)*\.png$/.test(s.watch.still)) say(`immagine «${s.watch.still}»: attesa un PNG dentro public`);
      if (s.watch.camera !== undefined && !["close", "release"].includes(s.watch.camera)) say(`camera «${s.watch.camera}» sconosciuta`);
      if (s.watch.camera === "release" && !(s.fx ?? []).some((f) => f.kind === "cardOut" || f.kind === "gaugeHero")) say("la camera «release» vuole un momento forte nella scena");
    }
    if (s.out !== undefined && s.out !== "blink") say(`passaggio «${s.out}» sconosciuto`);
    if (s.out === "blink" && !s.text?.accent) say("il battito di ciglia vuole una parola in colore da far crescere");
    if (s.text) {
      if (s.text.lines.length < 1 || s.text.lines.length > 3) say(`${s.text.lines.length} righe di testo: da 1 a 3`);
      if (s.text.lines.some((l) => l.includes("…") || l.includes("..."))) say("puntini di sospensione nel testo");
      // accanto all'orologio restano ~735 px: a 110 px sono 14 caratteri (misurato: «Every session.» entra, «Know your limits.» no)
      if (s.watch && (s.text.size ?? "title") === "title") for (const l of s.text.lines) if (l.length > 14) say(`la riga «${l}» ha ${l.length} caratteri, al massimo 14 accanto all'orologio`);
      const words = s.text.lines.flatMap((l) => l.split(" "));
      // «desk.» tagliata nell'anteprima del 17/09: cinque parole a una per battito in una scena di quattro battiti
      const perWord = s.watch ? 0.5 : 1;
      const room = s.len - (s.text.at ?? 0) - (s.watch?.exit ? 2 : 0);
      if (words.length * perWord + 1 > room) say(`${words.length} parole a ${s.watch ? "mezzo battito l'una" : "una per battito"} più uno per leggerle fanno ${words.length * perWord + 1} battiti, la scena ne ha ${room}`);
      if (s.text.accent !== undefined && !words.includes(s.text.accent)) say(`«${s.text.accent}» non è tra le parole del testo`);
      if (s.text.at !== undefined && (!half(s.text.at) || s.text.at >= s.len)) say(`il testo al battito ${s.text.at} esce dalla scena`);
    }
    for (const f of s.fx ?? []) {
      if (!FX.includes(f.kind)) { say(`effetto «${f.kind}» sconosciuto`); continue; }
      const len = "len" in f ? f.len : 0;
      if (!half(f.at) || !half(len)) say(`l'effetto ${f.kind} ha tempi che non sono mezzi battiti (at ${f.at})`);
      else if (f.at + len > s.len || f.at >= s.len) say(`l'effetto ${f.kind} al battito ${f.at} esce dalla scena`);
      if (f.kind === "tap" && !(f.x >= 0 && f.x <= 480 && f.y >= 0 && f.y <= 480)) say(`tocco (${f.x}, ${f.y}) fuori dallo schermo 480×480`);
      if ((f.kind === "tap" || f.kind === "longPress" || f.kind === "haptic" || f.kind === "cardOut" || f.kind === "gaugeHero") && !s.watch) say(`l'effetto ${f.kind} vuole l'orologio in scena`);
      if (f.kind === "gaugeHero") {
        if (!(f.size > 0 && f.cx - f.size / 2 >= 0 && f.cy - f.size / 2 >= 0 && f.cx + f.size / 2 <= 480 && f.cy + f.size / 2 <= 480)) say(`il gauge (${f.cx}, ${f.cy}, ${f.size}) esce dallo schermo 480×480`);
        if (!(f.value >= 0 && f.value <= 100 && f.week >= 0 && f.week <= 100)) say("il gauge vuole valori in percentuale 0-100");
        if (s.watch?.view !== "front") say("il gauge esce solo dal display frontale");
      }
      if (f.kind === "cardOut") {
        const r = f.rect;
        if (!(Array.isArray(r) && r.length === 4 && r.every((v) => typeof v === "number") && r[0] >= 0 && r[1] >= 0 && r[2] > 0 && r[3] > 0 && r[0] + r[2] <= 480 && r[1] + r[3] <= 480)) say(`la card (${String(r)}) esce dallo schermo 480×480`);
        if (s.watch?.view !== "front") say("la card esce solo dal display frontale");
      }
      if (f.kind === "optionsBuild") for (const r of [f.yes, f.no]) if (!(r[0] >= 0 && r[1] >= 0 && r[2] > 0 && r[3] > 0 && r[0] + r[2] <= 480 && r[1] + r[3] <= 480)) say(`il tasto (${String(r)}) esce dallo schermo 480×480`);
      const HERO = ["cardOut", "gaugeHero", "optionsBuild"];
      if (HERO.includes(f.kind) && (s.fx ?? []).filter((x) => HERO.includes(x.kind)).length > 1) say("un solo momento forte per scena");
    }
  }
  if (bad.length) throw new TimelineError(bad);
  return t;
};
