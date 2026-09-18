import React from "react";
import { useCurrentFrame } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { floatAt } from "./heroes.ts";
import { UiCard } from "./UiCard.tsx";
import { UI } from "./UiTokens.ts";

/**
 * Le card che galleggiano sopra il display visto di taglio (piano 5, Franz 16:44): ognuna sale dal vetro, si ferma a mezz'aria
 * appena inclinata verso chi guarda, respira; quando arriva la successiva arretra di un passo (più piccola, più su, più tenue).
 * Coordinate: l'origine è il centro del display nel mockup laterale; `k` = pixel del quadro per pixel del mockup (per la scala del
 * testo si impagina a grandezza piena e si rimpicciolisce, come gli altri componenti). Le card sono componenti veri (`UiCard`).
 */
export const Floating: React.FC<{ scene: Scene; g: Grid; k: number }> = ({ scene, g, k }) => {
  const frame = useCurrentFrame();
  const e = (scene.fx ?? []).find((f): f is Extract<Fx, { kind: "float" }> => f.kind === "float");
  if (!e) return null;
  const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
  const p = (frame - from) / len;
  if (p < 0) return null;
  const W = 400;                                   // larghezza della card a mezz'aria, in pixel del mockup (poi ×k)
  return (
    <>
      {e.cards.map((c, i) => {
        const f = floatAt(p, i, e.cards.length);
        if (f.rise <= 0) return null;
        // sale dal vetro a destra del centro (il titolo sta a sinistra); le precedenti salgono e arretrano nel mucchio, più su e più tenui
        const y = -30 - 150 * f.rise - 70 * f.depth + 5 * f.bob;
        const x = 150 + 16 * f.depth;
        const s = (0.6 + 0.4 * f.rise) * Math.pow(0.9, f.depth);
        const a = f.rise * (1 - 0.35 * f.depth);
        return (
          <div key={i} style={{ position: "absolute", left: x, top: y, width: 0, height: 0, opacity: a, zIndex: 10 + i }}>
            {/* ombra sul vetro: la card sta sopra il display, la luce dall'alto la proietta sotto */}
            <div style={{ position: "absolute", left: -W / 2, top: -y - 8, width: W, height: 22, borderRadius: "50%", background: "rgba(0,0,0,.5)", filter: "blur(9px)", opacity: f.rise * (1 - 0.6 * f.depth), scale: String(s) }} />
            <div style={{ translate: "-50% -50%", width: W, scale: String(s / k), perspective: 1400 }}>
              <div style={{ transform: "rotateX(14deg)", transformOrigin: "50% 100%" }}>
                <div style={{ zoom: k }}>
                  <UiCard w={427} name={c.name} age={c.age} text={c.text} badge={c.badge} icon={c.icon === "bell" ? "check" : c.icon} light={1} />
                  {c.icon === "bell" ? <div style={{ position: "absolute", right: 24, top: 22, width: 30, height: 30, borderRadius: "50%", background: UI.followed }} /> : null}
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </>
  );
};
