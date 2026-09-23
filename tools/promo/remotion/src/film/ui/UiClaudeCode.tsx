import React from "react";
import { CC, ccBoxHeight, ccKind, ccLines, ccTool } from "./claudeCode.ts";
import { soft } from "../moves.ts";
import { UI } from "./UiTokens.ts";
import { THEME } from "../theme.ts";

/**
 * Il terminale di Claude Code (regole e misure in `claudeCode.ts`): la stessa colonna nella scena «Watch it work.» e nelle
 * finestre degli account, così al taglio le righe restano dove sono. Come in Claude Code la casella «>» sta in fondo e la
 * conversazione si appoggia sopra: ogni frase nasce lì, intera, e cresce in altezza spingendo su il resto — l'intestazione
 * sale e col tempo esce in alto (Franz, 21/09 20:24).
 */
export type CcRow = { text: string; on: number; cursor?: boolean; ghost?: boolean };   // `ghost`: tiene il suo posto ma non si vede (il volo del terminale la porta via, 23/09)

// la mascotte di Claude Code a pixel: corpo, occhi, le braccia ai lati, quattro zampe
const CLAWD = [".XXXXXXXXX.", ".XXKXXXKXX.", "XXXXXXXXXXX", "XXXXXXXXXXX", ".XXXXXXXXX.", ".X.X...X.X.", ".X.X...X.X."];
export const Clawd: React.FC<{ cell: number }> = ({ cell }) => (
  <svg width={11 * cell} height={7 * cell} viewBox="0 0 11 7" shapeRendering="crispEdges" style={{ display: "block" }}>
    {CLAWD.flatMap((r, y) => [...r].map((c, x) => (c === "." ? null : <rect key={`${x}-${y}`} x={x} y={y} width={1.02} height={1.02} fill={c === "K" ? "#000" : UI.coral} />)))}
  </svg>
);

/** Il prompt inviato, evidenziato come in Claude Code; le righe dopo la prima rientrano sotto il testo, non sotto «>». Lo usa
 *  anche il takeover dell'invio, che lo porta al suo posto: deve essere lo stesso disegno. */
export const CcPrompt: React.FC<{ prompt: string }> = ({ prompt }) => (
  <div style={{ paddingLeft: "2ch", textIndent: "-2ch" }}>
    <span style={{ background: UI.track, boxDecorationBreak: "clone", WebkitBoxDecorationBreak: "clone", padding: "4px 0.4ch" }}>{"> "}{prompt}</span>
  </div>
);

export const UiClaudeCode: React.FC<{
  width: number;
  header?: { path: string; model: string };
  chrome?: number;                       // 0-1: intestazione, casella e riga di stato
  prompt?: string;
  promptOn?: number;                     // 0-1: la riga del prompt (ci si posa il testo dettato; il suo posto c'è da subito)
  rows: CcRow[];
  status?: string;
  account?: { dot: string; label: string };
  frame: number;
  font?: number;
}> = ({ width, header, chrome = 1, prompt, promptOn = 1, rows, status, account, frame, font = CC.font }) => {
  const row = (font / CC.font) * CC.row;
  const blink = frame % 30 < 16;
  // la riga nuova cresce in altezza (così la pila sale in modo continuo) e intanto si accende
  const rise = (on: number) => soft(Math.min(1, Math.max(0, on)));
  const first = rows.findIndex((r) => r.on > 0);
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width, height: "100%", fontFamily: "Cousine", fontSize: font, lineHeight: `${row}px`, whiteSpace: "pre-wrap", color: THEME.white }}>
      {/* l'intestazione col logo resta ferma in alto (Franz, 21/09 20:35); sale solo la conversazione */}
      {header ? (
        <div style={{ position: "absolute", left: 0, top: CC.top, display: "flex", alignItems: "center", gap: 28, height: 3 * row, opacity: chrome }}>
          <Clawd cell={row * 0.22} />
          <div>
            <div style={{ fontWeight: 700 }}>Claude Code</div>
            <div style={{ color: UI.text2 }}>{header.model}</div>
            <div style={{ color: UI.text2 }}>{header.path}</div>
          </div>
        </div>
      ) : null}
      {/* la zona che scorre: fra l'intestazione e la casella. Le righe più vecchie, spinte su, passano sotto l'intestazione e
          sfumano, come nella parte di un terminale che scorre */}
      <div style={{ position: "absolute", left: 0, width, top: header ? CC.top + 3 * row + row / 4 : 0, bottom: CC.bottom + ccBoxHeight(row) + row / 2, overflow: "hidden",
        maskImage: `linear-gradient(180deg, transparent 0, #000 ${row * 0.6}px)`, WebkitMaskImage: `linear-gradient(180deg, transparent 0, #000 ${row * 0.6}px)` }}>
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, display: "flex", flexDirection: "column" }}>
        {prompt ? <div style={{ marginTop: row / 2, flex: "none", opacity: promptOn }}><CcPrompt prompt={prompt} /></div> : null}
        {rows.map((r, i) => {
          if (r.on <= 0) return null;
          const k = ccKind(r.text);
          const h = rise(r.on) * (ccLines(r.text, width, font) * row + (i === first && prompt ? row / 2 : 0));
          const inner: React.CSSProperties = { position: "absolute", left: 0, right: 0, bottom: 0, opacity: r.ghost ? 0 : Math.min(1, r.on * 1.4) };
          const line = k === "result"
            ? <div style={{ color: UI.text2, paddingLeft: "5ch", textIndent: "-3ch" }}>{"⎿  "}{r.text.replace(/^⎿\s*/, "")}</div>
            : k === "says"
              ? <div style={{ paddingLeft: "2ch", textIndent: "-2ch" }}><span style={{ color: THEME.white }}>● </span>{r.text.replace(/^⏺\s*/, "")}</div>
              : (() => { const t = ccTool(r.text); return <div style={{ paddingLeft: "2ch", textIndent: "-2ch" }}><span style={{ color: UI.briefGood }}>● </span><span style={{ fontWeight: 700 }}>{t.name}</span>{t.args}</div>; })();
          return <div key={i} style={{ position: "relative", height: h, flex: "none", overflow: "hidden" }}><div style={inner}>{line}</div></div>;
        })}
      </div>
      </div>
      {/* la casella dove si scrive, fra due righe, e sotto la riga di stato: come Claude Code in attesa. Tutto alla misura
          del testo, come in un terminale vero, dove ogni carattere occupa la stessa cella (Franz, 21/09 20:18: la barra era
          sproporzionata). Il nome dell'account, se c'è, sta sulla riga di sopra come titolo del riquadro. */}
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 26, opacity: chrome }}>
        {/* la riga sopra la casella è alta una riga anche senza nome: così al taglio fra le scene la linea non si sposta */}
        {account ? (
          <div style={{ display: "flex", alignItems: "center", gap: "0.5ch", color: UI.text2, height: row }}>
            <div style={{ flex: 1, height: 1.5, background: UI.track }} />
            <span style={{ display: "inline-block", width: font * 0.3, height: font * 0.3, borderRadius: "50%", background: account.dot }} />
            <span style={{ color: account.dot }}>{account.label.split(" · ")[0]}</span>{account.label.includes(" · ") ? ` · ${account.label.split(" · ").slice(1).join(" · ")}` : ""}
            <div style={{ width: "1ch", height: 1.5, background: UI.track }} />
          </div>
        ) : <div style={{ display: "flex", alignItems: "center", height: row }}><div style={{ flex: 1, height: 1.5, background: UI.track }} /></div>}
        <div>{"> "}<span style={{ display: "inline-block", width: "0.6em", height: "1.05em", background: THEME.white, verticalAlign: "-0.2em", opacity: blink ? 1 : 0 }} /></div>
        <div style={{ height: 1.5, background: UI.track }} />
        <div style={{ color: UI.text2 }}>{status ?? "? for shortcuts"}</div>
      </div>
    </div>
  );
};
