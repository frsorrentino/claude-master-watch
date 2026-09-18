import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { THEME } from "../theme.ts";
import { blinkAt } from "./blink.ts";

/**
 * «One glance.»: la parola in colore cresce dal suo posto nel titolo fino a riempire il quadro, poi due palpebre nere si
 * chiudono e si riaprono sulla scena dopo (riferimento: LangEase «Audio» che riempie il quadro; il battito è nostro).
 * Stesso carattere e stesso colore del titolo: è la stessa parola, non un effetto sopra.
 */
export const Blink: React.FC<{ word: string; cut: number; from?: [number, number] }> = ({ word, cut, from }) => {
  const f = useCurrentFrame();
  const { height } = useVideoConfig();
  const b = blinkAt(f, cut);
  return (
    <div style={{ position: "absolute", inset: 0, overflow: "hidden", pointerEvents: "none" }}>
      {b.word ? (
        // la parola sta dove sta nel titolo (seconda riga, a sinistra) e cresce dal suo bordo sinistro
        // parte esattamente dal posto della parola nel titolo (misurato sul fotogramma): niente doppione, è la stessa parola
        <div style={{ position: "absolute", left: from ? from[0] : THEME.leftMargin, top: from ? from[1] : height / 2, transformOrigin: "0 50%", scale: String(b.scale), translate: "0 -50%", fontFamily: "Inter", fontWeight: 600, fontSize: THEME.title, lineHeight: 1.04, letterSpacing: "-0.02em", color: THEME.accent, whiteSpace: "nowrap" }}>{word}</div>
      ) : null}
      <div style={{ position: "absolute", left: 0, right: 0, top: 0, height: (height / 2) * b.lid, background: "#000" }} />
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: (height / 2) * b.lid, background: "#000" }} />
    </div>
  );
};
