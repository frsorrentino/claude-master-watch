#!/usr/bin/env bash
# I tre film di riferimento del piano 3 e i sei fotogrammi «metro», uno per momento forte (§7 del piano). Tutto finisce in
# materiali/riferimenti/ (ignorata dal repo: materiale di Google e Zelios, solo per il confronto a vista). Serve yt-dlp in un venv.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd); R=$here/materiali/riferimenti; mkdir -p "$R"; cd "$R"
venv=${YTENV:-$R/.ytenv}
[ -x "$venv/bin/yt-dlp" ] || { python3 -m venv "$venv" && "$venv/bin/pip" -q install yt-dlp; }
declare -A ID=([canvas]=4Leardp_AGc [langease]=SgmuplXU2iY [ask]=sxUBThVQLjU)
for n in "${!ID[@]}"; do [ -f "$n.mp4" ] || nice -n 15 "$venv/bin/yt-dlp" -q --no-warnings -f "bv*[height<=720][ext=mp4]/b[height<=720]" -o "$n.%(ext)s" "https://www.youtube.com/watch?v=${ID[$n]}"; done
# tavole a 2 fotogrammi al secondo, 10 per riga
for n in canvas langease ask; do
  [ -f "$n-tavola.png" ] && continue
  mkdir -p "$n"; ffmpeg -v error -y -i "$n.mp4" -vf "fps=2,scale=320:-1" "$n/f%03d.png"
  python3 - "$n" <<'PY'
import sys, glob; from PIL import Image
n=sys.argv[1]; fs=sorted(glob.glob(f'{n}/f*.png')); ims=[Image.open(f) for f in fs]; w,h=ims[0].size; cols=10; rows=(len(ims)+cols-1)//cols
W=Image.new('RGB',(cols*w,rows*h)); [W.paste(im,((i%cols)*w,(i//cols)*h)) for i,im in enumerate(ims)]; W.save(f'{n}-tavola.png')
PY
done
# i sei fotogrammi metro (piano 3 §7)
mkdir -p metro
while read -r name file t; do ffmpeg -nostdin -v error -y -ss "$t" -i "$file.mp4" -frames:v 1 "metro/$name.png"; done <<'LIST'
1-card-esce      canvas   31.5
1b-tasto-isolato canvas   15.0
2-tasto-contorno ask      13.5
2b-riquadro-contorno ask  78.0
3-onda-voce      ask      77.5
4-piano-inclinato canvas  36.0
5-gauge-contatore langease 11.5
6-frustata       ask      4.5
LIST
ls metro
