#!/usr/bin/env bash
# Pleos Connect Virtual Console 조작 스크립트.
# 콘솔은 브라우저 세션 heartbeat(POST /browser/session, 1초 간격)가 있는 동안에만 쓰기를 받는다.
# 없으면 {"error":"emulator_inactive"}를 돌려준다. 보낸 상태는 heartbeat가 끝나도 유지된다.
#
#   vc.sh park                         기어 P, 속도 0 (정차: 규제 0x0)
#   vc.sh idle                         기어 D, 속도 0 (공회전: requiresDO, 규제 0x50)
#   vc.sh drive [kph]                  기어 D, 속도(기본 40) (주행: requiresDO, 규제 0x50)
#   vc.sh set <feature-id> <value> [area]
#                                      카탈로그 기능 하나를 바꾼다. 예: vc.sh set seat-occupancy true rear_left
#   vc.sh features                     카탈로그 기능 id·값 범위·area 목록
#   vc.sh put '<path?query>'           임의 PUT. 예: vc.sh put '/vehicle/controls?speed=20&accelerator=0&brake=0&steering=0'
#   vc.sh get <path>                   GET. 예: vc.sh get /vehicle/state
#   vc.sh hold | release               세션을 백그라운드로 계속 살려 둔다 / 멈춘다. 새 세션이 활성화될 때마다 콘솔이
#                                      앰비언트 색을 #ffffff로 다시 발행하므로(F2-ui 리허설 실측: activation-dds-published),
#                                      앰비언트 색을 유지해야 하는 시연·리허설은 hold로 시작하고 release로 끝낸다.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
PORT="${VC_PORT:-8088}"
BASE="http://127.0.0.1:$PORT"
HB=""

"$ADB" forward "tcp:$PORT" tcp:8088 >/dev/null

hb_stop() {
  if [ -n "$HB" ]; then kill "$HB" 2>/dev/null || true; HB=""; fi
  curl -s -X POST "$BASE/browser/session?client=vc-sh&closing=1" >/dev/null || true
}

hb_start() {
  ( while true; do curl -s -X POST "$BASE/browser/session?client=vc-sh" >/dev/null || true; sleep 1; done ) &
  HB=$!
  disown "$HB"
  trap hb_stop EXIT
  sleep 1.5
}

put() {
  local out
  out=$(curl -s -X PUT "$BASE$1")
  if printf '%s' "$out" | grep -q '"error"'; then
    echo "error: PUT $1 -> $out" >&2
    return 1
  fi
}

controls() { put "/vehicle/controls?speed=$1&accelerator=$2&brake=0&steering=0"; }

case "${1:-}" in
  park)
    hb_start; controls 0 0; put "/vehicle/gear?gear=Park" ;;
  idle)
    hb_start; controls 0 0; put "/vehicle/gear?gear=Drive" ;;
  drive)
    hb_start; put "/vehicle/gear?gear=Drive"; controls "${2:-40}" 20 ;;
  set)
    [ $# -ge 3 ] || { echo "usage: vc.sh set <feature-id> <value> [area]" >&2; exit 2; }
    query=$(curl -s "$BASE/vehicle/catalog" | python3 -I -c '
import json, sys, urllib.parse
fid, value, area = sys.argv[1], sys.argv[2], (sys.argv[3] if len(sys.argv) > 3 else None)
feature = next((f for f in json.load(sys.stdin)["features"] if f["id"] == fid), None)
if feature is None:
    sys.exit(f"unknown feature id: {fid} (see: vc.sh features)")
control = feature["control"]
params = {k: str(v) for k, v in (control.get("params") or {}).items()}
field_by_area = control.get("field_by_area") or {}
field = field_by_area.get(str(area)) or control.get("field")
if field is None:
    areas = ", ".join(field_by_area)
    example = next(iter(field_by_area), "<area>")
    sys.exit(f"{fid} needs an area: {areas} (e.g. vc.sh set {fid} {value} {example})")
params[field] = value
if control.get("area_field") and area is not None:
    params[control["area_field"]] = area
print(feature["write_endpoint"] + "?" + urllib.parse.urlencode(params))
' "$2" "$3" "${4:-}")
    hb_start; put "$query"; echo "PUT $query" ;;
  features)
    curl -s "$BASE/vehicle/catalog" | python3 -I -c '
import json, sys
for f in json.load(sys.stdin)["features"]:
    c = f.get("control", {})
    if c.get("kind") == "enum":
        rng = "|".join(str(o["value"]) for o in c.get("options", []))
    elif c.get("kind") == "range":
        rng = str(c.get("min")) + ".." + str(c.get("max")) + f.get("unit", "")
    else:
        rng = c.get("kind", "")
    areas = ",".join(str(a["value"]) for a in f.get("areas", []))
    print(f["id"].ljust(32), rng.ljust(40), ("areas=" + areas) if areas else "")
' ;;
  put)
    [ $# -ge 2 ] || { echo "usage: vc.sh put '<path?query>'" >&2; exit 2; }
    hb_start; put "$2" ;;
  get)
    curl -s "$BASE${2:-/vehicle/state}"; echo ;;
  hold)
    pidfile="${TMPDIR:-/tmp}/pleos-vc-hold.pid"
    if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then echo "already holding (pid $(cat "$pidfile"))"; exit 0; fi
    nohup bash -c "while true; do curl -s -X POST '$BASE/browser/session?client=vc-hold' >/dev/null || true; sleep 1; done" >/dev/null 2>&1 &
    echo $! >"$pidfile"; sleep 1.5; echo "holding session (pid $!)" ;;
  release)
    pidfile="${TMPDIR:-/tmp}/pleos-vc-hold.pid"
    [ -f "$pidfile" ] && kill "$(cat "$pidfile")" 2>/dev/null || true
    rm -f "$pidfile"
    curl -s -X POST "$BASE/browser/session?client=vc-hold&closing=1" >/dev/null || true
    echo "released" ;;
  *)
    sed -n '2,19p' "$0" | sed 's/^# \{0,1\}//'; exit 2 ;;
esac
