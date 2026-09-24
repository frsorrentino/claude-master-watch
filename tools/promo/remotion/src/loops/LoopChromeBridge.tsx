import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { C, Cursor, Dots, MONO, SANS, Window, ramp, typed, useLoopFonts } from "./loopKit";

// chrome-bridge: «un turno, il modulo intero». Storyboard del sito, 23/09: a sinistra il terminale (40%), a destra
// Chrome su app.example/signup con l'accesso già fatto. Righe da config/plugins/chrome-bridge.php, scene 2-4.

type Line = { mark: string; text: string; at: number; kind: "call" | "out" | "ok" };

const LINES: Line[] = [
  { mark: "*", text: "navigate  app.example/signup", at: 36, kind: "call" },
  { mark: " ", text: "refs: n1 n2 n3 n4 · 1 turn", at: 74, kind: "out" },
  { mark: "*", text: "fill_form  3 fields", at: 90, kind: "call" },
  { mark: "*", text: 'click  ref=n4  "Send"', at: 138, kind: "call" },
  { mark: "*", text: 'assert  #success  "Done"', at: 170, kind: "call" },
  { mark: "+", text: "✓", at: 200, kind: "ok" },
];
const REFS_AT = [44, 52, 60, 68];   // n1-n4, una dopo l'altra mentre navigate si scrive
const FILL = [112, 128];            // i tre campi insieme
const PRESS = 162;                  // onda sul pulsante
const DONE = 172;
const CHECK = 200;                  // cornice verde sul messaggio
const RESET = [210, 232];

const FIELDS: [string, string][] = [["Name", "Ada Lovelace"], ["Email", "ada@example.com"], ["City", "London"]];

const lineEnd = (l: Line): number => (l.kind === "call" ? l.at + l.text.length : l.at);

const Ref: React.FC<{ id: string; opacity: number }> = ({ id, opacity }) => (
  <span style={{ position: "absolute", top: -14, right: 14, padding: "0 10px", borderRadius: 6, background: C.accent, color: C.page, fontFamily: MONO, fontSize: 24, lineHeight: "32px", opacity, scale: `${0.8 + 0.2 * opacity}` }}>{id}</span>
);

const Lock: React.FC = () => (
  <svg width="22" height="26" viewBox="0 0 22 26" style={{ flexShrink: 0 }}>
    <rect x="2" y="11" width="18" height="13" rx="3" fill={C.muted} />
    <path d="M6 11 V7.5 a5 5 0 0 1 10 0 V11" fill="none" stroke={C.muted} strokeWidth="2.6" />
  </svg>
);

export const LoopChromeBridge: React.FC = () => {
  useLoopFonts();
  const frame = useCurrentFrame();
  const back = ramp(frame, RESET[0], RESET[1]);
  const keep = 1 - back;
  const cursorOn = Math.floor(frame / 15) % 2 === 0;

  const visible = LINES.filter((l) => frame >= l.at && frame < RESET[1]);
  const current = visible[visible.length - 1];
  const typing = !!current && current.kind === "call" && frame < lineEnd(current);
  const fill = ramp(frame, FILL[0], FILL[1]) * keep;
  const ripple = ramp(frame, PRESS, PRESS + 14);
  const done = ramp(frame, DONE, DONE + 8) * keep;
  const check = ramp(frame, CHECK, CHECK + 6) * keep;

  return (
    <AbsoluteFill style={{ background: C.page, padding: 40, flexDirection: "row", gap: 24, fontFamily: SANS }}>
      <Window title="claude" style={{ width: 490 }} bodyStyle={{ padding: "26px 24px", fontFamily: MONO, fontSize: 24, lineHeight: 1.8 }}>
        <div style={{ opacity: 1 - ramp(frame, RESET[0], RESET[0] + 16) }}>
          {visible.map((l, i) => {
            const text = l.kind === "call" ? typed(l.text, frame, l.at) : l.text;
            const last = i === visible.length - 1;
            return (
              <div key={l.at} style={{ whiteSpace: "pre", color: l.kind === "out" ? C.muted : C.accentText }}>
                {`${l.mark} `}<span style={{ color: l.kind === "call" ? C.text : undefined }}>{text}</span>
                {last && typing ? <Cursor /> : null}
              </div>
            );
          })}
        </div>
        {!typing && !(frame >= RESET[0] && frame < RESET[1]) && cursorOn ? <div><Cursor /></div> : null}
      </Window>

      <div style={{ flex: 1, display: "flex", flexDirection: "column", background: C.window, border: `1px solid ${C.border}`, borderRadius: 10, overflow: "hidden" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 18, height: 72, padding: "0 16px", background: C.bar, borderBottom: `1px solid ${C.borderSubtle}` }}>
          <Dots />
          <div style={{ flex: 1, display: "flex", alignItems: "center", gap: 12, height: 46, padding: "0 18px", borderRadius: 23, background: C.page, border: `1px solid ${C.borderSubtle}`, fontFamily: SANS, fontSize: 24, color: C.text }}>
            <Lock />
            <span>app.example<span style={{ color: C.muted }}>/signup</span></span>
          </div>
        </div>
        <div style={{ flex: 1, padding: "40px 56px", display: "flex", flexDirection: "column", gap: 26 }}>
          {FIELDS.map(([label, value], i) => (
            <div key={label}>
              <div style={{ fontSize: 24, color: C.muted, marginBottom: 8 }}>{label}</div>
              <div style={{ position: "relative", height: 60, borderRadius: 10, border: `1px solid ${C.border}`, background: C.page, display: "flex", alignItems: "center", padding: "0 18px", fontSize: 26, color: C.text }}>
                {/* i tre valori entrano nello stesso momento: una sola chiamata, non tre */}
                <span style={{ opacity: fill, translate: `${(1 - fill) * 8}px 0` }}>{value}</span>
                <Ref id={`n${i + 1}`} opacity={ramp(frame, REFS_AT[i], REFS_AT[i] + 6) * keep} />
              </div>
            </div>
          ))}
          <div style={{ display: "flex", alignItems: "center", gap: 24, marginTop: 10 }}>
            <div style={{ position: "relative", width: 200, height: 60, borderRadius: 10, background: C.text, color: C.page, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 26, fontWeight: 600, overflow: "visible" }}>
              <div style={{ position: "absolute", inset: 0, borderRadius: 10, overflow: "hidden" }}>
                <div style={{ position: "absolute", left: 100 - 120 * ripple, top: 30 - 120 * ripple, width: 240 * ripple, height: 240 * ripple, borderRadius: "50%", background: C.accent, opacity: 0.55 * (1 - ripple) }} />
              </div>
              <span style={{ position: "relative", marginRight: 30 }}>Send</span>
              <Ref id="n4" opacity={ramp(frame, REFS_AT[3], REFS_AT[3] + 6) * keep} />
            </div>
            <div style={{ padding: "10px 20px", borderRadius: 10, border: `1px solid color-mix(in srgb, ${C.accent} ${Math.round(check * 100)}%, transparent)`, boxShadow: check > 0.01 ? `0 0 0 ${2 * check}px ${C.accent}` : undefined, fontSize: 26, color: C.accentText, opacity: done }}>Done</div>
          </div>
        </div>
      </div>
    </AbsoluteFill>
  );
};
