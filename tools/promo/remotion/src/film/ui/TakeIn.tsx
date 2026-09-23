import React from "react";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { CC, ccBoxHeight } from "./claudeCode.ts";
import { UiClaudeCode, type CcRow } from "./UiClaudeCode.tsx";
import { UiCard } from "./UiCard.tsx";
import { LIST_BODY, UI } from "./UiTokens.ts";
import { slotRect, takeInAt, takeInRect } from "./takeIn.ts";

type TakeInFx = Extract<Fx, { kind: "takeIn" }>;
type TermFx = Extract<Fx, { kind: "terminalPlane" }>;
/** All'arrivo la finestra resta ancora otto fotogrammi sopra la card vera e sfuma. Posizione e misura coincidono (misurato
 *  al pixel), ma la card vera sta sotto il vetro dell'orologio e ne prende il riflesso: così il riflesso arriva piano. */
const HANDOFF = 8;

/**
 * Il volo del terminale (piano 4, primo pezzo; Franz, 23/09 20:15): il quadro intero, cioè il terminale della scena prima
 * fermo al suo ultimo fotogramma, si rimpicciolisce ed entra nel display come la card ✓ della riga `slot` (tempi in
 * ui/takeIn.ts). La riga dell'esito vola a parte, si stringe fino alla larghezza del testo della card e ci si scioglie
 * dentro; all'arrivo la card ✓ vera, dentro il display, prende il suo posto. `f` fotogrammi dall'inizio del volo, `frames`
 * la sua durata; `dx`, `dy`, `u` il display nel quadro, come per i tasti del «yes».
 */
export const TakeIn: React.FC<{ e: TakeInFx; prev?: Scene; g: Grid; f: number; frames: number; dx: number; dy: number; u: number; width: number; height: number }> = ({ e, prev, g, f, frames, dx, dy, u, width, height }) => {
  const term = (prev?.fx ?? []).find((x): x is TermFx => x.kind === "terminalPlane");
  if (!prev || !term || f < 0 || f >= frames + HANDOFF) return null;
  const p = Math.min(1, f / frames);
  const k = takeInAt(p);
  const r = takeInRect(slotRect(e.slot, dx, dy, u, 2), p, { w: width, h: height });
  const out = f < frames ? 1 : 1 - (f - frames) / HANDOFF;
  const colW = width * CC.column;
  const last = term.lines.length - 1;
  const rows: CcRow[] = term.lines.map((text, i) => ({ text, on: 1, ghost: i === last }));
  // la riga dell'esito nel terminale: l'ultima, appoggiata sopra la casella (UiClaudeCode), dopo «● »
  const y0 = height - (CC.bottom + ccBoxHeight(CC.row) + CC.row / 2) - CC.row;
  const x0 = THEME.leftMargin + 2 * 0.6 * CC.font;
  const text = term.lines[last].replace(/^⏺\s*/, "");
  // va verso il testo della card COM'È ADESSO (la card in volo è ancora più grande): nella larghezza del suo testo, centrata
  // sulla prima riga. Mirando alla misura finale, a metà volo la riga sembrava minuscola dentro la card
  const c = r.w / 428;
  const s1 = ((428 - 48) * c) / (text.length * 0.6 * CC.font);
  const x1 = r.x + 24 * c;
  const y1 = r.y + (24 + 36 + LIST_BODY.shift + LIST_BODY.line / 2) * c - (CC.row / 2) * s1;
  const q = k.line;
  const lerp = (a: number, b: number) => a * (1 - q) + b * q;
  const mono: React.CSSProperties = { position: "absolute", fontFamily: "Cousine", fontSize: CC.font, lineHeight: `${CC.row}px`, color: THEME.white, whiteSpace: "pre" };
  return (
    <>
      <div style={{ position: "absolute", left: r.x, top: r.y, width: r.w, height: r.h, borderRadius: r.r, overflow: "hidden", background: UI.surface, opacity: out }}>
        {k.terminal > 0 ? (
          <div style={{ position: "absolute", left: 0, top: 0, width, height, transform: `scale(${r.w / width})`, transformOrigin: "0 0", opacity: k.terminal }}>
            <div style={{ position: "absolute", left: THEME.leftMargin, top: 0, bottom: 0, width: colW }}>
              <UiClaudeCode width={colW} header={{ path: term.path ?? "", model: term.model ?? "" }} chrome={1} prompt={term.prompt} promptOn={1} rows={rows} status={term.status}
                frame={spanFrames(g, prev.at, prev.len) + f} />
            </div>
            {/* il pallino della riga che vola resta al suo posto e se ne va col terminale */}
            <div style={{ ...mono, left: THEME.leftMargin, top: y0 }}>{"● "}</div>
          </div>
        ) : null}
        {k.card > 0 ? (
          <div style={{ position: "absolute", left: 0, top: 0, transform: `scale(${r.w / 428})`, transformOrigin: "0 0", opacity: k.card }}>
            <UiCard w={428} name={e.name} age={e.age} text={e.text} badge={e.badge} icon="check" checkDraw={0} body={LIST_BODY} bodyOpacity={k.body} />
          </div>
        ) : null}
      </div>
      {k.lineAlpha > 0 ? (
        <div style={{ ...mono, left: lerp(x0, x1), top: lerp(y0, y1), transform: `scale(${lerp(1, s1)})`, transformOrigin: "0 0", opacity: k.lineAlpha }}>{text}</div>
      ) : null}
    </>
  );
};
