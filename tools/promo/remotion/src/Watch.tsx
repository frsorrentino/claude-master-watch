import React from "react";
import { AbsoluteFill, OffthreadVideo, staticFile } from "remotion";

/** Un rettangolo nero sopra la clip, in pixel dello schermo dell'orologio (480×480): copre i testi di sistema in italiano. */
export type Mask = { x: number; y: number; w: number; h: number };

/** La cassa approvata (nera opaca, vetro) attorno allo schermo tondo di 480 px registrato dall'orologio. */
export const Watch: React.FC<{ src: string; d: number; masks?: Mask[] }> = ({ src, d, masks = [] }) => {
  const k = d / 480;
  return (
    <div
      style={{
        position: "relative", width: d * 1.1, height: d * 1.1, borderRadius: "50%",
        background: "radial-gradient(circle at 32% 28%, #2b2e33 0%, #0f1113 68%)",
        boxShadow: "0 40px 80px -24px rgba(0,0,0,.6), inset 0 0 0 2px #4a4f57",
        display: "grid", placeItems: "center",
      }}
    >
      <div
        style={{
          position: "absolute", right: -d * 0.035, top: "50%", translate: "0 -50%", width: d * 0.06, height: d * 0.2,
          borderRadius: d * 0.03, background: "linear-gradient(90deg,#0f1113,#2b2e33 60%,#0f1113)",
        }}
      />
      <div style={{ width: d * 1.02, height: d * 1.02, borderRadius: "50%", background: "#050506", display: "grid", placeItems: "center" }}>
        <div style={{ width: d, height: d, borderRadius: "50%", overflow: "hidden", position: "relative", background: "#000" }}>
          <OffthreadVideo src={staticFile(src)} muted style={{ width: "100%", height: "100%", objectFit: "cover" }} />
          {masks.map((m, i) => (
            <div key={i} style={{ position: "absolute", left: m.x * k, top: m.y * k, width: m.w * k, height: m.h * k, background: "#000" }} />
          ))}
          <AbsoluteFill
            style={{ borderRadius: "50%", background: "radial-gradient(120% 70% at 30% 8%, rgba(255,255,255,.14) 0%, rgba(255,255,255,0) 42%)" }}
          />
        </div>
      </div>
    </div>
  );
};
