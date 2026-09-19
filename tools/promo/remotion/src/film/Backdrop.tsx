import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { ACT_BG, THEME } from "./theme.ts";
import type { Act } from "./timeline.ts";

/**
 * Lo sfondo dell'atto. `from`: il colore con cui la scena NASCE, quando il campo pieno del takeover deve restare come suo
 * fondo invece di dissolversi (Franz, 19/09 13:32: «il campo celeste deve fare da sfondo alla successiva senza andarsene,
 * gli elementi della successiva vanno sopra»). In `fade` fotogrammi il colore scivola a quello dell'atto.
 * `light`: l'alone dietro l'orologio È la luce del display (ui/sleep.ts): quando il display dorme, si spegne con lui.
 * `haloR` 0-1: quanto è largo quell'alone — nel sonno si stringe verso la cassa, così il buio è un movimento e non un vuoto.
 * `field` 0-1: quanto resta acceso il campo dell'atto. Nel sonno cala al 18 %: a nero pieno il quadro sarebbe un buco
 * (Franz, 19/09 22:40).
 */
export const Backdrop: React.FC<{ act: Act; glowX?: number; from?: string; fade?: number; light?: number; haloR?: number; field?: number }> = ({ act, glowX = THEME.watchX, from, fade = 60, light = 1, haloR = 1, field = 1 }) => {
  const frame = useCurrentFrame();
  const [c0, c1, c2, glow] = ACT_BG[act];
  const t = from ? Math.min(1, Math.max(0, frame / Math.max(1, fade))) : 1;
  const e = t * t * (3 - 2 * t);
  return (
    <AbsoluteFill style={{ background: from ? from : undefined }}>
      <AbsoluteFill style={{ background: `radial-gradient(120% 120% at 70% 30%, ${c0} 0%, ${c1} 45%, ${c2} 100%)`, opacity: e }}>
        <AbsoluteFill style={{ mixBlendMode: "screen", background: `radial-gradient(${38 * haloR}% ${70 * haloR}% at ${glowX * 100}% 50%, ${glow} 0%, rgba(0,0,0,.0) 100%), #000`, opacity: Math.min(1, light) }} />
      </AbsoluteFill>
      {field < 1 ? <AbsoluteFill style={{ background: "#000", opacity: 1 - field }} /> : null}
    </AbsoluteFill>
  );
};
