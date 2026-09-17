import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Watch, type Mask } from "./Watch";
import scenes from "./scenes.json";

/** Una scena: la clip già tagliata a 30 fps, la sua durata, la didascalia e le toppe sui testi di sistema (secondi della clip). */
export type Scene = {
  file: string;
  seconds: number;
  caption?: string;
  masks?: (Mask & { from: number; to: number })[];
};

export const SCENES = scenes as Scene[];
export const FPS = 30;
export const FADE = 12;

export const durationInFrames = () =>
  SCENES.reduce((n, s) => n + Math.round(s.seconds * FPS), 0) - (SCENES.length - 1) * FADE;

const SceneView: React.FC<{ scene: Scene }> = ({ scene }) => {
  const frame = useCurrentFrame();
  const { fps, width, height } = useVideoConfig();
  const t = frame / fps;
  const zoom = interpolate(frame, [0, fps * 1.4], [1.06, 1], { extrapolateRight: "clamp", easing: Easing.out(Easing.cubic) });
  const captionIn = interpolate(frame, [fps * 0.3, fps * 0.9], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const d = Math.round(height * 0.66);
  const masks = (scene.masks ?? []).filter((m) => t >= m.from && t < m.to);
  return (
    <AbsoluteFill
      style={{
        background: "radial-gradient(120% 120% at 70% 30%, #3a4468 0%, #242a42 45%, #14172a 100%)",
        flexDirection: "row", alignItems: "center", justifyContent: "center", gap: width * 0.05,
      }}
    >
      {scene.caption ? (
        <div
          style={{
            width: width * 0.3, opacity: captionIn, translate: `0 ${(1 - captionIn) * 18}px`, color: "#eef0fb",
            fontFamily: "Google Sans, Inter, sans-serif", fontSize: 64, fontWeight: 500, lineHeight: 1.12, letterSpacing: -0.5,
          }}
        >
          {scene.caption}
        </div>
      ) : null}
      <div style={{ scale: String(zoom) }}>
        <Watch src={scene.file} d={d} masks={masks} />
      </div>
    </AbsoluteFill>
  );
};

export const Release: React.FC = () => (
  <TransitionSeries>
    {SCENES.flatMap((s, i) => [
      <TransitionSeries.Sequence key={`s${i}`} durationInFrames={Math.round(s.seconds * FPS)}>
        <SceneView scene={s} />
      </TransitionSeries.Sequence>,
      i < SCENES.length - 1 ? (
        <TransitionSeries.Transition key={`t${i}`} presentation={fade()} timing={linearTiming({ durationInFrames: FADE })} />
      ) : null,
    ])}
  </TransitionSeries>
);
