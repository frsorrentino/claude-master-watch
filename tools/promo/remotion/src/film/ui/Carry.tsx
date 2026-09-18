import React from "react";
import { useCurrentFrame } from "remotion";
import { carryAt, isStroke, mixColor, strokeAt } from "./carry.ts";
import type { Glyph, Key } from "./carry.ts";

/** I glifi dei badge dell'app, in un quadrato 24×24: gli stessi segni che l'utente vede sul display. */
const GLYPHS: Record<Glyph, React.ReactNode> = {
  play: <path d="M8.5 6v12l10-6z" fill="currentColor" />,
  question: <path d="M9.2 9.3a2.9 2.9 0 1 1 4.3 2.5c-.9.5-1.5 1.1-1.5 2.2v.5M12 18h.01" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />,
  check: <path d="M5.5 12.5l4 4L18.5 7.5" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />,
  bell: <path d="M12 3a5 5 0 0 0-5 5v3.5L5.5 14v1h13v-1L17 11.5V8a5 5 0 0 0-5-5zm-2 14a2 2 0 0 0 4 0" fill="currentColor" />,
  mic: <path d="M12 3a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3zM7 11a5 5 0 0 0 10 0M12 16v4M9 20h6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />,
  none: null,
};

/**
 * L'oggetto che attraversa un taglio: da `from` (com'è nell'ultima scena) a `to` (com'è nella prima della scena dopo), in
 * `frames` fotogrammi centrati sul taglio. Contenitore che cambia posto, misura, raggio e colore; glifo che si scambia in
 * dissolvenza a metà strada; linea ↔ arco per punti. Sopra tutto, a livello del film.
 */
export const Carry: React.FC<{ from: Key; to: Key; frames: number }> = ({ from, to, frames }) => {
  const f = useCurrentFrame();
  const t = f / Math.max(1, frames);
  if (t < 0 || t > 1) return null;
  // ai due capi l'oggetto è ancora quello della scena (che lo disegna da sé): qui compare e sparisce in 3 fotogrammi
  const on = Math.min(1, f / 3, (frames - f) / 3);
  if (isStroke(from) || isStroke(to)) {
    const pts = strokeAt(from, to, t);
    const d = pts.map(([x, y], i) => `${i ? "L" : "M"} ${x.toFixed(1)} ${y.toFixed(1)}`).join(" ");
    const w = (from.stroke ?? 4) + ((to.stroke ?? 4) - (from.stroke ?? 4)) * t;
    return (
      <svg style={{ position: "absolute", inset: 0, pointerEvents: "none", opacity: on }} width="100%" height="100%">
        <path d={d} fill="none" stroke={mixColor(from.color, to.color, t)} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }
  const c = carryAt(from, to, t);
  const glyph = (g: Glyph | undefined, a: number, color: string | undefined) =>
    g && g !== "none" && a > 0 ? (
      <svg viewBox="0 0 24 24" style={{ position: "absolute", inset: "18%", width: "64%", height: "64%", opacity: a, color: color ?? "#fff" }}>{GLYPHS[g]}</svg>
    ) : null;
  return (
    <div style={{ position: "absolute", left: c.x - c.w / 2, top: c.y - c.h / 2, width: c.w, height: c.h, borderRadius: c.r, background: c.color, opacity: on, pointerEvents: "none", boxShadow: "0 18px 40px -14px rgba(4,5,12,.6)" }}>
      {glyph(from.glyph, c.fromGlyph, from.glyphColor)}
      {glyph(to.glyph, c.toGlyph, to.glyphColor)}
    </div>
  );
};
