import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame, useVideoConfig } from "remotion";
import raw from "./timeline.json";
import { beatToFrame, spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { MOVE_BEATS, poseAt } from "./moves.ts";
import { totalBeats, validateTimeline } from "./timeline.ts";
import type { Scene } from "./timeline.ts";
import { Backdrop } from "./Backdrop.tsx";
import { PhotoWatch } from "./PhotoWatch.tsx";
import { WordMask } from "./WordMask.tsx";
import { THEME } from "./theme.ts";
import { useFilmFonts } from "./fonts.ts";
import { fxLayers } from "./Fx.tsx";

/** Scaletta sbagliata = il film non parte: l'errore elenca tutti i problemi. */
export const TIMELINE = validateTimeline(raw);
export const GRID: Grid = { bpm: TIMELINE.bpm, fps: TIMELINE.fps, offsetSeconds: TIMELINE.offsetSeconds };
export const filmFrames = (): number => beatToFrame(GRID, totalBeats(TIMELINE)) - beatToFrame(GRID, 0);

export const SceneView: React.FC<{ scene: Scene; overlay?: React.ReactNode; around?: React.ReactNode }> = ({ scene, overlay, around }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const total = spanFrames(GRID, scene.at, scene.len);
  const beat = spanFrames(GRID, scene.at, 1);
  const w = scene.watch;
  const pose = w ? poseAt(frame, total, beat * MOVE_BEATS, w.enter, w.exit) : null;
  const cx = (scene.text ? THEME.watchX : 0.5) * width;
  const textAt = spanFrames(GRID, scene.at, scene.text?.at ?? 0);
  return (
    <AbsoluteFill>
      <Backdrop act={scene.act} glowX={scene.text ? THEME.watchX : 0.5} />
      {w && pose ? (
        <div style={{ position: "absolute", width: 0, height: 0, left: cx + pose.x * width, top: height / 2 + pose.y * height, transformOrigin: "0 0", scale: String(pose.scale) }}>
          <PhotoWatch view={w.view} clip={w.clip} clipStart={w.clipStart} tilt={pose.tilt} overlay={overlay} around={around}
            glassPx={w.view === "threeQuarter" ? THEME.q34GlassPx : THEME.frontGlassPx} />
        </div>
      ) : null}
      {scene.text ? (
        <Sequence from={textAt} layout="none">
          <div style={{ position: "absolute", left: w ? THEME.leftMargin : 0, right: w ? undefined : 0, top: 0, bottom: 0, display: "flex", alignItems: "center", justifyContent: w ? "flex-start" : "center" }}>
            <WordMask lines={scene.text.lines} accent={scene.text.accent} size={scene.text.size} sub={scene.text.sub}
              perWordFrames={w ? Math.round(beat / 2) : beat} exitAt={total - textAt - 8} align={w ? "left" : "center"} />
          </div>
        </Sequence>
      ) : null}
    </AbsoluteFill>
  );
};

export const Film: React.FC = () => {
  useFilmFonts();
  const zero = beatToFrame(GRID, 0);
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      {TIMELINE.scenes.map((s) => (
        <Sequence key={s.id} name={s.id} from={beatToFrame(GRID, s.at) - zero} durationInFrames={spanFrames(GRID, s.at, s.len)}>
          <SceneView scene={s} {...fxLayers(s, GRID)} />
        </Sequence>
      ))}
    </AbsoluteFill>
  );
};
