import React from "react";
import { Easing, Sequence, interpolate, useCurrentFrame } from "remotion";
import { spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import type { Scene } from "./timeline.ts";
import { THEME } from "./theme.ts";
import { cardOutAt } from "./ui/heroes.ts";
import { UI } from "./ui/UiTokens.ts";

const clampBoth = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** Il punto del tocco: compare pieno, si allarga e svanisce in 12 fotogrammi. */
export const TapDot: React.FC<{ x: number; y: number }> = ({ x, y }) => {
  const f = useCurrentFrame();
  const s = interpolate(f, [0, 12], [0.55, 1.25], { ...clampBoth, easing: Easing.out(Easing.cubic) });
  const o = interpolate(f, [0, 3, 12], [0, 0.55, 0], clampBoth);
  return <div style={{ position: "absolute", left: x - 34, top: y - 34, width: 68, height: 68, borderRadius: "50%", background: "#fff", opacity: o, scale: String(s) }} />;
};

/** L'arco della pressione lunga si chiude attorno alla cassa, partendo dalle ore 12. */
export const LongPressArc: React.FC<{ frames: number }> = ({ frames }) => {
  const f = useCurrentFrame();
  const p = interpolate(f, [0, frames], [0, 1], clampBoth);
  const o = interpolate(f, [frames, frames + 8], [1, 0], clampBoth);
  const r = 100 / 3 + 2.4, c = 2 * Math.PI * r;
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0, opacity: o, rotate: "-90deg" }}>
      <circle cx="50" cy="50" r={r} fill="none" stroke={THEME.accent} strokeWidth="0.9" strokeLinecap="round" strokeDasharray={`${c * p} ${c}`} />
    </svg>
  );
};

/** Due anelli di vibrazione dalla cassa, a cinque fotogrammi l'uno dall'altro. */
export const HapticRings: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0 }}>
      {[0, 5].map((d) => {
        const r = interpolate(f - d, [0, 18], [100 / 3, 100 / 3 + 9], { ...clampBoth, easing: Easing.out(Easing.quad) });
        const o = interpolate(f - d, [0, 2, 18], [0, 0.5, 0], clampBoth);
        return <circle key={d} cx="50" cy="50" r={r} fill="none" stroke="#fff" strokeWidth="0.5" opacity={o} />;
      })}
    </svg>
  );
};

/** Il posto lasciato dalla card che esce (piano 3): un fantasma del colore della superficie, senza testo, finché non rientra
 *  (una toppa nera si leggeva come un buco nel render: master, 18/09 02:22). */
export const CardHole: React.FC<{ rect: [number, number, number, number]; frames: number }> = ({ rect, frames }) => {
  const f = useCurrentFrame();
  const [x, y, w, h] = rect;
  const patch = cardOutAt(f / frames).patch;
  return (
    <>
      <div style={{ position: "absolute", left: x, top: y, width: w, height: h, borderRadius: 42, background: UI.bg, opacity: patch }} />
      <div style={{ position: "absolute", left: x, top: y, width: w, height: h, borderRadius: 42, background: UI.surface, opacity: 0.38 * patch }} />
    </>
  );
};

export const fxLayers = (scene: Scene, g: Grid): { overlay: React.ReactNode; around: React.ReactNode } => {
  const at = (b: number) => spanFrames(g, scene.at, b);
  const fx = scene.fx ?? [];
  return {
    overlay: fx.map((e, i) =>
      e.kind === "tap" ? <Sequence key={i} from={at(e.at)} durationInFrames={14} layout="none"><TapDot x={e.x} y={e.y} /></Sequence>
      : e.kind === "cardOut" ? <Sequence key={i} from={at(e.at)} durationInFrames={at(e.at + e.len) - at(e.at)} layout="none"><CardHole rect={e.rect} frames={at(e.at + e.len) - at(e.at)} /></Sequence> : null),
    around: fx.map((e, i) =>
      e.kind === "longPress" ? <Sequence key={i} from={at(e.at)} durationInFrames={at(e.at + e.len) - at(e.at) + 8} layout="none"><LongPressArc frames={at(e.at + e.len) - at(e.at)} /></Sequence>
      : e.kind === "haptic" ? <Sequence key={i} from={at(e.at)} durationInFrames={26} layout="none"><HapticRings /></Sequence> : null),
  };
};
