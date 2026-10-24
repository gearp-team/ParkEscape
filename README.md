# PleosApp

> 한 줄 소개: (작성 예정)

Pleos Connect(현대자동차그룹 차량용 인포테인먼트 플랫폼)에서 동작하는 차량용 Android 앱입니다.

## 실행 방법

1. Pleos Connect Emulator v2.1.0 AVD를 실행합니다. 설치 방법은 Pleos 개발 문서의 [Pleos Connect Emulator 설치 및 실행](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)을 참고하세요.
2. 에뮬레이터에 앱을 설치합니다.
   - 소스에서 빌드: `./gradlew installDebug`
   - 제출용 APK: `adb install -r apk/TEAM_PleosApp_release.apk`
3. 앱 라이브러리에서 **PleosApp**을 실행합니다.

## 테스트 환경 및 필요한 설정

| 항목 | 버전 |
|---|---|
| JDK | 17 |
| Gradle (wrapper) | 8.10.2 |
| Android Gradle Plugin | 8.8.2 |
| Kotlin | 2.0.0 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 28 |
| 에뮬레이터 | Pleos Connect v2.1.0 (Android 14, API 34) |

- 차량 상태는 Pleos Connect Virtual Console에서 바꿀 수 있습니다. `adb forward tcp:8088 tcp:8088` 후 브라우저에서 `http://127.0.0.1:8088/`을 엽니다.
- 에뮬레이터를 콜드 부팅한 직후 1~2분은 시스템 UI가 안정되지 않아 앱이 종료될 수 있습니다. 잠시 기다린 뒤 실행하세요.
- SDK 없이 화면만 확인하려면 `gradle.properties`에서 `pleos.useFakeSdk=true`로 바꿔 빌드합니다.
- 릴리스 서명 키가 없으면(`keystore.properties` 없음) release 빌드는 debug 키로 서명됩니다.

## 사용 SDK와 퍼미션

| SDK | 버전 | 사용 API | 퍼미션 |
|---|---|---|---|
| Vehicle | 2.3.2 | `EvBattery.registerEvBatteryStateOfCharge` | `pleos.car.permission.CAR_ENERGY` |
| NaviHelper | 3.0.5 | `NaviHelper.getCurrentLocationInfo` | `pleos.car.permission.NAVI_ROUTE` |
| AAOS Car API | – | `CarUxRestrictionsManager` (주행 규제) | – |

## APK

[`apk/TEAM_PleosApp_release.apk`](apk/) — `./gradlew copyReleaseApk`로 생성합니다.
