import React from "react";
import { AbsoluteFill, useCurrentFrame, useVideoConfig } from "remotion";
import { splitAt } from "./split.ts";
import { UI } from "./UiTokens.ts";
import { bump } from "../moves.ts";
import { mixColor } from "./carry.ts";
import { THEME } from "../theme.ts";
import { UiClaudeCode } from "./UiClaudeCode.tsx";
import { CC } from "./claudeCode.ts";

export type SplitSide = { color: string; to?: string; dot: string; account: string; session: string; status: string; tabs: string[]; lines: string[]; prompt?: string; path?: string; model?: string; live?: number };   // `live`: le ultime righe che arrivano mentre la finestra è aperta

/**
 * I due terminali degli account (ui/split.ts per i tempi). Ognuno è una finestra di terminale come quella di Crostini
 * (Franz, 20/09 19:12): barra dei tab in alto — una scheda per sessione, quella attiva più chiara — e sotto le righe che
 * Claude scrive. Il primo si stringe da destra, il secondo entra nella metà libera, poi il lato chiude a sinistra e resta
 * il secondo, che è il fondo della scena dopo.
 */
export const Split: React.FC<{ left: SplitSide; right: SplitSide; open: number; hold: number; close: number; total: number; beat: number }> = ({ left, right, open, hold, close, total, beat }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const s = splitAt(frame / Math.max(1, total), open, hold, close, total / beat);
  const x = s.edge * width;
  // nella chiusura la finestra che resta DIVENTA la scena dopo: il nero vira al colore del campo e il testo si dissolve
  // (Franz, 20/09 20:17)
  const chiude = Math.min(1, Math.max(0, (0.5 - s.edge) / 0.5));
  const pannello = (side: SplitSide, from: number, to: number, lato: "left" | "right", chrome: number) => {
    const resta = lato === "right" ? 1 - Math.min(1, chiude * 1.6) : 1;   // il contenuto della finestra che resta si spegne
    const w = Math.max(0, to - from);
    if (w < 30) return null;
    // Il riempimento segue la larghezza: a tutto quadro il testo sta dove stava nella scena prima (margine dei titoli),
    // stringendosi si avvicina al bordo. Così il terminale di prima DIVENTA questo, non ne compare uno nuovo
    // (Franz, 20/09 19:38). E il testo va a capo davvero, come in una finestra che si restringe.
    const pad = 46 + (THEME.leftMargin - 46) * Math.min(1, w / width);
    // sotto un quarto di quadro il testo andrebbe a capo ogni due parole: la finestra si sta chiudendo, il contenuto
    // si spegne prima di diventare coriandoli
    const leggibile = Math.min(1, Math.max(0, (w / width - 0.12) / 0.14)) * resta;
    // La finestra dell'altro account entra GIÀ PIENA e continua a lavorare: una sessione appena nata non tornava con il
    // racconto (Franz, 21/09 21:22). Solo le ultime `live` righe arrivano mentre è aperta, una a battito da quando si ferma.
    const vive = (sd: SplitSide, i: number) => {
      const live = sd.live ?? 0, n = sd.lines.length;
      if (i < n - live) return 1;
      return Math.min(1, Math.max(0, (frame - (open + (i - (n - live))) * beat) / 12));
    };
    const salto = (t: number) => bump(0.16)(t * t * (3 - 2 * t));
    const entra = (i: number) => salto(Math.min(1, Math.max(0, (s.cards - i * 0.16) / 0.5)));
    return (
      <div style={{ position: "absolute", left: from, top: 0, width: w, height, overflow: "hidden", background: lato === "right" ? mixColor(side.color, side.to ?? side.color, chiude) : side.color }}>
        {/* la barra delle schede compare mentre la finestra si forma */}
        <div style={{ position: "absolute", left: 0, right: 0, top: 0, height: 58, display: "flex", alignItems: "stretch", gap: 2, background: "#111418", opacity: chrome * resta }}>
          {side.tabs.map((t, i) => (
            <div key={i} style={{ display: "flex", alignItems: "center", gap: 12, padding: "0 20px", background: i === 0 ? side.color : "transparent",
              color: i === 0 ? THEME.white : UI.text2, fontFamily: "Inter", fontSize: 24, whiteSpace: "nowrap", opacity: entra(i) * resta }}>
              <span style={{ width: 12, height: 12, borderRadius: "50%", background: side.dot }} />{t}<span style={{ color: UI.text2, opacity: 0.6 }}>×</span>
            </div>
          ))}
          <div style={{ display: "flex", alignItems: "center", padding: "0 16px", color: UI.text2, fontSize: 26, opacity: 0.7 }}>+</div>
          <div style={{ flex: 1 }} />
          <div style={{ display: "flex", alignItems: "center", gap: 18, padding: "0 22px", color: UI.text2, fontSize: 22, opacity: 0.8 }}>— □ ×</div>
        </div>
        {/* il corpo è il terminale di Claude Code della scena prima (Franz, 21/09 19:46), nella stessa colonna: a finestra
            piena combacia riga per riga, e mentre la finestra si stringe il testo va a capo davvero */}
        <div style={{ position: "absolute", left: pad, top: 0, bottom: 0, width: Math.min(width * CC.column, w - pad - 46), opacity: leggibile }}>
          <UiClaudeCode width={Math.min(width * CC.column, w - pad - 46)} header={side.path ? { path: side.path, model: side.model ?? "" } : undefined}
            chrome={1} prompt={side.prompt} promptOn={1}
            rows={side.lines.map((l, i) => ({ text: l, on: vive(side, i) }))}
            status={side.status} account={{ dot: side.dot, label: `${side.account} · ${side.session}` }} frame={frame} />
        </div>
      </div>
    );
  };

  return (
    <AbsoluteFill>
      {pannello(right, x, width, "right", 1)}
      {pannello(left, 0, x, "left", Math.min(1, (1 - s.edge) * 4))}
      {s.edge > 0.02 && s.edge < 0.98 ? <div style={{ position: "absolute", left: x - 2, top: 0, width: 4, height, background: "linear-gradient(180deg, rgba(235,244,255,0) 0%, rgba(235,244,255,.22) 50%, rgba(235,244,255,0) 100%)" }} /> : null}
    </AbsoluteFill>
  );
};
