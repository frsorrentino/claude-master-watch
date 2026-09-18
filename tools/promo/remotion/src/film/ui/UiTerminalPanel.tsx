import React from "react";
import { UI } from "./UiTokens.ts";
import { soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

/**
 * Il terminale dell'orologio (`TerminalStyle`: mono 13 sp, intestazione «Terminal · 08:08» con i filetti) che, lasciando il
 * display, diventa la finestra del terminale del PC (Franz, 18/09 08:46: «ricreare quello del PC come sfondo per mostrare la
 * corrispondenza»). Nello spazio del display (1 dp = 2 px). `morph` 0-1: 0 = com'è sul display (nero, senza cornice),
 * 1 = finestra del PC (superficie scura, angoli 14, barra del titolo, prompt); le righe sono le stesse in entrambi, e arrivano
 * una per volta (`shown`: quante righe sono visibili, con frazione per l'ultima che entra).
 */
export const UiTerminalPanel: React.FC<{ w: number; header: string; title: string; lines: string[]; shown: number; morph: number }> = ({ w, header, title, lines, shown, morph }) => {
  const m = soft(clamp(morph));
  const bar = 44 * m, pad = 26 + 6 * m;                 // 13 dp di rientro sul display (misurato: la riga parte a x 62)
  return (
    <div style={{ width: w, boxSizing: "border-box", borderRadius: 14 * m, background: `rgba(30,34,41,${m})`, boxShadow: m > 0 ? `0 ${30 * m}px ${60 * m}px ${-10 * m}px rgba(4,5,12,${0.7 * m}), inset 0 0 0 ${1.5 * m}px rgba(235,244,255,${0.09 * m})` : undefined, overflow: "hidden", fontFamily: "Cousine", color: UI.text }}>
      {/* barra del titolo del PC: nasce dall'intestazione dell'orologio */}
      <div style={{ height: bar, display: "flex", alignItems: "center", gap: 10, padding: `0 ${pad}px`, background: `rgba(255,255,255,${0.04 * m})`, opacity: m, fontFamily: "Inter", fontSize: 22, color: UI.text2 }}>
        {[0, 1, 2].map((k) => <span key={k} style={{ width: 16, height: 16, borderRadius: "50%", background: "rgba(235,244,255,.22)" }} />)}
        <span style={{ marginLeft: 10 }}>{title}</span>
      </div>
      <div style={{ padding: `${8 + 10 * m}px ${pad}px ${10 + 14 * m}px` }}>
        {/* intestazione dell'orologio: «— Terminal · 08:08 —» verde con i filetti, sparisce diventando PC */}
        <div style={{ display: "flex", alignItems: "center", gap: 12, height: 34 * (1 - m), marginBottom: 21 * (1 - m), opacity: 1 - m, overflow: "hidden", fontFamily: "Roboto", fontWeight: 500, fontSize: 30, color: UI.briefGood }}>
          <span style={{ flex: 1, height: 2, background: "rgba(139,180,247,.5)" }} /><span>{header}</span><span style={{ flex: 1, height: 2, background: "rgba(139,180,247,.5)" }} />
        </div>
        {lines.map((l, i) => {
          const e = clamp(shown - i);
          const prompt = l.startsWith("$ ");
          return (
            <div key={i} style={{ fontSize: 27, lineHeight: "40px", whiteSpace: "pre", opacity: e, translate: `0 ${(1 - e) * 10}px`, color: prompt ? UI.text : UI.text2, fontWeight: 400 }}>
              {m > 0.5 && prompt ? <span style={{ color: UI.coral }}>❯ </span> : null}{prompt && m > 0.5 ? l.slice(2) : l}
            </div>
          );
        })}
        {/* il cursore del PC: lampeggia solo nella finestra */}
        <div style={{ height: 40 * m, opacity: m, fontSize: 27, lineHeight: "40px" }}><span style={{ color: UI.coral }}>❯ </span><span style={{ display: "inline-block", width: 14, height: 26, background: UI.text, verticalAlign: "-4px", opacity: Math.floor(shown * 3) % 2 === 0 ? 1 : 0.15 }} /></div>
      </div>
    </div>
  );
};
