import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { C, Cursor, MONO, SANS, Window, ramp, useLoopFonts } from "./loopKit";

// fable-director: «il prezzo prima, il blocco a 3×». Storyboard del sito, 23/09: sopra il terminale, sotto la statusline
// di Claude Code. Righe del terminale da config/plugins/fable-director.php; statusline copiata dalla modalità expert
// di statusline-ctx.sh (segmenti ✦ modello, ctx, 5H in riga 1; bdg e dlg in riga 2 dopo «└ »; a 2× l'allarme giallo
// a parole intere, a 3× il blocco rosso in testa alla riga 1 e il resto in penombra).

type Line = { mark: string; text: string; at: number; kind: "call" | "out" | "ok"; cps?: number };

const LINES: Line[] = [
  { mark: "*", text: "budget-open --expected-output 60000", at: 4, kind: "call", cps: 2 },
  { mark: "*", text: '  --agents 4 --paths "reviews/*"', at: 24, kind: "call", cps: 2 },
  { mark: " ", text: "fd-executor · canary 1/240", at: 50, kind: "out" },
  { mark: "+", text: "✓", at: 72, kind: "ok" },
  { mark: " ", text: "fd-executor · batch 239", at: 82, kind: "out" },
];
// l'ultima riga cresce a pezzi mentre il lotto avanza
const COUNT: [number, string][] = [[96, "80"], [114, "80 · 160"], [132, "80 · 160 · 240/240"]];
const OPEN = 44;              // la riga 2 della statusline compare con il budget aperto
const CANARY = [50, 72];
const BATCH = [84, 136];
const BLOCK = 200;            // la spesa tocca 3× e si ferma
const RESET = [210, 232];

const easeOut = Easing.bezier(0.16, 1, 0.3, 1);
const seg = (f: number, a: number, b: number, from: number, to: number, easing?: (t: number) => number) =>
  interpolate(f, [a, b], [from, to], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing });

/** Spesa sulla stima, 0-3×. */
const ratioAt = (f: number): number => {
  if (f < CANARY[0]) return 0;
  if (f < BATCH[0]) return seg(f, CANARY[0], CANARY[1], 0, 0.1);
  if (f < 138) return seg(f, BATCH[0], BATCH[1], 0.1, 1);
  if (f < 180) return seg(f, 138, 180, 1, 2);
  if (f < RESET[0]) return seg(f, 180, BLOCK, 2, 3, easeOut);
  return seg(f, RESET[0], RESET[1], 3, 0, Easing.inOut(Easing.cubic));
};

const SL = { quiet: C.muted, half: "#4c524e", yellow: C.warn, red: "#e5484d" };

const Statusline: React.FC<{ frame: number; ratio: number }> = ({ frame, ratio }) => {
  const reset = frame >= RESET[0];
  const blocked = frame >= BLOCK && !reset;
  const open = frame >= OPEN && !reset;
  const dlg = frame >= CANARY[0] && frame < CANARY[1] ? "⟲1" : frame >= BATCH[0] && frame < BATCH[1] ? "⟲4" : null;
  // micro-barra 0-3×: una cella per checkpoint intero raggiunto, come in statusline-ctx.sh
  const cells = Math.max(1, Math.min(2, Math.floor(ratio) + 1));
  const bar = "▓".repeat(cells) + "░".repeat(3 - cells);
  // per difetto, come una spesa che sale: 2.96 si legge 2.9, il 3.0 arriva con il blocco
  const r = (Math.floor(ratio * 10 + 1e-6) / 10).toFixed(1);
  const sep = <span style={{ color: SL.half }}> · </span>;
  const row1 = blocked ? SL.half : SL.quiet;
  return (
    <div style={{ fontFamily: MONO, fontSize: 24, lineHeight: 1.7, whiteSpace: "pre", padding: "0 6px" }}>
      <div style={{ color: row1 }}>
        {blocked ? <span style={{ background: SL.red, color: "#0b0d0c", padding: "2px 0" }}>{` ✕ BUDGET 3.0× OF ESTIMATE `}</span> : null}
        {blocked ? " " : null}
        ✦ FABLE 5.1{sep}ctx 24%/1M{sep}5H 38%<span style={{ color: SL.half }}> 17:30</span>
      </div>
      <div style={{ opacity: open && !blocked ? 1 : 0, height: "1.7em" }}>
        <span style={{ color: SL.half }}>└ </span>
        {ratio >= 2 ? <span style={{ color: SL.yellow }}>⚠ BUDGET {r}× OF ESTIMATE</span> : <span style={{ color: SL.quiet }}>bdg {bar} {r}×</span>}
        {dlg ? <>{sep}<span style={{ color: SL.quiet }}>dlg {dlg}</span></> : null}
      </div>
    </div>
  );
};

/** La scala della spesa: 1× la stima, 2× l'avviso, 3× il blocco (le tre soglie del README, come nel grafico della pagina). */
const Gauge: React.FC<{ frame: number; ratio: number }> = ({ frame, ratio }) => {
  const W = 1200;
  const x = (r: number) => (r / 3) * W;
  const hit = ramp(frame, BLOCK - 2, BLOCK + 4) * (1 - ramp(frame, RESET[0], RESET[0] + 10));
  return (
    <div style={{ position: "relative", width: W, height: 110 }}>
      <div style={{ position: "absolute", left: 0, right: 0, top: 20, height: 18, borderRadius: 9, background: C.window, border: `1px solid ${C.border}`, overflow: "hidden" }}>
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: Math.min(x(ratio), x(2)), background: C.accent }} />
        <div style={{ position: "absolute", left: x(2), top: 0, bottom: 0, width: Math.max(0, x(ratio) - x(2)), background: SL.yellow }} />
      </div>
      {[1, 2, 3].map((t) => {
        const color = t === 3 ? `color-mix(in srgb, ${SL.red} ${Math.round(hit * 100)}%, ${C.muted})` : t === 2 && ratio >= 2 ? SL.yellow : C.muted;
        return (
          <div key={t} style={{ position: "absolute", left: x(t) - (t === 3 ? 2 : 1), top: t === 3 ? 6 : 14, width: t === 3 ? 4 : 2, height: t === 3 ? 46 : 30, background: t === 3 ? `color-mix(in srgb, ${SL.red} ${Math.round(hit * 100)}%, ${C.faint})` : C.faint }}>
            <span style={{ position: "absolute", top: t === 3 ? 56 : 48, right: t === 3 ? 0 : undefined, left: t === 3 ? undefined : -14, fontFamily: MONO, fontSize: 26, color }}>{t}×</span>
          </div>
        );
      })}
    </div>
  );
};

export const LoopFableDirector: React.FC = () => {
  useLoopFonts();
  const frame = useCurrentFrame();
  const ratio = ratioAt(frame);
  const fade = 1 - ramp(frame, RESET[0], RESET[0] + 16);
  const cursorOn = Math.floor(frame / 15) % 2 === 0;

  const visible = frame < RESET[1] ? LINES.filter((l) => frame >= l.at) : [];
  const count = frame < RESET[1] ? COUNT.filter(([at]) => frame >= at).pop() : undefined;

  return (
    <AbsoluteFill style={{ background: C.page, padding: 40, gap: 34, fontFamily: SANS }}>
      <Window title="claude" style={{ height: 556 }} bodyStyle={{ display: "flex", flexDirection: "column", padding: "24px 28px 14px" }}>
        <div style={{ flex: 1, fontFamily: MONO, fontSize: 26, lineHeight: 1.75, opacity: fade }}>
          {visible.map((l) => {
            const text = l.kind === "call" ? l.text.slice(0, Math.max(0, Math.min(l.text.length, (frame - l.at) * (l.cps ?? 1)))) : l.text;
            return (
              <div key={l.at} style={{ whiteSpace: "pre", color: l.kind === "out" ? C.muted : C.accentText }}>
                {`${l.mark} `}<span style={{ color: l.kind === "call" ? C.text : undefined }}>{text}</span>
              </div>
            );
          })}
          {count ? <div style={{ whiteSpace: "pre", color: C.muted }}>{`  ${count[1]}`}</div> : null}
        </div>
        {/* la casella d'ingresso di Claude Code, vuota: la sessione lavora da sola */}
        <div style={{ border: `1px solid ${C.border}`, borderRadius: 10, padding: "8px 18px", fontFamily: MONO, fontSize: 26, color: C.muted, whiteSpace: "pre", marginBottom: 10 }}>
          {"> "}{cursorOn ? <Cursor /> : null}
        </div>
        <Statusline frame={frame} ratio={ratio} />
      </Window>
      <Gauge frame={frame} ratio={ratio} />
    </AbsoluteFill>
  );
};
