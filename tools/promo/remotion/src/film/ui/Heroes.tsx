import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import geo from "../mockup.geometry.json";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Pose } from "../moves.ts";
import type { Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { cardOutAt } from "./heroes.ts";
import { Plane3D } from "./Plane3D.tsx";
import { UiCard } from "./UiCard.tsx";

/** Larghezza della card da protagonista, nel quadro: riempie lo spazio a sinistra dell'orologio tenendo il margine del testo. */
export const HERO_CARD_PX = 900;

/**
 * I momenti forti di una scena, disegnati sopra l'orologio. Il display frontale sta a `watchCx + pose.x·width`,
 * `height/2 + pose.y·height`, con 480 unità = 2·displayR·glassPx/(2·glassR)·pose.scale pixel: così un componente in coordinate
 * del display (rettangolo della scaletta) parte esattamente da dove sta sul fotogramma vero.
 */
export const Heroes: React.FC<{ scene: Scene; g: Grid; watchCx: number; pose: Pose | null; glassPx: number }> = ({ scene, g, watchCx, pose, glassPx }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  if (!pose) return null;
  const u = ((glassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR * pose.scale) / 480;
  const dx = watchCx + pose.x * width, dy = height / 2 + pose.y * height;
  return (
    <>
      {(scene.fx ?? []).map((e, i) => {
        if (e.kind !== "cardOut") return null;
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const c = cardOutAt((frame - from) / len);
        if (c.alpha <= 0) return null;
        const [rx, ry, rw, rh] = e.rect;
        // da dove sta sul display a protagonista nel centro sinistro, in linea retta: è la crescita a dare il volo
        const x0 = dx + (rx + rw / 2 - 240) * u, y0 = dy + (ry + rh / 2 - 240) * u;
        const s1 = HERO_CARD_PX / rw, x1 = THEME.leftMargin + HERO_CARD_PX / 2, y1 = height / 2 + 8 * c.drift;
        const x = x0 + (x1 - x0) * c.travel, y = y0 + (y1 - y0) * c.travel;
        const scale = u + (s1 - u) * c.travel;
        // un asse solo: attorno alla verticale, il lato destro (quello che si stacca per ultimo) più vicino; fuori resta appena girata
        const yaw = 3 * c.travel + 16 * c.swing + 1.2 * c.drift * c.travel;
        return (
          <React.Fragment key={i}>
          {/* il campo si pulisce: una vignetta scurisce i bordi finché la card è fuori */}
          <div style={{ position: "absolute", inset: 0, opacity: 0.55 * c.travel, background: "radial-gradient(60% 60% at 40% 50%, rgba(0,0,0,0) 30%, rgba(0,0,0,.85) 100%)" }} />
          <div style={{ position: "absolute", left: x, top: y, width: 0, height: 0, opacity: c.alpha }}>
            <div style={{ translate: "-50% -50%", width: "max-content", scale: String(scale) }}>
              <Plane3D ry={yaw} perspective={2600 / scale}>
                <UiCard w={rw} name={e.name} age={e.age} text={e.text} light={c.travel} />
              </Plane3D>
            </div>
          </div>
          </React.Fragment>
        );
      })}
    </>
  );
};
