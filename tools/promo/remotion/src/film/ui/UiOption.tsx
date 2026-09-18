import React from "react";
import { UI } from "./UiTokens.ts";

/**
 * Il tasto della domanda (`WideButton.kt`), nello spazio del display (1 dp = 2 px): pillola a tutta larghezza, 56 dp,
 * primaria piena (#D3E3FD su #0A2050) o tonale (#23272E su #F2F4F7), testo Roboto 500 a 30 px (misurato: «1 · yes» a 13 s
 * della clip `answer`). `build` 0-1: nasce come contorno (0-0,5) e si riempie (0,5-1); `ring` 0-1: l'anello corallo della
 * pressione lunga che corre attorno alla pillola, da ore 12 in senso orario.
 */
/** La pillola come tracciato che parte da ore 12 (centro del lato alto) e gira in senso orario: così il tratteggio che si allunga
 *  disegna l'anello della pressione lunga come `LongPressArc`, dalle 12. */
const pill = (x: number, y: number, w: number, h: number): string => {
  const r = h / 2, cx = x + w / 2;
  return `M ${cx} ${y} H ${x + w - r} A ${r} ${r} 0 0 1 ${x + w - r} ${y + h} H ${x + r} A ${r} ${r} 0 0 1 ${x + r} ${y} Z`;
};

export const UiOption: React.FC<{ w: number; h: number; label: string; primary?: boolean; build?: number; ring?: number }> = ({ w, h, label, primary = false, build = 1, ring = 0 }) => {
  const fill = primary ? UI.primary : UI.surface, ink = primary ? UI.onPrimary : UI.text;
  const outline = Math.min(1, build / 0.5), filled = Math.min(1, Math.max(0, (build - 0.5) / 0.4));
  const r = h / 2, pad = 6;
  // il contorno si disegna con un tratteggio che si allunga: perimetro della pillola = 2·(w − h) + π·h
  const per = 2 * (w - h) + Math.PI * h;
  const rw = w + 2 * pad, rh = h + 2 * pad, rper = 2 * (rw - rh) + Math.PI * rh;
  return (
    <div style={{ position: "relative", width: w, height: h }}>
      <svg width={rw} height={rh} viewBox={`${-pad} ${-pad} ${rw} ${rh}`} style={{ position: "absolute", left: -pad, top: -pad, overflow: "visible" }}>
        <rect x={0} y={0} width={w} height={h} rx={r} fill={fill} fillOpacity={filled} />
        <rect x={1} y={1} width={w - 2} height={h - 2} rx={r - 1} fill="none" stroke={primary ? UI.primary : UI.text2} strokeWidth={2} strokeDasharray={`${per * outline} ${per}`} opacity={1 - filled} />
        {ring > 0 ? <path d={pill(-pad, -pad, rw, rh)} fill="none" stroke={UI.coral} strokeWidth={6} strokeLinecap="round" strokeDasharray={`${rper * Math.min(1, ring)} ${rper}`} /> : null}
      </svg>
      <div style={{ position: "absolute", left: 30, top: 0, height: h, display: "flex", alignItems: "center", fontFamily: "Roboto", fontWeight: 500, fontSize: 30, color: ink, opacity: filled, whiteSpace: "nowrap" }}>{label}</div>
    </div>
  );
};
