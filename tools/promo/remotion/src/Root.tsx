import React from "react";
import { Composition } from "remotion";
import { FPS, Release, durationInFrames } from "./Release";
import { Compare } from "./film/Compare.tsx";
import { Film, SHORT_TIMELINE, ShortFilm, filmFrames } from "./film/Film.tsx";
import { framesOf } from "./film/cut.ts";
import { LogoProva } from "./film/LogoProva.tsx";
import { ShapeBoard } from "./film/ShapeBoard.tsx";
import type { ShapeSpan } from "./film/Shape.tsx";
import { WallProva } from "./film/WallProva.tsx";
import { Loops } from "./loops/index.tsx";

export const RemotionRoot: React.FC = () => (
  <>
    <Composition id="Release" component={Release} fps={FPS} width={1920} height={1080} durationInFrames={durationInFrames()} />
    <Composition id="Film" component={Film} defaultProps={{ stems: "all" as const }} fps={30} width={1920} height={1080} durationInFrames={filmFrames()} />
    <Composition id="Short" component={ShortFilm} defaultProps={{ stems: "all" as const, blur: true, shape: { from: 58 } as ShapeSpan | null, effects: { shadow: true, ripple: true, light: true } }} fps={30} width={1920} height={1080} durationInFrames={framesOf(SHORT_TIMELINE)} />
    <Composition id="ShapeBoard" component={ShapeBoard} fps={30} width={1920} height={1080} durationInFrames={framesOf(SHORT_TIMELINE)} />
    <Composition id="LogoProva" component={LogoProva} fps={30} width={1920} height={1080} durationInFrames={110} />
    <Composition id="WallProva" component={WallProva} fps={30} width={1920} height={1080} durationInFrames={130} />
    <Composition id="Compare" component={Compare} fps={30} width={1920} height={1080} durationInFrames={90} />
    <Loops />
  </>
);
