import React from "react";
import { Composition } from "remotion";
import { LOOP } from "./loopKit";
import { LoopClaudeMaster } from "./LoopClaudeMaster";

export const Loops: React.FC = () => (
  <>
    <Composition id="LoopClaudeMaster" component={LoopClaudeMaster} fps={LOOP.fps} width={LOOP.width} height={LOOP.height} durationInFrames={LOOP.frames} />
  </>
);
