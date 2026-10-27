#!/usr/bin/env bash
# 화면 글자로 누르기(uiautomator 접근성 트리). Compose 화면의 Text·contentDescription을 찾는다.
#
#   ui.sh tap "<글자>"        글자(또는 설명)가 들어 있는 첫 노드의 가운데를 누른다
#   ui.sh find "<글자>"       찾은 노드의 bounds·enabled를 출력한다(누르지 않음)
#   ui.sh texts               화면의 글자 목록
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
cmd="${1:-}"; needle="${2:-}"
dump=$("$ADB" exec-out uiautomator dump /dev/tty 2>/dev/null | sed 's/UI hierchary dumped to.*//')
python3 -I - "$cmd" "$needle" "$ADB" <<PY
import re, subprocess, sys, xml.etree.ElementTree as ET
cmd, needle, adb = sys.argv[1], sys.argv[2], sys.argv[3]
root = ET.fromstring('''$dump''')
nodes = list(root.iter('node'))
if cmd == 'texts':
    for n in nodes:
        t = n.get('text') or n.get('content-desc')
        if t: print(t, n.get('bounds'), 'enabled=' + n.get('enabled'))
    sys.exit(0)
hits = [n for n in nodes if needle and (needle in (n.get('text') or '') or needle in (n.get('content-desc') or ''))]
if not hits:
    sys.exit(f'not found: {needle}')
parent = {c: p for p in root.iter() for c in p}
n = hits[0]
# 글자 노드를 감싼 클릭 가능한 노드(버튼)의 상태를 본다
target = n
while target is not None and target.get('clickable') != 'true':
    target = parent.get(target)
target = target if target is not None else n
x1, y1, x2, y2 = map(int, re.findall(r'\d+', target.get('bounds')))
print(f"{needle} bounds={target.get('bounds')} enabled={target.get('enabled')} clickable={target.get('clickable')}")
if cmd == 'tap':
    subprocess.run([adb, 'shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2)], check=True)
PY
