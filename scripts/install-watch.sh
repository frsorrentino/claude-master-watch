#!/usr/bin/env bash
# Installa l'APK dell'orologio via ADB WiFi.
# L'adb del SDK è x86_64 (macchina arm64): client e server girano sotto qemu;
# il server va lanciato esplicitamente perché l'auto-spawn fallisce (no binfmt).
#
# Uso:
#   ./scripts/install-watch.sh pair <IP:PORTA_PAIRING> <CODICE>   # solo la prima volta
#   ./scripts/install-watch.sh <IP:PORTA> [file.apk]              # connetti e installa (default: wear debug)
set -euo pipefail

Q=/usr/bin/qemu-x86_64-static
A="$HOME/android-sdk/platform-tools/adb"
adbq() { "$Q" "$A" "$@"; }

if ! adbq version >/dev/null 2>&1 || ! pgrep -f "adb server nodaemon" >/dev/null; then
    pkill -f "adb server nodaemon" 2>/dev/null || true
    nohup "$Q" "$A" server nodaemon >/tmp/adb-server.log 2>&1 &
    sleep 2
fi

if [ "${1:-}" = "pair" ]; then
    [ $# -eq 3 ] || { echo "uso: $0 pair IP:PORTA_PAIRING CODICE"; exit 1; }
    adbq pair "$2" "$3"
    echo "Pairing ok. Ora: $0 IP:PORTA_CONNESSIONE [file.apk]"
    exit 0
fi

[ $# -ge 1 ] || { echo "uso: $0 [pair IP:PORTA CODICE] | IP:PORTA [file.apk]"; exit 1; }
TARGET="$1"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
APK="${2:-$HERE/wear/build/outputs/apk/debug/wear-debug.apk}"
[ -f "$APK" ] || { echo "APK non trovato: $APK (prima: ./gradlew :wear:assembleDebug)"; exit 1; }

adbq connect "$TARGET"
adbq -s "$TARGET" install -r "$APK"
echo "Installato $APK su $TARGET"
