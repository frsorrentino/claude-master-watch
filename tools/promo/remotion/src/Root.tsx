import React from "react";
import { Composition } from "remotion";
import { FPS, Release, durationInFrames } from "./Release";
import { Compare } from "./film/Compare.tsx";
import { Film, filmFrames } from "./film/Film.tsx";
import { LogoProva } from "./film/LogoProva.tsx";

export const RemotionRoot: React.FC = () => (
  <>
    <Composition id="Release" component={Release} fps={FPS} width={1920} height={1080} durationInFrames={durationInFrames()} />
    <Composition id="Film" component={Film} defaultProps={{ stems: "nosfx" as const }} fps={30} width={1920} height={1080} durationInFrames={filmFrames()} />
    <Composition id="LogoProva" component={LogoProva} fps={30} width={1920} height={1080} durationInFrames={110} />
    <Composition id="Compare" component={Compare} fps={30} width={1920} height={1080} durationInFrames={90} />
  </>
);
