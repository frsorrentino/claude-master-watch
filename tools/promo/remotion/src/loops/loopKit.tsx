import React, { useEffect, useState } from "react";
import { continueRender, delayRender, interpolate, staticFile } from "remotion";

// Loop per le pagine dei plugin su francescosorrentino.com: 8 s, 1280×800, finestre scure anche sul tema chiaro.
export const LOOP = { fps: 30, frames: 240, width: 1280, height: 800 };

export const C = {
  page: "#0b0d0c",
  window: "#131614",
  bar: "#181c1a",
  border: "#2a2f2c",
  borderSubtle: "#202422",
  text: "#e8ebe9",
  muted: "#9ca39f",
  faint: "#3a403c",
  accent: "hsl(106 75% 42%)",
  accentText: "hsl(106 75% 52%)",
  warn: "hsl(40 92% 58%)",
};

export const SANS = "Geist, sans-serif";
export const MONO = "'Geist Mono', monospace";

const FACES: [string, string][] = [
  ["Geist", "fonts/geist-latin.woff2"],
  ["Geist Mono", "fonts/geist-mono-latin.woff2"],
];

/** Il render aspetta i caratteri: senza, i primi fotogrammi escono con il carattere di ripiego. */
export const useLoopFonts = (): void => {
  const [handle] = useState(() => delayRender("caratteri dei loop"));
  useEffect(() => {
    Promise.all(FACES.map(([family, file]) => new FontFace(family, `url(${staticFile(file)})`, { weight: "100 900" }).load().then((f) => document.fonts.add(f))))
      .then(() => continueRender(handle));
  }, [handle]);
};

export const ramp = (frame: number, from: number, to: number, out: [number, number] = [0, 1]): number =>
  interpolate(frame, [from, to], out, { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

/** Quanti caratteri di `text` sono scritti al fotogramma `frame`, uno per fotogramma da `start`. */
export const typed = (text: string, frame: number, start: number): string => text.slice(0, Math.max(0, Math.min(text.length, frame - start)));

export const Dots: React.FC = () => (
  <div style={{ display: "flex", gap: 8 }}>
    {[0, 1, 2].map((i) => <span key={i} style={{ width: 12, height: 12, borderRadius: 6, background: C.faint }} />)}
  </div>
);

export const Window: React.FC<{ title: string; style?: React.CSSProperties; bodyStyle?: React.CSSProperties; children?: React.ReactNode }> = ({ title, style, bodyStyle, children }) => (
  <div style={{ display: "flex", flexDirection: "column", background: C.window, border: `1px solid ${C.border}`, borderRadius: 10, overflow: "hidden", ...style }}>
    <div style={{ display: "flex", alignItems: "center", gap: 16, height: 52, padding: "0 16px", whiteSpace: "nowrap", background: C.bar, borderBottom: `1px solid ${C.borderSubtle}`, fontFamily: MONO, fontSize: 24, color: C.muted, flexShrink: 0 }}>
      <Dots />
      <span>{title}</span>
    </div>
    <div style={{ flex: 1, position: "relative", ...bodyStyle }}>{children}</div>
  </div>
);
