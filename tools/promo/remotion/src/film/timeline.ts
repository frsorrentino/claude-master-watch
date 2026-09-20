import type { Grid } from "./beats.ts";
import type { Move } from "./moves.ts";
import { SLEEP_CUT, TITLE_AT } from "./ui/sleep.ts";

/** La scaletta: scene in fila, in battiti. Dentro una scena i tempi (text.at, fx[].at) partono dall'inizio della scena; mezzi battiti ammessi. */
export type Act = "open" | "know" | "act" | "control" | "close";
export type Fx =
  | { kind: "tap"; at: number; x: number; y: number }               // x, y nello schermo dell'orologio, 0-480
  | { kind: "longPress"; at: number; len: number }
  | { kind: "haptic"; at: number }
  | { kind: "counter"; at: number; len: number; to: number; suffix: string }
  | { kind: "typed"; at: number; len: number; text: string }
  | { kind: "terminal"; at: number; every: number; lines: string[] }
  | { kind: "cardOut"; at: number; len: number; rect: [number, number, number, number]; name: string; age: string; text: string; badge?: string; icon?: "check" | "play"; fromOut?: boolean; toCenter?: boolean }   // la card ferma sul display (rettangolo 0-480) esce e torna (piano 3); badge: colore dell'account, icona di stato
  | { kind: "gaugeHero"; at: number; len: number; cx: number; cy: number; size: number; value: number; week: number; suffix: string; phrase: string }   // il gauge della quota (centro e lato nel display) esce, si disegna col contatore, torna
  | { kind: "aside"; at: number; len: number; panel: "quota" | "note" | "pace" | "work" | "questions" | "context"; n?: number; note?: string; quote?: string; lines?: string[]; bars?: number[]; rows?: { name: string; pct: number; week?: number }[]; out?: "bars" | "curtain" }   // il dato della Panoramica compare fermo a sinistra e si anima sul posto; `out: "bars"`: le sue barre diventano il passaggio alla scena dopo
  | { kind: "panelHero"; at: number; len: number; panel: "work" | "context"; rect: [number, number, number, number]; n?: number; note?: string; bars?: number[]; rows?: { name: string; pct: number }[] }   // un pannello della Panoramica esce e si anima (barre che si riempiono, percentuali che contano)
  | { kind: "optionsBuild"; at: number; len: number; pressAt?: number; yes: [number, number, number, number]; no: [number, number, number, number]; yesLabel: string; noLabel: string }   // `pressAt`: battiti dall'inizio dell'effetto in cui parte la pressione lunga   // i tasti della domanda nascono da contorno fuori dal display, l'anello corre su «yes»
  | { kind: "spoken"; at: number; len: number; voice: string; words: string }   // file in public/audio/
  | { kind: "shake"; at: number }
  | { kind: "float"; at: number; len: number; cx?: number; bottom?: number; width?: number; cards: ({ kind?: "card"; name: string; age: string; text: string; badge: string; icon: "check" | "play" | "bell"; hold?: number } | { kind: "text"; lines: string[]; accent?: string; hold?: number } | { kind: "brief"; panel: "quota" | "note" | "pace" | "work" | "questions" | "context"; n?: number; note?: string; quote?: string; lines?: string[]; bars?: number[]; rows?: { name: string; pct: number }[]; hold?: number })[] }   // card e scritte che salgono dal vetro, alternate (vista laterale)   // vibrazione: l'orologio trema per 10 fotogrammi (la notifica arriva)
  | { kind: "musicStop"; at: number; len: number }   // stop and go della musica: tace sul battito, riparte dopo `len` battiti (piano 4 §3)
  | { kind: "terminalPlane"; at: number; len: number; rect: [number, number, number, number]; header: string; title: string; lines: string[]; every: number };   // il terminale dell'orologio esce e diventa la finestra del PC; le righe arrivano ogni `every` battiti   // stop and go della musica: tace sul battito, riparte dopo `len` battiti (piano 4 §3)
export type WatchCue = { view: "front" | "threeQuarter" | "drawn" | "side"; clip: string; clipStart?: number; rate?: number; freeze?: boolean; still?: string; steady?: boolean; exitBeats?: number; enter?: Move; exit?: Move; camera?: Camera };   // freeze: la clip resta ferma su clipStart (schermo fermo durante la lettura)
/** Camera della scena (piano 4): `close` = ci si avvicina mentre il momento forte è fuori (default); `release` = si parte
 *  vicini (dopo un battito di ciglia) e la camera torna indietro mentre il componente è fuori, che atterra sull'orologio piccolo. */
export type Camera = "close" | "release" | "around";   // around: l'orologio compare attorno alla card già ferma al centro, grande
export type TextCue = { lines: string[]; accent?: string; size?: "title" | "service"; at?: number; sub?: string; place?: "top" };   // place top: in alto a sinistra, piccolo, senza uscita (il titolo che resta sopra al protagonista)
/** Passaggio alla scena dopo (piano 4 §2 bis): `blink` = la parola in colore cresce fino a riempire il quadro e il suo nero è un battito di ciglia. */
/** Una chiave del passaggio (piano 4 §2 bis): dove sta e che forma ha l'oggetto che attraversa il taglio, nel display (0-480)
 *  o nel quadro (`space: "frame"`). Il taglio interpola da `carryOut` della scena a `carryIn` della scena dopo. */
export type CarryKey = { shape: "circle" | "pill" | "square" | "line" | "arc"; x: number; y: number; w: number; h: number; color: string; glyph?: "play" | "question" | "check" | "bell" | "mic" | "none"; glyphColor?: string; stroke?: number; space?: "display" | "frame" };
/** Il takeover che chiude la scena (piano 5 §2): il componente, già protagonista nel quadro (`x`, `y`, `w`, `h`, raggio `r`,
 *  colore `color`), cresce fino a coprire tutto e il suo colore diventa lo sfondo della scena dopo. `len` in battiti, a cavallo
 *  del taglio. `body`: cosa si vede dentro mentre cresce. */
export type TakeoverCue = { len: number; x: number; y: number; w: number; h: number; r: number; color: string; toColor: string; body?: "card" | "words" | "plain"; text?: string; words?: string[]; card?: { name: string; age: string; text: string; badge: string; icon: "check" | "play" } };   // `card`: il takeover parte come una scheda della corsia e poi cresce
export type Scene = { id: string; at: number; len: number; act: Act; watch?: WatchCue; text?: TextCue; fx?: Fx[]; endCard?: boolean; out?: "blink"; carryOut?: CarryKey; carryIn?: CarryKey; takeover?: TakeoverCue; flip?: FlipCue; glow?: GlowCue; sleep?: SleepCue; blinds?: BlindsCue; bgFrom?: string; bgFadeBeats?: number; blackOutFrames?: number };
/* `blackOutFrames`: un breve nero prima del taglio, quando la scena si chiude su un movimento e la musica riprende
   subito dopo — il vuoto fa respirare lo stacco (Franz, 19/09 15:43). */
/** La scheda che si volta e sul retro ha la domanda: `len` battiti a cavallo del taglio con la scena dopo. */
export type FlipCue = { len: number; w: number; card: { name: string; age: string; text: string; badge?: string; icon?: "check" | "play" }; question: { name: string; age: string; text: string } };
/** La luce della notifica che cresce dal display, copre il quadro e si ritira sull'inquadratura nuova: `len` battiti a
 *  cavallo del taglio, `cx`/`cy` il centro del display in frazioni di quadro. */
export type GlowCue = { len: number; cx?: number; cy?: number; color?: string };
/** Il display che si addormenta e la notifica che lo risveglia: `len` battiti a cavallo del taglio. Il display cala ad
 *  ambient, nel buio la camera si sposta sull'inquadratura della scena dopo, e sul battito il display si riaccende.
 *  La musica si azzera con lui e rientra `musicBackBeats` battiti dopo il taglio (una battuta, 4 battiti, se non detto).
 *  `musicFrom`: il secondo della traccia da cui riparte. Dopo un silenzio la musica non riprende da dove sarebbe arrivata:
 *  attacca il giro principale (Franz, 19/09 19:41). Misurato su `music.v9.wav`: l'attacco è a 8,699 s, +7,2 dB sulla
 *  battuta prima. */
export type SleepCue = { len: number; musicBackBeats?: number; musicFrom?: number };
/** La tapparella che chiude una sezione: dura `len` battiti a cavallo del taglio con la scena dopo. */
export type BlindsCue = { len: number };
/** `musicDelayBeats`: di quanti battiti la musica entra dopo l'inizio del film, per far cadere il culmine del crescendo
 *  dove serve (Franz, 19/09 15:05: «la musica è una battuta avanti rispetto a quando serve»). */
/** `musicDelayFrames`: la fase della traccia, in fotogrammi. Misurata il 19/09 sui transienti di `music.v9.wav`: i battiti
 *  del brano cadono 35 ms PRIMA di quelli della griglia (110,00 bpm esatti, quindi è fase, non deriva). Un fotogramma di
 *  ritardo sulla traccia li rimette insieme a 2 ms, senza spostare di un fotogramma tutti i tagli già approvati. */
export type Timeline = Grid & { music?: string; musicDelayBeats?: number; musicDelayFrames?: number; scenes: Scene[] };

export class TimelineError extends Error {
  problems: string[];
  constructor(problems: string[]) {
    super(`Scaletta sbagliata:\n- ${problems.join("\n- ")}`);
    this.problems = problems;
  }
}

const ACTS = ["open", "know", "act", "control", "close"];
const VIEWS = ["front", "threeQuarter", "drawn", "side"];
const MOVES = ["riseIn", "slideIn", "slideOut", "pushIn", "pullOut", "settleSmall", "zoomLeft", "diveIn"];
const FX = ["tap", "longPress", "haptic", "counter", "typed", "terminal", "spoken", "cardOut", "gaugeHero", "optionsBuild", "musicStop", "terminalPlane", "shake", "float", "panelHero", "aside"];
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
  let prev: Scene | undefined;
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
      if (s.watch.camera !== undefined && !["close", "release", "around"].includes(s.watch.camera)) say(`camera «${s.watch.camera}» sconosciuta`);
      if ((s.watch.camera === "release" || s.watch.camera === "around") && !(s.fx ?? []).some((f) => f.kind === "cardOut" || f.kind === "gaugeHero")) say(`la camera «${s.watch.camera}» vuole un momento forte nella scena`);
    }
    if (s.out !== undefined && s.out !== "blink") say(`passaggio «${s.out}» sconosciuto`);
    for (const k of [s.carryOut, s.carryIn]) if (k && (k.space ?? "display") === "display" && !(k.x >= 0 && k.x <= 480 && k.y >= 0 && k.y <= 480)) say(`chiave del passaggio (${k.x}, ${k.y}) fuori dal display`);
    // un takeover deve durare: sotto i 3 battiti non si legge come trasformazione (Franz, 18/09: «servono animazioni che prendano più tempo»)
    // il minimo era 3 battiti «perché si legga come trasformazione»: ma con un campo di colore pieno 3 battiti sono
    // 0,8 s di quadro vuoto (Franz, 19/09 11:45). Il minimo vero è 2,5: sotto, la crescita non si vede.
    if (s.takeover && !(s.takeover.len >= 2.5)) say(`il takeover dura ${s.takeover.len} battiti: il minimo è 2,5`);
    // la tapparella deve stare davanti e dietro al taglio: sotto i 6 battiti le barre non fanno in tempo a diventare listelli
    // il sonno del display: sotto i 2,5 battiti non c'è tempo per calare, restare al buio e riaccendersi sul battito
    if (s.sleep && !(half(s.sleep.len) && s.sleep.len >= 2.5)) say(`il sonno del display dura ${s.sleep.len} battiti: il minimo è 2,5, in battiti o mezzi battiti`);
    if (s.sleep && !s.watch) say("il sonno del display vuole l'orologio in scena");
    if (s.sleep?.musicFrom !== undefined && !(s.sleep.musicFrom >= 0)) say(`la musica riparte dal secondo ${s.sleep.musicFrom} della traccia: serve un tempo dentro il brano`);
    if (s.sleep?.musicBackBeats !== undefined && !half(s.sleep.musicBackBeats)) say(`la musica rientra al battito ${s.sleep.musicBackBeats} dopo il taglio: servono battiti o mezzi battiti`);
    if (s.sleep && s === t.scenes[t.scenes.length - 1]) say(`la scena «${s.id}» addormenta il display ma non c'è una scena dopo da risvegliare`);
    if (s.blinds && !(s.blinds.len >= 6)) say(`la tapparella dura ${s.blinds.len} battiti: il minimo è 6`);
    if (s.blinds && s === t.scenes[t.scenes.length - 1]) say(`la scena «${s.id}» ha la tapparella ma non c'è una scena dopo da scoprire`);
    if (s.out === "blink" && !s.text?.accent) say("il battito di ciglia vuole una parola in colore da far crescere");
    if (s.text) {
      if (s.text.lines.length < 1 || s.text.lines.length > 3) say(`${s.text.lines.length} righe di testo: da 1 a 3`);
      if (s.text.lines.some((l) => l.includes("…") || l.includes("..."))) say("puntini di sospensione nel testo");
      // accanto all'orologio restano ~735 px: a 110 px sono 14 caratteri (misurato: «Every session.» entra, «Know your limits.» no)
      if (s.watch && (s.text.size ?? "title") === "title") for (const l of s.text.lines) if (l.length > 14) say(`la riga «${l}» ha ${l.length} caratteri, al massimo 14 accanto all'orologio`);
      const words = s.text.lines.flatMap((l) => l.split(" "));
      // «desk.» tagliata nell'anteprima del 17/09: cinque parole a una per battito in una scena di quattro battiti
      const perWord = 0.5;   // mezzo battito a parola ovunque
      // se la scena prima addormenta il display, il titolo di questa si scrive PRIMA del taglio (Film.tsx lo disegna a
      // cavallo): quei battiti contano come spazio, se no una scena corta dopo il risveglio risulta troppo stretta
      const early = prev?.sleep ? (SLEEP_CUT - TITLE_AT) * prev.sleep.len : 0;
      const room = s.len + early - (s.text.at ?? 0) - (s.watch?.exit ? 2 : 0);
      // due battiti perché la frase intera resti ferma: è la pausa che la rende leggibile, non la velocità
      if (words.length * perWord + 2 > room) say(`${words.length} parole a mezzo battito l'una più due per leggerle fanno ${words.length * perWord + 2} battiti, la scena ne ha ${room}`);
      if (s.text.accent !== undefined && !words.includes(s.text.accent)) say(`«${s.text.accent}» non è tra le parole del testo`);
      if (s.text.at !== undefined && (!half(s.text.at) || s.text.at >= s.len)) say(`il testo al battito ${s.text.at} esce dalla scena`);
    }
    for (const f of s.fx ?? []) {
      if (!FX.includes(f.kind)) { say(`effetto «${f.kind}» sconosciuto`); continue; }
      const len = "len" in f ? f.len : 0;
      if (!half(f.at) || !half(len)) say(`l'effetto ${f.kind} ha tempi che non sono mezzi battiti (at ${f.at})`);
      // il parlato fa eccezione: l'audio è montato sul film, non sulla scena, e continua oltre il taglio — la voce legge
      // le due risposte mentre il display è già sui tasti (Franz, 19/09 05:12: «più vicino alla lettura delle 2 risposte»)
      else if (f.kind !== "spoken" && (f.at + len > s.len || f.at >= s.len)) say(`l'effetto ${f.kind} al battito ${f.at} esce dalla scena`);
      else if (f.kind === "spoken" && f.at >= s.len) say(`il parlato comincia al battito ${f.at}, fuori dalla scena`);
      if (f.kind === "tap" && !(f.x >= 0 && f.x <= 480 && f.y >= 0 && f.y <= 480)) say(`tocco (${f.x}, ${f.y}) fuori dallo schermo 480×480`);
      if ((f.kind === "tap" || f.kind === "longPress" || f.kind === "haptic" || f.kind === "shake" || f.kind === "cardOut" || f.kind === "gaugeHero") && !s.watch) say(`l'effetto ${f.kind} vuole l'orologio in scena`);
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
      if (f.kind === "optionsBuild" && f.pressAt !== undefined && !(half(f.pressAt) && f.pressAt < f.len)) say(`la pressione al battito ${f.pressAt} non sta dentro l'arco dei tasti`);
      if (f.kind === "optionsBuild") for (const r of [f.yes, f.no]) if (!(r[0] >= 0 && r[1] >= 0 && r[2] > 0 && r[3] > 0 && r[0] + r[2] <= 480 && r[1] + r[3] <= 480)) say(`il tasto (${String(r)}) esce dallo schermo 480×480`);
      const HERO = ["cardOut", "gaugeHero", "optionsBuild", "terminalPlane", "panelHero"];
      if (HERO.includes(f.kind) && (s.fx ?? []).filter((x) => HERO.includes(x.kind)).length > 1 && s.id !== "limits") say("un solo momento forte per scena");
    }
    prev = s;
  }
  if (bad.length) throw new TimelineError(bad);
  return t;
};
