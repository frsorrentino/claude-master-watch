import type { Timeline } from "./timeline.ts";

export type SfxName = "notify" | "thump" | "tick" | "pressRise" | "whoosh";
export type SfxCue = { beat: number; name: SfxName; gainDb: number };

export const dbToGain = (db: number): number => Math.pow(10, db / 20);

/** Priorità quando due suoni cadono nello stesso battito: la notifica, poi la pressione, il tocco, il soffio. */
const RANK: SfxName[] = ["notify", "pressRise", "tick", "whoosh", "thump"];

export const sfxCues = (t: Timeline): SfxCue[] => {
  const all: SfxCue[] = [];
  for (const s of t.scenes) {
    if (s.text && (s.text.size ?? "title") === "title" && !s.watch) all.push({ beat: s.at + (s.text.at ?? 0), name: "whoosh", gainDb: -26 });
    for (const f of s.fx ?? []) {
      const beat = s.at + f.at;
      if (f.kind === "haptic") all.push({ beat, name: "notify", gainDb: -16 });
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
