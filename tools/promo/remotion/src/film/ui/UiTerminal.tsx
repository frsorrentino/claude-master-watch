import React from "react";
import { useCurrentFrame } from "remotion";
import { soft } from "../moves.ts";
import { THEME } from "../theme.ts";

const clamp = (t: number) => Math.min(1, Math.max(0, t));

/**
 * Le righe del terminale escono dall'orologio su un piano inclinato nello spazio (piano 3, metro: Canvas 36,0 s, il codice in
 * prospettiva con le righe vicine a fuoco). Il piano è simulato in 2D, non con `rotateY`: Chrome rasterizza il testo in 3D a
 * bassa risoluzione e lo sgrana. Ogni riga nuova entra da destra (l'orologio) e si posa sotto l'ultima, a fuoco e piena; le
 * righe vecchie salgono nel piano: un passo più piccole, più a sinistra, più tenui e più sfocate (profondità di campo).
 * Un'inclinazione leggera (`skewY`) dà la fuga del piano verso l'alto a sinistra. Monospazio come il terminale dell'app.
 */
export const UiTerminal: React.FC<{ lines: string[]; everyFrames: number; startFrame?: number }> = ({ lines, everyFrames, startFrame = 0 }) => {
  // montato dall'inizio della colonna (le righe non ancora arrivate occupano già il loro spazio): il titolo non salta quando parte
  const f = useCurrentFrame() - startFrame;
  const newest = lines.reduce((n, _, i) => (f >= i * everyFrames ? i : n), -1);
  return (
    <div style={{ transform: "skewY(-7deg)", transformOrigin: "100% 50%", fontFamily: "Noto Sans Mono", fontSize: 48, lineHeight: 1.45, whiteSpace: "pre" }}>
      {lines.map((l, i) => {
        const e = soft(clamp((f - i * everyFrames) / 12));
        const age = Math.max(0, newest - i);
        const depth = Math.min(age, 4);
        return (
          <div key={i} style={{ opacity: e * (1 - 0.14 * depth), translate: `${(1 - e) * 300 - 34 * depth}px 0`, scale: String(1 - 0.07 * depth), transformOrigin: "0 50%",
            filter: depth > 0 ? `blur(${Math.min(2.6, 0.8 * depth)}px)` : undefined, color: age === 0 ? THEME.white : THEME.dim }}>
            {l}
          </div>
        );
      })}
    </div>
  );
};
