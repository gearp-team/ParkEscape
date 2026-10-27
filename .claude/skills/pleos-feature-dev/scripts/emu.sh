#!/usr/bin/env bash
# Pleos Connect 에뮬레이터 보조 스크립트.
#
#   emu.sh boot              AVD를 (꺼져 있으면) 띄우고, 부팅 완료와 SystemUI 안정화까지 기다린다
#   emu.sh settle            SystemUI 크래시 수가 20초 동안 늘지 않을 때까지 기다린다(최대 3분)
#   emu.sh install <apk>     APK를 설치한다
#   emu.sh launch [comp]     앱을 실행한다. 기본값은 app/build.gradle.kts의 applicationId + /.MainActivity
#   emu.sh shot <png>        스크린샷을 저장한다
#   emu.sh state             주행 상태와 그 상태의 UX 규제 설정을 출력한다
#   emu.sh logs              앱 프로세스의 로그를 출력한다
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
AVD="${PLEOS_AVD:-Pleos_Connect_v2.1}"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

app_id() {
  sed -n 's/.*applicationId *= *"\([^"]*\)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -1
}

booted() {
  [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]
}

crash_count() {
  "$ADB" logcat -d -b crash 2>/dev/null | grep -c 'Process: com.android.systemui' || true
}

cmd_settle() {
  # 콜드 부팅 직후 이미지 자체 버그로 SystemUI가 반복해서 죽고, 그때 실행한 앱은 'remove task'로 종료된다.
  local prev now waited=0
  prev=$(crash_count)
  while [ "$waited" -lt 180 ]; do
    sleep 20
    waited=$((waited + 20))
    now=$(crash_count)
    if [ "$now" = "$prev" ]; then
      echo "settled (systemui crashes so far: $now)"
      return 0
    fi
    echo "systemui still crashing ($prev -> $now), waiting..."
    prev=$now
  done
  echo "warning: systemui did not settle within 180s" >&2
  return 1
}

cmd_boot() {
  if ! "$ADB" devices | grep -q '^emulator-'; then
    local log="${TMPDIR:-/tmp}/pleos-emulator.log"
    nohup "$SDK/emulator/emulator" -avd "$AVD" >"$log" 2>&1 &
    echo "started $AVD (log: $log)"
  fi
  "$ADB" wait-for-device
  local waited=0
  until booted; do
    sleep 3
    waited=$((waited + 3))
    if [ "$waited" -ge 300 ]; then echo "error: boot timeout" >&2; exit 1; fi
  done
  echo "boot completed"
  cmd_settle
}

cmd_state() {
  local state name
  state=$("$ADB" shell dumpsys car_service --services CarDrivingStateService | sed -n 's/.*Current Driving State: *\(-\{0,1\}[0-9]*\).*/\1/p' | head -1)
  case "$state" in
    0) name=parked ;; 1) name=idling ;; 2) name=moving ;; *) name=unknown ;;
  esac
  echo "driving state: $state ($name)"
  # 상태별 설정 표에서 현재 상태의 블록을 고른다(첫 'unknown' 블록은 상태를 알 수 없을 때의 값).
  "$ADB" shell dumpsys car_service --services CarUxRestrictionsManagerService \
    | awk -v s="State:$name" 'index($0, s) { f = 1; next } f && /Requires DO|Restrictions:/ { print "  " $0; n++ } n == 2 { exit }'
}

case "${1:-}" in
  boot) cmd_boot ;;
  settle) cmd_settle ;;
  install)
    [ $# -ge 2 ] || { echo "usage: emu.sh install <apk>" >&2; exit 2; }
    "$ADB" install -r "$2" ;;
  launch)
    comp="${2:-$(app_id)/.MainActivity}"
    "$ADB" shell am start -W -n "$comp" | grep -E 'Status|TotalTime' ;;
  shot)
    [ $# -ge 2 ] || { echo "usage: emu.sh shot <png>" >&2; exit 2; }
    "$ADB" exec-out screencap -p >"$2" && echo "saved $2" ;;
  state) cmd_state ;;
  logs)
    pid=$("$ADB" shell pidof "$(app_id)" | tr -d '\r')
    [ -n "$pid" ] || { echo "error: $(app_id) is not running" >&2; exit 1; }
    "$ADB" logcat -d --pid="$pid" ;;
  *)
    sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'; exit 2 ;;
esac
