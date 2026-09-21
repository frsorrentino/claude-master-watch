import React from "react";
import { Easing, Sequence, interpolate, useCurrentFrame } from "remotion";
import { spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import type { Scene } from "./timeline.ts";
import { THEME } from "./theme.ts";
import { cardOutAt, gaugeHeroAt } from "./ui/heroes.ts";
import { UiGauge } from "./ui/UiGauge.tsx";
import { UI } from "./ui/UiTokens.ts";

const clampBoth = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** Il punto del tocco: compare pieno, si allarga e svanisce in 12 fotogrammi. */
export const TapDot: React.FC<{ x: number; y: number }> = ({ x, y }) => {
  const f = useCurrentFrame();
  const s = interpolate(f, [0, 12], [0.55, 1.25], { ...clampBoth, easing: Easing.out(Easing.cubic) });
  const o = interpolate(f, [0, 3, 12], [0, 0.55, 0], clampBoth);
  return <div style={{ position: "absolute", left: x - 34, top: y - 34, width: 68, height: 68, borderRadius: "50%", background: "#fff", opacity: o, scale: String(s) }} />;
};

/** L'arco della pressione lunga si chiude attorno alla cassa, partendo dalle ore 12. */
export const LongPressArc: React.FC<{ frames: number }> = ({ frames }) => {
  const f = useCurrentFrame();
  const p = interpolate(f, [0, frames], [0, 1], clampBoth);
  const o = interpolate(f, [frames, frames + 8], [1, 0], clampBoth);
  const r = 100 / 3 + 2.4, c = 2 * Math.PI * r;
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0, opacity: o, rotate: "-90deg" }}>
      <circle cx="50" cy="50" r={r} fill="none" stroke={THEME.accent} strokeWidth="0.9" strokeLinecap="round" strokeDasharray={`${c * p} ${c}`} />
    </svg>
  );
};

/** Due anelli di vibrazione dalla cassa, a cinque fotogrammi l'uno dall'altro. */
export const HapticRings: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0 }}>
      {[0, 5].map((d) => {
        const r = interpolate(f - d, [0, 18], [100 / 3, 100 / 3 + 9], { ...clampBoth, easing: Easing.out(Easing.quad) });
        const o = interpolate(f - d, [0, 2, 18], [0, 0.5, 0], clampBoth);
        return <circle key={d} cx="50" cy="50" r={r} fill="none" stroke="#fff" strokeWidth="0.5" opacity={o} />;
      })}
    </svg>
  );
};

/**
 * La notifica che arriva al polso (Franz, 21/09 16:18): tre anelli concentrici nascono dal bordo della cassa uno dopo
 * l'altro e si sciolgono allargandosi, come un suono che vibra. Stesso disegno di HapticRings (la cassa ha raggio 100/3 nel
 * riquadro attorno all'orologio), più ampi e con un anello in più: questa è LA notifica del film.
 */
export const NotifyRings: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0, overflow: "visible" }}>
      {[0, 5, 10].map((d, i) => {
        const r = interpolate(f - d, [0, 24], [100 / 3, 100 / 3 + 13], { ...clampBoth, easing: Easing.out(Easing.cubic) });
        const o = interpolate(f - d, [0, 2, 24], [0, 0.6 - 0.12 * i, 0], clampBoth);
        return <circle key={d} cx="50" cy="50" r={r} fill="none" stroke="#fff" strokeWidth={0.55 - 0.1 * i} opacity={o} />;
      })}
    </svg>
  );
};

/** Il posto lasciato dalla card che esce (piano 3): un fantasma del colore della superficie, senza testo, finché non rientra
 *  (una toppa nera si leggeva come un buco nel render: master, 18/09 02:22). */
export const CardHole: React.FC<{ rect: [number, number, number, number]; frames: number; fromOut?: boolean; around?: boolean }> = ({ rect, frames, fromOut, around }) => {
  const f = useCurrentFrame();
  const [x, y, w, h] = rect;
  // con la camera «around» il fantasma se ne va mentre l'orologio si materializza: sotto c'è la scheda vera, che deve vedersi
  // (altrimenti il display resta con un rettangolo vuoto al posto della card — visto nel giro del 19/09 05:45)
  const t = Math.min(1, Math.max(0, f / 30));
  const patch = cardOutAt(f / frames, fromOut, true).patch * (around ? 1 - t * t * (3 - 2 * t) : 1);
  return (
    <>
      <div style={{ position: "absolute", left: x, top: y, width: w, height: h, borderRadius: 42, background: UI.bg, opacity: patch }} />
      <div style={{ position: "absolute", left: x, top: y, width: w, height: h, borderRadius: 42, background: UI.surface, opacity: 0.38 * patch }} />
    </>
  );
};

/** Il posto lasciato dal gauge che esce: la superficie della card sopra gli anelli, con i soli binari in trasparenza. */
export const GaugeHole: React.FC<{ cx: number; cy: number; size: number; frames: number }> = ({ cx, cy, size, frames }) => {
  const f = useCurrentFrame();
  const patch = gaugeHeroAt(f / frames).patch;
  return (
    <div style={{ position: "absolute", left: cx - size / 2 - 2, top: cy - size / 2 - 2, width: size + 4, height: size + 4, borderRadius: "50%", background: UI.surfaceHigh, opacity: patch, display: "grid", placeItems: "center" }}>
      <div style={{ opacity: 0.38 }}><UiGauge size={size} outer={0} inner={0} ghost /></div>
    </div>
  );
};

export const fxLayers = (scene: Scene, g: Grid, prev?: Scene): { overlay: React.ReactNode; around: React.ReactNode } => {
  const at = (b: number) => spanFrames(g, scene.at, b);
  const fx = scene.fx ?? [];
  return {
    overlay: fx.map((e, i) =>
      e.kind === "tap" ? <Sequence key={i} from={at(e.at)} durationInFrames={14} layout="none"><TapDot x={e.x} y={e.y} /></Sequence>
      : e.kind === "cardOut" ? <Sequence key={i} from={at(e.at)} durationInFrames={at(e.at + e.len) - at(e.at)} layout="none"><CardHole rect={e.rect} frames={at(e.at + e.len) - at(e.at)} fromOut={e.fromOut} around={scene.watch?.camera === "around"} /></Sequence>
      : e.kind === "gaugeHero" ? <Sequence key={i} from={at(e.at)} durationInFrames={at(e.at + e.len) - at(e.at)} layout="none"><GaugeHole cx={e.cx} cy={e.cy} size={e.size} frames={at(e.at + e.len) - at(e.at)} /></Sequence>
      : null),
    around: fx.map((e, i) =>
      // l'anello attorno all'orologio era ridondante con quello che corre sul tasto ricostruito, che è più grande e si
      // legge meglio: ne resta uno solo (Franz, 19/09 13:42)
      e.kind === "longPress" ? null
      : e.kind === "haptic" ? <Sequence key={i} from={at(e.at)} durationInFrames={26} layout="none"><HapticRings /></Sequence> : null)
      // l'onda dei puntini: tre nella scena che dorme (3, 2 e 1 battito prima del taglio, come ui/dots.ts) e la quarta,
      // quella della notifica, al primo fotogramma della scena che si risveglia
      // sulla notifica, e solo lì, anelli concentrici attorno all'orologio: il suono e la vibrazione che arrivano al polso.
      // Sui puntini niente anelli: bastano il loro crescendo e il respiro del display (Franz, 21/09 16:18)
      .concat(prev?.sleep ? [<Sequence key="notify-rings" from={0} durationInFrames={40} layout="none"><NotifyRings /></Sequence>] : []),
  };
};
