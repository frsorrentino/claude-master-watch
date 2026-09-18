import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { UI } from "./UiTokens.ts";
import { UiGauge } from "./UiGauge.tsx";
import { soft } from "../moves.ts";
import { BLIND_CUT } from "./blinds.ts";

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
  // quando parte la tapparella le barre HTML lasciano il posto ai listelli in Three, che nascono identici: prima si riempiono
  // fino in fondo e il resto del pannello se ne va, poi al fotogramma esatto dell'innesco spariscono
  const sceneTotal = spanFrames(g, scene.at, scene.len);
  const blindStart = scene.blinds ? sceneTotal - Math.round(spanFrames(g, scene.at, scene.blinds.len) * BLIND_CUT) : Infinity;
  return (
    <>
      {list.map((e, i) => {
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const t = (frame - from) / len;
        if (t < 0 || t >= 1) return null;
        // il pannello resta fermo fino a POCO PRIMA che entri il successivo e se ne va in 8 fotogrammi: mai due insieme
        // (Franz, 18/09 21:54: la quota sbordava di un battito e mezzo sopra il ritmo)
        const next = list[i + 1];
        const gone = next ? spanFrames(g, scene.at, next.at) - 2 : from + len;
        const fade = e.out === "bars" ? 1 : 1 - soft(clamp((frame - (gone - 8)) / 8));
        const a = Math.min(soft(clamp(t / 0.12)), fade) * (e.out === "bars" && frame >= blindStart ? 0 : 1);   // entra in dissolvenza; esce in dissolvenza, salvo l'ultima che diventa la transizione
        const d = soft(clamp((t - 0.12) / 0.45));                                        // il dato si disegna sul posto
        return (
          <div key={i} style={{ position: "absolute", left: THEME.leftMargin, top: height / 2, width: 760, translate: "0 -50%", opacity: a, fontFamily: "Inter", color: THEME.white }}>
            {e.panel === "quota" ? (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefGood }}>Quota</div>
                {/* un gauge solo: i due anelli SONO i due valori (fuori le 5 ore, dentro la settimana) — Franz, 18/09 21:47 */}
                <div style={{ display: "flex", alignItems: "center", gap: 46, marginTop: 20 }}>
                  <UiGauge size={210} outer={((e.rows?.[0]?.pct ?? 0) / 100) * clamp(d * 1.4)} inner={((e.rows?.[0]?.week ?? 0) / 100) * clamp(d * 1.4 - 0.15)} />
                  <div>
                    <div style={{ display: "flex", alignItems: "baseline", gap: 14 }}>
                      <span style={{ fontSize: 104, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums", color: UI.briefRing }}>{Math.round((e.rows?.[0]?.pct ?? 0) * clamp(d * 1.4))}<span style={{ fontSize: "0.42em", color: THEME.dim, marginLeft: 8 }}>%</span></span>
                      <span style={{ fontSize: 36, color: THEME.dim }}>of your 5 hours</span>
                    </div>
                    <div style={{ display: "flex", alignItems: "baseline", gap: 14, marginTop: 18 }}>
                      <span style={{ fontSize: 76, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums", color: UI.briefWeek }}>{Math.round((e.rows?.[0]?.week ?? 0) * clamp(d * 1.4 - 0.15))}<span style={{ fontSize: "0.42em", color: THEME.dim, marginLeft: 8 }}>%</span></span>
                      <span style={{ fontSize: 36, color: THEME.dim }}>this week</span>
                    </div>
                  </div>
                </div>
                {e.note ? <div style={{ marginTop: 24, fontSize: 36, color: THEME.dim }}>{e.note}</div> : null}
              </>
            ) : e.panel === "note" ? (
              <div style={{ fontSize: 76, fontWeight: 600, lineHeight: 1.12, letterSpacing: "-0.02em" }}>
                {(e.lines ?? []).map((l, k) => <div key={k} style={{ opacity: clamp(d * 2 - k * 0.5), translate: `0 ${(1 - clamp(d * 2 - k * 0.5)) * 18}px` }}>{k === (e.lines ?? []).length - 1 ? <span style={{ color: THEME.accent }}>{l}</span> : l}</div>)}
              </div>
            ) : e.panel === "pace" ? (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefRing }}>5-hour pace</div>
                <div style={{ display: "flex", alignItems: "baseline", gap: 16, marginTop: 6 }}>
                  <span style={{ fontSize: 132, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums" }}>{Math.round((e.n ?? 0) * clamp(d * 1.15))}</span>
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
                  {/* anche qui il numero sale da 0, come gli altri pannelli (Franz, 18/09 22:01) */}
                  <span style={{ fontSize: 132, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums" }}>{Math.round((e.n ?? 1) * clamp(d * 2))}</span>
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
                {/* semplice passaggio da 0 a 1, senza anelli */}
                <div style={{ fontSize: 190, fontWeight: 600, lineHeight: 1, letterSpacing: "-0.04em", color: UI.waiting, marginTop: 4, fontVariantNumeric: "tabular-nums" }}>{Math.round((e.n ?? 1) * clamp(d * 2))}</div>
                <div style={{ marginTop: 18, fontSize: 40, color: THEME.dim }}>{e.note}</div>
                <div style={{ marginTop: 10, fontSize: 44, color: THEME.white, opacity: clamp(d * 1.6 - 0.5), maxWidth: 700 }}>{e.quote}</div>
              </>
            ) : (
              <>
                <div style={{ fontSize: 40, fontWeight: 500, color: UI.briefGood }}>Context</div>
                {(e.rows ?? []).map((r, k) => {
                  // mentre le barre si riempiono per la tapparella, anche i numeri finiscono di salire: nessun dato resta a metà
                  const p = Math.max(clamp(d * 1.6 - k * 0.35), e.out === "bars" ? soft(clamp((frame - (blindStart - 16)) / 16)) : 0);
                  // l'ultima scheda non svanisce: le due barre si riempiono fino in fondo e da lì nasce la tapparella (Blinds)
                  const g2 = e.out === "bars" ? soft(clamp((frame - (blindStart - 16)) / 16)) : 0;
                  return (
                    <div key={k} style={{ marginTop: k ? 34 : 18 }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", fontSize: 50, opacity: 1 - (e.out === "bars" ? clamp((frame - (blindStart - 14)) / 12) : 0) }}>
                        <span>{r.name}</span>
                        <span style={{ color: UI.briefRing, fontVariantNumeric: "tabular-nums" }}>{Math.round(r.pct * p)} %</span>
                      </div>
                      {/* il finale: le barre si riempiono fino in fondo e restano ferme; da lì nasce la tapparella in Three */}
                      <div style={{ marginTop: 12, height: 16, borderRadius: 8, background: "rgba(139,180,247,.18)", width: 760 }}>
                        <div style={{ width: `${r.pct * p + (100 - r.pct * p) * g2}%`, height: "100%", borderRadius: 8, background: UI.briefRing }} />
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
