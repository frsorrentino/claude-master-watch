import React from "react";
import { UI } from "./UiTokens.ts";

/**
 * Il segno dell'app in piccolo, in linea con il testo (piano 4, filo 1: è il punto finale di ogni titolo). Stessa geometria di
 * `LogoMark` ridotta a un quadrato di lato `size`: arco da 130° per 280°, riempito al 70 % (`fill` 0-1), «>_» corallo.
 * `fill` 0 = solo binario (il segno vuoto), 1 = com'è nel logo.
 */
export const Sign: React.FC<{ size: number; fill?: number; style?: React.CSSProperties }> = ({ size, fill = 0.7, style }) => {
  const c = 50, u = 100 / 480 * (300 / 92), g = u * 0.72, w = 7.2 * g, cy = c - 2 * u;
  const r = 40.2 * u, aw = 5.6 * u, C = 2 * Math.PI * r, sweep = (280 / 360) * C;
  const ring = (len: number, color: string) => (
    <circle cx={c} cy={c} r={r} fill="none" stroke={color} strokeWidth={aw} strokeLinecap="round" strokeDasharray={`${len} ${C}`} transform={`rotate(130 ${c} ${c})`} />
  );
  return (
    <svg viewBox="0 0 100 100" width={size} height={size} style={{ display: "inline-block", overflow: "visible", ...style }}>
      {ring(sweep, UI.track)}
      {fill > 0.01 ? ring(sweep * fill, UI.coral) : null}
      <polyline points={`${c - 22 * g},${cy - 17 * g} ${c - 3 * g},${cy} ${c - 22 * g},${cy + 17 * g}`} fill="none" stroke={UI.coral} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round" />
      <rect x={c + 2 * g} y={cy + 13.4 * g} width={21 * g} height={w} rx={w / 2} fill={UI.coral} />
    </svg>
  );
};
