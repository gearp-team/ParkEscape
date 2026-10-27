---
name: pleos-qa-check
description: "PleosApp 기능·제출물 QA 절차와 점검 스크립트. 빌드·테스트·lint, 매니페스트 퍼미션↔실제 SDK 호출↔README 표 대조(check_permissions.py), Repository 인터페이스↔Pleos 구현↔Fake 짝 맞춤, 정차·공회전·주행 상태와 1단/2단 화면 확인, 공모전 제출 규격(APK 이름·서명·README·임시 식별자·비밀값·실명) 점검(check_submission.sh), 통과/실패/미검증 보고서 형식을 정한다. pleos-qa가 기능마다, 그리고 제출 전에 읽는다. 사용자가 이 앱의 퍼미션·빌드·주행 상태 화면·제출 규격만 따로 검증해 달라고 하면 이 스킬을 직접 쓴다. 아이디어부터 제출까지 이어지는 하네스 흐름 안의 점검은 pleos-app-studio가 이 스킬을 호출한다."
---

# PleosApp QA

QA의 목적은 "파일이 있다"가 아니라 "서로 맞물린다"를 확인하는 것이다. 이 앱에서 가장 자주 어긋나는 연결 지점은 네 곳이다.

| 연결 지점 | 한쪽 | 다른 쪽 | 어긋나면 |
|---|---|---|---|
| 퍼미션 | 코드가 호출하는 SDK API의 `@RequiresPermission` | `AndroidManifest.xml`, README 표 | 실행 시 SecurityException, 또는 미승인 퍼미션으로 마켓 심사 반려 |
| 데이터 계층 | Repository 인터페이스 | Pleos 구현과 Fake 구현 | Fake 모드에서만 되거나 실제 SDK에서만 되는 기능 |
| 주행 규제 | `DrivingRestrictions` 비트 | 화면 분기 | 주행 중 금지 UI 노출, 또는 정차 중에도 기능이 숨음 |
| 명세 | `docs/app-plan.md`의 완료 조건 | 실제 동작 | 시연 시나리오 실패 |

스크립트는 저장소 루트에서 실행한다.

## 기능 QA 순서

기능 하나가 끝날 때마다 아래 순서로 확인한다. 앞 단계가 실패하면 뒤 단계 결과를 믿을 수 없으므로 실패를 먼저 보고한다.

1. **빌드·테스트·lint**
   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```
   lint 결과는 `app/build/reports/lint-results-debug.xml`에서 새 경고 수를 센다.
2. **퍼미션 대조**
   ```bash
   python3 .claude/skills/pleos-qa-check/scripts/check_permissions.py
   ```
   FAIL은 그대로 실패 항목이다. WARN(같은 이름의 메서드가 여러 SDK에 있음)은 호출 위치를 열어 어느 SDK인지 확인한다.
3. **데이터 계층 짝 맞춤** — 새 Repository 인터페이스마다 `Pleos…`/`Car…` 구현과 `Fake…` 구현이 모두 있고 `AppContainer`가 둘 다 연결하는지 읽어서 확인한다. Fake 모드 빌드도 확인한다.
   ```bash
   ./gradlew assembleDebug -Ppleos.useFakeSdk=true
   ```
4. **Pleos 구현 읽기** — 리스너 해제가 `awaitClose`에서 등록 때와 같은 listener·area로 되는지, capability 미지원 경로가 있는지, `VehicleException`을 쓰지 않는지 확인한다.
5. **주행 규제 분기 읽기** — 화면이 규제 값을 하드코딩하지 않고 비트별 속성으로 분기하는지, 주행 중에 남는 요소가 명세의 "주행/정차 동작"과 같은지 확인한다.
6. **에뮬레이터 확인** — `pleos-feature-dev`의 스크립트로 상태를 바꾸며 스크린샷을 남긴다.
   ```bash
   S=.claude/skills/pleos-feature-dev/scripts
   $S/emu.sh boot && ./gradlew installDebug && $S/emu.sh launch
   $S/vc.sh park  && $S/emu.sh shot _workspace/05_build/shots/{F}_park.png
   $S/vc.sh idle  && $S/emu.sh state && $S/emu.sh shot _workspace/05_build/shots/{F}_idle.png
   $S/vc.sh drive 40 && $S/emu.sh state && $S/emu.sh shot _workspace/05_build/shots/{F}_moving.png
   $S/vc.sh park
   ```
   명세의 시연 시나리오에 나오는 신호는 `vc.sh set <feature-id> <value> [area]`로 바꿔 화면 변화를 확인한다. 스크린샷은 Read로 열어 눈으로 확인한 내용을 보고서에 쓴다. 찍기만 하고 보지 않으면 확인한 것이 아니다.
7. **완료 조건 대조** — 명세의 완료 조건 문장마다 통과·실패·미검증을 판정한다.

## 제출 점검 순서

```bash
./gradlew clean lintDebug testDebugUnitTest copyReleaseApk
.claude/skills/pleos-qa-check/scripts/check_submission.sh
```

`check_submission.sh`는 APK 파일명·크기·서명·패키지·debuggable 여부, README 필수 항목, 임시 식별자, git에 들어간 비밀값, `.gitignore`를 자동으로 본다. `REVIEW`로 표시된 항목(커밋 작성자, 문서 속 실명)은 사람이 확인할 일이므로 보고서에 목록을 그대로 옮기고 리더에게 확인을 요청한다. 공모전 제출 규격의 근거는 키트 `CLAUDE.md` 1.2절과 `contest/CONTEST_SOURCE.md`의 GitHub 저장소 제출 안내다.

## 보고서 형식

기능 QA는 `_workspace/05_build/05_qa_{기능 id}.md`, 제출 점검은 `_workspace/06_submission/06_qa_submission.md`에 쓴다.

```markdown
# QA: F1 뒷좌석 점유 경고 — FAIL (통과 9 / 실패 2 / 미검증 1)

## 실패
| # | 항목 | 위치 | 재현 | 고칠 방향 |
|---|---|---|---|---|
| 1 | 매니페스트에 CAR_SEATS 누락 | app/src/main/AndroidManifest.xml | check_permissions.py FAIL | `<uses-permission android:name="pleos.car.permission.CAR_SEATS" />` 추가, README 표 갱신 |

## 미검증
| # | 항목 | 이유 |
|---|---|---|
| 1 | 1단(portrait) 배치 | 창 분할을 재현하지 못함 |

## 통과
- 빌드·테스트·lint(새 경고 0)
- …

## 스크린샷
- _workspace/05_build/shots/F1_park.png — 정차: 경고 카드 표시
```

맨 윗줄 판정은 실패가 0개면 `PASS`, 1개 이상이면 `FAIL`이다. 미검증이 있으면 PASS여도 개수를 밝힌다.

## 수정 요청 형식

실패 항목은 `pleos-android-dev`에게 바로 보낸다. 한 메시지에 실패 항목 전부를 담고, 항목마다 `파일:줄`, 무엇이 틀렸는지, 고칠 방향을 쓴다. 고친 코드를 직접 쓰지는 않는다.
