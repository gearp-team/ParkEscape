#!/usr/bin/env bash
# SPIKE-W1 측정(debug 빌드의 SpikeActivity가 설치돼 있어야 한다: ./gradlew installDebug).
#
#   spike_w1.sh signals     퍼즐 신호 8종 + 좌석 점유: capability·get·콜백 값·지연
#   spike_w1.sh data        signals와 같은 조작을 F2-data Repository 4종(DataCheckActivity)으로 확인
#   spike_w1.sh ambswitch   앰비언트 스위치 Off/On/Auto: getAmbientLightSwitch 값과 L1·L2 콜백
#   spike_w1.sh gate        정차↔공회전↔주행 전환: 앱 규제 콜백·화면 그리기·홈 차단 로그 시각
#   spike_w1.sh record      공회전 전환·정차 복귀 화면 녹화: 프레임마다 앱 영역 색(흐림 뒤 퍼즐 잔존 여부)
#   spike_w1.sh touch       차단 화면이 덮인 동안 앱 버튼 좌표 탭이 앱에 들어가는지
#   spike_w1.sh appswitch   차량 설정 앱으로 갔다 돌아올 때 규제 값(상시 리스너 A, STARTED Flow B)
#   spike_w1.sh window [split|join]  윈도 바를 끌어 세로 1단으로 나누기/되돌리기, 앱 창 크기(dp)와 TaskView 경계
#
# 결과는 _workspace/05_build/spike_out/<명령>_<시각>/(logcat.txt, steps.tsv, result.txt)에 남는다.
# 끝나면 VC를 정차로 되돌린다. 콜드 부팅 직후라면 먼저 emu.sh settle.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
DIR="$(cd "$(dirname "$0")" && pwd)"
python3 -I "$DIR/spike_w1.py" "$@"
