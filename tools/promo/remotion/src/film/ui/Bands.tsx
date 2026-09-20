import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { bandsAt } from "./bands.ts";

/** Le due bande degli account dietro l'orologio (ui/bands.ts per i tempi): si aprono, e alla fine quella di sinistra
 *  si allarga fino a coprire il quadro — è il fondo con cui nasce la sezione delle statistiche. */
export const Bands: React.FC<{ left: string; right: string; openFrames: number; winFrames: number; total: number }> = ({ left, right, openFrames, winFrames, total }) => {
  const frame = useCurrentFrame();
  const { split, win } = bandsAt(frame / Math.max(1, total), openFrames, winFrames, total);
  const bordo = 50 + 50 * win;                 // dove finisce la banda di sinistra, in percentuale di quadro
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{ background: right, opacity: split }} />
      <AbsoluteFill style={{ background: left, opacity: split, clipPath: `inset(0 ${100 - bordo}% 0 0)` }} />
      {/* la giunzione non è una riga netta: una sfumatura stretta, come fra due campi di luce */}
      <AbsoluteFill style={{ opacity: split * (1 - win), background: `linear-gradient(90deg, rgba(0,0,0,0) ${bordo - 4}%, rgba(0,0,0,.28) ${bordo}%, rgba(0,0,0,0) ${bordo + 4}%)` }} />
    </AbsoluteFill>
  );
};
