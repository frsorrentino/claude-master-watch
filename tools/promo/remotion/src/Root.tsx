import React from "react";
import { Composition } from "remotion";
import { FPS, Release, durationInFrames } from "./Release";

export const RemotionRoot: React.FC = () => (
  <Composition id="Release" component={Release} fps={FPS} width={1920} height={1080} durationInFrames={durationInFrames()} />
);
