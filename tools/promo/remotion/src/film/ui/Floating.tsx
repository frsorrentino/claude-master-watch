import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { floatAt, railAt } from "./heroes.ts";
import { UiCard } from "./UiCard.tsx";
import { UI } from "./UiTokens.ts";
import { THEME } from "../theme.ts";

/**
 * Le card che **scorrono sopra** l'orologio appoggiato a terra (Franz, 18/09 18:24). Coordinate di schermo, non del mockup:
 * ogni card nasce dal vetro (`glassY`), sale e continua a salire mentre arriva la successiva; chi arriva in cima esce dal quadro.
 * Un flusso, non un mucchio: a regime se ne vedono due o tre, la più bassa è la più nuova e la più nitida. Nel flusso le card
 * si alternano alle scritte (Franz, 18/09 18:30): il titolo della scena non sta più a lato, scorre anche lui.
 */
export const Floating: React.FC<{ scene: Scene; g: Grid; glassY: number }> = ({ scene, g, glassY }) => {
  const frame = useCurrentFrame();
  const { width } = useVideoConfig();
  const e = (scene.fx ?? []).find((f): f is Extract<Fx, { kind: "float" }> => f.kind === "float");
  if (!e) return null;
  const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
  const p = (frame - from) / len;
  if (p < 0) return null;
  const W = 760, cx = width / 2;                // colonna centrale: le card e le scritte si alternano sopra l'orologio
  const TOP = 60;                               // la corsia finisce qui: sopra, le schede sono uscite
  return (
    <>
      {e.cards.map((c, i) => {
        const f = floatAt(p, i, e.cards.length);
        if (f.u <= 0 || f.u >= 1) return null;
        // quota lineare nel tempo (scorrimento continuo); scala e opacità vengono dalla quota, come nella lista dell'orologio
        const y = glassY - (glassY - TOP) * f.u + 6 * f.bob;
        const rail = railAt(f.u);
        const isText = c.kind === "text";
        const s = isText ? 1 : rail.scale;                       // il testo non si deforma: sale liscio
        const a = isText ? Math.min(1, Math.sin(Math.PI * f.u) * 2) : rail.alpha;
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
            {f.u < 0.22 && !isText ? <div style={{ position: "absolute", left: -W / 2, top: glassY - y - 10, width: W, height: 26, borderRadius: "50%", background: "rgba(0,0,0,.5)", filter: "blur(12px)", opacity: 1 - f.u / 0.22 }} /> : null}
            <div style={{ translate: "-50% -50%", width: W, scale: String(s), perspective: 1600 }}>
              <div style={{ transform: "rotateX(10deg)", transformOrigin: "50% 100%", position: "relative" }}>
                <div style={{ zoom: W / 427 }}>
                  <UiCard w={427} name={c.name} age={c.age} text={c.text} badge={c.badge} icon={c.icon === "bell" ? "check" : c.icon} light={1} />
                </div>
                {c.icon === "bell" ? <div style={{ position: "absolute", right: 42, top: 34, width: 34, height: 34, borderRadius: "50%", background: UI.followed }} /> : null}
              </div>
            </div>
          </div>
        );
      })}
    </>
  );
};
