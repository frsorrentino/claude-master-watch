import React from "react";
import { Freeze, Img, OffthreadVideo, staticFile } from "remotion";
import geo from "./mockup.geometry.json";
import { applyH, homography, toMatrix3d } from "./homography.ts";
import type { Quad } from "./homography.ts";
import { MAX_TILT } from "./moves.ts";
import { Watch } from "../Watch";

/** `light`: luminosità del display, 1 = normale (ui/sleep.ts: il display che dorme e si risveglia). */
type Props = { view: "front" | "threeQuarter" | "drawn"; light?: number; clip: string; clipStart?: number; rate?: number; freeze?: boolean; still?: string; reveal?: number; bodyOpacity?: number; contentOpacity?: number; focus?: number; glassPx: number; tilt: number; overlay?: React.ReactNode; around?: React.ReactNode };

const Ui: React.FC<{ clip: string; clipStart?: number; rate?: number; freeze?: boolean; still?: string; overlay?: React.ReactNode }> = ({ clip, clipStart = 0, rate = 1, freeze, still, overlay }) => {
  const video = <OffthreadVideo src={staticFile(clip)} muted trimBefore={Math.round(clipStart * 30)} playbackRate={rate} style={{ width: "100%", height: "100%", objectFit: "cover" }} />;
  return (
  <div style={{ position: "absolute", inset: 0, borderRadius: "50%", overflow: "hidden", background: "#000" }}>
    {still ? <Img src={staticFile(still)} style={{ width: "100%", height: "100%" }} /> : freeze ? <Freeze frame={0}>{video}</Freeze> : video}
    {overlay ? <div style={{ position: "absolute", left: 0, top: 0, width: 480, height: 480, transformOrigin: "0 0", scale: "var(--k)" }}>{overlay}</div> : null}
    {/* ombra interna: lo schermo sta sotto la cupola, ai bordi scurisce */}
    <div style={{ position: "absolute", inset: 0, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(0,0,0,0) 80%, rgba(0,0,0,.55) 100%)" }} />
  </div>
  );
};

const Front: React.FC<Props> = ({ clip, clipStart, rate, freeze, still, glassPx, tilt, overlay, around, light = 1 }) => {
  const G = geo.front;
  const k = glassPx / (2 * G.glassR);
  const disc = (r: number): React.CSSProperties => ({ position: "absolute", left: G.cx - r, top: G.cy - r, width: 2 * r, height: 2 * r, borderRadius: "50%" });
  const blade = (tilt / MAX_TILT) * 60;          // la lama di luce scorre con l'inclinazione e da fermo non c'è
  const bladeOn = Math.min(1, Math.abs(tilt) / 3);
  const A = G.caseR * 1.5;
  return (
    <div style={{ width: G.size * k, height: G.size * k, translate: `${-G.cx * k}px ${-G.cy * k}px` }}>
      <div style={{ width: G.size, height: G.size, position: "relative", transformOrigin: "0 0", scale: String(k) }}>
        <Img src={staticFile("mockup/front_body.png")} style={{ position: "absolute", inset: 0, filter: "drop-shadow(26px 34px 34px rgba(4,5,12,.62))" }} />
        {/* vetro sintetico: nero sopra tutta la cupola tranne il bordo vero (la foto lì riflette il telefono) */}
        <div style={{ ...disc(G.coverR + 6), background: "radial-gradient(closest-side, #000 97%, rgba(0,0,0,0) 100%)" }} />
        <div style={{ ...disc(G.displayR), ["--k" as string]: String((2 * G.displayR) / 480), filter: light === 1 ? undefined : `brightness(${light})` }}><Ui clip={clip} clipStart={clipStart} rate={rate} freeze={freeze} still={still} overlay={overlay} /></div>
        <div style={{ ...disc(G.coverR), overflow: "hidden", mixBlendMode: "screen" }}>
          {/* alone in alto a sinistra, finestra sfocata, lama di luce, filo sul bordo */}
          <div style={{ position: "absolute", inset: 0, background: "radial-gradient(47.5% 31% at 31% 24%, rgba(235,245,255,.18) 0%, rgba(235,245,255,.113) 50%, rgba(235,245,255,.048) 75%, rgba(235,245,255,.013) 90%, rgba(235,245,255,0) 100%)" }} />
          <svg viewBox="0 0 100 100" style={{ position: "absolute", inset: 0, filter: "blur(18px)", opacity: 0.12 }}>
            <polygon points="25,25 40,20 43,35 27.5,41" fill="#fff" />
            <line x1="33.5" y1="22.5" x2="35.5" y2="38" stroke="#000" strokeWidth="1" /><line x1="26" y1="32.5" x2="41.5" y2="27.5" stroke="#000" strokeWidth="1" />
          </svg>
          <div style={{ position: "absolute", inset: "-20%", rotate: "24deg", translate: `${blade}% 0`, opacity: bladeOn, background: "linear-gradient(90deg, rgba(0,0,0,0) 40%, rgba(255,255,255,.07) 48%, rgba(255,255,255,.11) 50%, rgba(255,255,255,.07) 52%, rgba(0,0,0,0) 60%)" }} />
          <div style={{ position: "absolute", inset: 0, borderRadius: "50%", boxShadow: "inset 5px 5px 7px -3px rgba(235,244,255,.3)" }} />
        </div>
        {around ? <div style={{ position: "absolute", left: G.cx - A, top: G.cy - A, width: 2 * A, height: 2 * A }}>{around}</div> : null}
      </div>
    </div>
  );
};

const ThreeQuarter: React.FC<Props> = ({ clip, clipStart, rate, freeze, still, glassPx, overlay, reveal = 1, bodyOpacity = 1, contentOpacity = 1, focus = 0 }) => {
  const Q = geo.q34;
  const k = glassPx / (2 * Q.b);
  const H = homography(480, Q.quad as Quad);
  // il logo piatto: un quadrato dritto, centrato dove cade il centro del display e grande come il display visto di tre quarti;
  // inclinarsi = passare da questa matrice a quella della prospettiva vera, così alla fine combacia col vetro senza scatti
  const [qx, qy] = applyH(H, [240, 240]);
  const q = Q.quad as Quad;
  const side = (Math.hypot(q[1][0] - q[0][0], q[1][1] - q[0][1]) + Math.hypot(q[2][0] - q[3][0], q[2][1] - q[3][1]) + Math.hypot(q[3][0] - q[0][0], q[3][1] - q[0][1]) + Math.hypot(q[2][0] - q[1][0], q[2][1] - q[1][1])) / 4;
  const flat = [side / 480, 0, qx - side / 2, 0, side / 480, qy - side / 2, 0, 0, 1];
  const M = flat.map((f, i) => f + (H[i] - f) * reveal);
  const ox = Q.cx + (qx - Q.cx) * focus, oy = Q.cy + (qy - Q.cy) * focus;      // focus 1: al centro c'è il display, non il vetro
  return (
    <div style={{ width: Q.size * k, height: Q.size * k, translate: `${-ox * k}px ${-oy * k}px`,
      /* il cinturino finisce con la foto: sfuma nel buio prima che il bordo entri in quadro */
      maskImage: "linear-gradient(180deg, rgba(0,0,0,0) 0%, #000 11%, #000 89%, rgba(0,0,0,0) 100%)" }}>
      <div style={{ width: Q.size, height: Q.size, position: "relative", transformOrigin: "0 0", scale: String(k) }}>
        <Img src={staticFile("mockup/q34_body.png")} style={{ position: "absolute", inset: 0, opacity: bodyOpacity, filter: "drop-shadow(30px 36px 36px rgba(4,5,12,.62))" }} />
        <div style={{ position: "absolute", left: 0, top: 0, width: 480, height: 480, transformOrigin: "0 0", transform: toMatrix3d(M), opacity: contentOpacity, filter: "blur(0.4px) brightness(.95)", ["--k" as string]: "1" }}>
          <Ui clip={clip} clipStart={clipStart} rate={rate} freeze={freeze} still={still} overlay={overlay} />
        </div>
        {/* qui il telefono non c'è: i riflessi VERI della foto sopra l'interfaccia */}
        <Img src={staticFile("mockup/q34_reflections.png")} style={{ position: "absolute", inset: 0, mixBlendMode: "screen", opacity: bodyOpacity }} />
      </div>
    </div>
  );
};

/** Il punto (0,0) del componente è il centro del vetro: chi lo usa lo mette dove vuole e lo inclina. */
export const PhotoWatch: React.FC<Props> = (p) => (
  <div style={{ position: "absolute", left: 0, top: 0, transformOrigin: "0 0", transform: `perspective(1800px) rotateY(${p.view === "threeQuarter" ? p.tilt / 3 : p.tilt}deg)` }}>
    {p.view === "front" ? <Front {...p} /> : p.view === "threeQuarter" ? <ThreeQuarter {...p} /> : (
      <div style={{ translate: "-50% -50%" }}><Watch src={p.clip} d={Math.round(p.glassPx * 0.86)} /></div>
    )}
  </div>
);
