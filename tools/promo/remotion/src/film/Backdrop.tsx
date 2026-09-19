import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { ACT_BG, THEME } from "./theme.ts";
import type { Act } from "./timeline.ts";

/**
 * Lo sfondo dell'atto. `from`: il colore con cui la scena NASCE, quando il campo pieno del takeover deve restare come suo
 * fondo invece di dissolversi (Franz, 19/09 13:32: «il campo celeste deve fare da sfondo alla successiva senza andarsene,
 * gli elementi della successiva vanno sopra»). In `fade` fotogrammi il colore scivola a quello dell'atto.
 */
export const Backdrop: React.FC<{ act: Act; glowX?: number; from?: string; fade?: number }> = ({ act, glowX = THEME.watchX, from, fade = 60 }) => {
  const frame = useCurrentFrame();
  const [c0, c1, c2, glow] = ACT_BG[act];
  const t = from ? Math.min(1, Math.max(0, frame / Math.max(1, fade))) : 1;
  const e = t * t * (3 - 2 * t);
  return (
    <AbsoluteFill style={{ background: from ? from : undefined }}>
      <AbsoluteFill style={{ background: `radial-gradient(120% 120% at 70% 30%, ${c0} 0%, ${c1} 45%, ${c2} 100%)`, opacity: e }}>
        <AbsoluteFill style={{ mixBlendMode: "screen", background: `radial-gradient(38% 70% at ${glowX * 100}% 50%, ${glow} 0%, rgba(0,0,0,.0) 100%), #000`, opacity: 1 }} />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
