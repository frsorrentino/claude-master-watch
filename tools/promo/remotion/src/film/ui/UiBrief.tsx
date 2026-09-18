import React from "react";
import { UI } from "./UiTokens.ts";

/**
 * Le schede della Panoramica ricostruite (Franz, 18/09 19:49): «Now» con le barre del lavoro, «Open questions» e «Context»
 * con le percentuali. Misure e colori presi dai fotogrammi della clip: etichetta verde o ambra 27 px, numero grande 58 px,
 * barre alte 14 px con angoli tondi, riga di dettaglio 30 px grigia. Spazio del display (1 dp = 2 px), poi scalate da chi le usa.
 */
const Card: React.FC<{ w: number; label: string; labelColor: string; children: React.ReactNode }> = ({ w, label, labelColor, children }) => (
  <div style={{ width: w, boxSizing: "border-box", padding: "22px 26px 26px", borderRadius: 42, background: UI.surface, fontFamily: "Roboto", color: UI.text }}>
    <div style={{ fontWeight: 500, fontSize: 28, color: labelColor, marginBottom: 6 }}>{label}</div>
    {children}
  </div>
);

export const UiBriefWork: React.FC<{ w: number; n: number; note: string; bars: number[] }> = ({ w, n, note, bars }) => (
  <Card w={w} label="Now" labelColor={UI.briefGood}>
    <div style={{ display: "flex", alignItems: "baseline", gap: 12 }}>
      <span style={{ fontSize: 58, fontWeight: 500, lineHeight: 1 }}>{n}</span>
      <span style={{ fontSize: 34, color: UI.text2 }}>working</span>
    </div>
    <div style={{ display: "flex", gap: 10, marginTop: 14 }}>
      {bars.map((f, i) => <span key={i} style={{ flex: f, height: 14, borderRadius: 7, background: [UI.waiting, UI.busy, UI.idle][i % 3] }} />)}
    </div>
    <div style={{ marginTop: 14, fontSize: 30, color: UI.text2, whiteSpace: "nowrap", overflow: "hidden" }}>{note}</div>
  </Card>
);

export const UiBriefQuestions: React.FC<{ w: number; n: number; note: string }> = ({ w, n, note }) => (
  <Card w={w} label="Open questions" labelColor={UI.waiting}>
    <div style={{ fontSize: 58, fontWeight: 500, lineHeight: 1, color: UI.waiting }}>{n}</div>
    <div style={{ marginTop: 10, fontSize: 32, color: UI.text2 }}>{note}</div>
  </Card>
);

export const UiBriefContext: React.FC<{ w: number; rows: { name: string; pct: number }[] }> = ({ w, rows }) => (
  <Card w={w} label="Context" labelColor={UI.briefGood}>
    {rows.map((r, i) => (
      <div key={i} style={{ marginTop: i ? 18 : 8 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", fontSize: 34 }}>
          <span>{r.name}</span><span style={{ color: UI.briefRing }}>{r.pct} %</span>
        </div>
        <div style={{ marginTop: 8, height: 10, borderRadius: 5, background: "rgba(139,180,247,.18)" }}>
          <div style={{ width: `${r.pct}%`, height: "100%", borderRadius: 5, background: UI.briefRing }} />
        </div>
      </div>
    ))}
  </Card>
);
