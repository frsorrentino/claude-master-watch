import React from "react";
import { useCurrentFrame } from "remotion";
import { UI } from "./UiTokens.ts";
import { whipAt } from "./whip.ts";

/**
 * La frustata corallo tra gli atti (piano 3, metro: Ask 4,5 s, il nastro di colore che attraversa il quadro): una striscia
 * inclinata, sfocata, che passa in pochi fotogrammi sul battito del cambio d'atto. Unico accento del film, il corallo del segno.
 */
export const Whip: React.FC<{ frames?: number; width: number; height: number }> = ({ frames = 8, width, height }) => {
  const f = useCurrentFrame();
  const { x, light } = whipAt(f, frames);
  const band = 340;
  return (
    <div style={{ position: "absolute", inset: 0, overflow: "hidden", pointerEvents: "none" }}>
      <div style={{ position: "absolute", top: -height * 0.3, height: height * 1.6, width: band, left: x * (width + band * 2) - band, rotate: "-14deg", opacity: 0.55 * light,
        background: `linear-gradient(90deg, rgba(217,119,87,0) 0%, ${UI.coral} 45%, ${UI.coral} 55%, rgba(217,119,87,0) 100%)`, filter: "blur(26px)", mixBlendMode: "screen" }} />
    </div>
  );
};
