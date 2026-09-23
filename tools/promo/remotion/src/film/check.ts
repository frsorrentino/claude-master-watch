/** `npm run check`: la scaletta vera deve essere valida e ogni clip deve esistere. */
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { totalBeats, validateTimeline } from "./timeline.ts";

const here = new URL(".", import.meta.url).pathname;
// il film lungo e il corto (23/09): gli stessi controlli per tutte e due le scalette
for (const file of ["timeline.json", "timeline.short.json"]) {
const t = validateTimeline(JSON.parse(readFileSync(`${here}${file}`, "utf8")));
const missing = t.scenes.filter((s) => s.watch && !existsSync(`${here}../../public/${s.watch.clip}`)).map((s) => `${s.id}: manca public/${s.watch!.clip}`);
if (missing.length) { console.error(missing.join("\n")); process.exit(1); }
// La clip deve bastare per la scena: chiedere fotogrammi oltre la fine fa scadere il render (17/09: apertura 3,27 s su una clip da 3 s).
const short = t.scenes.flatMap((s) => {
  if (!s.watch || s.watch.freeze || s.watch.still) return [];
  const file = `${here}../../public/${s.watch.clip}`;
  const dur = Number(execFileSync("ffprobe", ["-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", file], { encoding: "utf8" }));
  const need = (s.watch.clipStart ?? 0) + (((s.len - (s.watch.hold ?? 0)) * 60) / t.bpm) * (s.watch.rate ?? 1);
  return need > dur - 0.04 ? [`${s.id}: servono ${need.toFixed(2)} s di ${s.watch.clip}, che ne dura ${dur.toFixed(2)}`] : [];
});
if (short.length) { console.error(short.join("\n")); process.exit(1); }
console.log(`scaletta valida (${file}): ${t.scenes.length} scene, ${totalBeats(t)} battiti, ${((totalBeats(t) * 60) / t.bpm).toFixed(1)} s`);
}
