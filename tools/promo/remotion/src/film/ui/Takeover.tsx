import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { TAKEOVER_CUT, takeoverAt } from "./takeover.ts";
import { mixColor } from "./carry.ts";
import { THEME } from "../theme.ts";
import { UI } from "./UiTokens.ts";
import { UiCard } from "./UiCard.tsx";

/** Cosa c'è dentro il componente che prende il quadro: la card protagonista, le parole dette, il tasto. */
export type TakeoverBody = { kind: "card"; text: string } | { kind: "words"; words: string[]; card?: { name: string; age: string; text: string; badge: string; icon: "check" | "play" } } | { kind: "plain" };

/**
 * Un takeover (piano 5): il componente parte dal suo posto (`x`, `y`, `w`, `h`, raggio `r`, colore `color`), **cresce fino a
 * coprire il quadro**, tiene, e il suo colore **diventa lo sfondo della scena dopo** (`toColor`); infine si posa e la scena
 * dopo resta sola. Il contenuto (testo della card, parole dette) sfuma durante la crescita e, per `words`, si ricompone come
 * righe monospazio nel become: sono loro a diventare le righe del terminale.
 * Disegnato a livello del film, sopra le due scene, centrato sul taglio (il taglio cade a `TAKEOVER_CUT` dell'arco).
 */
export const Takeover: React.FC<{
  x: number; y: number; w: number; h: number; r: number; color: string; toColor: string; frames: number; body?: TakeoverBody;
}> = ({ x, y, w, h, r, color, toColor, frames, body = { kind: "plain" } }) => {
  const f = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const t = takeoverAt(f / Math.max(1, frames));
  if (t.settle >= 1) return null;
  // il rettangolo cresce dal suo posto fino a coprire il quadro con abbondanza (il raggio si spegne mentre diventa fondo)
  const W = w + (width * 1.25 - w) * t.grow, H = h + (height * 1.35 - h) * t.grow;
  const cx = x + (width / 2 - x) * t.grow, cy = y + (height / 2 - y) * t.grow;
  const rad = r * (1 - t.grow) + 0 * t.grow;
  const col = mixColor(color, toColor, t.become);
  return (
    <div style={{ position: "absolute", inset: 0, overflow: "hidden", pointerEvents: "none", opacity: 1 - t.settle }}>
      <div style={{ position: "absolute", left: cx - W / 2, top: cy - H / 2, width: W, height: H, borderRadius: rad, background: col, boxShadow: t.grow < 1 ? `0 ${40 * (1 - t.grow)}px ${80 * (1 - t.grow)}px -20px rgba(4,5,12,.7)` : undefined }}>
      </div>
      {/* il contenuto sta in coordinate di SCHERMO, non dentro il rettangolo che cresce oltre il quadro */}
      {body.kind === "card" ? (
        // finché cresce si vede ancora che è la card (nome e testo), poi il campo resta nudo per la scena dopo
        <div style={{ position: "absolute", left: width * 0.14, top: height * 0.3, width: width * 0.62, opacity: Math.max(0, 1 - t.become * 2.2), fontFamily: "Roboto", fontSize: 44 + 26 * t.grow, lineHeight: 1.3, color: UI.text }}>
          <div style={{ display: "flex", alignItems: "center", gap: 18, marginBottom: 22, opacity: 1 - t.grow * 0.4 }}>
            <span style={{ width: 34 + 16 * t.grow, height: 34 + 16 * t.grow, borderRadius: "50%", background: "#2ECC71", display: "inline-block" }} />
            <span style={{ fontFamily: "Cousine", fontSize: 30 + 14 * t.grow, color: UI.text2 }}>storefront</span>
          </div>
          {body.text}
        </div>
      ) : null}
      {body.kind === "words" && body.card ? (
        // parte ESATTAMENTE come una scheda della corsia (Franz, 18/09 19:13) e sfuma mentre il rettangolo cresce
        <div style={{ position: "absolute", left: cx, top: cy, width: 0, height: 0, opacity: Math.max(0, 1 - t.grow * 2.6) }}>
          <div style={{ translate: "-50% -50%", width: w }}>
            <div style={{ zoom: w / 427 }}><UiCard w={427} name={body.card.name} age={body.card.age} text={body.card.text} badge={body.card.badge} icon={body.card.icon} light={1} /></div>
          </div>
        </div>
      ) : null}
      {body.kind === "words" ? (
        // le parole dette si dispongono come righe monospazio: nel become sono già le righe del terminale
        <div style={{ position: "absolute", left: width * 0.16, top: height * 0.34, width: width * 0.68, textAlign: "center", opacity: Math.min(1, Math.max(0, (t.grow - 0.35) / 0.4)) * (1 - t.settle), fontFamily: t.become > 0.5 ? "Cousine" : "Inter", fontWeight: t.become > 0.5 ? 400 : 600, fontSize: 70 - 26 * t.become, lineHeight: t.become > 0.5 ? "62px" : 1.25, color: t.become > 0.5 ? UI.text2 : THEME.white, whiteSpace: "pre-wrap", letterSpacing: t.become > 0.5 ? 0 : "-0.02em" }}>
          {body.words.map((l, i) => <div key={i} style={{ opacity: 1 - 0.14 * i * t.become }}>{l}</div>)}
        </div>
      ) : null}
    </div>
  );
};

export { TAKEOVER_CUT };
