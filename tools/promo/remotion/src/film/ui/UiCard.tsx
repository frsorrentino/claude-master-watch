import React from "react";
import { evolvePath } from "@remotion/paths";
import { CARD_BODY, UI, type CardBody } from "./UiTokens.ts";

const CHECK = "M5.5 12.5l4 4L18.5 7.5";

/**
 * La card di sessione dell'app (`SessionRow.kt`), ricostruita nello spazio del display 480×480 (1 dp = 2 px): raggio 21 dp,
 * riempimento 12 dp, badge quadrato 16 dp con angoli al 23 %, nome mono 13 sp, età 15 sp, testo 16 sp; sull'orologio di Franz i
 * caratteri sono al 110 % (misurato sul fotogramma: testo 36 px, interlinea 46), e le misure qui sono quelle. Stesse misure e stessi
 * colori della card vera, così a grandezza 1 combacia col fotogramma. `light` 0-1: quanto è fuori dallo schermo (ombra a terra
 * e luce dall'alto a sinistra, come sul vetro dell'orologio; sul display non ce n'è).
 */
export const UiCard: React.FC<{ w: number; name: string; age: string; text: string; badge?: string; icon?: "check" | "play"; light?: number; checkDraw?: number; body?: CardBody }> = ({ w, name, age, text, badge = UI.badge, icon = "check", light = 0, checkDraw, body = CARD_BODY }) => (
  <div style={{ width: w, boxSizing: "border-box", padding: "24px 24px 24.5px", borderRadius: 42, background: UI.surface, color: UI.text, fontFamily: "Roboto",
    boxShadow: `inset 1px 1px 0 rgba(235,244,255,${0.16 * light}), ${16 * light}px ${24 * light}px ${44 * light}px ${-6 * light}px rgba(4,5,12,${0.62 * light})` }}>
    <div style={{ display: "flex", alignItems: "center", gap: 16, height: 36 }}>
      {/* badge come `SessionBadge`: quadrato con angoli al 23 % per il lavoro, tondo per il personale; ✓ = finita, ▶ = al lavoro */}
      <div style={{ width: 32, height: 32, borderRadius: icon === "play" ? "50%" : 7.4, background: badge, display: "grid", placeItems: "center", flex: "none" }}>
        <svg viewBox="0 0 24 24" width="24" height="24">
          {icon === "play" ? <path d="M8.5 6v12l10-6z" fill="#000" /> : <path d={CHECK} fill="none" stroke="#fff" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" {...(checkDraw === undefined ? {} : evolvePath(checkDraw, CHECK))} />}
        </svg>
      </div>
      <div style={{ fontFamily: "Cousine", fontSize: 29, color: UI.text2, flex: 1, whiteSpace: "nowrap" }}>{name}</div>
      <div style={{ fontSize: 33, color: UI.text2, whiteSpace: "nowrap" }}>{age}</div>
    </div>
    {/* il corpo sale di 9,5 unità sotto l'intestazione: misurato il 19/09 con la correlazione 2D fra la card costruita e
        quella dentro il display (18 px di quadro a zoom 1,42 = 9,6 unità). Badge e nome combaciavano già; scendeva il testo.
        Il riempimento in basso cresce di altrettanto, così la card resta alta 213 unità come quella vera. */}
    <div style={{ marginTop: body.shift, fontSize: body.size, lineHeight: `${body.line}px`, letterSpacing: -0.6 }}>{text}</div>
  </div>
);
