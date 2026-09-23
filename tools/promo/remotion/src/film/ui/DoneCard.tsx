import React from "react";
import { useCurrentFrame } from "remotion";
import { UiCard } from "./UiCard.tsx";
import { doneCardAt, doneCardPlace } from "./doneCard.ts";

/** La card ✓ sul display (spazio 480): sale mentre il display si scurisce, oppure sta ferma in una riga della lista (`slot`);
 *  in tutti e due i casi il ✓ si disegna (ui/doneCard.ts). */
export const DoneCard: React.FC<{ name: string; age: string; text: string; badge?: string; beat: number; rest?: boolean; slot?: number }> = ({ name, age, text, badge, beat, rest, slot }) => {
  const f = useCurrentFrame();
  const { rise, draw } = doneCardAt(f, beat, rest);
  const { y, scrim } = doneCardPlace(rise, slot);
  return (
    <div style={{ position: "absolute", inset: 0 }}>
      <div style={{ position: "absolute", inset: 0, background: "rgba(8,9,12,0.82)", opacity: scrim }} />
      <div style={{ position: "absolute", left: 26, top: y }}>
        <UiCard w={428} name={name} age={age} text={text} badge={badge} icon="check" checkDraw={draw} />
      </div>
    </div>
  );
};
