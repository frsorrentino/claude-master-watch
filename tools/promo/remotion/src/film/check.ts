/** `npm run check`: la scaletta vera deve essere valida e ogni clip deve esistere. */
import { existsSync, readFileSync } from "node:fs";
import { totalBeats, validateTimeline } from "./timeline.ts";

const here = new URL(".", import.meta.url).pathname;
const t = validateTimeline(JSON.parse(readFileSync(`${here}timeline.json`, "utf8")));
const missing = t.scenes.filter((s) => s.watch && !existsSync(`${here}../../public/${s.watch.clip}`)).map((s) => `${s.id}: manca public/${s.watch!.clip}`);
if (missing.length) { console.error(missing.join("\n")); process.exit(1); }
console.log(`scaletta valida: ${t.scenes.length} scene, ${totalBeats(t)} battiti, ${((totalBeats(t) * 60) / t.bpm).toFixed(1)} s`);
