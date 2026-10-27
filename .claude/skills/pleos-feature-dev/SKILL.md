---
name: pleos-feature-dev
description: "PleosApp(Kotlin·Compose, Pleos Connect AAOS) 코드베이스에 기능을 추가·수정하는 절차와 에뮬레이터 검증 도구. SDK API 확인, Repository 인터페이스 → Pleos 구현 → Fake → AppContainer → ViewModel → 화면 순서, 주행 규제·1단/2단 처리, 매니페스트·README 퍼미션 갱신, 빌드·테스트, Virtual Console로 정차·공회전·주행 상태를 바꿔 확인하는 스크립트(vc.sh, emu.sh)를 담는다. pleos-android-dev가 구현할 때와 pleos-qa가 에뮬레이터를 조작할 때 읽는다. 사용자가 이 앱에 기능을 넣거나 고쳐 달라고 하면(차량 신호 표시, NaviHelper 연동, 주행 중 화면 등) 이 스킬을 쓴다."
---

# PleosApp 기능 구현

이 저장소의 구조·명령·함정은 프로젝트 `CLAUDE.md`에 있다. 이 스킬은 그 규칙을 기능 하나에 적용하는 순서다. 순서를 지키는 이유는 하나다. SDK 호출을 Repository 뒤에 숨기고 Fake를 함께 만들어 두면, 에뮬레이터나 SDK가 시연 당일에 실패해도 `pleos.useFakeSdk=true` 한 줄로 앱을 살릴 수 있다.

## 구현 순서

1. **명세를 읽는다.** `docs/app-plan.md`에서 기능 id의 행(데이터 소스, 주행/정차 동작, 실패 시 동작, Fake 데이터, 완료 조건)을 확인한다.
2. **API를 확인한다.** 쓰려는 메서드를 찾아 시그니처·퍼미션·콜백 형태를 확인한다.
   ```bash
   grep -n "fun registerSeatOccupancy" pleos-claude-kit/API_INDEX.md
   grep -rn "fun registerSeatOccupancy" pleos-claude-kit/sdk-artifacts/sources/
   ```
   둘 다 없으면 구현하지 말고 리더에게 보고한다. 리스너 인터페이스의 메서드 이름도 `API_INDEX.md`의 `### …-listener` 절에서 확인한다.
3. **capability를 확인하는 흐름을 넣는다.** 지원 여부와 area는 차량·에뮬레이터마다 다르다. `check…Capabilities`로 `accessType`과 area 목록을 받은 뒤, 지원하는 area로만 조회·등록한다. 미지원이면 UI에서 그 기능을 숨기거나 "지원하지 않는 차량" 상태를 보여 준다.
4. **Repository 인터페이스를 만든다.** `data/<영역>/<이름>Repository.kt`. 노출은 `Flow`/`StateFlow`로 하고 SDK 타입이 밖으로 새지 않게 앱 모델(data class)로 바꾼다.
5. **Pleos 구현을 만든다.** `Pleos<이름>Repository`. 리스너는 `callbackFlow`로 감싸고 `awaitClose`에서 등록 때와 같은 listener·area로 해제한다. 오류는 `describe()`(`e.message`)로 로그를 남긴다. `VehicleException`은 배포 AAR에서 난독화되어 쓸 수 없다.
6. **Fake 구현을 만든다.** `Fake<이름>Repository`. 명세의 Fake 데이터를 기본값으로 두고, 테스트에서 값을 바꿀 수 있는 setter를 둔다.
7. **AppContainer에 연결한다.** `isUsingFakeSdk`에 따라 Pleos/Fake를 고르는 `by lazy` 속성을 추가한다.
8. **ViewModel과 화면을 만든다.** `combine`으로 UI 상태 하나를 만들고, 화면은 `BoxWithConstraints`로 1단/2단을, `DrivingRestrictions`로 주행 중 축소를 처리한다. 주행 중에는 1~2초 안에 읽히는 정보만 남긴다. 규제 비트는 값으로 하드코딩하지 말고 비트별 속성(`limitContent`, `noKeyboard` 등)으로 분기한다. 이 에뮬레이터는 주행 중 `0x50`(동영상·설정 금지)뿐이지만 다른 지역은 더 엄격하다.
9. **퍼미션을 갱신한다.** 새 API의 퍼미션을 `AndroidManifest.xml`에 추가하고 README의 "사용 SDK와 퍼미션" 표에 행을 추가한다. 쓰지 않게 된 퍼미션은 지운다.
10. **테스트를 쓴다.** ViewModel이 Fake 값과 규제 변화를 반영하는지 `app/src/test/`에 단위 테스트를 추가한다(기존 `HomeViewModelTest` 형식).
11. **빌드한다.**
    ```bash
    ./gradlew assembleDebug testDebugUnitTest lintDebug
    ```
12. **에뮬레이터에서 확인한다.** 아래 절차로 정차·공회전·주행과 관련 신호 변화를 확인하고 스크린샷을 남긴다.

## 에뮬레이터 확인

스크립트는 저장소 루트에서 실행한다.

```bash
S=.claude/skills/pleos-feature-dev/scripts
$S/emu.sh boot                                   # 꺼져 있으면 띄우고, SystemUI 안정화까지 기다린다
./gradlew installDebug && $S/emu.sh launch
$S/emu.sh shot _workspace/05_build/shots/F1_park.png
$S/vc.sh drive 40 && $S/emu.sh state             # 주행 상태와 규제 확인
$S/emu.sh shot _workspace/05_build/shots/F1_moving.png
$S/vc.sh set seat-occupancy true rear_left       # 기능이 쓰는 신호 바꾸기
$S/vc.sh park
```

- `vc.sh features`로 바꿀 수 있는 신호 id·값 범위·area를 본다. 신호 목록의 뜻은 `.claude/skills/pleos-idea-evaluation/references/platform-capability.md` 4절에 있다.
- 콜드 부팅 직후에는 SystemUI가 반복해서 죽어 앱이 꺼진다. `emu.sh boot`이 안정화까지 기다리지만, 직접 띄웠다면 `emu.sh settle`을 먼저 실행한다.
- 앱 로그는 `emu.sh logs`로 본다. Repository들은 `Pleos…Repo` 태그로 오류를 남긴다.
- 확인이 끝나면 `vc.sh park`로 정차 상태로 되돌린다. 다음 검증이 주행 상태에서 시작하면 결과가 헷갈린다.
- Virtual Console은 세션을 새로 열 때마다(=`vc.sh` 호출마다) 앰비언트 색을 흰색으로, 풍량 0을 2로 되돌린다. 여러 단계를 이어 가는 시연·리허설은 `vc.sh hold`로 세션을 살려 둔 채 진행하고 끝나면 `vc.sh release`를 실행한다.
- 앱 데이터는 `adb shell pm clear --user 10 <pkg>`로 지운다(앱 사용자 10). 세로 1단 배치는 `scripts/spike_w1.sh window split`으로 나누고 `window join`으로 되돌린다.
- 1단(portrait) 배치는 에뮬레이터의 창 분할(3 핑거 스와이프)로 확인하거나, 키트의 폼팩터 테스트 가이드(`pleos-claude-kit/docs/pleos-only/app-test-guide/form-factor-test-guide.md`)를 따른다. 확인하지 못했으면 보고에 "1단 미확인"이라고 쓴다.

## 완료 조건

- 명세의 완료 조건 문장을 모두 만족한다.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`가 통과한다(lint는 새 경고 0건).
- `pleos.useFakeSdk=true`로도 빌드되고 화면이 뜬다.
- 정차와 주행 상태의 스크린샷이 있다.
- 매니페스트 퍼미션과 README 표가 실제 호출과 일치한다.

## 하지 않을 일

- 숨김 API(`Vehicle.getGear()`)와 차량 하드웨어 직접 접근
- 내비 카테고리가 아닌 앱의 경로 안내 직접 구현(경로는 NaviHelper로 내장 내비에 맡긴다)
- 커밋(사용자가 요청할 때 리더가 한다)
- 버전 카탈로그의 버전 올리기(Sample App 검증 조합에 고정되어 있다. 올려야 하면 리더에게 먼저 보고한다)
