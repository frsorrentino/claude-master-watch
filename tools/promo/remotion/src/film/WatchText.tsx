import React, { useEffect, useState } from "react";
import { Easing, Sequence, continueRender, delayRender, interpolate, staticFile, useCurrentFrame } from "remotion";
import { spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import type { Scene } from "./timeline.ts";
import { THEME } from "./theme.ts";

const clampBoth = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const big: React.CSSProperties = { fontFamily: "Inter", fontWeight: 600, color: THEME.white, letterSpacing: "-0.02em" };

/** «11 %» enorme che conta da zero. */
export const Counter: React.FC<{ to: number; suffix: string; frames: number }> = ({ to, suffix, frames }) => {
  const f = useCurrentFrame();
  const v = Math.round(interpolate(f, [0, frames], [0, to], { ...clampBoth, easing: Easing.out(Easing.cubic) }));
  return <div style={{ ...big, fontSize: 300, lineHeight: 1, fontVariantNumeric: "tabular-nums" }}>{v}<span style={{ color: THEME.accent }}>{suffix}</span></div>;
};

/** La frase dettata si compone in grande, con cursore. */
export const TypedLine: React.FC<{ text: string; frames: number }> = ({ text, frames }) => {
  const f = useCurrentFrame();
  const n = Math.round(interpolate(f, [0, frames], [0, text.length], clampBoth));
  return <div style={{ ...big, fontSize: 64, lineHeight: 1.2, maxWidth: 760 }}>{text.slice(0, n)}<span style={{ color: THEME.accent, opacity: f % 16 < 8 ? 1 : 0 }}>|</span></div>;
};

/** Le righe nuove del terminale escono dallo schermo, grandi, monospazio: nascono a destra (verso l'orologio) e si impilano. */
export const TerminalLines: React.FC<{ lines: string[]; everyFrames: number }> = ({ lines, everyFrames }) => {
  const f = useCurrentFrame();
  return (
    <div style={{ fontFamily: "Noto Sans Mono", fontSize: 46, lineHeight: 1.5, color: THEME.white, whiteSpace: "pre" }}>
      {lines.map((l, i) => {
        const e = interpolate(f, [i * everyFrames, i * everyFrames + 10], [0, 1], { ...clampBoth, easing: Easing.out(Easing.cubic) });
        return <div key={i} style={{ opacity: e, translate: `${(1 - e) * 420}px 0`, scale: String(0.6 + 0.4 * e), transformOrigin: "100% 50%" }}>{l}</div>;
      })}
    </div>
  );
};

type Word = { start: number; end: number; word: string };

/** Le parole compaiono mentre l'orologio le dice (tempi da whisper); quella in corso è in colore. */
export const SpokenWords: React.FC<{ file: string; fps: number }> = ({ file, fps }) => {
  const f = useCurrentFrame();
  const [words, setWords] = useState<Word[]>([]);
  const [handle] = useState(() => delayRender(`parole ${file}`));
  useEffect(() => { fetch(staticFile(`audio/${file}`)).then((r) => r.json()).then((w) => { setWords(w); continueRender(handle); }); }, [file, handle]);
  const t = f / fps;
  return (
    <div style={{ ...big, fontSize: 54, lineHeight: 1.25, maxWidth: 780 }}>
      {words.filter((w) => w.start <= t).map((w, i) => <span key={i} style={{ color: t < w.end ? THEME.accent : THEME.white }}>{w.word} </span>)}
    </div>
  );
};

/** `offsetFrames`: i fotogrammi già passati dall'inizio della scena quando comincia la colonna del testo che ospita questi elementi. */
export const watchTextFor = (scene: Scene, g: Grid, offsetFrames = 0): React.ReactNode => {
  const at = (b: number) => spanFrames(g, scene.at, b);
  return (scene.fx ?? []).map((e, i) => {
    const node = e.kind === "counter" ? <Counter to={e.to} suffix={e.suffix} frames={at(e.at + e.len) - at(e.at)} />
      : e.kind === "typed" ? <TypedLine text={e.text} frames={at(e.at + e.len) - at(e.at)} />
      : e.kind === "terminal" ? <TerminalLines lines={e.lines} everyFrames={at(e.every)} />
      : e.kind === "spoken" ? <SpokenWords file={e.words} fps={g.fps} /> : null;
    return node ? <Sequence key={i} from={at(e.at) - offsetFrames} layout="none">{node}</Sequence> : null;
  });
};
