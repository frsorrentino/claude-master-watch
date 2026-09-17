import React from "react";
import { UI } from "./UiTokens.ts";

/** La card di sessione dell'app, ricostruita (spazio 400×… px, poi scalata da chi la usa). `flip` 0-1: l'icona di stato gira
 *  da «lavora» (▶ su blu) a «fatto» (✓ su verde) attorno all'asse verticale, e il testo cambia a metà giro. */
export const UiCard: React.FC<{ name: string; age: string; busyText: string; doneText: string; flip: number }> = ({ name, age, busyText, doneText, flip }) => {
  const done = flip >= 0.5;
  const turn = flip < 0.5 ? flip * 180 : (flip - 1) * 180;                 // 0→90°, poi −90°→0: a metà l'icona è di taglio e cambia faccia
  return (
    <div style={{ width: 400, padding: "20px 24px 24px", borderRadius: 30, background: UI.surfaceHigh, boxShadow: `0 40px 80px -30px rgba(0,0,0,.7), 0 0 0 1.5px ${done ? "rgba(52,199,89,.55)" : "rgba(76,125,255,.55)"}, 0 0 46px -8px ${done ? "rgba(52,199,89,.45)" : "rgba(76,125,255,.45)"}`, fontFamily: "Inter", color: UI.text }}>
      <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
        <div style={{ width: 34, height: 34, borderRadius: 9, background: done ? UI.idle : UI.accent, display: "grid", placeItems: "center", transform: `rotateY(${turn}deg)` }}>
          <svg viewBox="0 0 24 24" width="22" height="22">
            {done ? <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" /> : <path d="M8 5.5v13l11-6.5z" fill="#fff" />}
          </svg>
        </div>
        <div style={{ fontFamily: "Noto Sans Mono", fontSize: 24, color: UI.text2, flex: 1 }}>{name}</div>
        <div style={{ fontSize: 24, color: UI.text2 }}>{age}</div>
      </div>
      <div style={{ marginTop: 12, fontSize: 29, lineHeight: 1.22, fontWeight: 500 }}>{done ? doneText : busyText}</div>
    </div>
  );
};
