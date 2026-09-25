import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { frameToBeat } from "./beats.ts";
import { gridOf } from "./cut.ts";
import { SHORT_TIMELINE } from "./Film.tsx";
import { useFilmFonts } from "./fonts.ts";
import { contentAt, inkOn } from "./shape.ts";
import { restDisplay, sceneAt } from "./shapeDisplay.ts";
import { CameraFrame, Shape } from "./Shape.tsx";
import type { ShapeContent } from "./Shape.tsx";

const KEYS = SHORT_TIMELINE.shape ?? [];
const GRID = gridOf(SHORT_TIMELINE);
const DISPLAY = restDisplay(SHORT_TIMELINE);

/** Il contenuto di prova: l'id al centro, nell'inchiostro che si legge sul colore della chiave che lo porta. */
const probe = (beat: number): ShapeContent => (id) => (
  <div style={{ display: "flex", width: "100%", height: "100%", alignItems: "center", justifyContent: "center", fontFamily: "Inter", fontWeight: 600, fontSize: 28, color: inkOn(contentAt(KEYS, beat)?.key.color ?? "#000000") }}>{id}</div>
);

/**
 * La tavola della forma (specifica §6, passo 2): la sola forma del corto su fondo piatto, senza foto né scene, per
 * giudicare il percorso prima di toccare le scene. Il contorno è il display a riposo della scena: dove la forma si aggancia.
 */
export const ShapeBoard: React.FC = () => {
  useFilmFonts();
  const beat = frameToBeat(GRID, useCurrentFrame());
  const scene = sceneAt(SHORT_TIMELINE, beat);
  const [x, y, w, h] = DISPLAY(beat);
  return (
    <AbsoluteFill style={{ background: "#1b1f2e" }}>
      <CameraFrame keys={KEYS} g={GRID} display={DISPLAY}>
        <div style={{ position: "absolute", left: x, top: y, width: w, height: h, borderRadius: w / 2, boxSizing: "border-box", border: "1px solid rgba(255,255,255,.25)" }} />
      </CameraFrame>
      <Shape keys={KEYS} g={GRID} display={DISPLAY} content={probe(beat)} />
      <div style={{ position: "absolute", left: 40, top: 32, fontFamily: "Inter", fontWeight: 500, fontSize: 24, color: "rgba(255,255,255,.6)" }}>
        {beat.toFixed(1)} · {scene.id}
      </div>
    </AbsoluteFill>
  );
};
