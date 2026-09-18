#!/usr/bin/env python3
"""Gli intervalli di fotogrammi che contengono 3D (ThreeCanvas), letti dalla scaletta.

Perché esiste: con un ThreeCanvas in scena il contesto WebGL non si crea con `--gl=egl` né con `--gl=angle` (provato il
18/09 su questa macchina: «THREE.WebGLRenderer: Error creating WebGL context»), rende solo `swangle`, che è 2,6 s per
fotogramma contro 0,6. Far pagare swangle a tutti i 2.220 fotogrammi costa un'ora e mezza invece di venti minuti: si
rende il film in egl, i soli tratti 3D in swangle, e si monta. I tagli cadono dove il 3D non c'è ancora, quindi le due
passate mostrano lo stesso identico contenuto al confine.

Stampa una riga per tratto: `primo ultimo` (compresi). Senza argomenti usa la scaletta del film.
"""
import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
TIMELINE = HERE / "remotion/src/film/timeline.json"
BLIND_CUT = 0.42          # deve restare uguale a src/film/ui/blinds.ts
MARGIN = 2                # un paio di fotogrammi di margine: il taglio non cade sul primo listello


def beat_to_frame(g: dict, beat: float) -> int:
    return round((g["offsetSeconds"] + beat * 60 / g["bpm"]) * g["fps"])


def ranges(t: dict) -> list[tuple[int, int]]:
    out: list[tuple[int, int]] = []
    scenes = t["scenes"]
    for i, s in enumerate(scenes):
        nxt = scenes[i + 1] if i + 1 < len(scenes) else None
        if "blinds" in s and nxt:
            frames = beat_to_frame(t, s["at"] + s["blinds"]["len"]) - beat_to_frame(t, s["at"])
            start = beat_to_frame(t, nxt["at"]) - round(frames * BLIND_CUT)
            out.append((max(0, start - MARGIN), start + frames + MARGIN))
    return sorted(out)


def total_frames(t: dict) -> int:
    last = t["scenes"][-1]
    return beat_to_frame(t, last["at"] + last["len"])


if __name__ == "__main__":
    files = [a for a in sys.argv[1:] if not a.startswith("--")]
    t = json.loads(pathlib.Path(files[0] if files else TIMELINE).read_text())
    if "--total" in sys.argv:
        print(total_frames(t))
    else:
        for a, b in ranges(t):
            print(a, min(b, total_frames(t) - 1))
