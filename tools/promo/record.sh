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

# ---- Storia «Il rilascio 2.8.0, dal polso» (piano 16/09): nove scene, una per registrazione. ----
# Scena della storia sull'app aperta, senza cambiare schermata.
step() { sh am start -n $ACT --es demo_step "$1" >/dev/null 2>&1; pause "${2:-1.5}"; }
# Scena che deve arrivare come notifica: si torna al quadrante e la scena scatta dopo, ad app in secondo piano (con l'app
# in primo piano le notifiche non partono, prova del 16/09 21:58).
alert() { sh am start -n $ACT --es demo_step "$1" --ei demo_delay_ms 2500 >/dev/null 2>&1; pause 0.4; home 0.1; pause "${2:-4.0}"; }
dictate() { sh am start -n $ACT --es demo_dictation "$1" >/dev/null 2>&1; pause 0.3; }
home() { sh input keyevent KEYCODE_HOME; pause "${1:-1.5}"; }

scene_0() { step calm; home 3.0; snap quadrante; }
scene_1() {   # un'occhiata: tile, poi lista
  sh input swipe 420 240 60 240 380; pause 2.5; snap tile
  tap 240 "${Y_TILE_SESSIONS:-420}"; pause 2.4; snap lista
  scroll 260 2400; snap lista_giu
}
# Inizio pulito della storia: l'app chiusa toglie le notifiche rimaste. Le nostre avvisano una volta sola, e una notifica
# vecchia della stessa sessione trasformava quella nuova in un aggiornamento muto (prova del 16/09 22:43).
fresh() { sh am force-stop $PKG; sh input keyevent KEYCODE_WAKEUP; sh am start -n $ACT >/dev/null 2>&1; pause 2.5; step calm 1.0; home 1.0; }
scene_2() {   # arriva la domanda: notifica, ascolto, risposta
  fresh; alert question; snap notifica
  # Toccare il testo della notifica non apre l'app: si scorre fino alle azioni e si tocca «Open».
  drag 1.0 400 160 600; drag 1.2 400 160 600; snap azioni
  tap 240 "${Y_OPEN:-334}"; pause 2.8; snap domanda
  tap "${X_PLAY:-350}" "${Y_PLAY:-124}"; pause 3.0; snap ascolto
  hold 240 "${Y_YES:-440}"; pause 3.0; snap risposta
}
scene_3() {   # la seguo
  # Sulla card della Scheda e non sulla riga della lista: la lista scorre e la riga sotto il dito cambia (prova 17/09 01:45).
  go session/payments-api; pause 2.4; snap scheda
  hold 240 "${Y_CARD:-260}" 900; pause 2.5; snap seguita
}
scene_4() {   # il deploy è fatto
  home 1.0; alert deployed; snap esito_notifica
  drag 1.0 400 160 600; drag 1.2 400 160 600; snap esito_azioni
  tap 240 "${Y_OPEN_OUTCOME:-334}"; pause 2.8; snap scheda
  tap "${X_PLAY:-400}" "${Y_PLAY_CARD:-150}"; pause 1.5; snap ascolto
  scroll 300 2600; scroll 300 2400; snap scheda_giu
}
scene_5() {   # il passo dopo, a voce; poi il terminale dal vivo
  dictate "Great. Now update the changelog and tag the release"
  scroll "${S5:-1200}" 2000 1.0; snap fondo
  tap 240 "${Y_WRITE:-350}"; pause 2.5; snap inviato
  step followup 1.0; go terminal/payments-api; pause 9.0; snap terminale
}
scene_6() {   # quanta quota resta
  go quota; pause 2.4; snap panoramica; scroll 300 2600; snap ritmo; scroll 360 2400; snap lavoro
}
scene_7() {   # il post sul blog
  dictate "Draft a post about the 2.8.0 release"
  go launch; pause 2.2; snap progetti
  tap 240 "${Y_PROJECT_BLOG:-290}"; pause 1.5; snap progetto
  tap 240 "${Y_WRITE_FIRST:-300}"; pause 3.5; snap sessione_nata
}
scene_8() { home 3.0; snap chiusura; }

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
    if declare -F "scene_$CLIP" >/dev/null; then "scene_$CLIP"; else "clip_$CLIP"; fi
    pause 1.0
    sh pkill -INT screenrecord; wait $rec 2>/dev/null; pause 1.5
    timeout 120 "$A" -s "$D" pull /sdcard/promo_$CLIP.mp4 "$OUT/$CLIP.mp4" >/dev/null && sh rm -f /sdcard/promo_$CLIP.mp4
    echo "registrata: $OUT/$CLIP.mp4"
  else
    if declare -F "scene_$CLIP" >/dev/null; then "scene_$CLIP"; else "clip_$CLIP"; fi; [ "$CLIP" = measure ] || echo "prova $CLIP: $N screenshot"
  fi
}

case "$MODE" in
  setup) setup ;;
  teardown) teardown ;;
  probe|record) for c in "${@:-session overview new question}"; do for x in $c; do run "$x"; done; done ;;
esac
