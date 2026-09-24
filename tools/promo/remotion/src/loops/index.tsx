import React from "react";
import { Composition } from "remotion";
import { LOOP } from "./loopKit";
import { LoopChromeBridge } from "./LoopChromeBridge";
import { LoopClaudeMaster } from "./LoopClaudeMaster";
import { LoopFableDirector } from "./LoopFableDirector";

export const Loops: React.FC = () => (
  <>
    <Composition id="LoopClaudeMaster" component={LoopClaudeMaster} fps={LOOP.fps} width={LOOP.width} height={LOOP.height} durationInFrames={LOOP.frames} />
    <Composition id="LoopFableDirector" component={LoopFableDirector} fps={LOOP.fps} width={LOOP.width} height={LOOP.height} durationInFrames={LOOP.frames} />
    <Composition id="LoopChromeBridge" component={LoopChromeBridge} fps={LOOP.fps} width={LOOP.width} height={LOOP.height} durationInFrames={LOOP.frames} />
  </>
);
