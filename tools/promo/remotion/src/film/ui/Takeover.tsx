import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { TAKEOVER_CUT, sendAt, takeoverAt } from "./takeover.ts";
import { CC, ccPromptTop, ccStackBottom } from "./claudeCode.ts";
import { CcPrompt } from "./UiClaudeCode.tsx";
import { THEME } from "../theme.ts";
import { mixColor } from "./carry.ts";
import { UI } from "./UiTokens.ts";

/** Cosa c'è dentro il componente che prende il quadro: la card protagonista, le parole dette, il tasto. */
export type TakeoverBody = { kind: "card"; text: string } | { kind: "words"; words: string[]; card?: { name: string; age: string; text: string; badge: string; icon: "check" | "play" } } | { kind: "screen"; lines: string[] } | { kind: "plain" };

/**
 * Un takeover (piano 5): il componente parte dal suo posto (`x`, `y`, `w`, `h`, raggio `r`, colore `color`), **cresce fino a
 * coprire il quadro**, tiene, e il suo colore **diventa lo sfondo della scena dopo** (`toColor`); infine si posa e la scena
 * dopo resta sola. Il contenuto (testo della card, parole dette) sfuma durante la crescita e, per `words`, si ricompone come
 * righe monospazio nel become: sono loro a diventare le righe del terminale.
 * Disegnato a livello del film, sopra le due scene, centrato sul taglio (il taglio cade a `TAKEOVER_CUT` dell'arco).
 */
export const Takeover: React.FC<{
  x: number; y: number; w: number; h: number; r: number; tilt?: number; color: string; toColor: string; frames: number; body?: TakeoverBody;
  press?: number; beat?: number; land?: string;   // l'invio della schermata: il tocco su ✓ e il battito in fotogrammi, il prompt dove si posa il testo
}> = ({ x, y, w, h, r, tilt: tilt0 = 10, color, toColor, frames, body = { kind: "plain" }, press, beat = 16.36, land }) => {
  const f = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const t = takeoverAt(f / Math.max(1, frames));
  // l'invio finisce dopo il resto del takeover: il testo si posa sul prompt più di mezzo battito dopo il taglio
  const sending = body.kind === "screen" && press !== undefined && f <= Math.round(frames * TAKEOVER_CUT) + 1.1 * beat + 3;
  if (t.settle >= 1 && !sending) return null;
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
  // (il tasto della risposta invece è dritto: partiva storto e sembrava cambiare posto, Franz 21/09 18:59)
  const tiltDeg = tilt0 * (1 - Math.min(1, t.grow / 0.35));
  const tilt = `perspective(1600px) rotateX(${tiltDeg}deg)`;
  // la schermata di dettatura: il suo contenuto cresce con lei (un'unità della schermata = `u` pixel) e, se c'è l'invio, il
  // nero resta nero fino al taglio e si apre in un cerchio dal ✓ invece di virare di colore (Franz, 21/09 19:43-19:46)
  const U = w / 427;
  const ease = t.grow * t.grow * (3 - 2 * t.grow);
  const size = 29 * U + (120 - 29 * U) * ease;
  const u = size / 29;
  const send = body.kind === "screen" && press !== undefined ? sendAt(f, press, Math.round(frames * TAKEOVER_CUT), beat) : null;
  const okX = cx - 0.5 * u, okY = cy + 66.5 * u;          // il centro del ✓ (213·173 nella schermata, centro a 213,5·106,5)
  // il cerchio È il ✓ che cresce (Franz, 21/09 20:16): parte dal suo raggio, non da un punto
  const r0 = 32 * u, rMax = Math.hypot(Math.max(okX, width - okX), Math.max(okY, height - okY)) * 1.02;
  // dal fotogramma del taglio il disco c'è già, grande quanto il ✓: prima spariva il tasto un fotogramma prima del disco
  const hole = send && f >= Math.round(frames * TAKEOVER_CUT) ? r0 + (rMax - r0) * send.reveal : 0;
  const mask = hole > 0 ? `radial-gradient(circle at ${okX - (cx - W / 2)}px ${okY - (cy - H / 2)}px, transparent ${hole}px, #000 ${hole + 2}px)` : undefined;
  return (
    <div style={{ position: "absolute", inset: 0, overflow: "hidden", pointerEvents: "none", opacity: send ? 1 : 1 - t.settle }}>
      <div style={{ position: "absolute", left: cx - W / 2, top: cy - H / 2, width: W, height: H, borderRadius: rad, background: send ? color : col, transform: tilt, transformOrigin: "50% 100%",
        maskImage: mask, WebkitMaskImage: mask,
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
      {body.kind === "screen" ? (() => {
        // La schermata di dettatura cresce come cresceva la card della sessione (Franz, 21/09 19:34), tasti compresi (19:39),
        // con il contenuto ATTACCATO AL CENTRO. Posizioni di `UiDictation` (unità della card, 427×213, centro a 213,5·106,5).
        // Poi l'invio (`sendAt`, Franz 20:16-20:24): il dito sul ✓ con l'onda dentro, lo scatto; al taglio il ✓ stesso cresce
        // in un cerchio che schiarisce e mostra il terminale del PC, con un filo di luce sul bordo; la frase si ricompone sul
        // posto come prompt di Claude Code e va a posarsi sopra la casella, dove la scena dopo la prende in consegna.
        const at = (x0: number, y0: number, d: number): React.CSSProperties => ({ position: "absolute", left: (x0 - 213.5) * u - (d * u) / 2, top: (y0 - 106.5) * u - (d * u) / 2, width: d * u, height: d * u });
        const cutF = Math.round(frames * TAKEOVER_CUT);
        const keys = send ? 1 - Math.min(1, send.reveal * 2.5) : 1;
        const okShown = send ? (f < cutF ? 1 : 0) : 1;                       // al taglio il ✓ diventa il disco
        const okScale = send ? (1 - 0.045 * send.squash) * (1 + 0.13 * send.pop) : 1;
        const reformat = send?.reformat ?? 0, fly = send?.fly ?? 0;
        // il blocco del prompt: nasce sul posto della frase a una volta e mezzo, poi si posa sulla riga del prompt
        const colW = width * CC.column, big = 1.5;
        const top = land ? ccPromptTop(height, land, colW) : 0;
        const tx0 = cx, ty0 = cy + (71 - 106.5) * u;
        const blockH = land ? ccStackBottom(height) - top : 0;                 // le righe del prompt × la riga
        const dx0 = tx0 - (colW * big) / 2 - THEME.leftMargin, dy0 = ty0 - (blockH * big) / 2 - top;
        const sc = big + (1 - big) * fly, dx = dx0 * (1 - fly), dy = dy0 * (1 - fly);
        const flyEnd = cutF + 1.1 * beat;
        // prima esce la frase, poi entra il blocco: sovrapposti si leggevano due testi con gli a capo diversi (21/09 20:30)
        const blockOn = land ? Math.min(1, Math.max(0, (reformat - 0.45) / 0.55)) * (1 - Math.min(1, Math.max(0, (f - flyEnd) / 2))) : 0;
        return (
          <div style={{ position: "absolute", left: 0, top: 0, width: 0, height: 0, opacity: send ? 1 : 1 - t.settle }}>
            {/* il disco del ✓ che cresce: crema all'inizio, trasparente presto; il filo di luce resta sul bordo e si spegne */}
            {send && hole > 0 ? (
              <div style={{ position: "absolute", left: okX - hole, top: okY - hole, width: 2 * hole, height: 2 * hole, borderRadius: "50%",
                background: `rgba(250,245,233,${1 - Math.min(1, send.reveal / 0.18)})`, boxShadow: `inset 0 0 0 ${3 + 3 * (1 - send.reveal)}px rgba(255,250,240,${0.55 * (1 - send.reveal)})` }} />
            ) : null}
            <div style={{ position: "absolute", left: tx0, top: ty0, translate: "-50% -50%", whiteSpace: "nowrap", textAlign: "center", fontFamily: "Roboto", fontSize: size, lineHeight: `${36 * u}px`, letterSpacing: -0.2 * u, color: "#FFFFFF", opacity: 1 - Math.min(1, reformat / 0.55), transform: tilt, transformOrigin: "50% 50%" }}>
              {body.lines.map((l, i) => <div key={i}>{l}</div>)}
            </div>
            {land && blockOn > 0 ? (
              <div style={{ position: "absolute", left: THEME.leftMargin, top, width: colW, fontFamily: "Cousine", fontSize: CC.font, lineHeight: `${CC.row}px`, whiteSpace: "pre-wrap", color: "#FFFFFF",
                transformOrigin: "0 0", transform: `translate(${dx}px, ${dy}px) scale(${sc})`, opacity: blockOn }}>
                <CcPrompt prompt={land} />
              </div>
            ) : null}
            {keys > 0 ? (
              <div style={{ position: "absolute", left: cx, top: cy, opacity: keys, transform: tilt, transformOrigin: "50% 100%" }}>
                <svg viewBox="0 0 24 24" style={at(85, 160, 34)}><path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z" fill="#FFFFFF" /></svg>
                <svg viewBox="0 0 24 24" style={at(341, 160, 36)}><path d="M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z" fill="#FFFFFF" /></svg>
                {okShown ? (
                  <div style={{ ...at(213, 173, 64), borderRadius: "50%", overflow: "hidden", background: send ? `color-mix(in srgb, #D9D1BF ${Math.round(60 * send.squash)}%, #FAF5E9)` : "#FAF5E9", display: "grid", placeItems: "center", scale: String(okScale) }}>
                    {/* l'onda della pressione: un cerchio più scuro che si allarga dal centro e si spegne */}
                    {send && send.ripple > 0 && send.ripple < 1 ? <div style={{ position: "absolute", left: "50%", top: "50%", width: `${140 * send.ripple}%`, height: `${140 * send.ripple}%`, translate: "-50% -50%", borderRadius: "50%", background: `rgba(49,48,45,${0.16 * (1 - send.ripple)})` }} /> : null}
                    <svg viewBox="0 0 24 24" width={32 * u} height={32 * u} style={{ position: "relative" }}><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="#31302D" /></svg>
                  </div>
                ) : null}
                {/* il dito, come il «yes» della risposta: il cerchio grigio del «mostra tocchi» sul ✓ */}
                {send && send.finger > 0.001 ? (
                  <div style={{ ...at(213, 173, 80), borderRadius: "50%", background: "rgba(58,60,70,.38)", boxShadow: "0 0 0 2px rgba(255,255,255,.75)", opacity: Math.min(1, send.finger * 3), scale: String(0.9 + 0.1 * Math.min(1, send.finger * 3)) }} />
                ) : null}
              </div>
            ) : null}
          </div>
        );
      })() : null}
    </div>
  );
};

export { TAKEOVER_CUT };
