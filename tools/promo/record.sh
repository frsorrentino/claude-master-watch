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
# Scorrimento guidato: la lista dell'app scorre di PX pixel in MS millisecondi, in un solo movimento con partenza e arrivo
# morbidi (solo con la demo accesa). È il modo per avere un movimento continuo fino all'elemento da mostrare (18:08).
scroll() { sh am start -n $ACT --ei scroll_px "$1" --ei scroll_ms "${2:-2400}" >/dev/null 2>&1; pause "$(python3 -c "print(${2:-2400}/1000 + ${3:-1.8})")"; }
# Scorrimento con il dito, lento: il contenuto segue in modo continuo. La corona simulata manda scatti singoli e l'app
# aggancia lo scorrimento a ogni scatto: in video diventava una sequenza di salti (Franz, 16/09 18:07).
drag() { sh input swipe 240 "${2:-360}" 240 "${3:-160}" "${4:-950}"; pause "${1:-1.2}"; }
drags() { local n=$1 p=${2:-1.2}; for _ in $(seq "$n"); do drag "$p"; done; }
# Quanti trascinamenti servono per arrivare in fondo: si trascina finché lo schermo non cambia più. Si registra poi con quel
# numero esatto, così la clip non arriva mai in fondo a vuoto a «provare» a scorrere.
measure() {
  local label=$1 prev="" cur n=0
  prev=$(timeout 20 "$A" -s "$D" exec-out screencap -p | md5sum)
  while [ $n -lt 30 ]; do
    drag 1.4
    cur=$(timeout 20 "$A" -s "$D" exec-out screencap -p | md5sum)
    [ "$cur" = "$prev" ] && break
    prev=$cur; n=$((n + 1))
  done
  echo "$label: $n trascinamenti"
}

clip_question() {   # una domanda arriva: si legge e si conferma «yes» tenendo premuto
  sh am force-stop $PKG; sh input keyevent KEYCODE_WAKEUP
  go question/payments-api; pause 3.2; snap domanda
  # Con la domanda lunga «yes» sta in fondo, visibile per metà: la pressione lunga va lì.
  hold 240 "${Y_YES:-440}"; pause 3.2; snap risposta
}

clip_session() {   # dalla lista alla Scheda: testo, quota, sessione; poi il terminale
  open_list; snap lista; pause 1.6
  go session/storefront; pause 2.4; snap scheda
  scroll "${S1:-300}" 2400; snap scheda_quota
  scroll "${S2:-300}" 2200; snap scheda_sessione
  go terminal/storefront; pause 2.6; snap terminale
  scroll "${S3:-420}" 2800; snap terminale_giu
}

clip_overview() {   # la Panoramica: un movimento per gruppo, con una pausa per leggerlo
  open_list; pause 1.0
  go quota; pause 2.6; snap panoramica
  scroll "${O1:-380}" 2600; snap pan_ritmo
  scroll "${O2:-420}" 2600; snap pan_lavoro
  scroll "${O3:-420}" 2600; snap pan_contesto_oggi
  scroll "${O4:-400}" 2400; snap pan_fondo
}

clip_new() {   # una nuova sessione su un progetto
  open_list; pause 1.0
  go launch; pause 2.4; snap progetti
  tap 240 "${Y_PROJECT:-190}"; pause 1.6; snap progetto_scelto
  tap 240 "${Y_START:-410}"; pause 3.0; snap lanciata
}

clip_measure() {   # non è una clip: misura i trascinamenti di ogni schermata che scorre
  open_list; go session/storefront; pause 2.4; measure N_SESSION
  go terminal/storefront; pause 2.6; measure N_TERMINAL
  go quota; pause 2.6; measure N_OVERVIEW
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
    "clip_$CLIP"; [ "$CLIP" = measure ] || echo "prova $CLIP: $N screenshot"
  fi
}

case "$MODE" in
  setup) setup ;;
  teardown) teardown ;;
  probe|record) for c in "${@:-session overview new question}"; do for x in $c; do run "$x"; done; done ;;
esac
