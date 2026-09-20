import React from "react";
import { useCurrentFrame } from "remotion";
import { THEME } from "../theme.ts";
import { SLEEP_CUT } from "./sleep.ts";
import { dotsAt, DOT_BASE } from "./dots.ts";

/** I tre puntini dell'attesa, sotto il titolo che si è appena scritto (ui/dots.ts per i tempi). `pre` è quanti fotogrammi
 *  prima del taglio comincia questa sequenza, `frames` la finestra intera del sonno. */
export const AskDots: React.FC<{ pre: number; frames: number }> = ({ pre, frames }) => {
  const frame = useCurrentFrame();
  const d = dotsAt(SLEEP_CUT + (frame - pre) / frames);
  if (d.alpha <= 0 && d.collapse <= 0) return null;
  const size = 16;
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 20, height: size, marginTop: 38 }}>
      {d.collapse > 0 ? (
        // i tre diventano uno: il punto si allarga e si spegne mentre il display si riaccende
        <div style={{ width: size, height: size, borderRadius: "50%", background: THEME.white, opacity: d.collapse,
          scale: String(1 + 2.4 * (1 - d.collapse)), filter: `blur(${8 * (1 - d.collapse)}px)` }} />
      ) : (
        d.on.map((v, i) => (
          <div key={i} style={{ width: size, height: size, borderRadius: "50%", background: THEME.accent,
            opacity: d.alpha * (DOT_BASE + (1 - DOT_BASE) * v), scale: String(1 + 0.22 * v) }} />
        ))
      )}
    </div>
  );
};
