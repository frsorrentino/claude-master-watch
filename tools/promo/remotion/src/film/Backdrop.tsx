import React from "react";
import { AbsoluteFill } from "remotion";
import { ACT_BG, THEME } from "./theme.ts";
import type { Act } from "./timeline.ts";

export const Backdrop: React.FC<{ act: Act; glowX?: number }> = ({ act, glowX = THEME.watchX }) => {
  const [c0, c1, c2, glow] = ACT_BG[act];
  return (
    <AbsoluteFill style={{ background: `radial-gradient(120% 120% at 70% 30%, ${c0} 0%, ${c1} 45%, ${c2} 100%)` }}>
      <AbsoluteFill style={{ background: `radial-gradient(23% 42% at ${glowX * 100}% 50%, ${glow} 0%, rgba(0,0,0,0) 100%)` }} />
    </AbsoluteFill>
  );
};
