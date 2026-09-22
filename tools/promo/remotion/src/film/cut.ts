/** Una scaletta porta con sé la sua griglia: il film lungo e il corto ne hanno una ciascuno. */
import { beatToFrame } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { totalBeats } from "./timeline.ts";
import type { Timeline } from "./timeline.ts";

export const gridOf = (t: Timeline): Grid => ({ bpm: t.bpm, fps: t.fps, offsetSeconds: t.offsetSeconds });
export const framesOf = (t: Timeline): number => beatToFrame(gridOf(t), totalBeats(t));
