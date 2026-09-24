import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { C, Cursor, MONO, SANS, Window, ramp, typed, useLoopFonts } from "./loopKit";

// claude-master: «tre sessioni, una sola regia». Storyboard del sito, 23/09: a sinistra la griglia 2×2 delle schede
// tmux, a destra la sessione director. Testi presi dal terminale delle scene (config/plugins/claude-master.php).

type Line = { mark: string; text: string; at: number; kind: "call" | "out" | "ok" };

// fotogrammi a 30 fps; le chiamate si scrivono un carattere per fotogramma da `at`
const LINES: Line[] = [
  { mark: "*", text: "claude-master sessions", at: 8, kind: "call" },
  { mark: "*", text: "claude-master launch orbit", at: 60, kind: "call" },
  { mark: " ", text: "orbit-docs · tab opened", at: 92, kind: "out" },
  { mark: "*", text: 'claude-master talk atlas "ship it"', at: 108, kind: "call" },
  { mark: "*", text: "claude-master next", at: 162, kind: "call" },
  { mark: "+", text: "✓", at: 188, kind: "ok" },
];
const SESSIONS_ENTER = 36;
const NEEDS = [42, 60];      // atlas-shop lampeggia una volta
const ORBIT_OPEN = [92, 110];
const TALK_DONE = 148;       // atlas-shop passa da waiting a busy
const RESET = [210, 232];    // tutto torna al primo fotogramma

const lineEnd = (l: Line): number => (l.kind === "call" ? l.at + l.text.length : l.at);

type State = "waiting" | "busy" | "idle";
const STATE_COLOR: Record<State, string> = { waiting: C.warn, busy: C.accentText, idle: C.muted };

const StateRow: React.FC<{ state: State; frame: number; opacity?: number }> = ({ state, frame, opacity = 1 }) => {
  // il pallino di busy pulsa con periodo 60: 240 è un multiplo, il giro si chiude senza salto
  const pulse = state === "busy" ? 0.7 + 0.3 * Math.cos((frame / 60) * 2 * Math.PI) : 1;
  return (
    <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", gap: 14, opacity }}>
      <span style={{ width: 16, height: 16, borderRadius: 8, background: STATE_COLOR[state], opacity: pulse }} />
      <span style={{ fontFamily: MONO, fontSize: 28, color: state === "idle" ? C.muted : C.text }}>{state}</span>
    </div>
  );
};

const Skeleton: React.FC<{ widths: number[]; frame: number; busy?: number }> = ({ widths, frame, busy = 0 }) => (
  <div style={{ display: "flex", flexDirection: "column", gap: 16, marginTop: 30 }}>
    {widths.map((w, i) => {
      const last = i === widths.length - 1;
      // l'ultima riga di una sessione al lavoro si allunga e si accorcia, sempre con periodo 60
      const grow = last ? busy * 0.25 * (0.5 - 0.5 * Math.cos((frame / 60) * 2 * Math.PI)) : 0;
      return <span key={i} style={{ height: 12, borderRadius: 6, width: `${(w + grow) * 100}%`, background: C.faint }} />;
    })}
  </div>
);

const Card: React.FC<{ name: string; frame: number; flash?: number; border?: string; children: React.ReactNode; style?: React.CSSProperties }> = ({ name, flash = 0, border, children, style }) => (
  <Window
    title={name}
    style={{ flex: 1, borderColor: border ?? C.border, boxShadow: flash > 0 ? `0 0 0 ${2 * flash}px ${C.warn}` : undefined, ...style }}
    bodyStyle={{ padding: "26px 24px" }}
  >
    {children}
  </Window>
);

export const LoopClaudeMaster: React.FC = () => {
  useLoopFonts();
  const frame = useCurrentFrame();

  const back = ramp(frame, RESET[0], RESET[1]);                 // 0 → 1 durante il ritorno
  const scan = (i: number) => ramp(frame, SESSIONS_ENTER + i * 3, SESSIONS_ENTER + i * 3 + 4) - ramp(frame, SESSIONS_ENTER + i * 3 + 8, SESSIONS_ENTER + i * 3 + 16);
  const scanBorder = (i: number) => (scan(i) > 0.01 ? `color-mix(in srgb, ${C.accent} ${Math.round(scan(i) * 100)}%, ${C.border})` : undefined);

  // atlas-shop: il lampo una volta, poi «needs you» finché non gli si risponde
  const blink = Math.sin(ramp(frame, NEEDS[0], NEEDS[1]) * Math.PI);
  const needs = ramp(frame, NEEDS[0], NEEDS[0] + 6) * (1 - ramp(frame, TALK_DONE, TALK_DONE + 8));
  const atlasBusy = ramp(frame, TALK_DONE, TALK_DONE + 8) * (1 - back);

  const orbit = ramp(frame, ORBIT_OPEN[0], ORBIT_OPEN[1]) * (1 - back);

  const director = 1 - back;
  const visible = LINES.filter((l) => frame >= l.at && frame < RESET[1]);
  const current = visible[visible.length - 1];
  const typing = current && current.kind === "call" && frame < lineEnd(current);
  const cursorOn = Math.floor(frame / 15) % 2 === 0 || typing;

  return (
    <AbsoluteFill style={{ background: C.page, padding: 40, flexDirection: "row", gap: 24, fontFamily: SANS }}>
      <div style={{ width: 540, display: "grid", gridTemplateColumns: "1fr 1fr", gridTemplateRows: "1fr 1fr", gap: 20 }}>
        <Card name="atlas-shop" frame={frame} flash={blink} border={needs > 0.01 ? `color-mix(in srgb, ${C.warn} ${Math.round(needs * 70)}%, ${C.border})` : scanBorder(0)}>
          <div style={{ position: "relative", height: 36 }}>
            {/* uno esce, poi l'altro entra: incrociati, le due parole si leggerebbero una sull'altra */}
            <StateRow state="waiting" frame={frame} opacity={Math.max(0, 1 - 2 * atlasBusy)} />
            <StateRow state="busy" frame={frame} opacity={Math.max(0, 2 * atlasBusy - 1)} />
          </div>
          <div style={{ marginTop: 18, height: 40, opacity: needs }}>
            <span style={{ display: "inline-block", padding: "4px 14px", border: `1px solid ${C.warn}`, borderRadius: 20, fontFamily: MONO, fontSize: 24, color: C.warn }}>needs you</span>
          </div>
          <Skeleton widths={[0.9, 0.6]} frame={frame} busy={atlasBusy} />
        </Card>
        <Card name="ledger-api" frame={frame} border={scanBorder(1)}>
          <div style={{ position: "relative", height: 36 }}><StateRow state="busy" frame={frame} /></div>
          <Skeleton widths={[0.85, 0.7, 0.95, 0.45]} frame={frame} busy={1} />
        </Card>
        <Card name="field-notes" frame={frame} border={scanBorder(2)}>
          <div style={{ position: "relative", height: 36 }}><StateRow state="idle" frame={frame} /></div>
          <Skeleton widths={[0.75, 0.9, 0.5]} frame={frame} />
        </Card>
        <div style={{ position: "relative" }}>
          {/* la cella vuota: bordo tratteggiato, finché launch non ci apre orbit-docs */}
          <div style={{ position: "absolute", inset: 0, border: `1px dashed ${C.faint}`, borderRadius: 10, opacity: 1 - orbit }} />
          <div style={{ position: "absolute", inset: 0, display: "flex", opacity: orbit, scale: `${0.94 + 0.06 * orbit}` }}>
            <Card name="orbit-docs" frame={frame} border={`color-mix(in srgb, ${C.accent} ${Math.round((1 - ramp(frame, ORBIT_OPEN[1], ORBIT_OPEN[1] + 30)) * 100)}%, ${C.border})`}>
              <div style={{ position: "relative", height: 36 }}><StateRow state="busy" frame={frame} /></div>
              <Skeleton widths={[0.55, 0.8]} frame={frame} busy={1} />
            </Card>
          </div>
        </div>
      </div>

      <Window title="director" style={{ flex: 1 }} bodyStyle={{ padding: "28px 28px", fontFamily: MONO, fontSize: 26, lineHeight: 1.8 }}>
        <div style={{ opacity: director }}>
          {visible.map((l, i) => {
            const text = l.kind === "call" ? typed(l.text, frame, l.at) : l.text;
            const color = l.kind === "out" ? C.muted : C.accentText;
            const last = i === visible.length - 1;
            return (
              <div key={l.at} style={{ whiteSpace: "pre", color }}>
                {`${l.mark} `}
                <span style={{ color: l.kind === "call" ? C.text : color }}>{text}</span>
                {last && typing && cursorOn ? <Cursor /> : null}
              </div>
            );
          })}
        </div>
        {/* fermo, il cursore sta sulla riga dopo l'ultima; durante il ritorno sparisce e riappare sulla prima, come al fotogramma 0 */}
        {!typing && !(frame >= RESET[0] && frame < RESET[1]) && cursorOn ? <div><Cursor /></div> : null}
      </Window>
    </AbsoluteFill>
  );
};
