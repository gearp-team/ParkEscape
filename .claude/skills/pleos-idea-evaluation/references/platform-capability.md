# Pleos 플랫폼 역량 — 아이디어·기획 판단용 사실 모음

기준일 2026-10-24. 출처는 키트(`pleos-claude-kit/CLAUDE.md`, `API_INDEX.md`, `docs/`), 사용자 Playground 콘솔 화면, 이 저장소의 에뮬레이터 실측이다. 프로젝트 `CLAUDE.md`의 "실제로 확인한 함정"과 내용이 겹치면 그쪽이 최신이다.

## 목차
1. SDK 가용성
2. Vehicle SDK 도메인
3. NaviHelper
4. Virtual Console로 재현할 수 있는 신호
5. 에뮬레이터에서 확인한 사실
6. 아직 확인하지 않은 것
7. 심사에서 걸리는 정책 요약

## 1. SDK 가용성

| SDK | 버전 | 콘솔 상태 | 아이디어에서의 위치 |
|---|---|---|---|
| Vehicle 조회(get·register·check) | 2.3.2 | **사용 가능** | 핵심 기능에 쓴다 |
| NaviHelper | 3.0.5 | **사용 가능** | 핵심 기능에 쓴다 |
| Vehicle 제어(set·`CONTROL_CAR_*`) | 2.3.2 | 준비 중 | 보조 기능, 기능 플래그로 끌 수 있게 |
| FusedLocation | 1.1.3 | 준비 중 | 보조. 위치는 NaviHelper `getCurrentLocationInfo()`로 대체 가능 |
| TextToSpeech / SpeechToText | 3.3.1 | 준비 중 | 보조. ON_DEVICE 모드 동작 여부는 미확인 |
| LLM | – | **제공 종료** | 쓰지 않는다. 음성 비서 연동은 Gleo App Schema |

Vehicle SDK 퍼미션은 Pleos App Market과 협의·승인된 범위에서만 선언할 수 있다. 미승인 퍼미션을 선언하면 마켓 심사에서 반려된다.

## 2. Vehicle SDK 도메인

읽기(register·getCurrent·check) 퍼미션 기준이다. 정확한 메서드 이름과 퍼미션은 `pleos-claude-kit/API_INDEX.md`에서 `grep -n "<메서드명>"`으로 확인한다.

| 도메인 | 조회 퍼미션 | register API 수 | 대표 신호 |
|---|---|---|---|
| Door | CAR_DOORS | 3 | 도어 열림(`registerDoorPosition`), 잠금, 글로브박스 |
| Window / Wiper | CAR_WINDOWS | 5 / 1 | 창문 잠금, 스마트 글라스, 블라인드, 와이퍼 |
| HVAC / SideMirror | CAR_CLIMATE | 13 / 1 | 공조 전원·A/C·온도·풍량, 미러 열선 |
| Seat | CAR_SEATS | 15 | 좌석 점유(`registerSeatOccupancy`), 안전벨트 체결, 시트 열선·통풍, 시트 위치 |
| EvBattery / Fuel | CAR_ENERGY | 5 / 5 | SOC(`registerEvBatteryStateOfCharge`), 전비·연비, 연료량 |
| Odometer / DrivingMode / Brake | CAR_DRIVE | 8 / 3 / 1 | 주행 거리·트립 시간, 크루즈 상태·목표 속도, 브레이크 페달 |
| SteeringWheel | CAR_DRIVE, CAR_CLIMATE | 2 | 조향각, 핸들 열선 |
| Light / TurnSignal | CAR_LIGHTS | 11 / 1 | 실내·앰비언트·외부등, 방향지시등 |
| Safety | CAR_SAFETY | 9 | 경고등(telltale), 오일·워셔액·브레이크액 경고 |
| CarInfo / Display | CAR_INFO | 0 / 1 | VIN, 차량 정보, 표시 단위 |
| Toll | CAR_TOLL | 1 | ETCS(하이패스) 음성 안내 볼륨 |

- Safety 일부는 `PRIVATE_CAR_SAFETY`를 요구한다. 일반 앱에 승인될 가능성이 낮으므로 핵심에 쓰지 않는다.
- 차량 속도 리스너(`VehicleSpeedListener`)는 2.3.2에서 삭제됐다. 속도는 AAOS `CarPropertyManager`의 `PERF_VEHICLE_SPEED`로 읽는다.
- `Vehicle.getGear()`는 숨김 API라 쓰지 않는다. 기어·주행 여부는 `CarUxRestrictionsManager`(주행 규제)와 AAOS 표준 속성으로 판단한다.

## 3. NaviHelper

- 조회 API는 값을 반환하지 않고 리스너 콜백으로 결과를 준다: 목적지, 현재 위치, TBT, 경로 상태, 경유지, 즐겨찾기, 최근 목적지, 충전 사업자.
- 경로 요청: `requestRoute`, `requestRouteWithWaypoints`, `requestTrip`, `cancelRoute`, `requestReRoute`, `changeRouteOption`, `addWaypoint`/`removeWaypoint`.
- 이벤트: 경로 시작·완료·취소, 목적지 도착, 경유지 도착, 주행 정보, 구간 제한 속도, 카메라 경보, 주의 지점 등.
- 퍼미션: `NAVI_ROUTE`(조회·trip), `NAVI_ROUTE_SEARCH`(경로 요청·변경), `NAVI_CUSTOM_MAP`(내비 화면 요청).
- 단일 경로 정책: 내비 카테고리가 아닌 앱은 경로 안내(TBT·음성 안내)를 직접 하지 않고 NaviHelper로 내장 내비에 맡긴다.

## 4. Virtual Console로 재현할 수 있는 신호

에뮬레이터의 Virtual Console(`/vehicle/catalog`)이 제공하는 신호 63개다. 시연 시나리오는 이 목록과 아래 속도 제어로만 구성한다. 조작은 `pleos-feature-dev`의 `scripts/vc.sh`로 한다.

| 도메인 | 신호 |
|---|---|
| Motion | 기어 P/R/N/D, 브레이크 페달 0~100%, 누적 주행거리, 트립 주행 시간, 조향각 -470~500°, 핸들 열선 |
| Convenience | ETCS(하이패스) 전원·시나리오(카드 삽입/제거, 진입/진출), 도어 열림, 도어 잠금, 트렁크·프렁크, 실내등, 독서등, 앰비언트(스위치·밝기·색), 외부등 모드, 상향등, 비상등, 제동등, 도어등, 에스코트등, 방향지시등, 창문 위치·잠금, 시트 위치·이동(앞뒤·등받이·높이·틸트·요추), **좌석 점유**, 와이퍼, 글로브박스 |
| Energy | 파워 상태(Off/Ready/On/Driving), 주행 가능 거리, 배터리 에너지 kWh, **SOC %**, 배터리 용량, 충전 전류 제한, 충전 목표 %, 충전 스위치, 충전 포트 |
| Climate | 공조 전원, A/C, Auto, Dual, 내외기, 풍량 0~8, 풍향, 제상, 목표 온도 16~30℃, 시트 열선·통풍(V3~H3), 공기청정 |
| ADAS | 크루즈 상태(Not ready~Full autonomous), 크루즈 목표 속도 |
| Safety | 경고등 Off/On/Blink |
| 기타 | 차량 알림, 타이어 공기압 0~500kPa |
| **속도** | 카탈로그에는 없지만 `/vehicle/controls?speed=<km/h>&accelerator=&brake=&steering=`로 설정된다 |

## 5. 에뮬레이터에서 확인한 사실

- Vehicle SDK: `registerEvBatteryStateOfCharge` 동작(기본 100%), `getVINInfo`는 `"invalid"`, `getDoorLockState(DoorArea(ROW_1_LEFT))`는 area 미지원 오류. area는 capability 조회로 고른다.
- NaviHelper: 연결되고 `getCurrentLocationInfo()`가 37.4127, 127.0947(성남 수정구 시흥동)을 돌려준다.
- 주행 상태와 규제(이 에뮬레이터의 설정값, `dumpsys car_service --services CarUxRestrictionsManagerService`의 상태별 표):
  - 기어 P → 정차(Driving State 0), `requiresDO=false`, 규제 `0x0`
  - 기어 D, 속도 0 → 공회전(1), `requiresDO=true`, 규제 `0x50`(NO_VIDEO + NO_SETUP)
  - 기어 D, 속도 40 → 주행(2), `requiresDO=true`, 규제 `0x50`. 시속 40km/h에서도 `LIMIT_CONTENT`가 꺼져 있어 주소 같은 긴 텍스트가 그대로 보였다
  - 상태를 알 수 없을 때 → 규제 `0x1ff`(모든 비트)
  - 이 값은 키트가 말하는 "그외 지역"(한국 포함) 규칙과 맞는다. 북미 등은 키보드·검색까지 막히므로, 앱은 값을 하드코딩하지 말고 비트별로 처리한다
- `distractionOptimized` 선언 앱은 기어 D에서도 종료되지 않는다.
- 에뮬레이터에 FusedLocation 서비스(`ai.pleos.playground.service.location.fused`)와 음성 서비스(`ai.pleos.playground.caas`) 패키지가 있다.
- 앱을 설치하면 Gleo 쪽이 사용자 앱의 `assets/schema.json`을 스캔한다.
- 인터넷이 된다(에뮬레이트된 LTE, 외부 ping 성공). 자체 서버·외부 API 호출을 시연할 수 있다.
- Google Play 서비스(GMS) 패키지가 없다. Firebase(FCM 푸시 등), Google 지도 SDK, Play 위치 서비스, Google 로그인처럼 GMS에 기대는 라이브러리는 쓸 수 없다고 보고 설계한다(실차는 미확인).
- 콜드 부팅 직후 1~2분은 SystemUI가 반복해서 죽어 앱이 종료된다. 안정된 뒤 테스트한다.

## 6. 아직 확인하지 않은 것

아이디어가 아래에 기대면 카드의 `risks`와 기획의 리스크 절에 적고, 구현 첫 주에 확인한다.

- TTS·STT(ON_DEVICE)·FusedLocation이 에뮬레이터에서 실제로 동작하는지
- Vehicle 제어(set)가 에뮬레이터에서 동작하는지
- Gleo가 3rd-party `schema.json`의 액션을 실제로 호출하는지(안 되면 adb intent로 모사)
- NaviHelper 경로 요청 뒤 주행 시뮬레이션(위치 이동, TBT, 도착 이벤트)이 에뮬레이터에서 나오는지
- 좌석·도어 등 도메인별로 에뮬레이터가 지원하는 area 범위

## 7. 심사에서 걸리는 정책 요약

- 주행 규제: `CarUxRestrictions`의 비트를 따른다. NO_DIALPAD 1, NO_FILTERING 2, LIMIT_STRING_LENGTH 4, NO_KEYBOARD 8, NO_VIDEO 16, LIMIT_CONTENT 32, NO_SETUP 64(+ NO_TEXT_MESSAGE, NO_VOICE_TRANSCRIPTION). 지역을 하드코딩하지 말고 플랫폼이 내려 주는 비트만 반영한다.
- 동영상·게임·브라우저는 "미디어 앱"으로 분류되어 주행 중 실행·사용이 불가하다.
- 앱 카테고리 8종: Maps/Navigation, Mobility Services, Lifestyle, Entertainment, Business, Music/Audio, Kids, Social/Communication. 카테고리와 실제 기능이 다르면 심사에서 즉시 반려된다.
- 성능: 주행 중 비내비 앱 CPU 5% 미만, RAM 400MB 이하, 터치 피드백 0.25초 이내, 실행·로딩 10초 이내.
- 화면: portrait(1 window)와 landscape(2 window) 모두 대응하고 상태를 유지한다. 가릴 수 없는 Driving view가 있다.
- 개인정보: 최소 수집, 명시적 동의 버튼, 위치 정보 사용 시 이용약관·개인정보처리방침이 필요하다. 차량은 공유 기기이므로 계정 전환이 쉬워야 한다.
- 외부 서버 통신: 금지되지 않는다. 제한은 "차량 하드웨어는 SDK·표준 플랫폼 API로만 접근"이다(`vehicle-app-planning-guide/platform-api-architecture.md`). 통신은 HTTPS·TLS 1.2 이상, 서버 인증서 검증, 서비스와 무관한 서버와 통신 금지(`app-registration-guide/app-security-guide.md`). 주행 중 끊김에 대비해 캐시·재시도·오프라인 안내 UI를 둔다. 서버가 필요한 앱은 심사 중에 서버가 켜져 있어야 한다(`distribute/app-submission-guide.md`). 차량 데이터를 서버로 보내면 수집 항목·목적·보유기간 고지와 동의가 필요하고, 운전 중 행동 데이터는 마케팅에 쓰지 않는다(`vehicle-app-planning-guide/security-privacy.md`). 클라우드 Vehicle Data API는 유럽 한정이라 차량 데이터는 차 안의 SDK로 읽어 앱이 직접 보낸다.
