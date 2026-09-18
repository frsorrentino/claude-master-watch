import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { UI } from "./UiTokens.ts";
import { soft } from "../moves.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

/**
 * I dati della Panoramica accanto all'orologio (Franz, 18/09 20:33): NON escono dalla card e non volano — compaiono in
 * dissolvenza già fermi a sinistra, uno dopo l'altro, quando sul display passa la scheda corrispondente, e lì si animano
 * (le barre si riempiono, i numeri contano). Solo il gauge della quota conserva il volo: è l'eccezione approvata.
 */
export const Aside: React.FC<{ scene: Scene; g: Grid }> = ({ scene, g }) => {
  const frame = useCurrentFrame();
  const { height } = useVideoConfig();
  const list = (scene.fx ?? []).filter((f): f is Extract<Fx, { kind: "aside" }> => f.kind === "aside");
  if (!list.length) return null;
  return (
    <>
      {list.map((e, i) => {
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const t = (frame - from) / len;
        if (t < 0 || t >= 1) return null;
        const fade = e.out === "bars" ? 1 : 1 - soft(clamp((t - 0.88) / 0.12));
        const a = Math.min(soft(clamp(t / 0.12)), fade);   // entra in dissolvenza; esce in dissolvenza, salvo l'ultima che diventa la transizione
        const d = soft(clamp((t - 0.12) / 0.45));                                        // il dato si disegna sul posto
        return (
          <div key={i} style={{ position: "absolute", left: THEME.leftMargin, top: height / 2, width: 760, translate: "0 -50%", opacity: a, fontFamily: "Inter", color: THEME.white }}>
            {e.panel === "note" ? (
              <div style={{ fontSize: 76, fontWeight: 600, lineHeight: 1.12, letterSpacing: "-0.02em" }}>
                {(e.lines ?? []).map((l, k) => <div key={k} style={{ opacity: clamp(d * 2 - k * 0.5), translate: `0 ${(1 - clamp(d * 2 - k * 0.5)) * 18}px` }}>{k === (e.lines ?? []).length - 1 ? <span style={{ color: THEME.accent }}>{l}</span> : l}</div>)}
              </div>
            ) : e.panel === "pace" ? (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefRing }}>5-hour pace</div>
                <div style={{ display: "flex", alignItems: "baseline", gap: 16, marginTop: 6 }}>
                  <span style={{ fontSize: 132, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums" }}>{Math.round((e.n ?? 0) * d)}</span>
                  <span style={{ fontSize: 52, color: THEME.dim }}>% {e.note}</span>
                </div>
                {/* la linea del ritmo si disegna da sinistra: tratto pieno fino ad adesso, tratteggio sulla proiezione */}
                <svg width={700} height={190} style={{ marginTop: 22, overflow: "visible" }}>
                  <line x1={0} y1={188} x2={700} y2={188} stroke="rgba(235,244,255,.18)" strokeWidth={2} />
                  <path d="M 0 176 L 250 128 L 430 96" fill="none" stroke={UI.briefRing} strokeWidth={6} strokeLinecap="round" strokeDasharray={520} strokeDashoffset={520 * (1 - clamp(d * 1.5))} />
                  <path d="M 430 96 L 700 8" fill="none" stroke={UI.briefRing} strokeWidth={6} strokeLinecap="round" strokeDasharray="14 16" opacity={clamp(d * 1.5 - 0.7) * 0.8} />
                  <circle cx={430} cy={96} r={12} fill={UI.briefRing} opacity={clamp(d * 1.5 - 0.6)} />
                </svg>
              </>
            ) : e.panel === "work" ? (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefGood }}>Now</div>
                <div style={{ display: "flex", alignItems: "baseline", gap: 18, marginTop: 6 }}>
                  <span style={{ fontSize: 132, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em" }}>{e.n ?? 1}</span>
                  <span style={{ fontSize: 56, color: THEME.dim }}>working</span>
                </div>
                <div style={{ display: "flex", gap: 14, marginTop: 26 }}>
                  {(e.bars ?? [1, 1, 1]).map((f, k) => (
                    <span key={k} style={{ flex: f, height: 22, borderRadius: 11, background: [UI.waiting, UI.busy, UI.idle][k % 3], transform: `scaleX(${clamp(d * 1.7 - k * 0.3)})`, transformOrigin: "0 50%" }} />
                  ))}
                </div>
                <div style={{ marginTop: 22, fontSize: 38, color: THEME.dim }}>{e.note}</div>
              </>
            ) : e.panel === "questions" ? (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.waiting }}>Open questions</div>
                {/* il numero non sta fermo: attorno gli corre l'anello dell'attesa, e sotto appare la domanda che aspetta */}
                <div style={{ position: "relative", width: 260, height: 260, marginTop: 10 }}>
                  <svg width={260} height={260} style={{ position: "absolute", inset: 0, rotate: "-90deg" }}>
                    <circle cx={130} cy={130} r={112} fill="none" stroke="rgba(255,176,32,.18)" strokeWidth={10} />
                    <circle cx={130} cy={130} r={112} fill="none" stroke={UI.waiting} strokeWidth={10} strokeLinecap="round" strokeDasharray={2 * Math.PI * 112} strokeDashoffset={2 * Math.PI * 112 * (1 - clamp(d * 1.3))} />
                  </svg>
                  <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", fontSize: 150, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.04em", color: UI.waiting, scale: String(0.86 + 0.14 * clamp(d * 3)) }}>{e.n ?? 1}</div>
                </div>
                <div style={{ marginTop: 18, fontSize: 40, color: THEME.dim }}>{e.note}</div>
                <div style={{ marginTop: 10, fontSize: 44, color: THEME.white, opacity: clamp(d * 1.6 - 0.5), maxWidth: 700 }}>{e.quote}</div>
              </>
            ) : (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefGood }}>Context</div>
                {(e.rows ?? []).map((r, k) => {
                  const p = clamp(d * 1.6 - k * 0.35);
                  // l'ultima scheda non svanisce: le sue due barre si allungano e diventano i campi della scena dopo (Franz, 20:34)
                  const g2 = e.out === "bars" ? soft(clamp((t - 0.72) / 0.28)) : 0;
                  return (
                    <div key={k} style={{ marginTop: k ? 34 : 18 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", fontSize: 50, opacity: 1 - (e.out === "bars" ? clamp((t - 0.72) / 0.18) : 0) }}>
                        <span>{r.name}</span>
                        <span style={{ color: UI.briefRing, fontVariantNumeric: "tabular-nums" }}>{Math.round(r.pct * p)} %</span>
                      </div>
                      <div style={{ marginTop: 12, height: 16 + 900 * g2, borderRadius: 8 * (1 - g2), background: "rgba(139,180,247,.18)", position: "relative", left: -THEME.leftMargin * g2, width: 760 + (1920 - 760) * g2, transform: `translateY(${(k === 0 ? -1 : 1) * 420 * g2}px)` }}>
                        <div style={{ width: `${r.pct * p + (100 - r.pct * p) * g2}%`, height: "100%", borderRadius: 8 * (1 - g2), background: UI.briefRing, opacity: 1 - 0.45 * g2 }} />
                      </div>
                    </div>
                  );
                })}
              </>
            )}
          </div>
        );
      })}
    </>
  );
};
