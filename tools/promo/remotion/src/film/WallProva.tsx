import React from "react";
import { AbsoluteFill } from "remotion";
import { AccountWall } from "./ui/AccountWall.tsx";
import { useFilmFonts } from "./fonts.ts";

/** Prova isolata del muro degli account (revisione 3D, momento 2). Non è nel film. */
export const WallProva: React.FC<{ frames?: number }> = ({ frames = 130 }) => {
  useFilmFonts();
  return (
    <AbsoluteFill style={{ background: "#0B1A1F" }}>
      <AccountWall frames={frames} />
    </AbsoluteFill>
  );
};
