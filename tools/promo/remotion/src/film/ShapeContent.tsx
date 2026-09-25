import React from "react";
import { useCurrentFrame } from "remotion";
import { frameToBeat } from "./beats.ts";
import { gridOf } from "./cut.ts";
import type { Fx, Timeline } from "./timeline.ts";
import type { ShapeContent } from "./Shape.tsx";
import { SWAP_IN, SWAP_OUT } from "./shape.ts";
import { sceneAt } from "./shapeDisplay.ts";
import { THEME } from "./theme.ts";
import { soft } from "./moves.ts";
import { UiCard } from "./ui/UiCard.tsx";
import { LIST_BODY } from "./ui/UiTokens.ts";
import { BriefContext, BriefQuestions, BriefWork } from "./ui/Brief.tsx";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
/** Il pannello si disegna dopo che il suo testo è entrato: barre e numeri nascono in tre quarti di battito, come `Aside`. */
const DRAW_BEATS = 0.75;
const PANEL_PAD = 40;

/**
 * Cosa c'è dentro la forma, per id (la traccia `shape` del corto). I dati vengono dalle scene che una volta li
 * disegnavano da sole (la card ✓ della lista, i pannelli della Panoramica): una sola fonte, e la scena non li disegna più
 * dove c'è la forma. Il blocco è grande come la chiave che porta il contenuto (controscala, Shape.tsx): qui si impagina a
 * quella misura. Gli id dei tratti non ancora ribasati non hanno contenuto (passi 4-5).
 */
export const shapeContent = (t: Timeline): ShapeContent => (id, w, h) => <Content t={t} id={id} w={w} h={h} />;

const Content: React.FC<{ t: Timeline; id: string; w: number; h: number }> = ({ t, id, w, h }) => {
  const frame = useCurrentFrame();
  const beat = frameToBeat(gridOf(t), frame);
  const key = (t.shape ?? []).find((k) => k.content === id);
  if (!key) return null;
  const fx = sceneAt(t, key.at).fx ?? [];
  const d = soft(clamp((beat - key.at - SWAP_OUT - SWAP_IN) / DRAW_BEATS));
  if (id === "done") {
    // la card vera della lista (UiCard nello spazio del display): la forma è il suo fondo, la card ci sta sopra identica
    const e = fx.find((f): f is Extract<Fx, { kind: "doneCard" }> => f.kind === "doneCard");
    if (!e) return null;
    return (
      <div style={{ width: 428, transform: `scale(${w / 428})`, transformOrigin: "0 0" }}>
        <UiCard w={428} name={e.name} age={e.age} text={e.text} badge={e.badge} icon="check" checkDraw={1} body={LIST_BODY} />
      </div>
    );
  }
  const a = fx.find((f): f is Extract<Fx, { kind: "aside" }> => f.kind === "aside" && f.panel === id);
  if (!a) return null;
  return (
    <div style={{ position: "absolute", left: PANEL_PAD, top: 0, width: w - 2 * PANEL_PAD, height: h, display: "flex", flexDirection: "column", justifyContent: "center", fontFamily: "Inter", color: THEME.white }}>
      {id === "work" ? <BriefWork n={a.n} note={a.note} bars={a.bars} d={d} /> : id === "questions" ? <BriefQuestions n={a.n} note={a.note} quote={a.quote} d={d} /> : <BriefContext rows={a.rows} d={d} />}
    </div>
  );
};
