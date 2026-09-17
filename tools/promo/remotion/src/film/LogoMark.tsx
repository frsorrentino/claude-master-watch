import React from "react";

/** Il segno dell'app (scelto da Franz il 17/09: «>_» corallo dentro un quadrante aperto in basso, senza disco di fondo),
 *  disegnato nello spazio 480×480 del display. `draw` 0-1: l'arco si riempie fino al suo 70 %. */
export const LogoMark: React.FC<{ draw: number }> = ({ draw }) => {
  const c = 240, u = 300 / 92, g = u * 0.72, w = 7.2 * g, cy = c - 2 * u;
  const r = 40.2 * u, aw = 5.6 * u, C = 2 * Math.PI * r, sweep = (280 / 360) * C;
  const ring = (len: number, color: string) => (
    <circle cx={c} cy={c} r={r} fill="none" stroke={color} strokeWidth={aw} strokeLinecap="round" strokeDasharray={`${len} ${C}`} transform={`rotate(130 ${c} ${c})`} />
  );
  return (
    <svg viewBox="0 0 480 480" style={{ position: "absolute", inset: 0 }}>
      {ring(sweep, "#3A404C")}
      {draw > 0.01 ? ring(sweep * 0.7 * draw, "#D97757") : null}
      <polyline points={`${c - 22 * g},${cy - 17 * g} ${c - 3 * g},${cy} ${c - 22 * g},${cy + 17 * g}`} fill="none" stroke="#D97757" strokeWidth={w} strokeLinecap="round" strokeLinejoin="round" />
      <rect x={c + 2 * g} y={cy + 13.4 * g} width={21 * g} height={w} rx={w / 2} fill="#D97757" />
    </svg>
  );
};
