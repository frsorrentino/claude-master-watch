import React, { useEffect, useState } from "react";
import { continueRender, delayRender, staticFile, useCurrentFrame } from "remotion";
import { UI } from "./UiTokens.ts";
import { waveAmplitudeAt, wavePath, wavePathFrom } from "./wave.ts";
import { soft } from "../moves.ts";

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

/**
 * L'onda giro 2: dal ▶ del display (punto nel quadro) alla colonna delle parole, ampiezza piena sui picchi veri della voce
 * (fino a 150 px), tutta la larghezza della colonna. `f` parte con la voce; nei primi 14 fotogrammi il filo «arriva» dal ▶.
 */
export const UiWaveFrom: React.FC<{ file: string; from: [number, number]; x0: number; x1: number; y: number }> = ({ file, from, x0, x1, y }) => {
  const f = useCurrentFrame();
  const [env, setEnv] = useState<number[]>([]);
  const [handle] = useState(() => delayRender(`onda ${file}`));
  useEffect(() => { fetch(staticFile(`audio/${file}`)).then((r) => r.json()).then((e: Envelope) => { setEnv(e.env); continueRender(handle); }); }, [file, handle]);
  const a = waveAmplitudeAt(env, f);
  const reach = soft(Math.min(1, f / 14));
  const amp = (6 + 150 * a) * Math.min(1, Math.max(0, (f - 10) / 10));
  const d = wavePathFrom(from, x0, x1, y, amp, f * 0.3, reach);
  return (
    <svg style={{ position: "absolute", inset: 0, overflow: "visible", pointerEvents: "none" }} width="100%" height="100%">
      <path d={d} fill="none" stroke={UI.coral} strokeWidth={18} strokeLinecap="round" strokeLinejoin="round" opacity={0.3 + 0.4 * a} style={{ filter: "blur(9px)" }} />
      <path d={d} fill="none" stroke={UI.coral} strokeWidth={3.5} strokeLinecap="round" strokeLinejoin="round" opacity={0.95} />
    </svg>
  );
};
