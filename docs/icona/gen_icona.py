"""Icona dell'app «>_ nel quadrante» (scelta da Franz il 17-18/09/2026: variante A5, simboli al 72 %, senza disco di fondo).
Un'unica geometria, la stessa di tools/promo/remotion/src/film/LogoMark.tsx, per i quattro vettoriali Android e per l'SVG:
  python3 docs/icona/gen_icona.py        riscrive wear/src/main/res/drawable/{ic_launcher_fg,ic_launcher_mono,ic_app_mono,ic_tile}.xml
Quadrante: arco aperto in basso (130°→410°, senso orario dalle ore 3), riempito al 70 %, traccia più scura dietro."""
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "wear/src/main/res/drawable"
CORAL, TRACK = "#FFD97757", "#FF3A404C"

def mark(cx, cy, r_out, fg, track):
    """Percorsi del segno con raggio esterno del quadrante r_out. Restituisce una lista di <path> (stringhe)."""
    u = r_out / 43; g = 0.72 * u; w = 7.2 * g; yc = cy - 2 * u; r = 40.2 * u; aw = 5.6 * u
    def pt(deg): return cx + r * math.cos(math.radians(deg)), cy + r * math.sin(math.radians(deg))
    def arc(a0, a1, color):
        (x0, y0), (x1, y1) = pt(a0), pt(a1); large = 1 if (a1 - a0) > 180 else 0
        return (f'<path android:strokeColor="{color}" android:strokeWidth="{aw:.3f}" android:strokeLineCap="round" '
                f'android:pathData="M{x0:.3f},{y0:.3f} A{r:.3f},{r:.3f} 0 {large} 1 {x1:.3f},{y1:.3f}" />')
    out = []
    if track: out.append(arc(130, 410, track))
    out.append(arc(130, 130 + 280 * 0.7, fg))
    out.append(f'<path android:strokeColor="{fg}" android:strokeWidth="{w:.3f}" android:strokeLineCap="round" android:strokeLineJoin="round" '
               f'android:pathData="M{cx - 22 * g:.3f},{yc - 17 * g:.3f} L{cx - 3 * g:.3f},{yc:.3f} L{cx - 22 * g:.3f},{yc + 17 * g:.3f}" />')
    y = yc + 13.4 * g + w / 2
    out.append(f'<path android:strokeColor="{fg}" android:strokeWidth="{w:.3f}" android:strokeLineCap="round" '
               f'android:pathData="M{cx + 2 * g + w / 2:.3f},{y:.3f} L{cx + 23 * g - w / 2:.3f},{y:.3f}" />')
    return out

def vector(name, comment, size_dp, viewport, paths):
    body = "\n".join("    " + p for p in paths)
    (RES / name).write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- {comment}
     Generato da docs/icona/gen_icona.py: non modificare a mano. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size_dp}dp" android:height="{size_dp}dp"
    android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">
{body}
</vector>
''')

if __name__ == "__main__":
    vector("ic_launcher_fg.xml", "Primo piano dell'icona adattiva: «>_» corallo nel quadrante, su sfondo nero (@color/ic_bg). Il segno sta nella zona sicura (66 dp su 108).",
           108, 108, mark(54, 54, 30, CORAL, TRACK))
    vector("ic_launcher_mono.xml", "Livello monocromatico dell'icona adattiva (icone a tema): senza traccia, perché la tinta unica chiuderebbe il quadrante.",
           108, 108, mark(54, 54, 30, "#FFFFFFFF", None))
    vector("ic_app_mono.xml", "Glifo monocromo dell'app: notifiche, complication, badge senza icona propria. La traccia è al 35 % di opacità.",
           24, 48, mark(24, 24, 21, "#FFFFFFFF", "#59FFFFFF"))
    vector("ic_tile.xml", "Icona della tile: segno blu notte in un cerchio del colore base, come le tile di sistema (Franz, 13/09 17:38).",
           48, 48, ['<path android:fillColor="#FFD3E3FD" android:pathData="M24,0 A24,24 0 1 1 23.9,0 Z" />'] + mark(24, 24, 16.5, "#FF0A2050", "#400A2050"))
    print("scritti:", *sorted(p.name for p in RES.glob("ic_launcher_*.xml")), "ic_app_mono.xml ic_tile.xml")
