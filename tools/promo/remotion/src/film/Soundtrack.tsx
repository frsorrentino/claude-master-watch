import React from "react";
import { Audio, Sequence, staticFile } from "remotion";
import { beatToFrame } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { dbToGain, duckGain, sfxCues, sleepGain, stopGain } from "./sound.ts";
import type { Timeline } from "./timeline.ts";

/** "nosfx": musica e voce, finché i suoni d'interfaccia non sono nel progetto. */
export type Stems = "all" | "nosfx" | "music" | "voice" | "sfx";

/** Le tre battute di introduzione della traccia, che il film non usa (misurate: 6,528 s a 110 bpm). */
export const MUSIC_INTRO_SECONDS = 0;   // il film tiene il crescendo d'introduzione: il suo culmine è il momento dello zoom (Franz, 19/09 14:53)

export const Soundtrack: React.FC<{ t: Timeline; g: Grid; stems: Stems }> = ({ t, g, stems }) => {
  const on = (s: Stems) => stems === "all" || stems === s || (stems === "nosfx" && s !== "sfx");
  const spoken = t.scenes.flatMap((s) => (s.fx ?? []).flatMap((f) => (f.kind === "spoken" ? [{ from: beatToFrame(g, s.at + f.at), to: beatToFrame(g, s.at + f.at + f.len), voice: f.voice }] : [])));
  const windows = spoken.map((v) => [v.from, v.to] as [number, number]);
  // il sonno del display: la musica scende con l'ambient e riparte sul risveglio (sound.ts, `sleepGain`)
  const naps = t.scenes.flatMap((s, i) => {
    const next = t.scenes[i + 1];
    if (!s.sleep || !next) return [];
    const frames = beatToFrame(g, s.at + s.sleep.len) - beatToFrame(g, s.at);
    return [{ cut: beatToFrame(g, next.at), frames, back: beatToFrame(g, next.at + (s.sleep.musicBackBeats ?? 4)) }];
  });
  const stops = t.scenes.flatMap((s) => (s.fx ?? []).flatMap((f) => (f.kind === "musicStop" ? [[beatToFrame(g, s.at + f.at), beatToFrame(g, s.at + f.at + f.len)] as [number, number]] : [])));
  return (
    <>
      {/* la traccia ha tre battute di introduzione sommessa (RMS 0,08 · 0,11 · 0,14 contro 0,32 del corpo): il film le salta
          e parte dalla prima battuta piena, com'era nella v5 — Franz, 19/09 05:12: «la musica inizia dalla terza battuta
          anziché dalla prima». È anche ciò che rimette lo zoom della complication sulla pausa del brano. */}
      {on("music") && t.music ? (
        <Sequence from={Math.round(((t.musicDelayBeats ?? 0) * 60) / g.bpm * g.fps)} layout="none">
          <Audio src={staticFile(t.music)} trimBefore={Math.round(MUSIC_INTRO_SECONDS * g.fps)} volume={(f) => dbToGain(-6) * duckGain(f, windows, -12, 9) * stopGain(f, stops) * sleepGain(f, naps)} />
        </Sequence>
      ) : null}
      {on("voice") ? spoken.map((v, i) => <Sequence key={i} from={v.from} layout="none"><Audio src={staticFile(`audio/${v.voice}`)} volume={dbToGain(-3)} /></Sequence>) : null}
      {on("sfx") ? sfxCues(t).flatMap((c, i) => (c.name === "notify" ? [c, { ...c, name: "thump" as const, gainDb: c.gainDb - 4 }] : [c]).map((k, j) => (
        <Sequence key={`${i}-${j}`} from={beatToFrame(g, k.beat)} layout="none"><Audio src={staticFile(`audio/sfx/${k.name}.wav`)} volume={dbToGain(k.gainDb)} /></Sequence>
      ))) : null}
    </>
  );
};
