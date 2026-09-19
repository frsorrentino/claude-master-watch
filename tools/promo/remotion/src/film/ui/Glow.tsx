import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { glowAt } from "./glow.ts";
import { UI } from "./UiTokens.ts";

/**
 * L'alone della notifica che cresce dal display fino a riempire il quadro e poi si ritira (piano: «la luce diventa la
 * scena»). Mentre è pieno, sotto si rifà l'inquadratura: l'orologio passa dal centro alla colonna di destra e il titolo
 * entra a sinistra, e non si vede nulla di quel salto. Disegnato a livello di film, sopra le due scene.
 * `cx`, `cy` in frazioni di quadro: il centro del display da cui la luce nasce.
 */
export const Glow: React.FC<{ frames: number; cx?: number; cy?: number; color?: string }> = ({ frames, cx = 0.5, cy = 0.5, color = UI.waiting }) => {
  const f = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const g = glowAt(f / Math.max(1, frames));
  if (g.r <= 0.001) return null;
  // il raggio va da poco più del display alla diagonale del quadro, così ai bordi non resta mai un angolo scoperto
  const r = 320 + g.r * (Math.hypot(width, height) / 2 + 200);
  return (
    <div style={{ position: "absolute", inset: 0, pointerEvents: "none", overflow: "hidden" }}>
      <div style={{ position: "absolute", left: cx * width, top: cy * height, width: 0, height: 0 }}>
        <div style={{ translate: "-50% -50%", width: r * 2, height: r * 2, borderRadius: "50%",
          background: `radial-gradient(circle, ${color} 0%, ${color} ${52 + 30 * g.cover}%, rgba(233,183,79,0) 78%)`, opacity: 0.32 + 0.68 * g.cover }} />
      </div>
      {/* al culmine il campo è pieno e uniforme: è lì che, sotto, tutto cambia posto */}
      <div style={{ position: "absolute", inset: 0, background: color, opacity: g.cover }} />
    </div>
  );
};
