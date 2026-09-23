import React from "react";
import { useCurrentFrame } from "remotion";
import { UiCard } from "./UiCard.tsx";
import { doneCardAt } from "./doneCard.ts";

/** La card ✓ sul display (spazio 480): il display sotto si scurisce mentre la card sale, e il ✓ si disegna (ui/doneCard.ts). */
export const DoneCard: React.FC<{ name: string; age: string; text: string; badge?: string; beat: number; rest?: boolean }> = ({ name, age, text, badge, beat, rest }) => {
  const f = useCurrentFrame();
  const { rise, draw } = doneCardAt(f, beat, rest);
  const y = 134 + (1 - rise) * 346;   // a riposo al centro del display (card alta 213); parte da sotto il bordo
  return (
    <div style={{ position: "absolute", inset: 0 }}>
      <div style={{ position: "absolute", inset: 0, background: "rgba(8,9,12,0.82)", opacity: rise }} />
      <div style={{ position: "absolute", left: 26, top: y }}>
        <UiCard w={428} name={name} age={age} text={text} badge={badge} icon="check" checkDraw={draw} />
      </div>
    </div>
  );
};
