import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { WordMask } from "./WordMask.tsx";
import { THEME } from "./theme.ts";

export const REPO = "github.com/frsorrentino/claude-master-watch";

export const EndCard: React.FC<{ beat: number }> = ({ beat }) => {
  const f = useCurrentFrame();
  const o = (from: number) => interpolate(f, [from, from + 12], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const small: React.CSSProperties = { fontFamily: "Inter", fontWeight: 500, color: THEME.dim, textAlign: "center" };
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", gap: 22, paddingTop: 330 }}>
      <WordMask lines={["Claude Master"]} perWordFrames={beat} align="center" />
      <div style={{ ...small, fontSize: THEME.service, color: THEME.white, opacity: o(beat * 3) }}>Free. <span style={{ color: THEME.accent }}>Open source.</span></div>
      <div style={{ ...small, fontSize: 34, opacity: o(beat * 5) }}>{REPO}</div>
      <div style={{ ...small, position: "absolute", bottom: 64, left: 0, right: 0, fontSize: 22, lineHeight: 1.5, opacity: o(beat * 6) }}>
        An independent project, not affiliated with Anthropic.<br />Wear OS by Google and Pixel Watch are trademarks of Google LLC. Synthetic voice.
      </div>
    </AbsoluteFill>
  );
};
