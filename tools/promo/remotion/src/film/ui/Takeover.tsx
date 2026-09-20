import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { TAKEOVER_CUT, takeoverAt } from "./takeover.ts";
import { mixColor } from "./carry.ts";
import { UI } from "./UiTokens.ts";

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
  // La card si INGRANDISCE: un oggetto solo che cresce di scala, non un rettangolo che esce da dentro. Un fattore unico per
  // le due dimensioni, e con lui crescono raggio e filo di luce sul bordo — se il raggio si spegne e le proporzioni
  // cambiano, quello che si vede è un rettangolo grigio, non la scheda (Franz, 20/09 12:57).
  const kEnd = Math.max((width * 1.25) / w, (height * 1.35) / h);
  const k = 1 + (kEnd - 1) * t.grow;
  const W = w * k, H = h * k;
  const cx = x + (width / 2 - x) * t.grow, cy = y + (height / 2 - y) * t.grow;
  const rad = r * k;
  const col = mixColor(color, toColor, t.become);
  // la scheda nella corsia è inclinata di 10° (prospettiva): il takeover parte con la STESSA inclinazione e si raddrizza
  // mentre cresce, se no nel primo fotogramma il contenuto si scosta di qualche pixel (Franz, 20/09 16:12)
  const tiltDeg = 10 * (1 - Math.min(1, t.grow / 0.35));
  const tilt = `perspective(1600px) rotateX(${tiltDeg}deg)`;
  return (
    <div style={{ position: "absolute", inset: 0, overflow: "hidden", pointerEvents: "none", opacity: 1 - t.settle }}>
      <div style={{ position: "absolute", left: cx - W / 2, top: cy - H / 2, width: W, height: H, borderRadius: rad, background: col, transform: tilt, transformOrigin: "50% 100%",
        // il filo di luce sul bordo e l'ombra a terra sono quelli della scheda, ingranditi con lei: restano finché la
        // scheda è riconoscibile, poi si spengono mentre diventa il fondo della scena dopo
        boxShadow: `inset ${1 * k}px ${1 * k}px 0 rgba(235,244,255,${0.16 * (1 - t.become)}), 0 ${24 * k * (1 - t.become)}px ${44 * k * (1 - t.become)}px ${-6 * k}px rgba(4,5,12,${0.62 * (1 - t.become)})` }}>
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
      {body.kind === "words" && body.card ? (() => {
        // Nessuno scambio: è la SCHEDA che diventa il quadro (Franz, 20/09 14:00). Il contenuto — intestazione compresa —
        // sta ATTACCATO AL CENTRO della scheda e cresce con lei: così è solo un ingrandimento, senza scivolate a sinistra
        // e ritorni al centro (Franz, 20/09 15:30). Le righe restano quelle della card: l'andata a capo non cambia mai.
        const U = w / 427;
        const ease = t.grow * t.grow * (3 - 2 * t.grow);
        const size = 36 * U + (120 - 36 * U) * ease;             // il corpo: da quello della scheda alla misura finale
        const kText = size / (36 * U);
        const colW = (427 - 48) * U * kText;
        // il contenuto della card VERA è centrato nella scheda (riempimento 24 sopra e 24,5 sotto, intestazione 36 e corpo
        // che sale di 9,5: il blocco cade a metà). Ancorarlo più in basso lo faceva scendere di una dozzina di pixel nel
        // primo fotogramma dello zoom (Franz, 20/09 16:12). Alla fine si posa più in alto del centro (20/09 15:50).
        const cyContent = cy - 110 * ease;
        return (
          <div style={{ position: "absolute", left: cx, top: cyContent, width: 0, height: 0, opacity: 1 - t.settle, transform: tilt, transformOrigin: "50% 100%" }}>
            <div style={{ translate: "-50% -50%", width: colW }}>
              <div style={{ display: "flex", alignItems: "center", gap: 16 * U * kText, height: 36 * U * kText }}>
                <div style={{ width: 32 * U * kText, height: 32 * U * kText, borderRadius: "50%", background: body.card.badge, flex: "none" }} />
                <div style={{ fontFamily: "Cousine", fontSize: 29 * U * kText, color: UI.text2, flex: 1, whiteSpace: "nowrap" }}>{body.card.name}</div>
                <div style={{ fontSize: 33 * U * kText, color: UI.text2, whiteSpace: "nowrap" }}>{body.card.age}</div>
              </div>
              {/* stesso incastro della card vera: il corpo sale di 9,5 unità sotto l'intestazione (UiCard) */}
              <div style={{ marginTop: -9.5 * U * kText, fontFamily: "Roboto", fontSize: size, lineHeight: 1.28, letterSpacing: -0.6 * U * kText, color: UI.text }}>{body.card.text}</div>
            </div>
          </div>
        );
      })() : null}
    </div>
  );
};

export { TAKEOVER_CUT };
