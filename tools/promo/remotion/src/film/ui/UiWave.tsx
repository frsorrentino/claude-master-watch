import React, { useEffect, useState } from "react";
import { continueRender, delayRender, staticFile, useCurrentFrame } from "remotion";
import { UI } from "./UiTokens.ts";
import { waveAmplitudeAt, wavePath } from "./wave.ts";

type Envelope = { fps: number; env: number[] };

/**
 * La forma d'onda della voce (piano 3, metro: Ask 77,5 s, l'arco di luce sotto «Say it»): parte dal tasto ▶ e corre sotto le
 * parole dette, corallo, guidata dall'ampiezza vera della traccia. Un nastro morbido e un filo a fuoco: profondità, non bordo.
 */
export const UiWave: React.FC<{ file: string; width?: number; height?: number }> = ({ file, width = 800, height = 150 }) => {
  const f = useCurrentFrame();
  const [env, setEnv] = useState<number[]>([]);
  const [handle] = useState(() => delayRender(`onda ${file}`));
  useEffect(() => { fetch(staticFile(`audio/${file}`)).then((r) => r.json()).then((e: Envelope) => { setEnv(e.env); continueRender(handle); }); }, [file, handle]);
  const a = waveAmplitudeAt(env, f);
  const amp = 4 + (height / 2 - 10) * a;
  const d = wavePath(width, height / 2, amp, f * 0.35);
  const on = Math.min(1, f / 10);
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} style={{ display: "block", overflow: "visible", opacity: on }}>
      <path d={d} fill="none" stroke={UI.coral} strokeWidth={14} strokeLinecap="round" opacity={0.35 + 0.35 * a} style={{ filter: "blur(7px)" }} />
      <path d={d} fill="none" stroke={UI.coral} strokeWidth={3} strokeLinecap="round" opacity={0.9} />
    </svg>
  );
};
