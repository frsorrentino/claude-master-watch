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
 * `shadeIn`: in quanti fotogrammi l'ombra entra. Deve crescere INSIEME all'orologio che sale, non comparire di colpo
 * (Franz, 20/09 10:23).
 * Quando la scena nasce su un campo chiaro (`from`), sotto ci va un'ombra soffusa attorno all'orologio (Franz, 20/09 09:46):
 * è l'alone delle altre scene al contrario — lì luce sul fondo scuro, qui ombra sul fondo chiaro — e dà profondità mentre
 * il colore del takeover resta quello approvato. Si spegne man mano che il campo diventa il fondo dell'atto.
 */
/** L'ombra sotto l'orologio serve solo su un campo CHIARO: su un campo scuro (il blu dell'atto, 22/09) scurirebbe la cassa
 *  nera fino a perderla. Luminanza percepita di un colore #RRGGBB, soglia a metà. */
const isLight = (hex: string): boolean => {
  const n = parseInt(hex.slice(1, 7), 16);
  return (0.299 * ((n >> 16) & 255) + 0.587 * ((n >> 8) & 255) + 0.114 * (n & 255)) / 255 > 0.5;
};
export const Backdrop: React.FC<{ act: Act; glowX?: number; from?: string; fade?: number; keep?: boolean; light?: number; haloR?: number; field?: number; shadeIn?: number; colors?: [string, string, string, string] }> = ({ act, glowX = THEME.watchX, from, fade = 60, keep = false, light = 1, haloR = 1, field = 1, shadeIn = 0, colors }) => {
  const frame = useCurrentFrame();
  const [c0, c1, c2, glow] = colors ?? ACT_BG[act];   // `colors`: la tavolozza della scaletta, se ne ha una
  // `keep`: il campo di colore resta, il colore dell'atto non entra mai (Franz, 20/09 10:10)
  const t = keep ? 0 : from ? Math.min(1, Math.max(0, frame / Math.max(1, fade))) : 1;
  const e = t * t * (3 - 2 * t);
  const st = shadeIn > 0 ? Math.min(1, Math.max(0, frame / shadeIn)) : 1;
  const shade = st * st * (3 - 2 * st);       // l'ombra cresce con l'orologio che entra: stessa curva, stesso tempo
  return (
    <AbsoluteFill style={{ background: from ? from : undefined }}>
      {from && isLight(from) ? <AbsoluteFill style={{ mixBlendMode: "multiply", opacity: (1 - e) * shade,
        background: `radial-gradient(72% 78% at ${glowX * 100}% 68%, rgba(26,38,72,.62) 0%, rgba(26,38,72,.34) 45%, rgba(26,38,72,0) 100%)` }} /> : null}
      <AbsoluteFill style={{ background: `radial-gradient(120% 120% at 70% 30%, ${c0} 0%, ${c1} 45%, ${c2} 100%)`, opacity: e }}>
        <AbsoluteFill style={{ mixBlendMode: "screen", background: `radial-gradient(${38 * haloR}% ${70 * haloR}% at ${glowX * 100}% 50%, ${glow} 0%, rgba(0,0,0,.0) 100%), #000`, opacity: Math.min(1, light) }} />
      </AbsoluteFill>
      {field < 1 ? <AbsoluteFill style={{ background: "#000", opacity: 1 - field }} /> : null}
    </AbsoluteFill>
  );
};
