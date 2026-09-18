import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { LogoThree } from "./ui/LogoThree.tsx";
import { logoSpinAt } from "./ui/logo3d.ts";
import { THEME } from "./theme.ts";
import { useFilmFonts } from "./fonts.ts";

/** Prova isolata della chiusura in 3D (revisione, momento 3): il segno si presenta e il nome entra sotto. Non è nel film. */
export const LogoProva: React.FC<{ frames?: number }> = ({ frames = 90 }) => {
  useFilmFonts();
  const f = useCurrentFrame();
  const s = logoSpinAt(Math.min(1, f / frames));
  return (
    <AbsoluteFill style={{ background: "#07090F" }}>
      <LogoThree frames={frames} />
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", paddingTop: 560 }}>
        <div style={{ fontFamily: "Inter", fontWeight: 600, fontSize: 92, letterSpacing: "-0.02em", color: THEME.white, opacity: s.name, translate: `0 ${(1 - s.name) * 26}px` }}>Claude Master</div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
