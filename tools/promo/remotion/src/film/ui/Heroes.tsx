import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { cardOutAt } from "./heroes.ts";
import { Plane3D } from "./Plane3D.tsx";
import { UiCard } from "./UiCard.tsx";

/** I momenti forti di una scena, disegnati sopra l'orologio e sotto il titolo. `watchCx`: dove sta il centro del display. */
export const Heroes: React.FC<{ scene: Scene; g: Grid; watchCx: number }> = ({ scene, g, watchCx }) => {
  const frame = useCurrentFrame();
  const { height } = useVideoConfig();
  return (
    <>
      {(scene.fx ?? []).map((e, i) => {
        if (e.kind !== "cardOut") return null;
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const p = (frame - from) / len;
        if (p <= 0 || p >= 1) return null;
        const c = cardOutAt(p);
        // dal centro del display allo spazio sotto il titolo; in volo si inclina di più, all'arrivo resta appena girata
        const x = watchCx + (THEME.leftMargin + 360 - watchCx) * c.travel, y = height / 2 + (865 - height / 2) * c.travel;
        const scale = 1.24 + 0.56 * c.travel, ry = 16 * c.travel + 20 * Math.sin(Math.PI * c.travel), rx = 5 * c.travel + 6 * Math.sin(Math.PI * c.travel);
        return (
          <div key={i} style={{ position: "absolute", left: x, top: y, width: 0, height: 0, opacity: c.alpha }}>
            <div style={{ translate: "-50% -50%", width: "max-content", scale: String(scale) }}>
              <Plane3D rx={rx} ry={ry}><UiCard name={e.name} age={e.age} busyText={e.busyText} doneText={e.doneText} flip={c.flip} /></Plane3D>
            </div>
          </div>
        );
      })}
    </>
  );
};
