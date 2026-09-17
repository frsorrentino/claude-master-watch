import React from "react";
import { Composition } from "remotion";
import { FPS, Release, durationInFrames } from "./Release";
import { Compare } from "./film/Compare.tsx";
import { Film, filmFrames } from "./film/Film.tsx";

export const RemotionRoot: React.FC = () => (
  <>
    <Composition id="Release" component={Release} fps={FPS} width={1920} height={1080} durationInFrames={durationInFrames()} />
    <Composition id="Film" component={Film} fps={30} width={1920} height={1080} durationInFrames={filmFrames()} />
    <Composition id="Compare" component={Compare} fps={30} width={1920} height={1080} durationInFrames={90} />
  </>
);
