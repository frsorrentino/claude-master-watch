import React from "react";
import { Audio, Sequence, staticFile } from "remotion";
import { beatToFrame } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { dbToGain, duckGain, sfxCues } from "./sound.ts";
import type { Timeline } from "./timeline.ts";

/** "nosfx": musica e voce, finché i suoni d'interfaccia non sono nel progetto. */
export type Stems = "all" | "nosfx" | "music" | "voice" | "sfx";

export const Soundtrack: React.FC<{ t: Timeline; g: Grid; stems: Stems }> = ({ t, g, stems }) => {
  const on = (s: Stems) => stems === "all" || stems === s || (stems === "nosfx" && s !== "sfx");
  const spoken = t.scenes.flatMap((s) => (s.fx ?? []).flatMap((f) => (f.kind === "spoken" ? [{ from: beatToFrame(g, s.at + f.at), to: beatToFrame(g, s.at + f.at + f.len), voice: f.voice }] : [])));
  const windows = spoken.map((v) => [v.from, v.to] as [number, number]);
  return (
    <>
      {on("music") && t.music ? <Audio src={staticFile(t.music)} volume={(f) => dbToGain(-6) * duckGain(f, windows, -12, 9)} /> : null}
      {on("voice") ? spoken.map((v, i) => <Sequence key={i} from={v.from} layout="none"><Audio src={staticFile(`audio/${v.voice}`)} volume={dbToGain(-3)} /></Sequence>) : null}
      {on("sfx") ? sfxCues(t).flatMap((c, i) => (c.name === "notify" ? [c, { ...c, name: "thump" as const, gainDb: c.gainDb - 4 }] : [c]).map((k, j) => (
        <Sequence key={`${i}-${j}`} from={beatToFrame(g, k.beat)} layout="none"><Audio src={staticFile(`audio/sfx/${k.name}.wav`)} volume={dbToGain(k.gainDb)} /></Sequence>
      ))) : null}
    </>
  );
};
