import { HUSH, SLEEP_CUT } from "./ui/sleep.ts";
import type { Timeline } from "./timeline.ts";

export type SfxName = "wearNotify" | "notify" | "thump" | "tick" | "tick2" | "tick4" | "pressRise" | "whoosh" | "shutter";
export type SfxCue = { beat: number; name: SfxName; gainDb: number };

export const dbToGain = (db: number): number => Math.pow(10, db / 20);

/** Priorità quando due suoni cadono nello stesso battito: la notifica, poi la pressione, il tocco, il soffio. */
const RANK: SfxName[] = ["wearNotify", "notify", "shutter", "pressRise", "tick", "tick2", "tick4", "whoosh", "thump"];

export const sfxCues = (t: Timeline): SfxCue[] => {
  const all: SfxCue[] = [];
  t.scenes.forEach((s, k) => {
    // i tre puntini dell'attesa tengono il tempo: un tick per puntino, ognuno più forte e due semitoni più su, e la
    // notifica arriva come quarto tempo (Franz, 21/09 16:01). Stessi battiti di ui/dots.ts: 3, 2 e 1 prima della notifica.
    const next = t.scenes[k + 1];
    if (s.sleep && next) (["tick", "tick2", "tick4"] as const).forEach((name, i) => all.push({ beat: next.at - (3 - i), name, gainDb: -24 + 4 * i }));
  });
  for (const s of t.scenes) {
    // niente soffio all'ingresso delle frasi: sotto la musica non aggiungeva nulla e si sentiva come un difetto (Franz, 19/09 16:00)
    // il battito di ciglia è uno scatto fotografico soft, sul taglio (Franz, 18/09)
    if (s.out === "blink") all.push({ beat: s.at + s.len, name: "shutter", gainDb: -22 });
    for (const f of s.fx ?? []) {
      const beat = s.at + f.at;
      // campanella e tonfo restano SPENTI finché nel film non c'è l'evento che li giustifica (Franz, 19/09 05:58): oggi
      // nessuna scena mostra una notifica con la campanella, e il tremito di «It asks» da solo non basta. Il suono esiste
      // ed è pronto: si accende agganciando `haptic` alla scena che lo mostrerà.
      // il tremito porta il suono di notifica VERO dell'orologio (`wearNotify`, Tethys.ogg tirato giù dal Pixel Watch 5 il
      // 19/09: `settings get system notification_sound` lo dà come predefinito). `haptic` tiene la campanella del progetto
      // per le scene che disegnano anche gli anelli.
      if (f.kind === "shake") all.push({ beat, name: "wearNotify", gainDb: -2 });
      if (f.kind === "haptic") all.push({ beat, name: "notify", gainDb: -2 });   // la notifica suona nel silenzio: sta davanti, non sotto la musica (Franz, 19/09 18:12)
      if (f.kind === "tap") all.push({ beat, name: "tick", gainDb: -20 });
      if (f.kind === "longPress") all.push({ beat, name: "pressRise", gainDb: -18 });
      if (f.kind === "terminal") f.lines.forEach((_, i) => all.push({ beat: beat + i * f.every, name: "tick", gainDb: -24 }));
    }
  }
  const byBeat = new Map<number, SfxCue>();
  for (const c of all) {
    const k = Math.floor(c.beat);
    const had = byBeat.get(k);
    if (!had || RANK.indexOf(c.name) < RANK.indexOf(had.name)) byBeat.set(k, c);
  }
  return [...byBeat.values()].sort((a, b) => a.beat - b.beat);
};

export const duckGain = (frame: number, windows: [number, number][], depthDb: number, rampFrames: number): number => {
  let down = 0;
  for (const [a, b] of windows) {
    const inn = Math.min(1, Math.max(0, (frame - (a - rampFrames)) / rampFrames));
    const out = Math.min(1, Math.max(0, (b + rampFrames - frame) / rampFrames));
    down = Math.max(down, Math.min(inn, out));
  }
  const smooth = down * down * (3 - 2 * down);
  return dbToGain(depthDb * smooth);
};

/**
 * La musica si azzera con il display (Franz, 19/09 18:12). Mentre lo schermo cala ad ambient la traccia cala con lui fino
 * al SILENZIO; nel silenzio si sente solo la notifica e il display che si riaccende; la musica rientra una battuta esatta
 * dopo, sul battito (`back`). La traccia continua a correre muta sotto, così rientra dove il brano sarebbe arrivato.
 * `naps`: `cut` = il fotogramma del risveglio, `frames` = la finestra intera, `back` = il fotogramma in cui la musica torna.
 * L'avanzamento si conta DAL TAGLIO come per le scene (ui/sleep.ts, `sleepP`): contarlo dall'inizio lo sfaserebbe di un
 * fotogramma per via degli arrotondamenti.
 */
export type Nap = { cut: number; frames: number; back: number };
/** In quanti fotogrammi la musica cade: tre, non i otto della dissolvenza del display. Seguendo la luce lo stacco aveva
 *  «una frazione di secondo di troppo» (Franz, 19/09 20:39): il display può calare piano, la musica no. */
export const MUSIC_FALL = 3;
/** Di quanto il silenzio ARRIVA PRIMA del battito: l'ultimo colpo del brano cadeva sul battito e lo stacco sembrava in
 *  ritardo (Franz, 19/09 21:56). Cinque fotogrammi = 0,17 s. */
export const MUSIC_EARLY = 5;
export const sleepGain = (frame: number, naps: Nap[]): number => {
  let g = 1;
  for (const n of naps) {
    const p = SLEEP_CUT + (frame - n.cut) / n.frames;
    if (p < 0 || frame >= n.back + 2) continue;
    const hush = n.cut - Math.round(n.frames * (SLEEP_CUT - HUSH)) - MUSIC_EARLY;   // il silenzio arriva un soffio prima del battito
    // rientro in due fotogrammi: sul battito la musica c'è già, ma senza lo schiocco di un taglio netto
    if (frame >= n.back) { g = Math.min(g, (frame - n.back + 1) / 2); continue; }
    g = Math.min(g, Math.max(0, Math.min(1, (hush - frame) / MUSIC_FALL)));
  }
  return Math.max(0, g);
};

/** Stop and go (piano 4 §3): la musica tace di colpo (2 fotogrammi) nelle finestre date e rientra in 3. Non è un abbassamento: è
 *  un vuoto costruito sul battito del gesto (il tocco di ▶), poi la band riparte da dove sarebbe arrivata. */
export const stopGain = (frame: number, windows: [number, number][]): number => {
  for (const [a, b] of windows) {
    if (frame < a || frame >= b + 3) continue;
    if (frame < a + 2) return 1 - (frame - a + 1) / 2;
    if (frame >= b) return (frame - b + 1) / 3;
    return 0;
  }
  return 1;
};
