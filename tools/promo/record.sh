#!/usr/bin/env bash
# Copione dei video promozionali (Franz, 16/09 16:05): casi d'uso reali sull'orologio, in demo e in inglese, senza mani.
#   WATCH=IP:PORTA tools/promo/record.sh probe [clip…]   uno screenshot dopo ogni passo, per ricavare le coordinate
#   WATCH=IP:PORTA tools/promo/record.sh record [clip…]  registra le clip con screenrecord e le scarica in OUT
# Prima: demo accesa e inglese solo per l'app (lo fa `setup`), dopo: `teardown` rimette dati veri, lingua e schermo.
set -uo pipefail
MODE=${1:?probe|record|setup|teardown}; shift || true
A=${ADB:-/usr/bin/adb}; D=${WATCH:?serve WATCH=IP:PORTA}
OUT=${OUT:-$(cd "$(dirname "$0")" && pwd)/out/clips}; mkdir -p "$OUT"
PKG=it.pixelbox.cmwatch; ACT=$PKG/.wear.MainActivity
sh() { timeout 30 "$A" -s "$D" shell "$@"; }
pause() { sleep "$1"; }
# La corona: uno scatto alla volta con un ritmo da dito vero, non uno scatto secco.
crown() { local n=$1 dir=$2 gap=${3:-0.16}; for _ in $(seq "$n"); do sh input rotaryencoder scroll --axis "SCROLL,$dir"; sleep "$gap"; done; }
tap() { sh input tap "$1" "$2"; }
back() { sh input keyevent KEYCODE_BACK; }
N=0; CLIP=""
snap() { [ "$MODE" = probe ] || return 0; N=$((N + 1)); timeout 20 "$A" -s "$D" exec-out screencap -p > "$OUT/probe_${CLIP}_$(printf %02d $N)_$1.png"; }

open_list() {   # app a freddo: in demo c'è una domanda aperta e l'app si apre su di lei; un «indietro» porta alla lista
  sh am force-stop $PKG; sh input keyevent KEYCODE_WAKEUP
  sh am start -n $ACT >/dev/null; pause 2.5
  back; pause 1.6
}
# Verso una schermata con il collegamento interno dell'app: parte con la stessa animazione di un tocco e arriva sempre nel
# posto giusto. Con i tocchi a coordinate la lista non si fermava due volte uguale e si finiva sulla sessione sbagliata.
go() { sh am start -n $ACT --es cmwatch_uri "cmwatch://$1" >/dev/null 2>&1; }
hold() { sh input swipe "$1" "$2" "$1" "$2" "${3:-1100}"; }   # pressione lunga, per confermare una risposta

clip_question() {   # una domanda arriva: si legge, si scorre alle opzioni, si conferma «yes» tenendo premuto
  sh am force-stop $PKG; sh input keyevent KEYCODE_WAKEUP
  go question/ledger-api; pause 3.2; snap domanda
  hold 240 "${Y_YES:-300}"; pause 3.2; snap risposta
}

clip_session() {   # dalla lista alla Scheda di una sessione al lavoro: testo, quota, sessione, poi il terminale
  open_list; snap lista; pause 1.5
  go session/atlas-shop; pause 2.4; snap scheda
  crown 4 -1 0.22; pause 1.8; snap scheda_quota
  crown 5 -1 0.22; pause 2.0; snap scheda_sessione
  go terminal/atlas-shop; pause 2.6; snap terminale
  crown 7 -1 0.25; pause 2.2; snap terminale_giu
}

clip_overview() {   # la Panoramica scorsa con calma: quota, ritmo, lavoro, contesto, oggi
  open_list; pause 1.0
  go quota; pause 2.6; snap panoramica
  for tappa in ritmo adesso domande contesto oggi; do crown 4 -1 0.22; pause 1.7; snap "pan_$tappa"; done
}

clip_new() {   # una nuova sessione su un progetto
  open_list; pause 1.0
  go launch; pause 2.4; snap progetti
  tap 240 "${Y_PROJECT:-190}"; pause 1.6; snap progetto_scelto
  tap 240 "${Y_START:-350}"; pause 3.0; snap lanciata
}

setup() {
  # Il valore originale si salva una volta sola: rilanciando setup si salverebbe quello lungo messo qui.
  [ -s "$OUT/.timeout_prima" ] || sh settings get system screen_off_timeout > "$OUT/.timeout_prima"
  sh settings put system screen_off_timeout 1800000
  sh svc power stayon true
  sh cmd locale set-app-locales $PKG --locales en-US
  sh am start -n $ACT --ez demo true >/dev/null; pause 3
  echo "demo accesa, app in inglese, schermo sempre acceso"
}

teardown() {
  sh am start -n $ACT --ez demo false >/dev/null; pause 2
  sh cmd locale set-app-locales $PKG --locales ""
  sh svc power stayon false
  sh settings put system screen_off_timeout "$(cat "$OUT/.timeout_prima" 2>/dev/null || echo 15000)"
  echo "dati veri, lingua di sistema, schermo normale"
}

run() {
  CLIP=$1; N=0
  if [ "$MODE" = record ]; then
    sh rm -f /sdcard/promo_$CLIP.mp4
    # screenrecord in background sull'orologio; il copione recita mentre registra, poi si ferma con SIGINT.
    timeout 200 "$A" -s "$D" shell screenrecord --bit-rate 8000000 /sdcard/promo_$CLIP.mp4 & rec=$!
    pause 1.5
    "clip_$CLIP"
    pause 1.0
    sh pkill -INT screenrecord; wait $rec 2>/dev/null; pause 1.5
    timeout 120 "$A" -s "$D" pull /sdcard/promo_$CLIP.mp4 "$OUT/$CLIP.mp4" >/dev/null && sh rm -f /sdcard/promo_$CLIP.mp4
    echo "registrata: $OUT/$CLIP.mp4"
  else
    "clip_$CLIP"; echo "prova $CLIP: $N screenshot"
  fi
}

case "$MODE" in
  setup) setup ;;
  teardown) teardown ;;
  probe|record) for c in "${@:-session overview new question}"; do for x in $c; do run "$x"; done; done ;;
esac
