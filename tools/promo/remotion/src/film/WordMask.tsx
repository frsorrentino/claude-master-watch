import React from "react";
import { Easing, interpolate, useCurrentFrame } from "remotion";
import { THEME } from "./theme.ts";
import { Sign } from "./ui/Sign.tsx";

/** Ogni parola sale da una fessura con frenata morbida ed esce verso l'alto. Una sola parola in colore per frase. */
export const WordMask: React.FC<{
  lines: string[]; accent?: string; size?: "title" | "service"; perWordFrames: number; exitAt?: number; sub?: string; align?: "left" | "center";
}> = ({ lines, accent, size = "title", perWordFrames, exitAt, sub, align = "left" }) => {
  const frame = useCurrentFrame();
  const px = size === "title" ? THEME.title : THEME.service;
  const total = lines.reduce((n, l) => n + l.split(" ").length, 0);
  let k = 0;
  const word = (w: string, i: number) => {
    const start = i * perWordFrames;
    const up = interpolate(frame, [start, start + 14], [110, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1) });
    const away = exitAt === undefined ? 0 : interpolate(frame, [exitAt, exitAt + 8], [0, -115], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.in(Easing.cubic) });
    // il punto finale del titolo è il segno dell'app (piano 4, filo 1): la parola perde il suo «.» e il segno le sta accanto,
    // alla larghezza di un punto, sulla linea di base; entra ed esce con la parola
    const last = i === total - 1 && w.endsWith(".") && size === "title";
    const text = last ? w.slice(0, -1) : w;
    return (
      <span key={i} style={{ display: "inline-block", overflow: "hidden", verticalAlign: "bottom", padding: "0.08em 0 0.16em", marginRight: "0.26em" }}>
        <span style={{ display: "inline-block", translate: `0 ${up + away}%`, color: w === accent ? THEME.accent : THEME.white }}>
          {text}{last ? <Sign size={px * 0.5} style={{ marginLeft: "0.1em", verticalAlign: "-0.04em" }} /> : null}
        </span>
      </span>
    );
  };
  const subOut = exitAt === undefined ? 1 : interpolate(frame, [exitAt, exitAt + 8], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const subIn = interpolate(frame, [total * perWordFrames + 6, total * perWordFrames + 20], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <div style={{ fontFamily: "Inter", fontWeight: 600, fontSize: px, lineHeight: 1.04, letterSpacing: "-0.02em", textAlign: align, whiteSpace: "nowrap" }}>
      {lines.map((l, li) => <div key={li}>{l.split(" ").map((w) => word(w, k++))}</div>)}
      {sub ? <div style={{ marginTop: 26, fontWeight: 500, fontSize: THEME.service, letterSpacing: 0, color: THEME.dim, opacity: subIn * subOut }}>{sub}</div> : null}
    </div>
  );
};
