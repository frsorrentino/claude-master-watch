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
import { asideAt, contextFill, contextTextAt, fadeOutAt } from "./aside.ts";
import { BriefContext, BriefQuestions, BriefWork } from "./Brief.tsx";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

/**
 * I dati della Panoramica accanto all'orologio (Franz, 18/09 20:33): NON escono dalla card e non volano — compaiono in
 * dissolvenza già fermi a sinistra, uno dopo l'altro, quando sul display passa la scheda corrispondente, e lì si animano
 * (le barre si riempiono, i numeri contano). Solo il gauge della quota conserva il volo: è l'eccezione approvata.
 */
export const Aside: React.FC<{ scene: Scene; g: Grid }> = ({ scene, g }) => {
  const frame = useCurrentFrame();
  const { height } = useVideoConfig();
  // `inShape`: il pannello lo disegna la forma unica (corto, 25/09): qui resta solo il dato, se no i pannelli sarebbero due
  const list = (scene.fx ?? []).filter((f): f is Extract<Fx, { kind: "aside" }> => f.kind === "aside" && !f.inShape);
  if (!list.length) return null;
  // quando parte la tapparella le barre HTML lasciano il posto ai listelli in Three, che nascono identici: prima si riempiono
  // fino in fondo e il resto del pannello se ne va, poi al fotogramma esatto dell'innesco spariscono
  const sceneTotal = spanFrames(g, scene.at, scene.len);
  const blindStart = scene.blinds ? sceneTotal - Math.round(spanFrames(g, scene.at, scene.blinds.len) * BLIND_CUT) : Infinity;
  return (
    <>
      {list.map((e, i) => {
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const s = asideAt(frame, from, len, e.fadeIn === undefined ? undefined : spanFrames(g, scene.at + e.at, e.fadeIn), e.draw === undefined ? undefined : spanFrames(g, scene.at + e.at, e.draw));
        const t = s.t;
        if (t < 0 || t >= 1) return null;
        // il pannello resta fermo fino a POCO PRIMA che entri il successivo e se ne va in 8 fotogrammi: mai due insieme
        // (Franz, 18/09 21:54: la quota sbordava di un battito e mezzo sopra il ritmo)
        const next = list[i + 1];
        const gone = next ? spanFrames(g, scene.at, next.at) - 2 : from + len;
        const fade = e.out === "bars" ? 1 : fadeOutAt(frame, gone);
        const a = Math.min(s.enter, fade) * (e.out === "bars" && frame >= blindStart ? 0 : 1);   // entra in dissolvenza; esce in dissolvenza, salvo l'ultima che diventa la transizione
        const d = s.d;                                        // il dato si disegna sul posto
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
                {/* Lo stesso grafico del polso (ripresa del 21/09 sera, dati della demo puliti): una retta dalla base alle 05:17
                    fino ad adesso, il punto sulla tacca tratteggiata, poi la proiezione tratteggiata con la stessa pendenza
                    fino al reset. Misure prese sul fotogramma (riquadro 364×79 del display) in scala uguale sui due assi. */}
                <svg width={700} height={236} style={{ marginTop: 22, overflow: "visible" }}>
                  <line x1={0} y1={36} x2={700} y2={36} stroke="rgba(235,244,255,.18)" strokeWidth={2} />
                  <line x1={0} y1={188} x2={700} y2={188} stroke="rgba(235,244,255,.18)" strokeWidth={2} />
                  <line x1={416} y1={40} x2={416} y2={186} stroke="rgba(235,244,255,.32)" strokeWidth={3} strokeDasharray="8 8" opacity={clamp(d * 1.5 - 0.3)} />
                  <path d="M 0 186 L 419 130" fill="none" stroke={UI.briefRing} strokeWidth={8} strokeLinecap="round" strokeDasharray={430} strokeDashoffset={430 * (1 - clamp(d * 1.5))} />
                  <path d="M 419 130 L 700 90" fill="none" stroke={UI.briefRing} strokeWidth={6} strokeLinecap="round" strokeDasharray="14 16" opacity={clamp(d * 1.5 - 0.7) * 0.8} />
                  <circle cx={419} cy={130} r={13} fill={UI.briefRing} opacity={clamp(d * 1.5 - 0.6)} />
                  <text x={0} y={232} fontSize={34} fill={THEME.dim}>05:17</text>
                  <text x={700} y={232} fontSize={34} fill={THEME.dim} textAnchor="end">10:17</text>
                </svg>
              </>
            ) : e.panel === "work" ? (
              <BriefWork n={e.n} note={e.note} bars={e.bars} d={d} />
            ) : e.panel === "questions" ? (
              <BriefQuestions n={e.n} note={e.note} quote={e.quote} d={d} />
            ) : (
              // mentre le barre si riempiono per la tapparella, anche i numeri finiscono di salire: nessun dato resta a metà;
              // l'ultima scheda non svanisce: le due barre si riempiono fino in fondo e da lì nasce la tapparella (Blinds)
              <BriefContext rows={e.rows} d={d} fill={(k) => Math.max(contextFill(d, k), e.out === "bars" ? soft(clamp((frame - (blindStart - 16)) / 16)) : 0)}
                grow={e.out === "bars" ? soft(clamp((frame - (blindStart - 16)) / 16)) : 0} textOpacity={e.out === "bars" ? contextTextAt(frame, blindStart) : 1} />
            )}
          </div>
        );
      })}
    </>
  );
};
