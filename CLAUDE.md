# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

HMG 차량용 앱 개발 공모전("Code your App into Pleos App Market")에 낼 Pleos Connect(AAOS) 앱 **PleosApp**. Kotlin + Jetpack Compose, 단일 `:app` 모듈. 아직 기능은 없고, Vehicle·NaviHelper·주행 규제를 한 화면에 보여 주는 골격만 있다.

| 경로 | 설명 |
|---|---|
| `app/` | 앱 모듈 |
| `apk/` | 공모전 제출용 release APK(`copyReleaseApk`가 만든다, git에 포함) |
| `pleos-claude-kit/` | Pleos 공식 가이드·Dokka API·SDK 소스·공모전 원문을 모은 **참조 전용** 키트(git 제외). 메인 지시서는 이 파일 맨 아래에서 import |
| `keystore/`, `keystore.properties` | 릴리스 서명 키와 비밀번호(git 제외, 별도 백업 필요) |
| `pleos-claude-kit.zip`, `.omc/` | 키트 원본 압축본, oh-my-claudecode 상태. 건드리지 않는다 |

**임시 식별자**: 앱 이름 `PleosApp`, 패키지 `com.team.pleosapp`, 팀명 `TEAM`은 모두 임시다. Pleos Playground에 앱을 등록하기 전에 바꾼다: `app/build.gradle.kts`(namespace·applicationId), `app/src/*/java/com/team/pleosapp` 패키지, `gradle.properties`(`pleos.teamName`·`pleos.appName`), `strings.xml`의 `app_name`, `README.md`.

## 명령

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew :app:testDebugUnitTest --tests "com.team.pleosapp.ui.home.HomeViewModelTest"
```

```bash
./gradlew lintDebug
```

```bash
./gradlew installDebug
```

```bash
./gradlew copyReleaseApk
```

- `copyReleaseApk`는 `assembleRelease` 후 `apk/<pleos.teamName>_<pleos.appName>_release.apk`로 복사한다. 키트 규칙대로 이 태스크는 항상 통과해야 한다.
- `-Ppleos.useFakeSdk=true`(또는 `gradle.properties`)로 빌드하면 SDK 대신 Fake 데이터를 쓴다.
- 리소스 폴더 이름을 바꾼 직후 `resource … not found`가 나면 증분 빌드 문제다. `./gradlew clean` 후 다시 빌드한다.

## 구조

- **SDK 격리**: Pleos SDK와 AAOS Car API 호출은 `data/` 아래 Repository에만 둔다. 영역마다 인터페이스 + 실제 구현(`Pleos*`/`Car*`) + `Fake*` 구현이 있다. `AppContainer`가 `BuildConfig.USE_FAKE_SDK`에 따라 하나를 고르고, `PleosApplication`이 이것을 들고 있다(수동 DI).
- **콜백 → Flow**: SDK 리스너는 `callbackFlow`(Vehicle, 주행 규제)나 `MutableStateFlow`(NaviHelper. 조회 결과가 바인더 스레드의 리스너로 온다)로 바꿔 노출한다. 리스너 해제는 `awaitClose`에서 등록 때와 같은 객체로 한다. SDK 객체는 앱 프로세스 수명이라 `release()`를 부르지 않는다.
- **화면**: `HomeViewModel`이 세 Repository를 `combine`해 `HomeUiState` 하나로 만든다. `HomeScreen`은 화면 방향이 아니라 앱 창의 실제 크기(`BoxWithConstraints`)로 1단/2단을 고른다(Pleos는 portrait 1 window, landscape 2 window). 주행 규제는 `DrivingRestrictions`의 비트(`limitContent` 등)를 보고 UI를 줄인다. 규제 상태를 알 수 없으면 `FullyRestricted`로 간주한다.
- **새 SDK 기능을 붙일 때**: Repository 인터페이스 → 실제 구현 → Fake 구현 → `AppContainer` 순서로 추가하고, 매니페스트에는 실제 호출하는 API의 퍼미션만 넣고, README의 "사용 SDK와 퍼미션" 표를 갱신한다.
- **버전 고정**: AGP 8.8.2, Kotlin 2.0.0, compileSdk 35는 Vehicle SDK Sample App 검증 조합이다. 그래서 lint의 버전 권고(`GradleDependency`, `AndroidGradlePluginVersion`)는 꺼 두었다. androidx를 올릴 때는 compileSdk 36을 요구하지 않는지 확인한다.
- **릴리스 서명**: `keystore.properties`가 있으면 그 키로, 없으면 debug 키로 서명한다. v3 서명을 켰고(키 로테이션·lineage), minSdk 28이라 AGP는 v2를 생략하고 v3만 넣는다. 확인: `$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose apk/*.apk`. R8(minify)은 SDK 동작이 검증되기 전까지 꺼 둔다.

## 개발 환경

- JDK 17(Homebrew `openjdk@17`), Android SDK `~/Library/Android/sdk`. `JAVA_HOME`·`ANDROID_HOME`·`PATH`는 `~/.zshrc`에 있다.
- Pleos 이미지 `system-images;android-34;pleos_car_emulator;arm64-v8a`, AVD `Pleos_Connect_v2.1`(Cold boot, RAM 4GB, host GPU). Pleos Update Site는 `~/.android/repositories.cfg`에 등록되어 있다.

```bash
emulator -avd Pleos_Connect_v2.1
```

```bash
adb wait-for-device && until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
```

**주행 상태 바꾸기(Virtual Console API)**: 키트 지시서의 `cmd hmg_car_service … driving_regulation`은 이 에뮬레이터에 서비스가 없어 동작하지 않고, `cmd car_service inject-vhal-event`도 막혀 있다. Virtual Console을 쓰되, 콘솔은 브라우저 세션 heartbeat(`POST /browser/session`, 1초 간격)가 있는 동안에만 제어를 받는다(없으면 `{"error":"emulator_inactive"}`).

```bash
adb forward tcp:8088 tcp:8088 && (for i in $(seq 30); do curl -s -X POST "http://127.0.0.1:8088/browser/session?client=cli" >/dev/null; sleep 1; done) & sleep 2; curl -s -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"
```

기어 값은 `Park`/`Reverse`/`Neutral`/`Drive`. 다른 엔드포인트(`/vehicle/controls`, `/vehicle/body`, `/vehicle/hvac`, `/vehicle/state`, `/vehicle/catalog`)도 모두 `PUT`/`GET`이고 같은 heartbeat가 필요하다. 확인: `adb shell dumpsys car_service --services CarDrivingStateService | grep "Current Driving State"`(0 정차, 1 공회전, 2 주행).

## 실제로 확인한 함정 (키트 지시서보다 우선)

- **`VehicleException`을 쓸 수 없다.** 배포된 Vehicle 2.3.2 AAR에서 이 클래스는 난독화(`callback.exception.a`)되어 있고 `ErrorCode` enum은 없다. 키트 지시서 3.1절과 공식 문서의 `if (e is VehicleException) e.errorCode`는 컴파일되지 않는다. `e.message`에 ErrorCode 이름이 들어 있다(`data/vehicle/PleosVehicleRepository.kt`의 `describe()`). `onFailed`에는 `IllegalArgumentException`(지원하지 않는 area 등)도 들어온다.
- **콜드 부팅 직후 1~2분은 테스트하지 말 것.** 이미지 자체 버그로 SystemUI가 반복해서 죽고(`SeatTempDynamicInteractorView`의 `CalledFromWrongThreadException`), 그때 실행한 앱은 `remove task`로 종료된다. 확인: `adb logcat -d -b crash | grep -c 'Process: com.android.systemui'`가 더 늘지 않을 때까지 기다린다.
- 에뮬레이터 기본값: VIN `"invalid"`, EV SOC 100%, NaviHelper 현재 위치 37.4127, 127.0947(성남 수정구). `DOOR_LOCK`은 `DoorArea(DoorValue.ROW_1_LEFT)`를 지원하지 않으므로 area는 capability 조회 결과로 고른다.
- 기어 D·속도 0(공회전)에서 `requiresDistractionOptimization`은 true지만 `LIMIT_CONTENT` 비트는 켜지지 않았다. 규제 UI는 비트별로 따로 확인한다.
- 에뮬레이터에는 FusedLocation 서비스(`ai.pleos.playground.service.location.fused`)와 음성 서비스(`ai.pleos.playground.caas`) 패키지가 들어 있다. 실제 동작은 아직 검증하지 않았다.
- 앱을 설치하면 Gleo 쪽(`[GAA]` 로그)이 사용자 앱의 `schema.json`을 스캔한다. 실제로 Gleo가 앱 액션을 호출하는지는 아직 확인하지 않았다.

## 키트 경로 규칙

- import된 키트 지시서에 나오는 경로(`API_INDEX.md`, `api-reference/…`, `sdk-artifacts/sources/…`, `docs/…`, `contest/…`)는 **모두 `pleos-claude-kit/` 기준 상대경로**다. 키트 README는 폴더명을 `pleos-kit/`로 가정하지만 여기서는 `pleos-claude-kit/`다.
- 앱 코드나 작업 메모를 키트 안에 쓰지 말 것.
- `docs/` 파일의 첫 줄 `<!-- source: https://document.pleos.ai/... -->`가 원문 URL이다. 정책·요건을 근거로 들 때 이 URL을 인용한다.
- API를 호출하는 코드를 쓰기 전에 시그니처와 `@RequiresPermission`을 확인한다: `grep -n "<메서드명>" pleos-claude-kit/API_INDEX.md`, `grep -rn "fun <메서드명>" pleos-claude-kit/sdk-artifacts/sources/`.
- TextToSpeech·SpeechToText는 소스 jar가 없으므로 `API_INDEX.md`와 `api-reference/`(Dokka)만 근거로 쓴다. 42dot SDK 라이선스 3.4조가 디컴파일·디스어셈블을 금지하므로 AAR을 `javap` 등으로 열어 보지 않는다.

## Pleos 플랫폼·공모전 지시서 (키트)

@pleos-claude-kit/CLAUDE.md
