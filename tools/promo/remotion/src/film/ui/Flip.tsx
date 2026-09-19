import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { flipAt } from "./flip.ts";
import { UiCard } from "./UiCard.tsx";
import { UI } from "./UiTokens.ts";

/** Il retro della scheda: la stessa superficie, ma con la domanda e il pallino ambra di chi aspetta una risposta. */
const Question: React.FC<{ w: number; name: string; age: string; text: string }> = ({ w, name, age, text }) => (
  <div style={{ width: w, boxSizing: "border-box", padding: "24px 24px 19px", borderRadius: 42, background: UI.surface, color: UI.text, fontFamily: "Roboto" }}>
    <div style={{ display: "flex", alignItems: "center", gap: 16, height: 36 }}>
      <div style={{ width: 32, height: 32, borderRadius: 7.4, background: UI.waiting, display: "grid", placeItems: "center", flex: "none" }}>
        <svg viewBox="0 0 24 24" width="24" height="24"><path d="M9 9a3 3 0 1 1 3 3v2" fill="none" stroke="#000" strokeWidth="2.6" strokeLinecap="round" /><circle cx="12" cy="18.5" r="1.5" fill="#000" /></svg>
      </div>
      <div style={{ fontFamily: "Cousine", fontSize: 29, color: UI.text2, flex: 1, whiteSpace: "nowrap" }}>{name}</div>
      <div style={{ fontSize: 33, color: UI.waiting, whiteSpace: "nowrap" }}>{age}</div>
    </div>
    <div style={{ marginTop: 0, fontSize: 36, lineHeight: "46px", letterSpacing: -0.6 }}>{text}</div>
  </div>
);

/**
 * La scheda ferma al centro **si volta** e sul retro c'è la domanda: stesso oggetto, altra faccia. Sostituisce
 * l'ingrandimento a tutto quadro verso «It asks», che ripeteva il gesto già usato altrove (Franz, 19/09 09:33).
 * Due facce vere in prospettiva CSS (`backfaceVisibility`), non due immagini che si scambiano: mentre gira si vede lo
 * spessore del movimento. Disegnata a livello di film, sopra le due scene, centrata sul taglio.
 */
export const Flip: React.FC<{
  frames: number; w: number; card: { name: string; age: string; text: string; badge?: string; icon?: "check" | "play" }; question: { name: string; age: string; text: string };
}> = ({ frames, w, card, question }) => {
  const f = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const s = flipAt(f / Math.max(1, frames));
  if (s.alpha <= 0) return null;
  const k = w / 427;
  return (
    <div style={{ position: "absolute", left: width / 2, top: height / 2, width: 0, height: 0, opacity: s.alpha, perspective: 2400 }}>
      <div style={{ translate: "-50% -50%", width: w, transformStyle: "preserve-3d", transform: `translateZ(${120 * s.lift}px) rotateY(${s.yaw}rad)` }}>
        <div style={{ backfaceVisibility: "hidden" }}>
          <div style={{ zoom: k }}><UiCard w={427} name={card.name} age={card.age} text={card.text} badge={card.badge} icon={card.icon} light={1} /></div>
        </div>
        <div style={{ position: "absolute", inset: 0, backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}>
          <div style={{ zoom: k }}><Question w={427} name={question.name} age={question.age} text={question.text} /></div>
        </div>
      </div>
    </div>
  );
};
