import React from "react";
import { AbsoluteFill } from "remotion";
import { Backdrop } from "./Backdrop.tsx";
import { PhotoWatch } from "./PhotoWatch.tsx";

export const Compare: React.FC = () => (
  <AbsoluteFill>
    <Backdrop act="know" glowX={0.5} />
    {(["drawn", "front"] as const).map((view, i) => (
      <div key={view} style={{ position: "absolute", left: 1920 * (0.27 + 0.46 * i), top: 540 }}>
        <PhotoWatch view={view} clip="scenes/s1_list.mp4" clipStart={2} glassPx={700} tilt={0} />
      </div>
    ))}
  </AbsoluteFill>
);
