import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { railAt, railScrollVar, railStackPx } from "./heroes.ts";
import { UiCard } from "./UiCard.tsx";
import { UiBriefContext, UiBriefQuestions, UiBriefWork } from "./UiBrief.tsx";
import { UI } from "./UiTokens.ts";
import { THEME } from "../theme.ts";

/**
 * La corsia sopra l'orologio appoggiato a terra: una LISTA che scorre, come sul polso. Le schede stanno a passo costante
 * (`PITCH`), la corsia comincia SOPRA l'orologio senza toccarlo (`GAP`), la lista avanza di un passo alla volta e si sofferma
 * su ogni scheda (`railScroll`), e ogni scheda si deforma con la quota come nelle liste di Wear OS (`railAt`): stretta in
 * basso, larga al centro, stretta in cima. Le scritte scorrono nella stessa corsia, alternate alle schede, ma non si deformano.
 */
export const Floating: React.FC<{ scene: Scene; g: Grid; glassY?: number }> = ({ scene, g, glassY }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const e = (scene.fx ?? []).find((f): f is Extract<Fx, { kind: "float" }> => f.kind === "float");
  if (!e) return null;
  const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
  const p = (frame - from) / len;
  if (p < 0) return null;
  // dove sta la corsia: nella scena laterale sopra l'orologio, altrove dove dice la scaletta (colonna e fondo in frazioni di quadro)
  const W = (e.width ?? 560), cx = e.cx !== undefined ? e.cx * width : width / 2;
  const base = glassY ?? (e.bottom ?? 0.92) * height;
  const CLEAR = 150;                            // la corsia comincia sopra l'orologio, senza toccarlo
  const SPAN = 2.2;                             // quante schede si vedono insieme
  const CARD_H = W * 0.503;                     // altezza della scheda alla scala piena (427×215 nel display)
  const GAP = CARD_H * 0.043;                   // stacco come sul display (card 209 px, stacco 9): impilamento a stacco costante
  const yBottom = base - (glassY ? CLEAR : 0);
  // la sosta la decide la scaletta, scheda per scheda: le card lunghe si leggono, le scritte passano più svelte
  // l'ultima scheda si ferma al centro e resta lì: da quella posizione parte l'ingrandimento della transizione
  const holds = e.cards.map((c, i) => c.hold ?? (i === e.cards.length - 1 ? 1.6 : c.kind === "text" ? 0.2 : 0.5));
  const offset = railScrollVar(p, holds);
  return (
    <>
      {e.cards.map((c, i) => {
        const d = offset - i;                    // 0 = appena entrata in fondo alla corsia, SPAN = in cima
        const u = d / SPAN;
        const y = yBottom - railStackPx(d, CARD_H, GAP, SPAN);
        // la scheda non sparisce ai capi: resta piccola e appena trasparente finché non esce davvero dal quadro
        if (d <= -0.06 || y < -240) return null;
        const rail = railAt(u);
        const isText = c.kind === "text";
        const s = isText ? 1 : rail.scale;                       // il testo non si deforma: sale liscio
        const a = isText ? Math.min(1, Math.sin(Math.PI * u) * 2.2) : rail.alpha;
        if (c.kind === "text") return (
          <div key={i} style={{ position: "absolute", left: cx, top: y, width: 0, height: 0, opacity: a, zIndex: 10 + i }}>
            <div style={{ translate: "-50% -50%", width: W + 220, textAlign: "center", scale: String(s), fontFamily: "Inter", fontWeight: 600, fontSize: 82, lineHeight: 1.08, letterSpacing: "-0.02em", color: THEME.white }}>
              {c.lines.map((l, j) => <div key={j}>{l.split(" ").map((wd, n) => <span key={n} style={{ color: wd === c.accent ? THEME.accent : undefined }}>{wd}{n < l.split(" ").length - 1 ? " " : ""}</span>)}</div>)}
            </div>
          </div>
        );
        return (
          <div key={i} style={{ position: "absolute", left: cx, top: y, width: 0, height: 0, opacity: a, zIndex: 10 + i }}>
            {/* ombra sul vetro solo per la card più bassa: è quella appoggiata alla luce del display */}

            <div style={{ translate: "-50% -50%", width: W, scale: String(s), perspective: 1600 }}>
              <div style={{ transform: "rotateX(10deg)", transformOrigin: "50% 100%", position: "relative" }}>
                <div style={{ zoom: W / 427 }}>
                  {c.kind === "brief" ? (
                    c.panel === "work" ? <UiBriefWork w={427} n={c.n ?? 1} note={c.note ?? ""} bars={c.bars ?? [1, 1, 1]} />
                    : c.panel === "questions" ? <UiBriefQuestions w={427} n={c.n ?? 1} note={c.note ?? ""} />
                    : <UiBriefContext w={427} rows={c.rows ?? []} />
                  ) : (
                    <UiCard w={427} name={c.name} age={c.age} text={c.text} badge={c.badge} icon={c.icon === "bell" ? "check" : c.icon} light={1} />
                  )}
                </div>
                {c.kind !== "brief" && c.icon === "bell" ? <div style={{ position: "absolute", right: 42, top: 34, width: 34, height: 34, borderRadius: "50%", background: UI.followed }} /> : null}
              </div>
            </div>
          </div>
        );
      })}
    </>
  );
};
