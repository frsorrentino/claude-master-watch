import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { THEME } from "../theme.ts";
import { blinkAt } from "./blink.ts";

/**
 * «One glance.»: la parola in colore lascia il titolo e si posa in alto (dove sarà il titolo della scena dopo) mentre la card
 * esce dal display e si ferma grande al centro; poi due palpebre nere si chiudono e si riaprono sulla scena dopo. Stesso
 * carattere e stesso colore del titolo: è la stessa parola, non un effetto sopra.
 */
export const Blink: React.FC<{ word: string; cut: number; from: [number, number]; to: [number, number]; toSize?: number }> = ({ word, cut, from, to, toSize = THEME.service }) => {
  const f = useCurrentFrame();
  const { height } = useVideoConfig();
  const b = blinkAt(f, cut);
  const x = from[0] + (to[0] - from[0]) * b.travel, y = from[1] + (to[1] - from[1]) * b.travel;
  const size = THEME.title + (toSize - THEME.title) * b.travel;
  return (
    <div style={{ position: "absolute", inset: 0, overflow: "hidden", pointerEvents: "none" }}>
      {b.word ? (
        <div style={{ position: "absolute", left: x, top: y, translate: "0 -50%", fontFamily: "Inter", fontWeight: 600, fontSize: size, lineHeight: 1.04, letterSpacing: "-0.02em", color: THEME.accent, whiteSpace: "nowrap" }}>{word}</div>
      ) : null}
      <div style={{ position: "absolute", left: 0, right: 0, top: 0, height: (height / 2) * b.lid, background: "#000" }} />
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: (height / 2) * b.lid, background: "#000" }} />
    </div>
  );
};
