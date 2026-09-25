import React from "react";
import { THEME } from "../theme.ts";
import { UI } from "./UiTokens.ts";
import { contextFill, workBar, workCount } from "./aside.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

/**
 * I tre pannelli della Panoramica (Work, Open questions, Context), tolti da `Aside` perché li disegna anche la forma unica
 * (corto, 25/09): stesso markup, stesse animazioni sul disegno `d` (0-1). `Aside` li mette fermi a sinistra; la forma li
 * mette dentro la sua scatola. Larghezza 760 come in `Aside`.
 */
export const BriefWork: React.FC<{ n?: number; note?: string; bars?: number[]; d: number }> = ({ n = 1, note, bars = [1, 1, 1], d }) => (
  <>
    <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefGood }}>Now</div>
    <div style={{ display: "flex", alignItems: "baseline", gap: 18, marginTop: 6 }}>
      {/* anche qui il numero sale da 0, come gli altri pannelli (Franz, 18/09 22:01) */}
      <span style={{ fontSize: 132, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums" }}>{workCount(n, d)}</span>
      <span style={{ fontSize: 56, color: THEME.dim }}>working</span>
    </div>
    <div style={{ display: "flex", gap: 14, marginTop: 26 }}>
      {bars.map((f, k) => (
        <span key={k} style={{ flex: f, height: 22, borderRadius: 11, background: [UI.waiting, UI.busy, UI.idle][k % 3], transform: `scaleX(${workBar(d, k)})`, transformOrigin: "0 50%" }} />
      ))}
    </div>
    <div style={{ marginTop: 22, fontSize: 38, color: THEME.dim }}>{note}</div>
  </>
);

export const BriefQuestions: React.FC<{ n?: number; note?: string; quote?: string; d: number }> = ({ n = 1, note, quote, d }) => (
  <>
    <div style={{ fontSize: 40, fontWeight: 500, color: UI.waiting }}>Open questions</div>
    {/* semplice passaggio da 0 a 1, senza anelli */}
    <div style={{ fontSize: 190, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.04em", color: UI.waiting, marginTop: 4, fontVariantNumeric: "tabular-nums" }}>{Math.round(n * clamp(d * 2))}</div>
    <div style={{ marginTop: 18, fontSize: 40, color: THEME.dim }}>{note}</div>
    <div style={{ marginTop: 10, fontSize: 44, color: THEME.white, opacity: clamp(d * 1.6 - 0.5), maxWidth: 700 }}>{quote}</div>
  </>
);

/** `fill(k)`, `grow`, `textOpacity`: il finale del film lungo, dove le barre si riempiono fino in fondo e diventano la
 *  tapparella (Aside li passa); senza, il pannello si disegna e basta. */
export const BriefContext: React.FC<{ rows?: { name: string; pct: number }[]; d: number; fill?: (k: number) => number; grow?: number; textOpacity?: number }> = ({ rows = [], d, fill, grow = 0, textOpacity = 1 }) => (
  <>
    <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefGood }}>Context</div>
    {rows.map((r, k) => {
      const p = fill ? fill(k) : contextFill(d, k);
      return (
        <div key={k} style={{ marginTop: k ? 34 : 18 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", fontSize: 50, opacity: textOpacity }}>
            <span>{r.name}</span>
            <span style={{ color: UI.briefRing, fontVariantNumeric: "tabular-nums" }}>{Math.round(r.pct * p)} %</span>
          </div>
          {/* il finale: le barre si riempiono fino in fondo e restano ferme; da lì nasce la tapparella in Three */}
          <div style={{ marginTop: 12, height: 16, borderRadius: 8, background: "rgba(139,180,247,.18)", width: 760 }}>
            <div style={{ width: `${r.pct * p + (100 - r.pct * p) * grow}%`, height: "100%", borderRadius: 8, background: UI.briefRing }} />
          </div>
        </div>
      );
    })}
  </>
);
