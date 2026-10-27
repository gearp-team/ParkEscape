# 리서치 요약

> 기준일 2026-10-24 · 워크플로 `wf_292b6c8d-cf9` + 보강 `wf_74fc91b0-51d` + 파격 아이디어 `wf_a6a0c91b-439` · 축 8/8 완료 · 실패한 축 없음
> 발견 사항 70건: 확인 20, 반박 3(고친 내용으로 반영), 불확실 4, 미검증 43(결정에 덜 중요해 검증하지 않음)
> 표기: ✅ 확인됨(사실로 인용 가능) · ⚠ 미확인 · ✏ 반박되어 고친 내용 · ○ 미검증(정성 참고로만). 원자료: `_workspace/01_research/01_workflow_result.json`, `01b_workflow_result.json`(보강), `_workspace/02_ideation/02c_wow_judging.json`(파격 아이디어 경쟁 사례)

## 한눈에 보기

1. **지금 쓸 수 있는 SDK는 Vehicle 조회와 NaviHelper뿐이다.** TTS·STT·FusedLocation·Vehicle 제어는 콘솔에서 "준비 중"이라 신청도 안 된다. 핵심 흐름은 이 두 SDK로만 성립해야 한다(platform-01 ✅).
2. **속도·기어·경고등은 일반 앱 API로 안정적으로 읽을 수 없다.** 속도 리스너는 삭제됐고 `getGear()`는 숨김, 경고등은 PRIVATE 퍼미션 표기다. 브레이크는 눌림 여부만 준다. 공모전 공식 예시인 "안전운전 점수"는 브레이크·조향각·크루즈·트립 같은 확인된 신호로 다시 정의해야 한다(platform-03 ✅).
3. **정차(P)에서만 제약이 없다.** 도로교통법 제49조제1항제11호·제11호의2는 운전 중 영상 표시와 조작을 금지하고 정지 중에는 허용한다(regulation-safety-01 ✅). 이 에뮬레이터는 P에서 `0x0`, 공회전과 주행에서 `0x50`(동영상·설정 금지)을 준다(platform-07 ✅). 신호대기(D) 정지에 대한 법 해석과 플랫폼 정책의 관계는 아직 불확실하므로, 깊은 상호작용은 P단 기준으로 설계한다(regulation-safety-02 ⚠).
4. **시장은 작고 비어 있다.** Pleos Connect 탑재 차종은 국내 현대차 3종(그랜저·아반떼·투싼, 유럽에는 아이오닉 3)이고, Pleos App Market은 2026-09 기준 18종으로 내비·음악·영상 대형 서비스 중심이다. 차량 관리·라이프스타일은 지금 비어 있지만 42dot이 순차 추가를 예고했다(market-01·03 ✅). Pleos 앱마켓의 MAU·누적 탑재 대수는 공개되지 않아, 예상 MAU는 판매 대수에 가정을 곱하는 퍼널로만 쓸 수 있다(market-02 ✅).
5. **기획 배경에 쓸 수 있는 확인된 수치:** 등록 자동차 약 2,668만 대, 전기차 약 110만 대(4.1%, 반기 21.8% 증가)(user-needs-01 ✅). 통근 근로자의 하루 평균 이동 시간 73.9분, 수도권 82.0분(이동 수단 구분 없음, "차 안 시간"으로 바꿔 쓰지 않는다)(user-needs-02 ✅). 전기차 충전 불편은 조사마다 "고장"과 "부족·질서"로 갈린다(user-needs-05 ✅).
6. **음성 연동과 클라우드 데이터는 핵심으로 약속하지 않는다.** Gleo 연동 공식 경로는 App Schema(음성 function 최대 3개, 7초 응답, Pleos 승인 필요)뿐이다(tech-trends-05 ✅). 클라우드 Vehicle Data API는 유럽에서만 제공된다(tech-trends-02 ✅).
7. **위치를 쓰면 동의·약관이 필수다.** 위치정보법상 동의 없는 수집은 금지되고, 사업화 시 위치기반서비스 신고, 14세 미만은 법정대리인 동의가 필요하다. Pleos 제출 양식의 "위치정보 활용" 항목은 모든 앱이 답해야 한다(regulation-safety-05 ✅).

## 아이디어·기획에 바로 걸리는 제약

- 핵심 기능 = Vehicle 조회 + NaviHelper + 주행 규제(CarUxRestrictions). 나머지 SDK는 꺼도 앱이 살아 있는 보조 기능으로만.
- 속도·급가속·경고등 기반 기능은 핵심에서 뺀다. 필요하면 구현 첫 주에 AAOS `PERF_VEHICLE_SPEED` 경로를 먼저 검증한다.
- 깊은 상호작용(입력, 설정, 긴 글, 영상)은 P단(주차·충전 대기)에서만. 주행·공회전 화면은 1~2초 요약과 큰 터치 하나.
- 예상 MAU는 "탑재 추정 대수(근거) × 활성화율(가정) × 이용률(가정)"처럼 가정을 드러내 쓴다.
- 기획 배경 수치는 위 ✅ 항목을 검증 메모의 표현대로 인용한다. 반박된 수치(tech-trends-06의 원 주장)는 쓰지 않는다.
- 10/28 온라인 설명회에서 확인할 것: 준비 중 SDK 범위, Vehicle State 승인 퍼미션 목록, "SDK 추가 권한" 항목 작성법, 속도 신호 접근.

## 보강 리서치: 타 브랜드와 경쟁 기능 (2026-10-24, 2개 축)

8. **해외 생태계는 둘로 갈린다.** Google 내장 차량은 Play 앱이 700개 이상(16개 브랜드·100개 이상 모델, 2026-10)으로 커졌다. 앞의 market-04에 적힌 "300개 이상"은 최신 값이 아니다(global-oem-apps-01 ✅). BMW(85개 이상)·NIO(40개 이상, 2023)는 Pleos처럼 큐레이션형 자체 스토어이고, Tesla·Rivian·GM은 서드파티를 거의 열지 않는다. 서드파티 차량 앱의 이용자·매출은 거의 공개되지 않았고, 수익 배분 조건이 공개된 곳은 Google Play뿐이다(한국은 15%/30%). Pleos 수수료는 미공개다(global-oem-apps-02·03·06·07 ○).
9. **폰 미러링이 가장 강한 대체재지만, 국내 국산차에서는 순정 화면이 강하다.** 미국에서 CarPlay·Android Auto가 있는 사람의 83%가 실제로 쓴다(Edison 2025, 모집단은 "운전하거나 탄 18세 이상"). 반면 국내 최근 3년 내 국산 신차 구입자의 68%는 순정 내비를 주로 쓰고 스마트폰 내비는 25%다(수입차는 반대, 컨슈머인사이트 2022)(global-oem-apps-08·09 ✅). 정차 중 영상은 Android Auto에도 들어올 예정이라 내장 앱만의 영역이 아니다(global-oem-apps-10 ⚠).
10. **상위 5개 아이디어의 기존 기능 대조**
    - **1위 소모품 사용량 시계:** 엔진오일은 현대·기아 일부 차량(미국 매뉴얼 기준)에 운전 이력 기반 오일 수명 관리가 있어 차별점이 아니다. 차별점은 와이퍼·공조 필터처럼 **오일 외 소모품**을 사용량으로 알리는 것으로 좁혀야 하고, BMW CBS 선례가 있어 "업계 최초"라고 쓸 수 없다(competitor-benchmark-01 ✏).
    - **2위 휴게소 재출발 체크:** 현대·기아(시동 끄고 운전석 문 열 때)와 GM(시동 끌 때)의 후석 승객 알림은 모두 하차 시점에만 동작한다. 다시 출발하는 시점에 빈자리를 확인하는 순정 기능은 없어 **차별점이 유지된다**(competitor-benchmark-03 ✅). 국내 승용차 후석 방치 공식 통계는 없다(competitor-benchmark-04 ○).
    - **3위 펫 드라이브 가드:** Tesla Dog Mode, 기아·현대 Pet Mode는 모두 주차 중 공조 유지 기능이다. 탑승 중 창문 열림·하차 시 경고는 없어 **방향이 다르다**(competitor-benchmark-05 ○).
    - **4위 차량 이상 징후 일지:** 현대 리모트 케어(선제케어)에 알림 이력과 정비 연결이 이미 있어 **가장 많이 겹친다**(competitor-benchmark-07 ✅).
    - **5위 계기판 운행일지:** 법인세법 시행령 제50조의2(현행 제6항)와 국세청고시 제2025-8호의 운행기록부 서식에 계기판 누적거리 칸이 있다. GPS 앱·통신사 서비스는 있지만 계기판 거리를 차량 신호로 직접 쓰는 순정 연계는 없다(competitor-benchmark-08 ✅, -09 ○).

## 보강 리서치 2: 파격 아이디어의 경쟁 사례 (2026-10-24, 1개 축)

11. **파격 아이디어 4개 모두 "세계 최초"라고 쓸 수 없다.** 아이디어별 비교와 심사 결과는 [wow-candidates.md](wow-candidates.md)에 있다.
    - **차 방탈출(W1):** Tesla가 2019년에 핸들·브레이크를 입력으로 쓰는 P단 전용 레이싱 게임을 OTA로 고객 차량에 배포했고, Ford는 2022년 Mach-E 방탈출을 콘셉트로 시연했다(Ford 쪽은 게임이 차를 작동시키는 방식). 남는 차별점은 차량을 제어하지 않고 읽기만 하면서 차 전체(깜빡이·창문·공조·조명·등받이)를 입력으로 쓰는 구조, 그리고 충전 시간에 맞춘 판 길이다(wow-benchmark-01 ✏). BMW·MINI 50만 대 이상에 AirConsole 정차 게임이 배포됐지만 이용 실적은 공개되지 않았다(-02 ○).
    - **차의 다마고치(W2):** 다마고치형 제품은 검색한 범위에서 찾지 못했다. 하지만 Allstate 특허(US9858832B1)가 운전 데이터로 캐릭터 속성을 바꾸는 개념을 공개했다. 점수·배지형은 Tmap(UBI 가입자 500만 육박)과 캐롯이 차지했고, 효과 연구는 점수표의 효과가 가장 크다고 본다(-04 ⚠).
    - **와이퍼 크라우드 날씨(W3):** Ford Local Hazard Information(와이퍼·안개등·비상등)과 Audi가 이미 상용화했다(-06 ✅). 우천 치사율은 맑은 날의 약 1.3배(전국), 1.4배(고속도로)다(-08 ○).
    - **경로 맞춤 오디오 가이드(W4):** Autio가 2026-08 Ford·Lincoln 차량에 들어갔다. holoride는 여정 시간에 맞춘 콘텐츠(elastic content)를 내세웠고, 관련 특허도 2008년부터 있다. 남는 차별점은 "경로상 지점 도착 시각에 이야기 끝 맞추기" 하나다(-09 ⚠).
12. **수치로 쓸 수 없는 것:** 충전 대기 시간, 충전 중 차 안 체류 비율, 인카 게임 이용 실적은 신뢰할 공개 수치가 없다. 기획서에 임의 수치를 넣지 않는다(-10 ○).

## 축별 현황

| 축 | 발견 | ✅ | ⚠ | ✏ | ○ |
|---|---|---|---|---|---|
| Pleos 플랫폼 역량과 제약 (`platform`) | 8 | 3 | 0 | 0 | 5 |
| 운전자·동승자 페인포인트 (`user-needs`) | 8 | 3 | 0 | 0 | 5 |
| 차량용 앱·인카 서비스 시장과 경쟁 (`market`) | 8 | 3 | 0 | 0 | 5 |
| SDV·인카 AI·차량 데이터 기술 트렌드 (`tech-trends`) | 8 | 2 | 0 | 1 | 5 |
| 규제·안전·개인정보 (`regulation-safety`) | 8 | 2 | 1 | 0 | 5 |
| 해외·타 브랜드 인카 앱 생태계 (`global-oem-apps`) | 10 | 3 | 1 | 0 | 6 |
| 상위 아이디어의 경쟁·유사 기능 (`competitor-benchmark`) | 10 | 3 | 0 | 1 | 6 |
| 파격 아이디어 4개의 경쟁·유사 사례 (`wow-benchmark`) | 10 | 1 | 2 | 1 | 6 |

## Pleos 플랫폼 역량과 제약 (`platform`)

### ✅ 확인된 사실

- **platform-01** 신청 없이 지금 쓸 수 있는 SDK는 Vehicle State(조회)와 NaviHelper뿐이다. FusedLocation, TextToSpeech, SpeechToText, Vehicle Control은 콘솔에서 '준비 중(Coming Soon)'이라 신청도 할 수 없다.
  - 근거: [A] [프로젝트 생성 및 SDK 사용 신청](https://document.pleos.ai/docs/pleos-only/create-my-project) (2026-10-24); [A] 사용 승인 상태 — `pleos-claude-kit/CLAUDE.md §2.2 (사용자 Playground 콘솔 'TEST' 프로젝트 화면 기록)` (2026-10-24); [A] 공모전 개발 환경·FAQ — `pleos-claude-kit/contest/CONTEST_SOURCE.md (guide/development, FAQ '어떤 SDK와 API를 쓸 수 있나요?')` (2026-10-24); [A] Pleos Connect SDK 문서 안내(서비스 상태 페이지 비공개) — `pleos-claude-kit/docs/pleos-only/connect-sdk-service-status.md` (2026-10-24); [A] Vehicle SDK 퍼미션 승인 기준 — `pleos-claude-kit/docs/pleos-only/connect-sdk-pleos.md (API policy 안내 박스), docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability.md ('SDK·API 사용 심사')` (2026-10-24); [A] 앱 심사 제출 항목 — `pleos-claude-kit/docs/pleos-only/distribute/app-submission-guide.md ('SDK 추가 권한')` (2026-10-24)
  - 검증 메모: 정밀화 2가지. (1) 원문 배지 표기는 오타인 'Comming Soon'이다. (2) 'SDK 추가 권한'은 양식에서 필수로 표시되지만, 설명은 '권한 신청이 필요한 SDK를 사용하는 앱일 경우' 프로젝트 ID를 넣으라는 조건부 문구다. 따라서 신청이 필요 없는 Vehicle State·NaviHelper만 쓰는 앱에서 이 항목을 어떻게 채울지는 10/28 설명회에서 함께 확인하는 것이 좋다.
  - 시사점: 아이디어의 핵심 흐름은 Vehicle 조회와 NaviHelper만으로 성립해야 한다. 음성(TTS/STT), 위치(FusedLocation), 차량 제어(set*)는 있으면 좋은 보조 기능으로 두고 기능 플래그와 폴백(예: 위치는 NaviHelper getCurrentLocationInfo)을 설계한다. 기획서 5번에는 준비 중 SDK를 쓴다면 오픈 후 확장 계획이라고 솔직하게 적어 심사 리스크를 줄인다. 10/28 온라인 설명회에서 준비 중 SDK 범위를 공식 확인한다.
  <details><summary>상세</summary>

  공식 문서 create-my-project는 준비 중 SDK로 FusedLocation, TextToSpeech, SpeechToText, Vehicle Control을 들고, 사용 가능 SDK로 Vehicle State와 NaviHelper를 든다. 두 SDK는 신청 없이 바로 쓴다고 적혀 있다. 이 내용을 document.pleos.ai 원문에서 2026-10-24에 다시 확인했다. 사용자 Playground 콘솔(TEST 프로젝트) 화면도 같다. Vehicle State의 조회 영역은 11개(제원, 주행, 도어/트렁크, EV·연료, 공조, 라이트, 안전/경고등, 좌석, 사이드미러, 통행료, 창문)로 기록돼 있다.
  충돌하는 출처: 공모전 사이트(개발 환경 페이지, FAQ)는 Vehicle, NaviHelper, TTS, STT, FusedLocation 5개를 참가자용 SDK로 나열하면서 제공 상태는 공식 문서에서 확인하라고만 한다. 공식 문서의 SDK 상태 안내 페이지(connect-sdk-service-status)는 현재 제공되지 않는다는 문구뿐이고, 확정되지 않은 기능은 별도 안내하지 않는다고 밝힌다. 오픈 일정을 알 방법이 없다.
  승인 관련 규칙: Vehicle SDK 퍼미션은 Pleos App Market과 사전 협의·승인된 범위에서만 선언할 수 있고, 미승인 퍼미션을 선언하거나 API를 호출하면 심사를 통과하지 못한다. 마켓 제출 양식에는 'SDK 추가 권한' 항목(프로젝트 ID 입력)이 필수로 있다. 'Vehicle State'가 구체적으로 어떤 퍼미션 목록까지 승인된 것인지는 공식 문서에 명시돼 있지 않다.
  참고: TTS·STT는 ON_DEVICE 모드면 앱 등록(Client ID/Secret)이 필요 없다고 문서에 적혀 있지만, 콘솔 상태가 준비 중이므로 실제 동작은 별개로 확인해야 한다(platform-06의 gaps 참고).

  </details>

- **platform-03** 차량 속도, 기어, 경고등(telltale)은 Vehicle SDK의 일반 앱 API로 안정적으로 읽을 수 없다. 속도 리스너는 2.3.2에서 삭제됐고, getGear()는 숨김 API이며, telltale API는 PRIVATE_CAR_SAFETY(signature|privileged)로 표기돼 있다. 브레이크는 페달 눌림 여부(Boolean)만 준다.
  - 근거: [A] VehicleSpeedListener 삭제 — `pleos-claude-kit/docs/pleos-only/release-note/sdk-261015.md (Vehicle SDK v2.3.2, Shutdown API)` (2026-10-19); [A] FAQ - 앱 실행 환경 및 플랫폼 리소스 — `pleos-claude-kit/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq.md ('클러스터 속도 조회 방법')` (2026-10-24); [A] Vehicle.getGear() @suppress @hide — `pleos-claude-kit/sdk-artifacts/sources/Vehicle-2.3.2/ai/pleos/playground/vehicle/Vehicle.kt:406-411` (2026-10-24); [A] Telltales의 PRIVATE_CAR_SAFETY 표기, 브레이크 페달 Boolean — `pleos-claude-kit/API_INDEX.md (### safety 785~812행, ### brake 495~498행)` (2026-10-24); [A] 공모전 예시 시나리오(안전운전 점수 앱) — `pleos-claude-kit/contest/CONTEST_SOURCE.md (guide/development '예시 시나리오·튜토리얼')` (2026-10-24)
  - 검증 메모: 결론은 유지하되 근거를 보정할 점 4가지. (a) 에뮬레이터에서 읽기 전용 'dumpsys package permission'을 실행해 보니 pleos.car.permission.PRIVATE_CAR_SAFETY가 ai.pleos.playground.service.vehicle에 prot=normal로 정의돼 있다. 문서 표기인 signature|privileged와 다르므로 에뮬레이터에서는 선언만으로 권한이 부여될 수 있다. 경고등을 쓰지 말아야 할 근거는 기술적으로 막혀서가 아니라, 문서 표기·미승인 퍼미션 심사 규칙과 Sample App 문서의 'PRIVATE_* permission은 예제에 포함하지 않습니다' 같은 정책 측면이다. (b) AAOS 경로의 권한 등급을 확인했다. PERF_VEHICLE_SPEED에 필요한 android.car.permission.CAR_SPEED는 dangerous(런타임 허용)다. GEAR_SELECTION에 필요한 CAR_POWERTRAIN은 normal이다. 두 등급은 Android 공식 문서와 에뮬레이터 dumpsys에서 모두 확인했다. 기술적으로는 서드파티 앱에 열려 있지만, Pleos 마켓 심사에서 허용되는지와 에뮬레이터에서 값이 나오는지는 여전히 확인하지 못했다(shell의 get-property-value는 user 빌드라 막힘). (c) NaviHelper에는 onCurrentRoadSpeedLimit(1초 주기)과 CameraAlert.isOverSpeed, isOverSectionAvrSpeed가 있다. 단속 카메라 구간의 과속 여부는 속도값 없이도 얻을 수 있으므로 안전운전 계열을 재정의할 때 재료로 쓸 수 있다. (d) Sample App 매니페스트는 pleos.car.permission.CAR_SPEED를 선언하지만, 이 퍼미션은 에뮬레이터에 정의돼 있지 않고 2.3.2에는 이를 쓰는 API도 없다. 속도 API가 있다는 근거로 쓰지 않는다.
  - 시사점: 속도·기어·경고등에 의존하는 아이디어(과속·급가속 점수, 기어 연동 자동화, 경고등 해설)는 핵심 기능에서 빼거나, AAOS CarPropertyManager 경로를 구현 첫 주 스파이크로 먼저 검증한 뒤에만 채택한다. 안전운전 계열은 브레이크 눌림·조향각·크루즈 등 확인된 신호로 재정의하거나 속도 의존을 보조로 둔다. 기획서에 속도 기반 기능을 쓰면 근거가 약하다는 점을 감안한다.
  <details><summary>상세</summary>

  속도: VehicleSpeedListener와 onSpeedUpdated가 Vehicle SDK 2.3.2에서 삭제됐다(릴리즈 노트 Shutdown API). 공식 FAQ는 클러스터와 같은 속도를 쓰려면 AAOS CarPropertyManager로 PERF_VEHICLE_SPEED를 읽으라고 안내하며, 필요한 권한을 선언하라고만 적는다. 이 경로가 마켓 심사에서 허용되는 권한인지, 에뮬레이터에서 값이 나오는지는 키트에 없고 이 조사에서도 확인하지 못했다.
  기어: Vehicle.getGear()는 소스(Vehicle.kt 406~411행)에서 @suppress @hide로 직접 확인했다. 기어·주행 여부는 CarUxRestrictions나 AAOS 표준 속성으로 판단해야 한다.
  경고등: checkTelltalesCapabilities, getTelltalesInfo, registerTelltales, unregisterTelltales는 API_INDEX에서 perm이 PRIVATE_CAR_SAFETY(signature|privileged)다. getTelltalesInfo는 @RequiresPermission이 CAR_SAFETY인데 perm 항목은 PRIVATE_CAR_SAFETY라 표기가 서로 달라, 일반 앱에 허용될지 불확실하다. 반면 안전벨트 미체결, 에어백, 엔진오일·브레이크액·워셔액, PowerDown 경고는 일반 CAR_SAFETY(normal)다.
  브레이크·가속: getCurrentBrakePedalStatus는 Boolean(눌림 여부)이고 가속도·급제동 강도 신호는 API에 없다. 쓸 수 있는 주행 습관 재료는 브레이크 페달 눌림 횟수, 조향각, 방향지시등, 크루즈 상태, 트립 주행거리·시간, EV 순간 전비 정도다.
  공모전 공식 예시 중 '안전운전 점수 앱'은 Vehicle SDK 차량 상태 데이터로 주행 습관을 점수화한다고 소개하지만, 속도·가속도를 Vehicle SDK로 얻을 수 없다는 점이 위 사실과 부딪힌다.

  </details>

- **platform-07** 주행 규제는 속도·기어·브레이크 신호로 판단되며, 이 에뮬레이터는 정차(P) 0x0, 공회전(D·0km/h)과 주행(D·40km/h) 모두 requiresDO=true에 0x50(NO_VIDEO+NO_SETUP)을 준다. 공식 지역 정책표는 주행 중 대용량 텍스트도 금지로 적지만 에뮬레이터 주행 설정에는 해당 비트가 꺼져 있어 일부 불일치한다.
  - 근거: [A] 지역별 주행 규제 정책 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy.md` (2026-10-24); [A] 주행 규제 해제 조건(북미·북미 외) — `pleos-claude-kit/docs/pleos-only/infolink-emulator/driving-restiction-policy.md ('주행 규제 판단 기준')` (2026-10-24); [A] 주행 규제 개발 가이드, 주행 안전을 고려한 설계 — `pleos-claude-kit/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide.md; vehicle-app-planning-guide/driver-distraction-guideline.md` (2026-10-24); [A] 운전자 주의 분산 요소 최소화 설계(미디어 앱) — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements.md` (2026-10-24); [A] 에뮬레이터 주행 규제 설정값 재확인 — `에뮬레이터 adb shell dumpsys car_service --services CarUxRestrictionsManagerService, getprop product.sw.target_region (2026-10-24 읽기 전용)` (2026-10-24); [A] 프로젝트 실측 정리(규제 비트, DO 동작) — `프로젝트 CLAUDE.md '실제로 확인한 함정'; .claude/skills/pleos-idea-evaluation/references/platform-capability.md §5` (2026-10-24)
  - 검증 메모: detail 보정. (a) '한국 전용 표는 없다'는 정확하지 않다. KOR/EUR/USA·CAN 지역 코드는 InfoLINK 보충 문서(infolink-emulator/driving-restiction-policy.md)에서 나온다. 이 문서의 QR 로그인 화면별 표에는 한국 열이 따로 있고, 주행 중 한국은 대부분 화면에서 QR이 ✅(Wi-Fi URL QR만 규격 미정)인 반면 북미·유럽·기타는 ⛔다(UX_RESTRICTIONS_NO_SETUP_QR_SCAN). 따라서 한국을 '그외 지역'과 똑같이 보는 추정은 적어도 QR 항목에서 어긋난다. 기본 정책표(북미/유럽/그외)에만 한국 표가 없다고 고쳐 써야 한다. (b) 기본 정책표에는 Parking과 Driving 행만 있고 Idling 행은 없다. 'DO 미선언 앱은 Parking에서만'도 표에는 Parking과 Drive만 있으므로, 공회전 때 막히는지는 에뮬레이터 idling requiresDO=true에서 추론한 것이다. (c) '1~2초 안에 파악'은 플래닝 가이드가 아니라 Pleos Connect UI Design Guide(designing-apps-for-driving.md, intro.md)의 문구다. 플래닝 가이드는 '짧은 시간 안에'라고만 적는다. (d) '운전과 무관한 헤드업 알림 금지'는 심사 가이드의 미디어 앱 절에 있는 규칙이다.
  - 시사점: 깊은 상호작용(설정, 입력, 긴 텍스트)은 P단·주차 브레이크 같은 확실한 정차 상태에만 두고, 주행·공회전 화면은 1~2초 요약과 단일 터치로 줄인다. 차량 설정·제어성 UI(도어, 시트 등)는 NO_SETUP으로 주행 중 잠기므로 정차 전용으로 기획한다. 규제는 지역을 하드코딩하지 말고 activeRestrictions 비트별로 처리하며, 기획서 4번 시나리오는 정차·공회전·주행 상태별 화면을 나눠 쓴다. 대용량 텍스트 금지 여부는 공식표와 에뮬레이터가 달라 UI를 더 엄격한 쪽으로 설계한다.
  <details><summary>상세</summary>

  기본 규칙: Distraction Optimization을 선언하지 않은 앱은 Parking에서만 쓸 수 있고 Drive에서는 쓸 수 없다. DO를 선언해도 CarUxRestrictions의 activeRestrictions 비트를 앱이 직접 반영해야 한다.
  규제 해제 조건(공식, 북미 외 지역): 시동·구동 준비가 꺼져 있거나, 속도 5km/h 이하이면서 기어 P, 주차 브레이크 ON, Auto Hold 활성(장착 차종) 중 하나를 만족할 때다. 북미는 P(수동 변속기는 N+주차 브레이크)가 필요하고 Auto Hold는 정차로 인정하지 않는다. 따라서 신호 대기처럼 D단 정차는 정차로 인정되지 않을 수 있다.
  지역별 정책표(공식): 지역 코드는 KOR, EUR, USA/CAN이며 한국 전용 표는 없다. '그외 지역' 표 기준 주행 중 키보드·다이얼패드·검색·미디어 앱 로그인·내비 검색은 허용, 디지털키·도어·시트·차량 설정(NO_SETUP 64), 동영상(NO_VIDEO 16), QR 로그인, 대용량 텍스트(LIMIT_CONTENT 32, LIMIT_STRING_LENGTH 4)는 금지다. 북미는 키보드 등 대부분 금지(검색은 음성한정), 유럽은 미디어 로그인·설정이 금지다. 한국에 '그외 지역' 표를 적용하는 것은 키트의 추정이다.
  에뮬레이터 실측(오늘 dumpsys CarUxRestrictionsManagerService 재확인): parked 0x0, idling 0x50, moving 0x50, unknown 0x1ff이고 Max String length 120, Max Cumulative Content Items 21, Max Content depth 3이다. NO_VIDEO와 NO_SETUP은 공식표와 맞지만 LIMIT_CONTENT·LIMIT_STRING_LENGTH가 주행 중에도 꺼져 있어 공식표의 대용량 텍스트 금지와 맞지 않는다. 에뮬레이터는 product.sw.target_region 값이 비어 있어 지역 코드로 분기를 시연할 수 없다. 기어 D에서도 DO 선언 앱은 종료되지 않는다(프로젝트 실측).
  추가 규칙: 동영상·게임·브라우저는 미디어 앱으로 정차 중에만 쓸 수 있고 주행이 시작되면 UI를 숨기고 일시정지해야 하며 주행 중 재개할 수 없다. 운전과 무관한 헤드업 알림은 금지다. UX 원칙은 1~2초 안에 파악되는 정보, 큰 터치 영역, 고대비, 텍스트 입력 대신 프리셋·최근 항목·음성이다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **platform-02** Vehicle SDK는 19개 도메인(도어, 창문, 와이퍼, 공조, 좌석, EV 배터리, 연료, 주행거리, 크루즈, 조향각, 조명, 방향지시등, 타이어, 안전 경고, ETCS, 차량 정보 등)의 상태를 capability, get, register(리스너) API로 제공하고, 조회 퍼미션은 도메인별 CAR_* 하나씩이다.
  - 근거: [A] Vehicle SDK API 인덱스와 요구 퍼미션 — `pleos-claude-kit/API_INDEX.md (### seat, safety, ev-battery, door, h-v-a-c, odometer, driving-mode, steering-wheel, tire, toll 절)` (2026-10-24); [A] Vehicle SDK 소개·퍼미션·Capability — `pleos-claude-kit/docs/pleos-only/connect-sdk-pleos/Vehicle/intro.md, vehicle-permission.md, check-capability.md` (2026-10-24); [A] Pleos Connect SDK 릴리즈 노트 — `pleos-claude-kit/docs/pleos-only/release-note/sdk-261015.md (Vehicle SDK v2.3.2)` (2026-10-19); [A] 에뮬레이터 실측 함정 목록 — `프로젝트 CLAUDE.md '실제로 확인한 함정'(VehicleException 난독화, 에뮬레이터 기본값)` (2026-10-24)
  - 시사점: 정차 중 '동승자 좌석·안전벨트 + EV 충전·전비 + 도어·창문·공조' 같은 신호 조합으로 차량 특화 아이디어를 설계할 수 있다. 기획서 5번에는 도메인별로 쓰는 API와 CAR_* 퍼미션을 최소 단위로 적는다. 모델별 차이 때문에 capability 확인과 미지원 시 UI 비활성 흐름을 시나리오에 넣는다.

- **platform-04** 에뮬레이터 Virtual Console은 신호 63개(Convenience 32, Climate 11, Energy 9, Motion 6, ADAS 2, Safety·Vehicle·기타 각 1)를 설정할 수 있고 속도는 /vehicle/controls?speed= 로만 바꾼다. 안전벨트, 연료, 오일·워셔액 경고 같은 일부 Vehicle SDK 신호는 대응 카드가 없다.
  - 근거: [A] [Virtual Console 카탈로그 63개 기능](http://127.0.0.1:8088/vehicle/catalog) (2026-10-24); [A] Pleos Connect Virtual Console 사용 가이드 — `pleos-claude-kit/docs/pleos-only/app-test-guide/virtual-console-guide.md` (2026-10-24); [A] Virtual Console 셋업 가이드 — `pleos-claude-kit/docs/connect/guide/getting-started/application-development/use-vehicle-emulator.md` (2026-10-24); [A] 프로젝트 정리 자료(신호 63개, 속도 제어) — `.claude/skills/pleos-idea-evaluation/references/platform-capability.md §4, 프로젝트 CLAUDE.md '주행 상태와 차량 신호 바꾸기'` (2026-10-24)
  - 시사점: 시연 시나리오는 카탈로그 63개 신호와 속도 제어로 재현되는 값으로만 구성한다. 좌석 점유, 도어·창문, EV SOC·충전, 공조, 방향지시등, 기어 P/D 전환은 시연에 쓸 수 있다. 안전벨트, 연료, 오일류 경고에 기대는 시나리오는 Fake 데이터로 대체하거나 제외하고, 기획서의 시연 시나리오에 재현 가능성을 명시한다.

- **platform-05** NaviHelper는 목적지 설정·취소, 경로 옵션 변경, 재탐색, 경유지 관리와 경로·TBT·도착·현재 위치 이벤트 수신을 지원하지만, 요청은 내장 Navigation 앱에 위임되고 결과는 리스너 콜백으로만 오며 실제로 오는 이벤트는 Navigation 앱 구현에 달려 있다.
  - 근거: [A] NaviHelper SDK 가이드 — `pleos-claude-kit/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro.md, destinations.md, routes.md, event-listeners.md` (2026-10-24); [A] NaviHelper API 인덱스 — `pleos-claude-kit/API_INDEX.md (### navi-helper, navi-helper-event-listener, trip-info, destination-info, route-info)` (2026-10-24); [A] Maps/Navigation 앱 개발 가이드(단일 경로 정책) — `pleos-claude-kit/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide.md` (2026-10-24); [A] API 호환성 및 가용성, FAQ — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability.md; docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq.md ('내비게이션 탑재 여부')` (2026-10-24); [A] NaviHelper 3.0.5 변경 — `pleos-claude-kit/docs/pleos-only/release-note/sdk-261015.md (NaviHelper v3.0.5)` (2026-10-19); [A] Navigation 앱 설치 확인, 현재 위치 실측 — `에뮬레이터 pm list packages(2026-10-24, 읽기 전용), 프로젝트 CLAUDE.md '실제로 확인한 함정'` (2026-10-24)
  - 시사점: 경로 요청을 앱 기능의 마지막 단계로 위임하는 설계(충전소·목적지 제안 후 requestRoute)는 정책에 맞고 기술 활용성의 근거가 된다. 자체 길안내·TBT UI나 음성 안내는 정책 위반 위험이 있어 피한다. 목적지 검색이 필요한 아이디어는 NaviHelper만으로 안 되므로 POI 확보 방법(즐겨찾기·최근 목적지 조회 등)을 따로 설계하고 poiId 문제는 스파이크로 확인한다.

- **platform-06** Gleo AI 연동은 앱이 assets/schema.json(App Schema)에 음성으로 호출될 function을 최대 3개 등록하고 Gleo가 intent로 앱을 호출하는 방식이다. 활성화는 Pleos의 스키마 검토·승인 뒤이고, 에뮬레이터에서 제3자 앱 호출이 실제로 되는지는 확인되지 않았다.
  - 근거: [A] Gleo Interface 가이드(App Schema, Callback, 전달 절차) — `pleos-claude-kit/docs/pleos-only/app-integration-guide/guide/core-concepts.md, app-capability.md, action_callback.md, deliver-schema.md, examples.md` (2026-10-24); [A] LLM SDK 제공 종료 릴리즈 노트 — `pleos-claude-kit/docs/pleos-only/release-note/playground-261015.md; emulator-261015.md ('Removed Features')` (2026-10-19); [A] 오디오 포커스 개발 가이드 — `pleos-claude-kit/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development.md (Gleo 호출 시 포커스)` (2026-10-24); [A] Gleo 연동 정리와 미확인 항목 — `pleos-claude-kit/CLAUDE.md §3.6, §6.11; 프로젝트 CLAUDE.md '실제로 확인한 함정'` (2026-10-24); [A] Gleo AI 구성요소 존재 확인 — `에뮬레이터 pm list packages, logcat 조회(2026-10-24, 읽기 전용)` (2026-10-24)
  - 시사점: Gleo 음성 호출은 차별점이 될 수 있지만 승인과 에뮬레이터 호출 여부가 불확실하므로 핵심 흐름은 화면 조작만으로도 완결되게 하고, Gleo는 function 3개 이내의 보조 진입점으로 설계한다. 시연은 adb intent로 모사할 수 있다(키트의 추론). 기획서에는 스키마 승인 뒤 활성화된다는 전제를 밝힌다.

- **platform-08** 심사에서 즉시 반려되는 대표 조건은 카테고리와 실제 기능의 불일치, 승인되지 않은 Vehicle 퍼미션 선언, 주행 중 미디어·영상 사용이다. 성능 기준은 주행 중 비내비 앱 CPU 5% 미만, RAM 400MB 이하, 터치 피드백 0.25초, 실행·로딩 10초이고, 화면은 portrait와 landscape 모두에서 기능이 같아야 한다.
  - 근거: [A] 앱 카테고리 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/app-category.md` (2026-10-24); [A] Maps/Navigation 앱 개발 가이드 — `pleos-claude-kit/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide.md ('금지 정책')` (2026-10-24); [A] 성능·기능 안정성·디스플레이 호환성 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt.md; distribute/app-review-guide/review-functionality-performance-stability.md` (2026-10-24); [A] 앱 구조 및 동작 모델, 보안·개인정보 설계 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model.md; security-privacy.md` (2026-10-24); [A] 앱 심사 제출 항목 — `pleos-claude-kit/docs/pleos-only/distribute/app-submission-guide.md` (2026-10-24); [A] 공모전 심사·시상, 유의사항 — `pleos-claude-kit/contest/CONTEST_SOURCE.md (guide/judging-awards, 공모전 참가 및 출품 유의사항 안내)` (2026-10-24)
  - 시사점: 기획서에는 카테고리를 하나 정하고 그 카테고리가 주행 중 허용하는 범위 안에서 기능을 설계한다. 허용 예외가 없는 Lifestyle, Entertainment, Business, Kids, Social 계열은 정차 중심으로 기획한다. 성능·화면 기준은 구현 단계 QA 체크리스트로 가져가고, 2차 PT 시연 가능성(에뮬레이터 구동)을 아이디어 선택 기준에 넣는다. 마켓 입점은 보장되지 않으므로 사업성 서술에서 입점을 전제하지 않는다.

### 조사 공백
- Playground 콘솔 승인 상태는 공식 문서(create-my-project)와 키트에 기록된 사용자 콘솔 화면(2026-10-24)을 근거로 했다. 이 조사에서 콘솔에 직접 로그인해 재확인하지 못했다. 또한 'Vehicle State'로 승인된 퍼미션의 구체적 목록은 공식 문서에 없다.
- FusedLocation, TextToSpeech/SpeechToText(ON_DEVICE), Vehicle set* 제어가 에뮬레이터에서 실제로 동작하는지 검증하지 못했다. 서비스 패키지(ai.pleos.playground.service.location.fused, ai.pleos.playground.caas) 설치만 확인했다.
- AAOS CarPropertyManager의 PERF_VEHICLE_SPEED 읽기 권한이 마켓 심사에서 승인되는지, 에뮬레이터에서 값이 나오는지 확인하지 못했다. 이 경로가 막히면 속도 기반 기능은 성립하지 않는다.
- Gleo가 에뮬레이터에서 제3자 앱 schema.json의 액션을 실제로 호출하는지, 스키마 검토·승인을 누구에게 어떻게 요청하고 얼마나 걸리는지 확인하지 못했다(문서는 문서 저자에게 문의하라고만 안내). 설치 시 [GAA]가 schema를 스캔한다는 프로젝트 기록은 오늘 로그에서 재확인하지 못했다.
- NaviHelper: 경로 요청 후 위치 이동·TBT·도착 이벤트가 에뮬레이터 Navigation 앱에서 나오는지, poiId의 출처와 임의값 허용 여부, requestTrip이 안내를 시작하는지 확인하지 못했다.
- 안전벨트 체결, 연료, 오일·워셔액 경고 등 Virtual Console 카탈로그에 카드가 없는 신호는 에뮬레이터 지원 여부를 확인하지 못했다. 도메인별 area 지원 범위도 미확인이다.
- 한국(product.sw.target_region=KOR) 전용 주행 규제 표가 없고 '그외 지역' 적용은 추정이다. 공식 표와 에뮬레이터 설정값의 대용량 텍스트(LIMIT_CONTENT, LIMIT_STRING_LENGTH) 불일치의 원인을 알 수 없고, 에뮬레이터는 지역 속성이 비어 있어 실차 값을 재현하지 못한다.
- Telltales API의 권한 표기가 API_INDEX 안에서 서로 다르다(@RequiresPermission은 CAR_SAFETY, perm 항목은 PRIVATE_CAR_SAFETY). 일반 앱에 실제로 허용되는지는 확인하지 못했다.
- 기준일(2026-10-24)이 10/28 온라인 설명회보다 앞서 SDK 사용 범위(특히 준비 중 SDK)에 대한 공식 답변과 2차 PT 심사 기준이 아직 없다. 공모전 공지 3건은 본문 날짜가 보이지 않았다.
- 질문 해석: '승인 상태'는 Playground 콘솔의 SDK 사용 신청 상태로, '에뮬레이터에서 확인된 범위'는 키트·프로젝트 CLAUDE.md 기록과 이번 조사의 읽기 전용 점검(Virtual Console 카탈로그 GET, dumpsys, pm list, getprop, logcat 조회)으로 좁혀 해석했다. 앱 설치나 SDK 호출 같은 상태를 바꾸는 실험은 하지 않았다.
- 웹 조사는 공식 사이트로 한정해 document.pleos.ai 3개 페이지와 appdevchallenge.auto-hmg.io 2개 페이지만 확인했다. 키트와 같은 내용을 재확인했고 새로 발견한 차이는 없다. 외부 블로그·커뮤니티 자료는 Pleos 관련해 거의 없어 조사하지 않았다.
- 공모전 사이트 문구(차량 속도 변경 가능, 5개 SDK를 참가자용으로 소개)와 공식 SDK 문서(속도 카드 없음, 일부 SDK 준비 중)가 서로 어긋나는데, 어느 쪽이 심사 평가 기준인지는 확인하지 못했다.

## 운전자·동승자 페인포인트 (`user-needs`)

### ✅ 확인된 사실

- **user-needs-01** 2026년 6월 말 국내 등록 자동차는 26,676,359대(국민 1.92명당 1대)이고 전기차는 1,095,218대(4.1%)로, 2025년 말 899,101대보다 21.8% 늘었다
  - 근거: [B] [전체 자동차 2668만대…인구 1.92명당 1대 (뉴시스, 국토교통부 자료 인용)](https://mobile.newsis.com/view/NISX20260729_0003728744) (2026-07-30); [C] [전기차 누적등록 첫 100만대 돌파…친환경차 13% 급증 (클리앙 게시글, 국토부 수치를 옮김. 수치 근거로는 쓰지 않음)](https://www.clien.net/service/board/news/19238114) (2026-07-30)
  - 검증 메모: 수치 자체는 맞다. 근거만 고치면 된다. 일 단위 수치의 B등급 출처는 뉴시스가 아니라 경북매일(https://kbmaeil.com/article/20260730500195, 2026-07-30)이다. 뉴시스는 천 단위 반올림 값(약 2667만6000대, 109만5000대 등)으로만 인용할 수 있다. 기획서에는 '약 2,668만 대, 전기차 약 110만 대(4.1%)'처럼 반올림해 쓰거나, 일 단위로 쓸 때는 경북매일을 출처로 단다.
  - 시사점: 전기차 약 110만 대, 반기 증가분 약 20만 대는 충전 맥락 아이디어의 모수를 설명하는 근거로 기획서 6번에 인용할 수 있다. 다만 Pleos 탑재 차량은 이 중 일부이므로 예상 MAU는 market 축의 탑재 차종·출시 근거와 곱해서 추정해야 한다.
  <details><summary>상세</summary>

  국토교통부 통계를 인용한 보도다. 2026년 상반기 신규등록 857,404대 중 전기차는 199,019대(23%)다. 친환경차(전기·하이브리드·수소)는 3,953,287대(14.8%)로 반기 동안 13.1% 늘었고, 내연기관차는 22,544,226대로 1.3% 줄었다. 전체 등록 대수는 0.6% 늘었다. 국토부 원문 보도자료는 열지 못했다(검색에서도 korea.kr 원문이 나오지 않음). 뉴시스 본문(B)과 클리앙 게시글이 옮겨 적은 국토부 표(C)의 숫자가 서로 일치해서 수치 근거는 뉴시스를 쓴다.

  </details>

- **user-needs-02** 국내 통근 근로자의 하루 평균 출·퇴근 소요 시간은 73.9분이고 수도권은 82.0분으로 가장 길다(통계청, 2024년 12월 발표)
  - 근거: [A] [직장인 출퇴근 평균 '74분'···수도권 '82분'으로 최장 (KTV, 통계청 발표 게재)](https://m.ktv.go.kr/news/sphere/T000021/view?content_id=717396) (2024-12-23); [B] [수도권 직장인, 출퇴근에 매일 1시간 22분 (데일리안, 통계청 자료)](https://www.dailian.co.kr/news/view/1443575/%EC%88%98%EB%8F%84%EA%B6%8C-%EC%A7%81%EC%9E%A5%EC%9D%B8-%EC%B6%9C%ED%87%B4%EA%B7%BC-%EB%A7%A4%EC%9D%BC-1%EC%8B%9C%EA%B0%84-2-2024) (2024-12-20); [B] [통근자 평균 출·퇴근 시간 72.6분…수도권 83.2분 전국 최장 (뉴시스, 2023년 6월 기준)](https://www.newsis.com/view/NISX20231220_0002565890) (2023-12-21)
  - 검증 메모: 수치는 맞다. 인용할 때 두 가지를 고친다. (1) 출처는 '통계청(2024.12.20 발표, 가명결합 기반 실험적 통계)'로 적는다. (2) 대상은 '통근 근로자 약 1,220만 명, 이동 수단 구분 없음'으로 밝히고, '이동 시간'으로 표현한다. 2025년 이후 발표분이 있는지는 국가데이터처 누리집에서 한 번 더 확인할 필요가 있다.
  - 시사점: 기획서 2번(기획 배경)에 '직장인은 출퇴근에 하루 약 74분, 수도권은 약 82분을 쓴다'로 인용할 수 있다. 이를 '차 안 시간'이나 '운전 시간'으로 바꿔 쓰면 사실과 달라지므로, 수단 구분이 없다는 점을 문장에 남기거나 '이동 시간'으로 표현해야 한다.
  <details><summary>상세</summary>

  통계청 '2024년 통근 근로자 이동 특성 분석 결과'다. 통계청 고용 행정자료와 통신 3사 기지국 위치자료를 가명결합했고 약 1,220만 명을 다뤘다(데일리안 보도). 출근 36.5분, 퇴근 37.4분(데일리안), 강원 57.7분으로 가장 짧고 30대 76.9분(KTV). 주의 1: 이동 수단 구분이 없어 승용차 운전자만의 시간이 아니다(두 기사 모두 수단별 분해 없음). 주의 2: 2023년 자료(72.6분, 수도권은 한 기사 안에서 83.2분과 82.2분으로 표기가 엇갈림, SK텔레콤 위치자료 기반)는 산출 방식이 달라 연도별 증감 비교에 쓰면 안 된다(2023년 자료). 2025년 발표분은 찾지 못했다. 통계청 원문(kostat.go.kr)은 열지 못하고 KTV 게재본과 언론 보도로 확인했다.

  </details>

- **user-needs-05** 전기차 충전의 가장 큰 불편을 두 조사가 다르게 꼽는다. 2025년 GS차지비 발표 조사는 충전기 고장(53%)과 수 부족(47%)이 상위이고, 한국환경공단 조사는 충전시설 부족(38.6%)과 충전질서 부족(21.2%)이 상위이며 고장은 14.3%다
  - 근거: [B] [전기차 충전기 사용자 경험 개선 토론회 보도 (GS차지비 소개 설문 1,400명)](https://kidd.co.kr/news/245319) (2026-03-18); [B] [한국환경공단 '전기차 및 충전 인프라 보급 확대를 위한 사용자 설문조사 보고서' 보도 (경북매일)](https://kbmaeil.com/article/202405260394867) (2024-05-26); [B] [전기차 충전기 보조금, '설치'에서 '운영'으로 전환해야 (뉴스1, 부제만 확인)](https://www.news1.kr/industry/auto-industry/6105532) (2026-03-18)
  - 검증 메모: 주장은 유지한다. 세부만 고친다. (1) GS차지비 조사는 '2025년 GS차지비가 실시하고 2026-03-18 국회 토론회에서 발표한 조사'로 적는다. '2025년 발표'로 읽히지 않게 하기 위해서다. 한국경제 기사(2026-03-18)를 근거로 추가하고, 고장은 52.7%(kidd 기사는 반올림해 53%로 표기)로 쓴다. 결제 오류 21.3%와 긴 대기시간 13.3%도 같은 조사 항목이다. (2) 환경공단 조사는 '2023년 12월~2024년 1월 조사, 2024년 5월 공개'로 시점을 밝힌다.
  - 시사점: 충전 불편이 '충전소를 못 찾거나 모자람'인지 '고장·점유 같은 운영 품질'인지에 따라 아이디어 방향이 갈린다. 기획서에는 한 조사의 수치만 단정해 인용하지 말고, 부족·고장·질서 세 갈래로 서술하거나 두 조사를 병기해야 한다. 충전 대기·점유를 겨냥한 앱은 정차 상태에서 깊은 상호작용을 설계할 수 있다.
  <details><summary>상세</summary>

  GS차지비 발표 조사는 전기차 사용자 1,400명, 2025년, 복수응답이며 고장 53%, 수 부족 47%, 불편한 위치 29.1%, 느린 충전 속도 27.4%다. 2026-03-18 국회 토론회에서 GS차지비 김정욱 대표가 소개했고 조사 수행기관과 표본 설계는 기사에 없다. 이 수치는 kidd.co.kr 기사에서만 확인했고, 뉴스1과 시대일보 기사 본문에는 수치가 없었다. 한국환경공단 보고서(하이데이터 수행)는 17개 시도 전기차 사용자 1,000명 인터넷 조사이고 조사 시기는 기사에 '지난해 12월 19일~1월 4일'로만 적혀 2023년 12월~2024년 1월로 추정된다. 충전시설 부족 38.6%, 충전질서 부족 21.2%, 기기 고장 14.3%, 충전 속도 9.2%, 충전 비용 8.7%(합 92.0%, 기준 모수와 복수응답 여부는 기사에 없음). 문항 구성이 달라 직접 비교할 수 없어 충돌로 남긴다. 뉴스1 토론회 기사 부제는 '충전기 1기당 전기차 2대씩 가능…충전기 양보다 질'이다(본문 수치는 미확인).

  </details>

### ○ 미검증 참고(정성 근거로만)

- **user-needs-03** 2025년 자동차 1대당 일평균 주행거리는 37.8km로 전년(38.7km)보다 2.3% 줄었고, 사업용(택시·버스·화물 등)은 94.7km로 비사업용(30.8km)의 약 3.1배다
  - 근거: [B] [2025년 자동차 일평균 주행거리 37.8km (파이낸셜뉴스, 한국교통안전공단 자료)](https://www.fnnews.com/news/202606291005221113) (2026-06-29)
  - 시사점: 업무 운전(택시·대리·화물)은 개인 차량보다 하루 주행거리가 3배 이상이라 차 안에서 보내는 시간과 대기 시간이 길 가능성이 크다(추정). 업무 운전자 페르소나를 세울 때 정량 근거로 쓸 수 있다. 개인 비사업용 차량은 하루 30.8km라 '차 안에서 오래 보낸다'는 서사를 과장하지 않도록 한다.

- **user-needs-04** 자가용 소유자 10명 중 9명은 하루 2시간 미만만 차를 쓰고, 소유자의 68%는 주 10시간 미만 이용한다고 답했다(쏘카 조사, 2023년)
  - 근거: [B] [쏘카 '2023 자가용 인식 조사' 보도 (ZDNet Korea)](https://zdnet.co.kr/view/?no=20230515084711) (2023-05-15)
  - 시사점: 개인 차량의 차 안 시간 총량은 길지 않으므로, '차 안 시간이 많다'가 아니라 충전 대기·픽업 대기처럼 특정 순간을 겨냥하는 편이 근거와 맞는다. 정차 시간 규모를 기획서에 쓰려면 이 조사를 단정적으로 인용하지 말고 근거 부족을 인정하거나 자체 가정으로 명시해야 한다.

- **user-needs-06** 충전소 지도 앱으로 풀리지 않는 충전 불편으로 충전이 끝난 차의 구역 점유와 고장 현황 정보의 부정확성이 반복해서 보고된다
  - 근거: [B] ['얌체' 전기차 충전 사라지려나? '80%·50분'까지만 (SBS Biz)](https://biz.sbs.co.kr/amp/article/20000105448) (2023-02-27); [B] [[디깅노트] 환경부 전기차 충전기 고장률이 1% 미만이라고? 알고보니 부실자료 (블로터)](https://v.daum.net/v/KaeteVD7rT) (2023-06); [B] [한국환경공단 전기차 사용자 설문조사 보고서 보도 (충전질서 부족 21.2%)](https://kbmaeil.com/article/202405260394867) (2024-05-26); [C] [지붕있는 급속충전소는 좋은데 나쁩니다 (클리앙 굴러간당)](https://www.clien.net/service/board/cm_car/19222348) (2026-07-07)
  - 시사점: 충전 대기열, 충전 완료 알림과 자리 비움 안내, 고장 현황 공유처럼 지도 앱에 없는 실시간 상태를 다루는 아이디어의 페인포인트 근거로 쓸 수 있다. 다만 충전 상태나 충전 구역 점유 정보를 Pleos SDK로 읽을 수 있는지는 이 조사 범위 밖이므로 platform 축에서 확인해야 한다.

- **user-needs-07** 65세 이상 운전면허 소지자는 2019년 3,337,165명에서 최근 4,747,426명으로 42.3% 늘었고, 75세 이상은 794,285명에서 1,000,906명이 됐다
  - 근거: [B] [75세 이상 운전자 첫 100만명 시대…“재시험 수준으로 적성검사 강화해야” (서울신문, 경찰청 자료)](https://m.seoul.co.kr/news/2024/03/20/20240320010008) (2024-03-20)
  - 시사점: 고령 운전자의 규모와 증가 속도는 기획서 배경에 인용할 수 있고, 큰 글씨·음성 중심·단순 조작 같은 설계 요구의 이유가 된다. 그러나 고령 운전자의 구체적 페인포인트 근거는 부족해서 이 페르소나만으로 아이디어를 채택하기에는 근거가 약하다.

- **user-needs-08** 2025년 국내 반려동물 양육 가구 비율은 29.2%로 농림축산식품부 조사에서 역대 최대로 발표됐지만, 반려동물·영유아 동승 시 차 안 불편을 직접 측정한 자료는 찾지 못했다
  - 근거: [B] [2025년 국내 반려동물 양육가구 비율 29.2%..역대 최대 (데일리벳, 농림축산식품부 조사)](https://www.dailyvet.co.kr/v/273697) (2026-02-18); [B] [첫 카시트 고를 때 확인할 점 (세계일보, 한국소비자원 과거 조사 인용)](https://www.segye.com/newsView/20260609514362) (2026-06-09)
  - 시사점: 반려동물 동승 아이디어는 타깃 규모(양육 가구 약 3분의 1에 가까운 29.2%)의 근거는 있으나 '차 안에서 겪는 불편'의 근거가 없다. 이 방향을 쓰려면 커뮤니티나 인터뷰 같은 정성 근거를 따로 확보해야 하고, 그전에는 기획서의 페인포인트 근거로 단정하면 안 된다.

### 조사 공백
- 정차·신호 대기·충전 대기처럼 주행하지 않는 시간의 국내 공식 통계를 찾지 못함. 차량 이용 시간은 쏘카 민간 조사(2023년)만 있고, 질문 2의 '하루 차량 이용 시간'은 일평균 주행거리(TS)와 쏘카 설문으로 대신 답했다.
- 승용차 운전자만의 통근 시간과 전국 승용차 통근 분담률을 찾지 못함. 통계청 통근 시간(73.9분)은 이동 수단 구분이 없고, 2025년 발표분도 확인하지 못함.
- 하루 평균 운행 '시간' 통계를 찾지 못함. 한국교통안전공단 자료는 주행거리만 있고 산출 방식과 표본이 기사에 없음.
- 급속충전 1회 평균 소요 시간과 충전 대기 시간의 국내 실측 통계를 찾지 못함. 2025년 컨슈머인사이트·한국소비자원 충전 대기 설문도 찾지 못함.
- 주차: 최신 전국 운전자 설문과 주차면수 통계를 찾지 못함. 서울 주택가 주차장 확보율 63.6%는 2020년 자료(2022년 보도)라 인용하지 않음. 주차 관련 아이디어는 근거가 비어 있다.
- 업무 운전(배달·영업·대리운전)의 대기 시간과 불편 통계를 찾지 못함. 사업용 차량 주행거리(TS)만 확보함.
- 전국 운전면허 소지자 총수와 2025~2026년 고령 면허 소지자 최신치를 찾지 못함. 고령 수치는 경찰청 자료를 옮긴 2024년 기사(기준 연도 2023년 추정)이다.
- 아이·반려동물 동승 시 차 안 불편을 직접 조사한 자료를 찾지 못함. 보배드림 글과 앱 스토어 리뷰는 열람하지 못했고, 클리앙 게시글 2건만 C등급 정성 근거로 열람함.
- 국토교통부 자동차 등록 원문 보도자료와 통계청 통근 원문(kostat.go.kr)은 직접 열지 못하고 언론·KTV 게재본으로 확인함. fact-checker가 원문 대조를 해야 한다.
- 검색 도구 요약이 열람하지 않은 출처에 수치를 붙인 사례가 있어(GS차지비 설문 수치가 뉴스1 본문에는 없음, 카시트 착용률 66.10%가 세계일보 본문에 없음) 직접 열어 본문을 확인한 수치만 채택하고 66.10%는 버렸다.
- Pleos SDK로 충전 상태, 충전소 점유, 동승자 인식 같은 신호를 읽을 수 있는지는 이 축의 범위 밖이라 확인하지 못함. platform 축 결과와 대조해야 한다.

## 차량용 앱·인카 서비스 시장과 경쟁 (`market`)

### ✅ 확인된 사실

- **market-01** Pleos Connect 탑재 차종은 국내 현대차 3개(더 뉴 그랜저, 디 올 뉴 아반떼, 디 올 뉴 투싼)이고, 현대차그룹의 공식 목표는 2030년까지 약 2천만 대다
  - 근거: [A] [현대차그룹, 차세대 인포테인먼트 '플레오스 커넥트'로 차량 경험 혁신](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [A] [Hyundai Motor Group launches Pleos software brand (Pleos 25)](https://www.hyundaimotorgroup.com/en/news/hyundai-motor-group-launches-pleos-software-brand-unveiling-new-sdv-technologies-and-collaborations) (2025-03-28); [B] [플레오스 커넥트 현대차·기아·제네시스 순차 적용 보도](https://dealsite.co.kr/articles/161078) (2026-04-30); [B] [디 올 뉴 아반떼 계약 개시, 플레오스 커넥트 전 트림 기본 적용](https://biz.newdaily.co.kr/site/data/html/2026/08/05/2026080500107.html) (2026-08-05); [B] [신형 투싼에 LLM 비서(글레오 AI)](https://inews24.com/view/2011170) (2026-10-02); [B] [현대차, 6년 만에 완전변경 투싼 계약 개시](https://zdnet.co.kr/view?no=20261021085544) (2026-10-21); [A] Pleos App Market 탑재 준비 지원 조건 — `pleos-claude-kit/contest/CONTEST_SOURCE.md 심사·시상(수상팀 지원)` (2026-10-24); [A] API 호환성 및 가용성 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability.md` (2026-10-24)
  - 검증 메모: 국내 3종이라는 사실은 맞다. 다만 전 세계 탑재 차종은 3종이 아니다. 현대차 유럽법인이 2026-04-20 공개한 아이오닉 3(A, Hyundai Motor Europe 보도자료. 지디넷 2026-04-21 B)는 '유럽 판매 모델 최초로 플레오스 커넥트 적용' 차량이다. 튀르키예 생산 유럽 전략 차종이라 국내 출시 가능성은 낮다고 보도됐다. 투싼도 미국에 2027년형으로 Pleos Connect를 탑재해 나온다는 보도가 있다. 그래서 '확장 가능성'은 '국내 3종(그랜저·아반떼·투싼) + 유럽 아이오닉 3'로 써야 한다. 기아 시기는 공식 보도자료에는 없다. 다만 정원정 기아 부사장이 2026-06-26 부산모빌리티쇼에서 "올해 플레오스 커넥트를 새로 적용할 기아 신차는 없다", "기아에는 내년 신차부터 적용될 예정"이라고 말했다(데일리안 B). 따라서 '기아는 2027년 신차부터(임원 발언, 보도 기준)'로 쓸 수 있다. 블로터 2026-09-15(B)는 2027년 하반기 출시 예정인 PV7에 플레오스 커넥트가 적용된다고 보도했다. 그러나 같은 부사장은 PBV(PV5·PV7·PV9)에는 전용 인포테인먼트를 쓴다고 해 두 출처가 충돌한다. PV7은 확장 근거로 쓰지 않는다. 제네시스의 차종별 시기는 찾지 못했다. 더 뉴 그랜저의 정식 출시일은 2026-05-14다.
  - 시사점: 기획서의 '확장 가능성'은 3개 차종 탑재 사실과 2030년 2천만 대 목표(장기 계획임을 명시)로 쓸 수 있다. 기아·제네시스 확대 시점을 단정하지 말고, 수상 후 탑재가 보장되지 않는다는 조건도 리스크에 적어야 한다.
  <details><summary>상세</summary>

  더 뉴 그랜저가 2026-05 첫 적용 차량이다(2026-04-30 현대차그룹 발표). 디 올 뉴 아반떼는 2026-08-05 계약을 시작했고 전 트림에 기본 적용된다. 디 올 뉴 투싼은 2026-10-01 뉴욕 월드 프리미어에서 공개됐고 현대차 SUV 최초 적용이며, 2026-10-21 계약을 시작했고 기본 트림부터 적용된다. 투싼 출고 일정은 찾지 못했다. 현대차그룹은 '전 세계 현대차·기아·제네시스 차량에 순차 적용'하겠다고 밝혔으나 기아·제네시스의 차종별 시기는 공식 자료에서 확인하지 못했다. 같은 부산모빌리티쇼(2026-06-26) 전시 EV(아이오닉 5·9, 코나 일렉트릭 등)는 Pleos Connect 탑재 차량으로 설명되지 않았다. 공모전 원문은 수상팀의 앱 마켓 탑재 준비 지원이 '지원 차종·지역 조건을 거치며 입점·배포를 보장하지 않는다'고 명시한다. 키트 문서도 SDK API 지원이 차량 모델별로 다르다고 적는다.

  </details>

- **market-02** 예상 MAU 추정에 쓸 수 있는 공식 근거는 차종별 계약·판매 대수뿐이며, Pleos 앱마켓의 MAU·다운로드·누적 탑재 대수는 공개된 값이 없다
  - 근거: [A] [더 뉴 그랜저 출시 첫날 계약 10,277대](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-new-grandeur-launch-10000-orders) (2026-05-15); [B] [현대차 9월 국내 판매 49,022대, 그랜저 8,898대·아반떼 7,305대](https://www.insight.co.kr/news/576191) (2026-10-01); [B] [디 올 뉴 아반떼 계약 개시(출고 일정)](https://biz.newdaily.co.kr/site/data/html/2026/08/05/2026080500107.html) (2026-08-05); [A] [포티투닷 앱 마켓 신규 앱 7종(사용자 수 언급 없음)](https://www.hyundaimotorgroup.com/ko/news/42dot-pleos-connect-app-market-new-apps) (2026-09-09); [A] 사업성: 예상 MAU, Retention Rate — `pleos-claude-kit/contest/CONTEST_SOURCE.md 심사·시상(사업성)` (2026-10-24)
  - 검증 메모: 판정을 바꿀 정도는 아니고 보완할 점만 있다. 9월 차종별 판매 수치는 B등급 언론 보도로 확인했다. 기획서에 인용할 때는 현대차 월간 판매실적 공식 보도자료(A)로 바꾸는 것이 좋다. 그랜저의 트림별 Pleos Connect 기본 적용 여부는 공식 자료에서 확인하지 못했다. 그러니 퍼널의 첫 항은 '탑재 추정 대수'로 두고 가정임을 밝힌다. 해외 탑재(유럽 아이오닉 3, 미국 투싼)와 기아의 2027년 적용(임원 발언)은 확대 계획에 넣을 수 있다. 다만 판매 대수가 공개되지 않았으므로 수치로 쓰지 않는다.
  - 시사점: MAU는 '누적 판매 대수 x 앱마켓 활성화율 x 앱 카테고리 이용률'처럼 가정을 드러낸 퍼널로 쓰고, 판매 대수만 출처 수치로 인용한다. 활성화율·이용률은 근거 없는 수치를 넣지 말고 '가정'으로 표시해야 한다. 공식 앱마켓 사용자 통계가 없다는 점 자체를 보수적 추정의 이유로 밝힌다.
  <details><summary>상세</summary>

  더 뉴 그랜저는 출시 첫날 계약 10,277대(2026-05-15 발표, 가솔린 58%·하이브리드 40%)였다. 2026년 9월 국내 판매는 그랜저 8,898대, 아반떼 7,305대(합 16,203대)로 현대차 국내 판매 49,022대의 약 3분의 1이다. 다만 9월 아반떼 판매에 구형 재고가 포함됐는지, 그랜저·아반떼 판매가 모두 Pleos Connect 탑재분인지는 출처에서 확인하지 못했다. 아반떼는 가솔린 출고가 3분기, 하이브리드 출고는 친환경차 고시 이후라 판매량과 인도량이 다를 수 있다. 앱 마켓 신규 앱 발표(2026-09-07)에도 사용자·다운로드 수치는 없었다. 공모전 사업성 심사항목은 타깃 고객 분석, 예상 MAU, Retention Rate, 확대 계획을 요구한다.

  </details>

- **market-03** Pleos App Market은 2026-09 기준 국내 18종이며 내비게이션·음악·영상 대형 서비스 중심이고, 마켓 자체는 주행 중 사용할 수 없고 Pleos 계정이 필요하다
  - 근거: [A] [포티투닷, 플레오스 커넥트 앱 마켓 신규 앱 7종 추가(총 18종)](https://www.hyundaimotorgroup.com/ko/news/42dot-pleos-connect-app-market-new-apps) (2026-09-09); [B] [포티투닷 앱 마켓 신규 7종, 국내 18종](https://www.fnnews.com/news/202609070949054267) (2026-09-07); [B] [포티투닷, 플레오스 커넥트에 외부 앱 7개 업데이트](https://www.wikitree.co.kr/articles/1157711) (2026-09-07); [A] [플레오스 커넥트 공개(초기 입점 서비스)](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [B] [네이버 지도·검색·웨일 차량 안으로](https://www.heraldk.com/article/2026062517181680534) (2026-06-25); [A] [Pleos App Market(앱 마켓) 오너스 매뉴얼](https://ownersmanual.hyundai.com/ivi/Connect-S/AVNT/KOR/Korean/007_Vehicleconvenience_8.html) (2026-10-24); [A] 결제 시스템 마이그레이션(Payment SDK 준비 중) — `pleos-claude-kit/docs/pleos-only/in-prep/_payment-system-migration.md` (2026-10-24)
  - 검증 메모: 사실관계는 맞고, 보완할 점만 있다. (1) 헤럴드경제 기사는 웨일 같은 네이버 서비스가 앱 마켓으로 배포되는지 밝히지 않았다. 그러니 웨일을 '앱 마켓 18종 중 하나'라고 단정하지 않는다. 18종 전체 목록은 여전히 확인하지 못했다. (2) 시사점의 '이 밖의 카테고리는 선점 여지'에는 반대 근거를 함께 적어야 한다. 42dot은 같은 발표에서 '게임과 차량 관리, 라이프스타일 등의 앱을 순차적으로 추가'하겠다고 밝혔다. 따라서 차량 관리·라이프스타일 분야는 지금 비어 있을 뿐, 파트너사가 곧 진입할 수 있다. (3) 오너스 매뉴얼 페이지는 차종을 특정하지 않는다. 그러니 '정차 상태에서만 설치'는 Pleos Connect 공통 동작으로 쓰고, 차종별 차이는 단정하지 않는다.
  - 시사점: 경쟁 앱은 18종뿐이고 대부분 내비·음악이므로 이 밖의 카테고리는 선점 여지가 있다고 쓸 수 있다. 앱 탐색·설치는 정차 상태에서만 가능하다는 점을 사용자 여정(설치는 정차, 이용은 상황별)에 반영해야 한다. 유료화는 Payment SDK가 열리기 전에는 시연할 수 없다.
  <details><summary>상세</summary>

  포티투닷이 2026-09-07 신규 7종을 추가해 총 18종이 됐다. 신규는 내비/지도 3종(카카오내비, what3words, Make My Day Route Planner)과 음악/오디오 4종(멜론, Radioline, 래디오오토, 픽클)이다. 기존 앱은 네이버 오토·네이버 지도·웨일(2026-06 발표), YouTube, Spotify, SBS 고릴라, essential;, 지니가 언급됐으나 18종 전체 목록은 찾지 못했다. 현재 그랜저와 아반떼에서 이용 가능하다고 보도됐다(투싼 포함 여부는 미확인). 현대차 오너스 매뉴얼에 따르면 앱 마켓은 앱 라이브러리에서 열며 Pleos 계정(QR 로그인)이 필요하고, 주행 중에는 사용할 수 없어 P단에서 정차해야 한다. 유료 앱 구매 이력과 재설치 기능이 있다. 키트 문서는 Payment SDK가 준비 중이라고 적는다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **market-04** Google Play for AAOS의 차량용 앱 생태계는 Google 공식 발표 기준 300개 이상(2025-05)이고, 영상·게임·브라우저는 정차 중 실행하는 Car Ready 방식이 주류다
  - 근거: [A] [Android for Cars at Google I/O 2025](https://android-developers.googleblog.com/2025/05/android-for-cars-google-io-2025.html) (2025-05-20); [A] [Android Automotive OS app experience overview](https://docs.partner.android.com/drivingux/automotive-os/app-experience/overview) (2026-03-02); [A] [XPENG Expands Collaboration with Google to Bring Google Play into Its AI-Powered Cockpits](https://www.xpeng.com/pressroom/news/01a1446eef56a12024aa8a02829e01ed) (2026-10-15)
  - 시사점: 차량 앱의 글로벌 표준 구도(영상·게임은 정차 한정, 미디어·내비는 주행 허용)가 Pleos 정책과 같은 방향이라는 근거로 쓸 수 있다. 인기 앱 순위가 없으므로 '인기 앱 사례' 대신 카테고리 구조와 정차·주행 구분을 인용한다.

- **market-05** 해외 완성차의 인카 앱은 자체 스토어로 큐레이션하고, 영상·화상통화·게임은 정차 중으로 제한하는 패턴이 공통이다
  - 근거: [A] [The BMW iX3 ushers in next-generation innovations at CES 2026](https://www.press.bmwgroup.com/global/article/detail/T0454649EN/the-bmw-iX3-ushers-in-next-generation-innovations-at-ces-consumer-electronics-show-2026) (2026-01-05); [A] [XPENG Google Play integration](https://www.xpeng.com/pressroom/news/01a1446eef56a12024aa8a02829e01ed) (2026-10-15); [B] [Hands On With Mercedes' New Third-Gen MBUX Infotainment With Android Apps](https://www.motor1.com/features/654869/mercedes-benz-mbux-android-infotainment-feature/) (2023-02-28); [B] [Nio debuts SkyOS operating system and in-car App Store](https://m.arenaev.com/nio_debuts_skyos_operating_system_and_incar_app_store_for_its_evs-amp-2457.php) (2023-09-22); [B] [Tesla Theater, Arcade, and Toybox (검색 결과 요약으로만 확인, 직접 열람 403)](https://www.tesla.com/ownersmanual/2012_2020_models/en_gb/GUID-79A49D40-A028-435B-A7F6-8E48846AB9E9.html) (2026-10-24)
  - 시사점: 차량 앱의 차별화는 '정차 중 몰입형'과 '주행 중 보조형'을 나눠 설계하는 데 있다는 해외 근거로 쓸 수 있다. 해외 사례는 앱 수보다 어떤 상황에서 허용되는지가 핵심이므로 기획서에 1단·2단 화면 구분의 배경으로 인용한다.

- **market-06** 인카 구독 모델은 하드웨어 기능 잠금보다 소프트웨어·서비스형이 수용도가 높았고, Pleos의 개발자 대상 결제·수익 배분 조건은 아직 공개되지 않았다
  - 근거: [B] [BMW admits heated seat subscriptions were a mistake, but commits to ConnectedDrive](https://www.techspot.com/news/111208-bmw-admits-heated-seat-subscriptions-mistake-but-commits.html) (2026-02-05); [B] [Hyundai bringt Netflix, YouTube und mehr ins Elektroauto](https://insideevs.de/news/802164/hyundai-elektroautos-netflix-youtube/) (연도 미표기(07-20)); [B] [Tesla FSD reaches 1.48 million active subscriptions](https://www.notateslaapp.com/news/4483/tesla-fsd-reaches-148-million-active-subscriptions) (2026-07-23); [A] [Pleos App Market 유료 앱 재설치](https://ownersmanual.hyundai.com/ivi/Connect-S/AVNT/KOR/Korean/007_Vehicleconvenience_8.html) (2026-10-24); [A] Payment SDK 준비 중 — `pleos-claude-kit/docs/pleos-only/in-prep/_payment-system-migration.md` (2026-10-24)
  - 시사점: 수익 모델은 '이미 달린 하드웨어 기능을 잠그는 방식'을 피하고 서비스 가치형 구독으로 제안하며, Pleos 결제 정책이 미정이므로 기획서에 '플랫폼 결제 정책 확정 시 적용'이라는 전제를 적는다. 수익 규모 수치는 출처 없이 쓰지 않는다.

- **market-07** 인카 결제·커머스는 현대차가 Car Pay로 이미 주유·주차·F&B를 제공하고 있으나 이용자·결제액 성과 수치는 공개되지 않았다
  - 근거: [B] [현대차·기아, 현대백화점 주차요금 카페이 MOU](https://news.bizwatch.co.kr/article/industry/2023/12/12/0022) (2023-12-12); [A] [현대 카페이(인카페이먼트) 안내](https://www.hyundai.com/kr/ko/customer-service/hyundai-car-pay/hyundai-car-pay-information) (2026-10-24); [B] [GS칼텍스 비대면 주유 개시, 현대차도 신차에 카페이 탑재](https://magazine.hankyung.com/business/article/202006100348b) (2020-06-10)
  - 시사점: 결제·주문형 아이디어는 현대차 Car Pay와 기능이 겹칠 수 있으므로 '플랫폼 결제와 연동하는 보완 서비스'로 포지셔닝해야 한다. 성과 수치가 없으므로 기획서에서 시장 성과를 단정하지 말고, 결제 시연은 Payment SDK 공개 전까지 모의로 설계한다.

- **market-08** 현대차그룹이 공식으로 밝힌 앱 마켓 확장 우선순위는 게임·엔터테인먼트·차량 관리·라이프스타일이며, 초기 입점은 내비·음악·영상 대형 서비스가 차지했다
  - 근거: [A] [플레오스 커넥트 공개(앱 생태계 확장 계획)](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [A] [포티투닷 앱 마켓 신규 앱 7종(향후 계획)](https://www.hyundaimotorgroup.com/ko/news/42dot-pleos-connect-app-market-new-apps) (2026-09-09); [A] [Pleos 25: 앱 생태계 파트너](https://www.hyundaimotorgroup.com/en/news/hyundai-motor-group-launches-pleos-software-brand-unveiling-new-sdv-technologies-and-collaborations) (2025-03-28); [B] [네이버, 현대차 SDV에 올라탄다](https://www.heraldk.com/article/2026062517181680534) (2026-06-25)
  - 시사점: 내비·음악·영상은 대형 사업자와 정면 경쟁이라 피하고, 차량 관리·라이프스타일·정차 중 엔터테인먼트처럼 플랫폼이 '다음 확장 영역'으로 지목한 곳에서 차량 신호 연동으로 차별화하면 사업성 서술(확대 가능성)에 근거가 생긴다.

### 조사 공백
- Pleos Connect 누적 탑재 대수, Pleos App Market의 다운로드·MAU·활성 사용자 수는 공식 자료와 언론 어디에서도 찾지 못했다.
- Pleos App Market 18종의 전체 목록을 찾지 못했다. 신규 7종과 일부 기존 앱(네이버, YouTube, Spotify, SBS 고릴라, essential;, 지니)만 확인했다.
- 기아·제네시스의 Pleos Connect 적용 시기는 공식 확인에 실패했다. '2027년 기아 도입' 보도는 C등급 해외 매체와 직원 SNS뿐이라 근거에서 제외했다.
- Pleos App Market 정식 오픈일이 불명확하다. 현대차그룹 태그 페이지에 2026-03-25 '차량용 앱스토어 출시' 영상 항목이 있으나 다른 보도는 5월 그랜저 출시와 함께 시작했다고 한다.
- 그랜저·아반떼 월 판매량이 신형(Pleos 탑재)만인지 구형 재고 포함인지, 투싼 출고 시점과 앱 마켓 제공 여부를 확인하지 못했다. 6월·8월 월별 판매는 일관된 값을 못 찾았다.
- Pleos Payment SDK 사양, 앱 마켓 수수료·수익 배분 조건을 찾지 못했다(키트는 준비 중이라고만 적음).
- 인카 커머스·구독의 이용자·매출 성과는 GM Marketplace, Mercedes pay+ 등 모두 공개 수치를 찾지 못했다. 수요 조사(Visa/PYMNTS 등)는 오래됐거나 벤더 후원이라 근거로 쓰지 않았다.
- Google Play for AAOS 인기 앱 순위와 2025-05 이후 앱 수 갱신치, Li Auto 앱 생태계, Mercedes 현행 앱 수, NIO 현행 앱 수(2023년 값만 있음)를 찾지 못했다.
- Tesla 오너스 매뉴얼은 직접 열람이 403으로 막혀 검색 결과 요약으로만 확인했다. BMW 열선 시트 수용도 발언의 원문(2023년 기사)과 GM 소프트웨어 매출, BMW 이외 구독 수치는 직접 확인하지 못했다.
- 질문 5(예상 MAU 근거)는 공식 사용자 통계가 없어 '탑재 차종·판매·확대 계획'까지로 좁혀 해석했다. 웹 검색 도구가 영어권 위주라 한국 매체 일부는 검색 요약을 거쳐 본문을 직접 연 것만 근거로 인용했다.

## SDV·인카 AI·차량 데이터 기술 트렌드 (`tech-trends`)

### ✅ 확인된 사실

- **tech-trends-02** Pleos의 차량 데이터 개방은 두 갈래다. 차 안에서 쓰는 Vehicle SDK(국내 공모전에서 사용 가능한 쪽)와, 클라우드 Vehicle Data API(2026년 8월 기준 유럽에서만 제공)이며, 후자는 한국 앱 아이디어의 근거로 쓸 수 없다.
  - 근거: [A] [Vehicle Data API 소개 (지역: 2026년 8월 기준 유럽 한정)](https://document.pleos.ai/en/api-reference/vehicle-data-api/intro) (2026-08); [A] Vehicle State 사용 가능 / Vehicle Control 준비 중 / 퍼미션 사전 협의 정책 — `pleos-claude-kit/CLAUDE.md §2.2 사용 승인 상태` (2026-10-24); [C] [evcc: Hyundai EU Data Act (Pleos Vehicle Data API, Europe only)](https://docs.evcc.io/en/vehicles/hyundai-eu-data-act.md); [B] [The EU Data Act has taken effect: focus on automotive (검색 결과 요약만 확인, 원문 미열람)](https://www.mayerbrown.com/de/insights/publications/2025/11/the-eu-data-act-has-taken-effect-focus-on-automotive-and-cloud-providers) (2025-11-07)
  - 검증 메모: 수정 불필요. 기획서에서 출처를 적을 때는 'Pleos 공식 문서(2026년 8월 기준 유럽 한정, 2026-10-24 확인)'로 쓰는 것이 정확하다.
  - 시사점: '차량 데이터 API 개방'을 근거로 외부 서버나 클라우드 연동 기반 아이디어를 세우면 한국에서는 성립하지 않는다. 아이디어는 온디바이스 Vehicle SDK 조회(승인 없이 가능한 범위)와 NaviHelper에 걸어야 하고, 기획서 5번(개발 계획)에 Vehicle Data API를 쓴다고 쓰지 않는다. 기획 배경에서 개방 흐름을 말할 때도 유럽 한정이라는 점을 구분해야 한다.
  <details><summary>상세</summary>

  Pleos 공식 문서: Vehicle Data API는 현대·기아·제네시스의 차량 상태(충전, 파워트레인, 위치, 주행, 센서, 공조)와 기본 데이터를 OAuth 2.0으로 조회하는 개방 API다. 'As of August 2026'에 유럽에서만 제공되고 다른 지역은 준비 중이다. 개인 사용자는 동의 절차 없이 API 키만 받으면 즉시 쓸 수 있고, 사업자는 차주 동의 후 상태 정보는 즉시, 기본 데이터는 24시간 뒤부터 쓸 수 있다. 제3자 문서(evcc)도 유럽 한정이라고 하고, 차량이 제조사 서버에 마지막으로 보고한 값을 돌려줄 뿐 차를 깨우지 않는다고 한다. 유럽 쪽 배경은 2025-09-12부터 적용된 EU Data Act인데, 이 부분은 법무법인 요약만 보았고 원문과 EU 집행위 가이드는 열어 보지 못했다. 반면 공모전에서 쓰는 온디바이스 Vehicle SDK는 키트 기준 2026-10-24 콘솔에서 Vehicle State(조회 11개 영역)가 '사용 가능', Vehicle Control은 '준비 중'이다. 또 Vehicle SDK 퍼미션은 Pleos App Market과 사전 협의·승인된 범위에서만 선언할 수 있고 미승인 퍼미션은 심사 불통과다.

  </details>

- **tech-trends-05** 개발자가 Gleo AI와 연동하는 공식 경로는 Gleo Interface(App Schema)뿐이다. LLM SDK는 2026-10-19에 종료됐고, Gleo Interface에는 호출 수·응답 시간·승인 절차·검증 범위의 제약이 있다.
  - 근거: [A] Pleos Playground v2.6.0 릴리스 노트: LLM SDK 제공 종료 — `pleos-claude-kit/docs/pleos-only/release-note/playground-261015.md (https://document.pleos.ai/docs/pleos-only/release-note/playground-261015)` (2026-10-19); [A] App Schema 작성 가이드: actionCalls 최대 3개, 영어 description — `pleos-claude-kit/docs/pleos-only/app-integration-guide/guide/app-capability.md` (2026-10-24); [A] Gleo Action·Callback: 7초 후 취소, success/asking/fail — `pleos-claude-kit/docs/pleos-only/app-integration-guide/guide/action_callback.md` (2026-10-24); [A] Gleo에 스키마 전달: 검토·승인 후 활성화, 부팅·설치 시 스캔 — `pleos-claude-kit/docs/pleos-only/app-integration-guide/guide/deliver-schema.md` (2026-10-24); [A] Gleo가 에뮬레이터에서 3rd-party schema를 실제 호출하는지 미확인 — `pleos-claude-kit/CLAUDE.md §3.6, §6(11), §7 및 프로젝트 CLAUDE.md '실제로 확인한 함정'` (2026-10-24)
  - 검증 메모: '호출 수 제약'을 'App Schema에 정의할 수 있는 음성 function(actionCalls)이 최대 3개'로 고쳐 쓴다. 호출 빈도 제한은 문서에 없다. adb intent로 모사하는 시연은 키트가 [추론]으로 표시한 방법이므로, 에뮬레이터에서 직접 확인한 뒤 시나리오에 넣는다.
  - 시사점: 음성 호출은 보조 진입점으로 설계해야 한다. 핵심 가치는 화면 앱 안에서 성립시키고, Gleo 액션은 3개 이내의 짧은 명령(7초 내 응답)으로 정의한다. 시연 시나리오에는 adb 모사 경로를 미리 준비해야 하며, 승인 전에는 활성화되지 않으므로 음성 연동을 심사 핵심으로 약속하면 위험하다.
  <details><summary>상세</summary>

  (1) Playground v2.6.0(2026-10-19) 릴리스 노트가 LLM SDK 제공 종료를 공지하고 Gleo AI 연동은 Gleo Interface를 쓰라고 한다. (2) 앱은 assets/schema.json에 App Schema를 넣어야 하며 actionCalls는 최대 3개다. 모든 description은 영어로 쓰고, component는 activity(UI 실행) 또는 service(브로드캐스트)다. (3) Gleo는 7초 뒤 Action을 취소하고, 앱 응답 status는 success·asking·fail이다. (4) 스키마는 당사 가이드라인 검토 후 승인되면 통합이 자동 활성화되며, 스캔은 Gleo 앱 부팅 시 한 번과 앱 설치·삭제 시에 일어난다. (5) 프로젝트 CLAUDE.md에 따르면 에뮬레이터에서 [GAA] 로그로 스캔은 확인했으나 Gleo가 실제로 앱 액션을 호출하는지는 아직 확인하지 못했고, 키트도 '미확인'이며 시연은 adb intent로 모사하는 것을 권한다. 키트 §1.1은 Gleo Interface 연동을 '강한 차별점'으로 본다.

  </details>

### ✏ 반박되어 고친 내용

- **tech-trends-06** 국내 전기차 누적 등록은 2026년 4월 15일 처음 100만 대를 넘었다(기후에너지환경부 2026-04-21 발표, 3월 말 98만 1,321대). 국토교통부 상반기 통계로는 2026년 6월 말 109.5만 대로, 전체 등록 차량 2,667.6만 대의 4.1%이며 2025년 말 89.9만 대보다 약 21.8% 늘었다. 정부의 2030년 목표는 충전기 123만 기 이상(급속 14.5만 기 포함)이다(2023년 환경부 로드맵에서 나와 2026년 보도에도 인용됨). 충전기 '약 51만 기(완속 약 46만 기)'는 기준일과 출처 부처가 없는 언론 수치이므로, 기후에너지환경부나 한국환경공단 원문으로 확인하기 전에는 기획서에 인용하지 않는다. 공공 충전요금 5단계 금액은 '행정예고안'이라고 밝혀서 쓴다.
  - 반박된 원 주장은 인용하지 않는다.
  - 근거: [B] [뉴시스: 전체 자동차 2668만대…(국토교통부 2026년 6월 말 등록 통계)](https://mobile.newsis.com/view/NISX20260729_0003728744) (2026-07-30); [B] [MTN: 전기차 공공 충전요금 5단계로 (충전기 약 51만 기, 완속 약 46만 기)](https://news.mtn.co.kr/news-detail/2026042915270477383) (2026-04-29); [B] [오토뷰: 채비, 환경부·서울에너지공사 급속충전기 수주 (2030 목표 급속 14.5만 기, 전체 123만 기 이상)](https://www.autoview.co.kr/ko-kr/articles/100703) (2026-06-25)
  - 시사점: 충전 중 정차 시간을 겨냥한 아이디어의 시장 근거로 '전기차 100만 대 시대'를 쓸 수 있다. 충전기 보급 수치는 기준일이 불분명하므로 인용 전에 정부 원문으로 확인해야 하고, '충전 대기가 길다'는 주장은 공식 통계가 없어 정성 근거로만 써야 한다.
  <details><summary>상세</summary>

  국토교통부 통계를 인용한 뉴시스(2026-07-30): 2026년 6월 말 전체 자동차 2,667.6만 대 중 전기차 1,095,000대(4.1%)이며 2025년 말 89.9만 대에서 21.8% 늘었다(작년 말 수치는 검색 요약 기준, 같은 기사 본문에서 열어 확인한 값은 109.5만·4.1%·상반기 신규 19.9만 대(약 23%)·친환경차 395.3만 대(14.8%)다). 국토부 보도자료 원문은 열지 못했다. 충전기 수는 MTN(2026-04-29)이 기후에너지환경부 발표를 인용해 전국 약 51만 기, 완속 약 46만 기라고 했으나 기준일과 급속 수는 없다. 채비 수주 기사(2026-06-25)는 2030년 목표로 급속 14.5만 기와 전체 123만 기 이상을 적었다. 공공 충전요금은 5단계 개편이 행정예고됐다(30kW 미만 294.3원/kWh, 200kW 이상 391.9원/kWh). 한 번 충전할 때 차 안에서 보내는 시간이나 충전 대기 시간의 국내 공식 통계는 찾지 못했다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **tech-trends-01** 현대차그룹은 Pleos Connect를 SDV 전환의 첫 상용 단계로 2026년 5월 더 뉴 그랜저에 처음 적용했고, 2030년까지 약 2천만 대로 넓히는 것을 목표로 한다.
  - 근거: [A] [Hyundai Motor Group Redefines In-Vehicle Experience with Pleos Connect (영문 보도자료)](https://www.hyundaimotorgroup.com/en/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [A] [현대차그룹 Pleos Connect 보도자료 (국문판)](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [A] Pleos Connect 소개: SDV 인포테인먼트 플랫폼, 차량 데이터·상태 API 공개 — `pleos-claude-kit/docs/connect/guide/connect.md (https://document.pleos.ai/docs/connect/guide/connect)` (2026-10-24); [B] [Hyundai Motor unveils next-gen AI infotainment system for SDV transformation](https://www.koreatimes.co.kr/amp/business/companies/20260430/hyundai-motor-unveils-next-gen-ai-infotainment-system-for-sdv-transformation) (2026-04-30)
  - 시사점: 기획서 2번(기획 배경)에서 'SDV 전환이 실제로 시작됐고 개방 생태계(Playground)가 열려 있다'는 근거로 쓸 수 있다. 다만 2천만 대는 목표치라고 명시해 인용해야 하며, MAU 추정에 쓰려면 market 축의 탑재 차종·판매량 근거와 함께 써야 한다.

- **tech-trends-03** 구글, BMW, 메르세데스는 모두 LLM 기반 대화형 비서를 2025~2026년에 차에 넣었고, 차량 설정·내비·EV 배터리 정보까지 맡는다. 서드파티 앱 연동은 아직 초기 단계다.
  - 근거: [A] [Google 공식 블로그: Gemini in cars with Google built-in](https://blog.google/products/android/cars-with-google-built-in-gemini-tips-2026) (2026-04-30); [B] [9to5Google: Gemini coming to Android Automotive](https://9to5google.com/2026/04/30/gemini-coming-to-android-automotive/) (2026-04-30); [B] [electrive: BMW amplía su asistente de voz con Alexa+](https://www.electrive.com/es/2026/01/06/bmw-expands-its-voice-assistant-with-alexa/) (2026-01-06); [B] [eWeek: Mercedes Adds Conversational Navigation With Gemini AI](https://www.eweek.com/news/mercedes-gemini-ai-navigation/) (2025-01-14)
  - 시사점: 목적지 검색, 충전소 찾기, 공조 조절처럼 비서가 이미 하는 기능만으로는 차별화가 어렵다. 앱은 비서가 갖지 못한 맥락(좌석 점유, 정차·충전 상태, 가족 구성)이나 정차 중의 깊은 상호작용에 집중하고, 비서는 음성 진입점으로 붙이는 편이 낫다.

- **tech-trends-04** Gleo AI(42dot, LLM 기반)는 현재 차량 제어와 편의 중심이며, 서드파티 앱 연동과 개인화는 계획 단계다.
  - 근거: [A] [현대차그룹 Pleos Connect 보도자료 (국문판)](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [B] [CBT News: Hyundai Motor Group Pleos Connect (Gleo AI 현재·향후 기능)](https://www.cbtnews.com/?p=243446) (2026-04-30); [A] Gleo AI 디자인 가이드: Status, Elevation, 대화 시작 방식 — `pleos-claude-kit/docs/connect/guide/docs-design/Experiences/gleo-ai.md (https://document.pleos.ai/docs/connect/guide/docs-design/Experiences/gleo-ai)` (2026-10-24)
  - 시사점: 기획서에 'Gleo가 우리 앱을 호출한다'를 이미 가능한 현실처럼 쓰면 감점 위험이 있다. '앱 연동이 예고된 시점에 맞춘 설계'로 쓰고, 좌석 단위 음성 인식은 동승자 케어 아이디어의 환경 근거로 쓸 수 있다.

- **tech-trends-07** EV 신호(SOC, 충전 목표·상태)와 NaviHelper의 충전소 정보는 지금 앱에서 조회할 수 있지만, 충전 제어는 준비 중이고 V2L은 Vehicle SDK 문서에서 API를 찾지 못했다.
  - 근거: [A] Vehicle SDK·NaviHelper API 색인 — `pleos-claude-kit/API_INDEX.md (Vehicle EV 조회·CONTROL_CAR_ENERGY, NaviHelper RouteInfo 충전 필드)` (2026-10-24); [A] Virtual Console Energy 신호 목록 — `.claude/skills/pleos-idea-evaluation/references/platform-capability.md §4` (2026-10-24); [A] V2L 가이드: 모바일 앱 화면 안내, 용어집 정의 — `pleos-claude-kit/docs/connect-mobile/connect-mobile-user-guide/03-04-vehicle-control-v2l-guide.md; docs/connect/guide/glossary.md (V2L)` (2026-10-24); [A] [Google 공식 블로그: Gemini가 EV 배터리·충전소를 안내](https://blog.google/products/android/cars-with-google-built-in-gemini-tips-2026) (2026-04-30)
  - 시사점: SOC와 충전 목표를 읽어 '충전 완료까지 남은 정차 시간을 활용'하는 앱은 승인 없이 시연 가능하다. 충전 제어와 V2L 연동은 근거가 없으므로 핵심 기능에 넣지 말고 '이후 확장'으로만 쓴다. 충전소 탐색 자체는 내비·비서가 이미 하므로 정차 시간 활용 쪽에서 차별화해야 한다.

- **tech-trends-08** 정차 중 엔터테인먼트는 Google과 주요 완성차가 '정차 때만 영상·게임·브라우저, 주행 전환 시 오디오 전용'으로 수렴하고 있고, Pleos도 같은 구조(정차 허용, 주행 중 영상 금지)이며 개인화와 동승자 경험은 아직 문서상 초기다.
  - 근거: [A] [Android for Cars: Parked apps (video, games, browsers)](https://developer.android.com/training/cars/parked) (2026-10-23); [B] [9to5Google: Android Auto video apps, Gemini updates](https://9to5google.com/2026/05/12/android-auto-video-apps-gemini-updates/) (2026-05-12); [B] [electrive: BMW iX3 정차 중 영상 앱·Zoom](https://www.electrive.com/es/2026/01/06/bmw-expands-its-voice-assistant-with-alexa/) (2026-01-06); [A] 지역별 주행 규제 정책, 주행 안전 설계 원칙 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy.md; driver-distraction-guideline.md` (2026-10-24); [A] [현대차그룹 Pleos Connect 보도자료: 앱 마켓 서비스와 게임·엔터 확장 예고](https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-group-redefines-in-vehicle-experience-with-pleos-connect-next-generation-infotainment-system) (2026-04-30); [A] 공모전 원문: 인카 엔터테인먼트, 패밀리 & 동승자 케어 — `pleos-claude-kit/contest/CONTEST_SOURCE.md (고객 니즈·페인포인트 예시)` (2026-10-24); [A] Pleos Account 기반 사용자 프로필 동기화 서비스 언급 — `pleos-claude-kit/docs/connect/guide/connect.md` (2026-10-24)
  - 시사점: 정차·충전 중에는 영상·게임·긴 입력 같은 깊은 상호작용이 가능하고, 주행 전환 시에는 오디오나 최소 화면으로 낮추는 '정차 1단 / 주행 2단' 설계가 업계 표준과 맞는다. 개인화는 SDK 프로필이 없으므로 앱 자체 설정과 좌석 점유 신호로 해결해야 한다. 이 영역은 해외 사례가 많아 단순 영상 앱은 독창성이 낮으니 차량 신호와 결합해야 한다.

### 조사 공백
- decisionCritical 상한(3건) 때문에 2천만 대 목표(tech-trends-01)는 비핵심으로 두었다. 기획서 6번(MAU)에 그대로 인용될 수치이므로 market 축에서 핵심 주장으로 표시해 검증해야 한다.
- tech-trends-06의 전기차 109.5만 대와 충전기 약 51만 기는 국토교통부·기후에너지환경부 보도자료 원문이 아니라 언론 인용으로 확인했다. 충전기 수는 기준일과 급속 수가 없고, 2025년 말 수치(89.9만 대, +21.8%)는 검색 요약 기준이다. 정부 원문(ev.or.kr 등) 대조가 필요하다.
- 충전 중 차량 안 체류 시간, 평균 충전 대기 시간, 정차·신호 대기 시간의 국내 공식 통계는 찾지 못했다(user-needs 축과 겹치는 질문).
- V2L의 출력과 하한 SOC 공식 수치는 확인하지 못했다. 검색 요약상 보도마다 상충(하한 15% 대 20%, 출력 3.6kW 대 하이브리드 1.5kW)하고 원문은 열지 못해 인용하지 않았다. V2L API 부재는 키트 문서·SDK 소스 검색 결과이며 비공개 API 가능성은 배제하지 못한다.
- EU Data Act 적용(2025-09-12)과 EU 집행위 가이드는 법무법인 요약 검색 결과만 보았고 원문을 열지 못했다. Vehicle Data API의 유럽 한정은 Pleos 공식 문서(2026-08 기준)로 확인했으나 한국 출시 계획은 찾지 못했다.
- Gleo AI의 한국어 인식률, 응답 속도, 오작동 같은 실사용 검증 자료와 출시 후 Gleo 앱 연동 일정은 찾지 못했다. proactive suggestion이 실제 출시 기능인지도 미확인이다.
- Mercedes 2026년형 C-클래스의 ChatGPT·Gemini·Bing 멀티에이전트 보도는 신뢰도 낮은 단일 출처라 제외했다. GM 400만 대 Gemini 롤아웃 수치도 같은 이유로 제외했다.
- SDV 표준(COVESA VSS 등)의 국내 완성차 채택 여부는 2026년 근거를 찾지 못했다. 검색에서 VSS가 AWS FleetWise 등에 쓰인다는 일반 설명만 확인했고, Pleos와의 관계는 문서에 없다.
- 질문 해석: 'EV 생태계 앱 기회'는 공모전 SDK로 구현 가능한 범위(조회 중심)로 좁혀 해석했다. 'SDV 차량 데이터 개방'은 온디바이스 Vehicle SDK와 클라우드 Vehicle Data API로 나누어 정리했다.
- Tesla Theater 등 정차 엔터 사례는 2020년 자료만 확인되어 제외했다. 동승자·개인화 쪽 2026년 해외 사례(좌석별 프로필 등)는 찾지 못했다.

## 규제·안전·개인정보 (`regulation-safety`)

### ✅ 확인된 사실

- **regulation-safety-01** 도로교통법 제49조제1항제11호·제11호의2는 운전 중 운전자가 볼 수 있는 위치에 영상을 표시하는 것과 영상표시장치를 조작하는 것을 모두 금지하고, 자동차가 정지하고 있으면 둘 다 허용한다. 지리안내·교통정보안내·긴급상황 안내·좌우 또는 전후방 시야 보조 영상은 장착·거치된 장치의 '표시'에 한해 예외이고, '조작' 금지에는 이 영상 예외가 없다.
  - 근거: [A] [국가법령정보센터 도로교통법 제49조(모든 운전자의 준수사항 등) 제1항 제11호·제11호의2](https://www.law.go.kr/LSW//lsLawLinkInfo.do?lsJoLnkSeq=1000189028&lsId=001638&chrClsCd=010202&print=print) (2026-10-24); [A] [도로교통법 시행령 [별표 8] 범칙행위 및 범칙금액(운전자) 제15호의2·제15호의3 (시행령 시행 2026-10-02)](https://www.law.go.kr/DRF/lawService.do?OC=test&target=law&MST=290387&type=XML) (2026-10-24); [A] [법제처 생활법령정보 > 운전 중 디지털 멀티미디어 방송(DMB) 등 시청 금지 (범칙금·벌점 15점)](https://easylaw.go.kr/CSP/CnpClsMain.laf?popMenu=ov&csmSeq=684&ccfNo=2&cciNo=1&cnpClsNo=5) (2026-09-15); [B] ['尹 출근길 생중계' 유튜버 도로교통법 위반 무죄 (뉴스1 보도, 서울북부지법 1심)](https://www.fnnews.com/news/202502172144270265) (2025-02-17)
  - 시사점: 동영상·게임·브라우저처럼 화면을 보게 하는 기능은 '정지 상태 전용'으로 설계하고, 주행 중에는 지리·교통·긴급·전후방 시야 보조에 해당하는 표시만 남긴다. 터치 같은 조작은 영상 예외와 무관하게 주행 중 금지이므로 주행 중 핵심 흐름은 읽기 전용 표시, 음성, 사전 설정 중심으로 짠다. 기획서에는 조항 번호(제49조제1항제11호·제11호의2)를 인용해 '정차 전용 기능'과 '주행 중 허용 기능'을 나눠 쓸 수 있다. 음성 명령이 '조작'인지는 해석 자료를 찾지 못했다.
  <details><summary>상세</summary>

  영상표시장치는 '방송 등 영상물을 수신하거나 재생하는 장치(운전자가 휴대하는 것을 포함)'로 정의된다. 제11호(표시 금지)의 단서는 가목 '자동차등이 정지하고 있는 경우'와 나목 '장착·거치한 영상표시장치에 1) 지리안내·교통정보안내 영상 2) 국가비상사태·재난상황 등 긴급 안내 영상 3) 좌우 또는 전후방을 볼 수 있도록 돕는 영상이 표시되는 경우'다. 제11호의2(조작 금지)의 단서는 가목 '정지하고 있는 경우'와 나목 '노면전차 운전자의 운전용 장치 조작'뿐이므로, 영상 유형 예외는 조작에 적용되지 않는다. 제재는 시행령 별표 8 제15호의2(표시)·제15호의3(조작)에서 같은 구간에 묶여 범칙금 승합 7만원, 승용 6만원, 이륜 4만원, 자전거 등 3만원이다(별표 8은 2025-06-02 개정본). 벌점 15점은 법제처 생활법령정보(정보 기준일 2026-09-15)에 적혀 있고, 시행규칙 별표 28 제17의2·17의3도 표의 15점 구간에 있다. 확인 시점은 2026-10-24이며, law.go.kr 조회 판(법률 제22067호, 2026-10-13 일부개정, 화면 머리글 시행 2027-04-14)에서 제49조의 개정 이력은 2021-10-19에서 끝나 제11호·제11호의2 문구는 그 이후 바뀌지 않았다. 참고로 서울북부지법 1심은 차량 카메라 영상을 휴대전화 화면에 띄운 사안에서, 이 조항이 '수신하거나 재생하는' 영상을 대상으로 한다고 보고 무죄를 선고했다(2025-02 보도, 확정 여부 미확인).

  </details>

- **regulation-safety-05** 위치정보법은 누구든 개인위치정보주체의 동의 없이 개인위치정보를 수집·이용·제공하지 못하게 하고, 위치기반서비스를 사업으로 하려면 신고(소상공인·1인 창조기업은 개시 후 1개월 내 신고)와 이용약관 명시, 14세 미만은 법정대리인 동의가 필요하며, 무동의 수집·이용은 5년 이하 징역 또는 5천만원 이하 벌금 대상이다. Pleos App Market 제출에도 위치정보 활용 표시와 위치정보 이용약관 URL이 필수다.
  - 근거: [A] [위치정보의 보호 및 이용 등에 관한 법률 제2조·제9조·제9조의2·제15조·제18조·제19조·제21조의2·제25조·제26조·제39조·제40조 (법률 제21066호)](https://www.law.go.kr/DRF/lawService.do?OC=test&target=law&MST=277359&type=XML) (2025-10-01); [A] 앱 심사 제출 (Pleos App Market 제출 항목) — `pleos-claude-kit/docs/pleos-only/distribute/app-submission-guide.md (§심사 제출 항목: 위치정보 활용, 위치정보 서비스 이용약관 및 기타)` (2026-10-24); [A] 사용자 개인정보 및 보안 심사 가이드 (위치정보 서비스 등록/신고 확인 요청) — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide/review-user-privacy-security.md (§사용자 개인정보 활용)` (2026-10-24); [A] SDK 승인 상태(FusedLocation 준비 중)와 FusedLocation 퍼미션 보호수준 dangerous — `pleos-claude-kit/CLAUDE.md §2.2, §3.3` (2026-10-24)
  - 검증 메모: 표현만 다듬는다. (1) '무동의 수집·이용은 5년 이하 징역 또는 5천만원 이하 벌금'은 제39조제3호, 즉 위치정보사업자·위치기반서비스사업자가 제18조제1항·제2항 또는 제19조제1항·제2항·제5항을 어긴 경우의 벌칙이다. '누구든지'를 대상으로 하는 제15조제1항 위반은 제40조제4호(3년 이하 징역 또는 3천만원 이하 벌금)다. (2) Pleos 제출에서 '위치정보 활용' 항목은 모든 앱이 답해야 하는 필수 항목이다. 위치정보 이용약관 URL은 위치정보나 GPS를 선택했거나 '활용함'일 때만 필수다(조건부 필수). (3) 제3자 제공 통보는 매회가 원칙이지만, 제19조제4항에 따라 동의를 받으면 최대 30일 범위에서 모아서 통보할 수 있다.
  - 시사점: 위치를 쓰는 아이디어(경로, 충전소·주차장 탐색, 가족·아이 위치 공유, 주행 기록)는 기획서에 동의 흐름, 위치정보 이용약관, 수집 최소화, 보유기간을 구체적으로 적어야 하고 사업화 단계에서는 위치기반서비스사업 신고가 필요하다. 아이 위치를 다루면 14세 미만 법정대리인 동의 설계가 필수다. 위치를 서버로 보내지 않고 기기 안에서만 처리하는 설계가 법적 부담을 줄이는지는 해석을 찾지 못했으므로 단정하지 말고 기획서에는 '수집 최소화·온디바이스 우선'으로 쓰는 편이 안전하다.
  <details><summary>상세</summary>

  근거는 국가법령정보센터 현행 위치정보법(법률 제21066호, 2025-10-01 시행, 소관 방송미디어통신위원회)이다. 제2조는 개인위치정보를 다른 정보와 쉽게 결합해 특정 개인의 위치를 알 수 있는 정보까지 포함해 정의한다. 제15조제1항은 동의 없는 수집·이용·제공을 금지하고 예외는 긴급구조·경찰 요청·다른 법률의 특별 규정이다. 제18조제1항·제19조제1항은 수집 시와 서비스 제공 시 사업자 정보, 권리와 행사방법, 서비스 내용, 보유목적·기간을 이용약관에 명시한 뒤 동의를 받게 한다. 제19조제2항·제3항은 제3자 제공 시 별도 고지·동의와 매회 통보를 요구한다. 제18조제3항은 필요 최소한만 수집하게 한다. 제25조제1항은 14세 미만 아동의 개인위치정보 수집·이용·제공에 법정대리인 동의와 동의 확인을, 제26조는 8세 이하 아동 등에 대해 생명·신체 보호 목적일 때 보호의무자 동의를 본인 동의로 본다. 제9조제1항은 개인위치정보를 대상으로 하는 위치기반서비스사업의 신고를 요구하고, 제9조의2는 소상공인·1인 창조기업이 신고 없이 시작할 수 있되 개시 후 1개월이 지나도 계속하면 개시일부터 1개월 안에 신고하게 한다. 제21조의2는 처리방침에 개인위치정보 항목을 포함하게 한다. 벌칙은 제39조제3호(동의 없이·동의 범위를 넘어 수집·이용·제공) 5년 이하 징역 또는 5천만원 이하 벌금, 제40조제2호(무신고 영업)와 제40조제4호(제15조제1항 위반)가 3년 이하 징역 또는 3천만원 이하 벌금이다. Pleos: 제출 항목에 '위치정보 활용'이 필수이고, 활용 시 위치정보 서비스 이용약관 URL이 조건부 필수이며, 심사 가이드는 위치정보를 저장·이용하는 서비스에 대해 서비스 국가 법률에 따른 등록·신고 확인을 요청할 수 있고 미확인·허위 시 판매가 중단될 수 있다고 한다. 키트 CLAUDE.md §3.3 기준으로 FusedLocation 퍼미션은 dangerous이고 콘솔 상태는 '준비 중'이며, NaviHelper 현재 위치(getCurrentLocationInfo)는 지금 쓸 수 있다.

  </details>

### ⚠ 불확실

- **regulation-safety-02** ⚠ 미확인 — 법은 '정지하고 있는 경우'에 영상 표시와 조작을 허용하지만 Pleos는 기어 P(Parking)만 제한 없는 상태로 보고, 에뮬레이터에서는 공회전(D, 0km/h)도 주행과 같은 제한(NO_VIDEO+NO_SETUP)이 걸린다. 즉 신호대기 중 사용은 법상 허용돼도 Pleos 앱에서는 제한되므로 Pleos 정책이 법보다 엄격하다.
  - 근거: [A] 지역별 주행 규제 정책 (Pleos 공식 문서, 키트 수집본) — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy.md (§주행 상황에 따른 앱 사용 여부, §그외 지역) / https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy` (2026-10-24); [A] Pleos Connect v2.1 에뮬레이터 관찰: 정차 0x0, 공회전·주행 0x50(requiresDO=true) — `CLAUDE.md (프로젝트) '실제로 확인한 함정' 중 '이 에뮬레이터의 주행 규제 설정' 항목` (2026-10-24); [A] 키트 지시서: DO 미선언 시 Drive 사용 불가, 한국은 '그외 지역' 표 적용 예상 — `pleos-claude-kit/CLAUDE.md §4.2 주행 규제` (2026-10-24); [A] [법제처 생활법령정보: 정지하고 있는 경우에는 영상표시장치를 조작할 수 있다](https://easylaw.go.kr/CSP/CnpClsMain.laf?popMenu=ov&csmSeq=684&ccfNo=2&cciNo=1&cnpClsNo=5) (2026-09-15); [B] [정부, 운전 중 DMB 시청·조작 금지… 위반시 벌금 (재경일보, 신호대기 등 정지 중 시청은 처벌 제외)](https://news.jkn.co.kr/post/718517) (2012-08-31); [A] [현대자동차 인포테인먼트 사용설명서 '안전을 위한 주의 사항' (운전 중 설정·기능 변경 금지)](https://ownersmanual.hyundai.com/ivi/STD_GEN5W/AVNT2/EU/Korean/precautionsforsafety.html) (2021); [A] 공모전 1차 서류심사 평가항목: 주행·정차 맥락 및 차량 기능과 유기적 결합 — `pleos-claude-kit/contest/CONTEST_SOURCE.md (심사 기준 '차량특화성')` (2026-10-24)
  - 검증 메모: 다음처럼 범위를 좁혀 쓴다. '도로교통법은 정지하고 있는 경우 영상 표시와 조작을 허용하고, 신호대기 정지도 여기에 포함된다는 정부·경찰 설명이 있다(2012 보도, 2016 경찰 발언, 법령 정의나 판례는 없음). Pleos Connect v2.1 에뮬레이터에서는 기어 P에서만 제한이 없고(0x0), 공회전(D, 0km/h)에서도 주행과 같은 0x50(NO_VIDEO+NO_SETUP)이 걸린다. 따라서 신호대기 같은 D단 정지 상태에 한해서는 에뮬레이터 정책이 법보다 엄격하다. 반대로 주행 중에는 Pleos 「그외 지역」 표가 키보드·검색을 허용해 법 제11호의2(주행 중 조작 금지)와의 관계가 불명하므로, Pleos 정책이 전반적으로 법보다 엄격하다고 말할 수 없다. 실차 정책은 플랫폼 버전·양산 차량에 따라 다를 수 있다(키트 문서). 또 에뮬레이터 0x50에는 LIMIT_CONTENT·LIMIT_STRING_LENGTH가 없으므로, 대용량 텍스트 제한은 비트와 별개로 앱이 직접 적용해야 한다.'
  - 시사점: 깊은 상호작용(설정, 입력, 영상, 긴 텍스트)은 신호대기(D)가 아니라 P단 상태(주차, 충전, 대기)를 기준으로 설계해야 시연과 심사에서 안전하다. 신호대기 시나리오는 법상 허용이어도 Pleos에서는 제한되니 Virtual Console의 '공회전' 상태로 직접 확인한다. 주행 중 화면은 읽기 전용·큰 터치·음성 중심으로 줄이고, 지역은 하드코딩하지 말고 플랫폼이 내려주는 activeRestrictions 비트로 분기한다. 기획서에서는 정차(P)와 주행의 화면 전환을 차량특화성 근거로 보여 줄 수 있다.
  <details><summary>상세</summary>

  법: '정지'는 도로교통법에 정의가 없고 제2조는 '정차'(5분 이내 정지, 주차 외)와 '주차'만 정의한다. 신호대기 중 정지한 동안의 시청이 처벌에서 제외된다는 설명은 2012년 규제개혁위 심사 단계 보도(재경일보 2012-08-31)에 있고, 2016년 YTN은 휴대전화(제10호)에 대해 경찰이 '바퀴가 멈춘 상태'를 정지로 본다고 전했다. 이 기사는 제10호 이야기라 영상표시장치에는 유추일 뿐이다 [추론]. Pleos: 키트 '지역별 주행 규제 정책'은 DO(Distraction Optimization)를 선언하지 않은 앱이 Parking이면 사용 가능, Drive이면 사용 불가라고 적는다. 본문은 Parking·Idling·Driving을 언급하지만 표는 Parking과 Driving 두 줄뿐이다. 프로젝트 CLAUDE.md의 에뮬레이터 관찰은 정차 0x0, 공회전(D, 0km/h)·주행(D, 40km/h) 모두 requiresDO=true에 0x50(NO_VIDEO+NO_SETUP), 상태 불명 0x1ff다. 키트 CLAUDE.md §4.2는 한국이 '그외 지역' 표를 따를 것으로 추론한다 [추론]. 그 표에서 Driving 중 키보드·다이얼패드·검색·미디어 앱 로그인·내비 검색은 허용, 차량 설정류·동영상·QR 로그인·대용량 텍스트는 금지다. DO를 선언해도 CarUxRestrictions의 ActiveRestrictions를 보고 UI를 줄일 의무가 있다. 출처가 충돌하는 지점은 두 가지다. 하나는 법의 '정지'와 Pleos의 'Parking'이 다르다는 점이다. 다른 하나는 플랫폼이 '그외 지역'에서 주행 중 키보드·검색을 허용하는 것과 법 제11호의2(주행 중 영상표시장치 조작 금지)가 어떻게 맞물리는지에 대한 공식 해석을 찾지 못했다는 점이다. 현대자동차 인포테인먼트 사용설명서(2021)는 운전 중 어떠한 설정이나 기능도 변경하지 말고 내비 메뉴는 출발 전이나 정차 중 사용하라고 안내한다. 공모전 심사항목 '차량특화성'은 주행·정차 맥락과 차량 기능의 결합을 평가한다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **regulation-safety-03** NHTSA Visual-Manual 가이드라인(1단계, 2013-04-26 연방관보)은 비구속 자율 권고이며, 운전 중 한 번에 도로에서 눈을 떼는 시간 2초 이하와 누적 12초 이하를 권고하고, 영상·문자 입력 등은 운전 중 아예 쓸 수 없게 하라고 권고한다. 대상은 미국 경차량의 순정(OE) 장착 기기이고 한국 규제가 아니다.
  - 근거: [A] [Visual-Manual NHTSA Driver Distraction Guidelines for In-Vehicle Electronic Devices, 78 FR 24818 (연방관보 전문)](https://www.federalregister.gov/documents/full_text/text/2013/04/26/2013-09883.txt) (2013-04-26); [A] [연방관보 문서 메타데이터(초록: nonbinding, voluntary, original equipment 대상)](https://www.federalregister.gov/api/v1/documents/2013-09883.json) (2013-04-26); [A] [Android Automotive OS 운전자 주의 분산 가이드라인 (DO는 activity 단위, 수치 없음)](https://source.android.com/docs/automotive/driver_distraction/guidelines) (2026-10-24); [A] 주행 안전을 고려한 설계 (Pleos 공식, AAOS 가이드라인 준수 명시) — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline.md (첫 단락) / https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline` (2026-10-24)
  - 시사점: 기획서에서 '2초 응시, 누적 12초' 같은 수치는 NHTSA 자율 권고임을 밝히고 인용하면 안전 설계 근거로 쓸 수 있지만, 한국 법 기준이나 Pleos 심사 기준으로 쓰면 틀린다. Pleos 심사 기준은 AAOS 가이드라인과 CarUxRestrictions이다. 영상·긴 텍스트·문자 입력 중심 기능은 NHTSA 기준에서도 주행 중 잠금 대상이라 정차 전용으로 설계하는 것이 일관된다.

- **regulation-safety-04** 국내에서 법적 구속력 있는 화면 조작 시간 수치 기준은 찾지 못했다. 확인된 국내 기준은 2014-04-30 국가기술표준원이 제정한 KS '자동차 내 디스플레이장치 운전자 인터페이스 요구 사항'(강제성 없는 권고)이고, 수치 기준으로 인용할 만한 것은 Google의 '2초 안에 읽기'와 '0.25초 응답'이다. Pleos 공식 문서에는 시간 수치가 없다.
  - 근거: [B] [산업통상자원부, 운전 중 DMB 시청 근본적 차단 (KS 표준 제정: 7.5초, 1.5초×5회, 동영상·화상 제한, 비강제)](https://www.autoview.co.kr/ko-kr/articles/51824) (2014-04-29); [B] [운전 중 DMB 시청 근본적 차단 KS 표준 제정 (메트로신문, 7.5초, 비강제 권고, 2012-08 국토부 지침 언급)](https://www.metroseoul.co.kr/article/2014042900246) (2014-04-29); [A] [Design for Driving: Interaction principles (2초 읽기, 0.25초 응답)](https://developers.google.com/cars/design/design-foundations/interaction-principles) (2026-10-24); [A] 앱 기능 및 성능 안정성 (터치 후 0.25초, 버튼 반응 2초) — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability.md (§기능 및 성능)` (2026-10-24)
  - 시사점: 기획서의 안전 설계 근거는 '법령(제49조) + 플랫폼 CarUxRestrictions + Google/NHTSA 권고 수치'의 3단으로 쓰고, KS 수치(7.5초)는 B등급 2014년 보도 기반이라 원문 확인 전에는 보조 근거로만 쓴다. 화면 하나는 2초 안에 읽히게, 터치 반응은 0.25초 이내로 설계하면 Pleos 심사 기준과 Google 원칙을 함께 만족한다.

- **regulation-safety-06** 개인정보보호법은 다른 정보와 쉽게 결합해 특정 개인을 알아볼 수 있는 정보까지 개인정보로 보고, 수집·이용에는 동의 4항목 고지, 최소 수집(입증책임은 처리자), 구분 동의, 만 14세 미만의 법정대리인 동의, 처리방침 공개, 안전조치를 요구한다. Pleos 심사는 여기에 익명화, 동의 버튼 외 행위를 동의로 간주 금지, 철회 시 즉시 삭제, 주행 행동 데이터의 마케팅 사용 금지를 더한다.
  - 근거: [A] [개인정보 보호법 제2조·제3조·제15조·제16조·제22조·제22조의2·제23조·제29조·제30조 (법률 제21445호)](https://www.law.go.kr/DRF/lawService.do?OC=test&target=law&MST=283839&type=XML) (2026-10-24); [A] 보안 및 개인정보 보호를 고려한 설계 (Pleos 공식) — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/security-privacy.md (§개인정보 최소 수집, §차량 외부로 나가는 데이터)` (2026-10-24); [A] 사용자 개인정보 및 보안 심사 가이드 (동의 요건, 철회 시 삭제, 아동 법정대리인 동의) — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide/review-user-privacy-security.md` (2026-10-24); [B] [개인정보위, 中 커넥티드카 조사…국외 이전·유출 우려 점검 (전자신문)](https://www.etnews.com/20261014000001) (2026-10-14); [B] [지커 현장조사 착수…개인정보위, 커넥티드카 7개사 조사 (스페셜타임스)](https://www.specialtimes.co.kr/news/articleView.html?idxno=473756) (2026-10-13)
  - 시사점: 공식 예시인 '안전운전 점수' 같은 주행 습관 데이터 앱은 점수 계산을 기기 안에서 하고 서버 전송·식별자 결합을 최소화하는 설계가 법과 Pleos 심사를 함께 통과하는 길이다. 기획서에 수집 항목, 목적, 보유기간, 동의 UI, 익명화, 마케팅 미사용을 명시하면 감점 요인이 줄고, 커넥티드카 개인정보 점검이 진행 중이라는 점은 사업성 항목의 리스크 인식으로 쓸 수 있다.

- **regulation-safety-07** 도로교통법은 운전자에게 모든 좌석 동승자의 안전띠(영유아는 보호장구 착용)와 동승자 위험 방지 조치를 의무로 두고, 승객의 안전운전 방해 소란행위를 방치한 운전을 금지한다. Pleos 심사는 아동 위험 유도 콘텐츠를 거부하고 아동 정보에 법정대리인 동의를 요구한다. 이 때문에 동승자·어린이 대상 앱은 주행 중 운전자 주의를 끌거나 승객을 소란하게 만드는 설계가 불리하다.
  - 근거: [A] [도로교통법 제2조제23호·제11조·제12조·제49조제1항제7호·제9호·제50조제1항 (법률 제22067호)](https://www.law.go.kr/DRF/lawService.do?OC=test&target=law&MST=291133&type=XML) (2026-10-24); [A] [도로교통법 시행령 [별표 8] 제3호 승객의 차 안 소란행위 방치 운전 (승용 9만원)](https://www.law.go.kr/DRF/lawService.do?OC=test&target=law&MST=290387&type=XML) (2026-10-24); [A] 콘텐츠 안전성 심사 가이드 (아동 위험 유도 금지, 전체 이용가 노출) — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide/review-content-safety.md (§위험한 내용의 콘텐츠, §미성년자 및 아동 보호)` (2026-10-24); [A] 주행 안전을 고려한 설계 (게임·인터넷 서핑·피트니스 유도 금지) — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline.md (§터치 조작 최적화)` (2026-10-24); [A] Vehicle SDK 좌석·안전벨트 API와 Virtual Console 카드 — `pleos-claude-kit/API_INDEX.md (getCurrentSeatOccupancy, registerSeatOccupancy, getCurrentSeatBeltBuckledState, hasSeatBeltNotBuckledWarning) / pleos-claude-kit/docs/pleos-only/app-test-guide/virtual-console-guide.md (Seat Occupancy, Telltales)` (2026-10-24)
  - 시사점: 동승자·어린이 앱은 주행 중에는 운전자를 건드리지 않는 청각 위주나 뒷좌석 전용 흐름으로 두고, 승객이 크게 호응하는 참여형 게임·댄스·노래 콘텐츠는 정차 상태로 한정하거나 운전자 개입이 없게 설계한다. 좌석 점유, 안전벨트, 스쿨존 신호는 어린이 안전 아이디어의 차량특화 근거로 지금 쓸 수 있다. 아이 정보를 저장하면 법정대리인 동의와 전체 이용가 화면 요건을 함께 설계한다.

- **regulation-safety-08** Pleos App Market 심사는 운전자 주의 분산과 사이버보안을 중점 검증하며, 동영상·게임·브라우저는 '미디어 앱'으로 정차 중에만 사용할 수 있고, 주행 중 기능 예외는 문서가 예시로 든 Music/Audio·Maps/Navigation·Mobility Services 세 카테고리에 한정된다. 카테고리를 벗어난 기능이나 잘못 고른 카테고리는 제출 거부 사유가 될 수 있다.
  - 근거: [A] App Review Guidelines: 운전자 주의 분산 요소 최소화 설계 (미디어 앱 정의와 제한) — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide.md / app-review-guide/review-driver-distraction-elements.md (§미디어 앱 설계)` (2026-10-24); [A] 앱 카테고리 (주행 중 제한적 허용 카테고리 예시) — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/app-category.md / https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category` (2026-10-24); [A] 카테고리 이탈 시 제출 거부 가능, 부적합 카테고리는 임의 변경 가능 — `pleos-claude-kit/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability.md (§기능 및 성능) / review-app-registration-info.md (§기본 정보)` (2026-10-24); [A] 앱 심사 제출 (출시 국가 법규 준수 증빙, 연령 등급 IARC) — `pleos-claude-kit/docs/pleos-only/distribute/app-submission-guide.md (§심사 제출 전 준비 사항)` (2026-10-24); [A] 키트 지시서 요약 ('카테고리와 실제 기능 불일치 시 심사 즉시 반려') — `pleos-claude-kit/CLAUDE.md §4.7 앱 카테고리` (2026-10-24)
  - 시사점: 주행 중에도 쓰게 하려는 기능은 반드시 Music/Audio, Maps/Navigation, Mobility Services 중 하나의 범위에 들어가는지 먼저 따져 카테고리를 정해야 한다. 그 밖의 카테고리(Lifestyle, Entertainment, Business, Kids, Social/Communication)는 문서가 주행 중 예외를 예시로 들지 않으므로 정차 중 중심으로 설계하는 편이 안전하다. 충전소·주차장 탐색이나 경유지 설정은 Mobility Services 예시와 맞아 주행 중 깊이 있는 흐름의 근거가 된다.

### 조사 공백
- 주행 중 IVI 터치·음성 조작이 도로교통법 제49조제1항제11호의2의 '영상표시장치 조작'에 해당하는지에 대한 경찰청·법원의 공식 해석을 찾지 못했다. 찾은 단속 설명(YTN 2016)은 휴대전화(제10호) 기사였고, 음성 명령 해석 자료는 없다.
- '정지'(신호대기 포함) 해석의 1차 자료(경찰청 질의회신, 판례)를 찾지 못했다. 2012년 규제개혁위 보도와 2016년 휴대전화 기사뿐이다.
- 법상 '영상'이 정지 이미지나 일반 앱 UI 화면까지 포함하는지는 불명확하다. 2025년 서울북부지법 1심(차량 카메라 영상은 대상 아님)은 언론 보도로만 확인했고 항소·확정 여부는 모른다.
- 국내 법적 구속력이 있는 화면 조작 시간 수치 기준을 찾지 못했다. KS 표준(2014) 번호와 현행 개정 여부, 국토부 2012 지침 원문은 확인하지 못했고 1.5초×5회 수치는 한 매체에서만 확인했다.
- NHTSA 2단계(휴대·애프터마켓, 2016-12 연방관보에 게재된 것까지만 확인)와 3단계(음성) 가이드라인의 최종 발행 여부를 확인하지 못했다. NHTSA 가이드라인은 미국 OE 기기 대상 자율 권고라 한국 적용 근거가 아니다.
- 위치를 기기 안에서만 처리하고 서버로 보내지 않는 앱이 위치정보법상 '수집'이나 위치기반서비스사업 신고 대상인지에 대한 방송미디어통신위원회 해석을 찾지 못했다. 공모전 프로토타입 단계의 신고 의무 여부도 모른다(법 문언은 '사업으로 영위').
- 차량 신호(속도·가속·기어·좌석 점유)와 VIN이 개인정보인지에 대한 개인정보위 공식 안내서를 찾지 못했다. 법 정의(결합 용이성)와 Pleos 심사 가이드의 문구만 확인했다.
- 한국 리전이 Pleos '그외 지역' 표를 따르는지 문서에 명시가 없고(추론), 에뮬레이터 설정이 양산 차량과 같은지 모른다. 문서 본문은 Idling을 언급하지만 표에는 없다.
- 국내 어린이·동승자 관련 공식 통계(카시트 착용률, 차량 내 어린이 방치 사고)의 1차 자료를 찾지 못했다(2차 인용만 확인). 이 부분은 user-needs 축에서 다룰 사항이다.
- Pleos 차량의 뒷좌석(동승자용) 디스플레이 지원 여부를 키트에서 확인하지 못했다(Connect-S/W/WF 폼팩터만 확인).
- 질문 해석: '국내 기준'은 법령(도로교통법·위치정보법·개인정보보호법)과 KS 권고로, 어린이·동승자 규정은 도로교통법 동승자 조항과 Pleos 심사 가이드로 좁혀 해석했다. 법령은 법률 해석이 아니라 조문과 공식 요약 확인이며 최종 법률 판단이 아니다.
- law.go.kr 화면 머리글은 도로교통법 시행일을 2027-04-14로 표시하고 목록은 현행 시행일을 2026-10-13으로 표시해 판 간 구분이 모호했다. 제49조 문구는 두 판에서 같았다.
- AAOS 가이드라인 페이지와 car_ux_restrictions 상세 페이지(404)에서 초·탭 수 같은 수치를 확인하지 못했다.

## 해외·타 브랜드 인카 앱 생태계 (`global-oem-apps`)

### ✅ 확인된 사실

- **global-oem-apps-01** Google built-in 차량의 Google Play 앱은 Google 공식 발표 기준 700개 이상(2026-10-13)이고 16개 브랜드·100개 이상 모델에 제공된다. 기존 리서치(market-04)의 '300개 이상(2025-05)'은 최신 값이 아니다
  - 근거: [A] [A new app experience is coming to cars with Google built-in (Google 블로그, Haris Ramic)](https://blog.google/products-and-platforms/platforms/android/cars-with-google-built-in-google-play-updates/) (2026-10-13); [B] [Google Play for cars gets redesigned interface with new apps and YouTube parked-to-drive playback (FoneArena)](https://www.fonearena.com/blog/494939/google-play-cars-redesigned-interface.html) (2026-10-14); [A] [Cars with Google built-in (브랜드 로고 17개 나열, 날짜 표기 없음)](https://built-in.google/cars/) (2026-10-24); [A] [Do more and have more fun with the next generation of Android in the car (Guemmy Kim)](https://blog.google/products-and-platforms/platforms/android/android-in-cars-updates) (2026-05-12); [A] [Android for Cars at Google I/O 2025 (300+ apps, 50+ models)](https://android-developers.googleblog.com/2025/05/android-for-cars-google-io-2025.html) (2025-05-20); [A] [XPENG Expands Collaboration with Google to Bring Google Play into Its AI-Powered Cockpits](https://www.xpeng.com/pressroom/news/01a1446eef56a12024aa8a02829e01ed) (2026-10-15)
  - 시사점: 기획서 시장 배경에 'Google 진영은 700개 이상으로 확장 중이고 현대차그룹은 큐레이션형 Pleos App Market(18종)을 운영한다'는 대비를 쓸 수 있다. 앱 수로는 경쟁할 수 없으므로 차량 신호와 결합하는 쪽에서 차별화해야 한다. 300개 수치는 더 이상 인용하지 않는다.
  <details><summary>상세</summary>

  Google 블로그(Android for Cars 제품 책임자 Haris Ramic, 2026-10-13)는 'your car's app library includes more than 700 apps', 'across 16 brands and more than 100 models'라고 적었다. 같은 글에 Google Play 개편(차량 화면 크기·방향에 맞춘 레이아웃), 신규 앱(STARZ, Firefox, Ludo King, JioHotstar), 'coming weeks에 자동 업데이트' 롤아웃이 있다. YouTube는 정차 중 영상을 보다가 주행(D)으로 바뀌면 배경 오디오로 이어지며 YouTube Premium이 필요하다. 2025-05 Google I/O 글은 '300개 이상 앱, 50개 이상 모델'이었다(docs/research-digest.md market-04). 같은 글 기준 Android Auto 호환 차량은 약 2.5억 대이고(2025-05 'almost 250 million', 2026-05-12 'more than 250 million'), 이용자 수는 공개되지 않았다. built-in.google/cars(날짜 없음, 2026-10-24 조회)는 로고 17개를 나열한다(Polestar, Volvo, Chevrolet, GMC, Cadillac, Renault, Honda, Acura, Nissan, Infiniti, Lincoln, Ford, Buick, Mitsubishi, Mazda, BYD, Alpine). 블로그의 '16개 브랜드'와 하나 어긋나는데 원인은 확인하지 못했으므로 브랜드 수는 블로그 문구로 인용한다. 이 목록에 현대·기아·제네시스·토요타·Tesla는 없다. XPENG은 2026-10-15 Google Play를 콕핏에 넣겠다고 발표했고 2027년 초 적용, 차종·시장은 추후 공개이며 앱 수는 'hundreds of apps'로만 적었다. 700개는 미디어·게임·브라우저·모바일 이식 앱을 모두 합친 수이고 카테고리 분포·앱별 이용자 수는 공개된 곳이 없다. 비교 기준으로 Pleos App Market은 2026-09 기준 18종이다(docs/research-digest.md market-03).

  </details>

- **global-oem-apps-08** 미국에서는 CarPlay·Android Auto가 있는 차의 운전자 83%가 실제로 쓰고(Edison, 2025), 폰 연동이 없어지면 52%가 스마트폰을, 35%가 순정 시스템을 쓰겠다고 답했다(McKinsey 2024, 재인용). 폰 미러링은 내장 앱의 가장 강한 대체재다
  - 근거: [B] [Edison Research Infinite Dial: CarPlay·Android Auto 접근 26%(2023)/32%(2024), 접근자 중 83% 사용 (RADIO ONLINE)](https://news.radio-online.com/cgi-bin/rol.exe/headline_id=n47613) (2025-07-30); [B] [In-Car Audio Behavior Shifts With CarPlay, Android Auto Growth (Radio Ink)](https://radioink.com/2025/07/31/in-car-audio-behavior-shifts-with-carplay-android-auto-growth/) (2025-07-31); [B] [McKinsey 조사 재인용: CarPlay·Android Auto가 신차 구매자에게 중요 (MacTech, 원 조사는 열지 못함)](https://www.mactech.com/2024/07/18/study-shows-apple-carplay-and-android-auto-are-important-for-new-car-buyers-globally-2/) (2024-07-18); [B] [J.D. Power 2024 U.S. 조사 재인용 (MacTrast)](https://www.mactrast.com/2024/08/u-s-consumers-continue-to-prefer-vehicles-with-apple-carplay-support-says-new-survey/) (2024-08-01); [C] [Edison Research Weekly Insights: Apple CarPlay and Android Auto Ownership & Usage (원문 리다이렉트로 열지 못함, 2025년 40% 접근율은 검색 요약)](https://www.edisonresearch.com/apple-carplay-and-android-auto-ownership-amp-usage/) (2025-07-30)
  - 검증 메모: 첫 문장의 범위를 이렇게 고친다. '미국에서는 최근 한 달 내 차를 운전하거나 탄 18세 이상 중 주 차량에 CarPlay·Android Auto가 있는 사람의 83%가 실제로 쓴다(Edison Infinite Dial 2025, 접근율 40%·실사용 33%). 글로벌 응답자 기준으로는 폰 연동이 없어지면 52%가 스마트폰, 35%가 순정 시스템을 쓰겠다고 답했다(McKinsey 2024-06, MacTech 재인용, 표본·국가 미확인).' '운전자'는 출처의 모집단(운전하거나 탄 사람)보다 좁은 표현이므로 바꾼다. J.D. Power는 '2024 U.S. APEAL Study'로 이름을 밝힌다. '가장 강한 대체재'는 해석이다. 52%의 '스마트폰'은 미러링이 아니라 폰을 직접 쓰는 경우도 포함한다. 2026년 41%는 인용하지 않는다.
  - 시사점: 기획서에서 '순정 앱이 폰 미러링과 경쟁한다'는 배경을 쓸 때 83%와 52%/35%는 미국·글로벌 수치이고 표본 정보가 부족하다는 점을 함께 적는다. 폰이 이미 채운 기능(음악·영상·길찾기)을 그대로 옮긴 앱은 대체되기 쉬우므로, 차량 신호가 있어야 성립하는 기능에 집중해야 한다.
  <details><summary>상세</summary>

  Edison Research Infinite Dial(RADIO ONLINE 2025-07-30 보도): 미국 18세 이상 중 최근 한 달 내 차를 탄 사람의 주 차량에 CarPlay 또는 Android Auto가 있는 비율은 2023년 26%, 2024년 32%이고, 이 기능이 있는 사람의 83%가 사용한다(2025). 표본 크기와 조사 방법은 보도에 없다. Edison 자체 글에서 2025년 접근율 40%, 2026 발표에서 41%라는 내용은 검색 요약으로만 보았고 원문을 열지 못했다. McKinsey(2024-06 조사, MacTech 2024-07-18이 Carscoops·Auto News를 재인용, 표본·국가 미표기): 폰 미러링이 없는 차는 '거래 파기 요인'이라는 응답이 글로벌 전기차 구매자 30%·내연차 구매자 35%, 미국 전기차 25%·내연차 38%다. 현재 차에서 두 기능이 사라지면(글로벌로 표기) 52%는 스마트폰, 35%는 순정 인포테인먼트를 쓰고 14%는 다음 구매에서 브랜드를 바꾼다고 했다. J.D. Power 2024 미국 조사(2024년형 신차 소유자 99,144명, 구매 90일 후, 2023-07~2024-05): 인포테인먼트 만족도 평균 805점, Android Auto 이용자 832점, CarPlay 이용자 840점(MacTrast 2024-08-01 재인용). 모두 미국·글로벌 수치이며 국내 폰 미러링 이용률 조사는 찾지 못했다(global-oem-apps-09 참고).

  </details>

- **global-oem-apps-09** 국내 최근 3년 내 신차 구입자 중 국산차는 68%가 순정 내비를 주로 쓰고 스마트폰 내비는 25%이며(현대 74%, 기아 69%, 제네시스 81%), 수입차는 순정 38%·스마트폰 58%다(컨슈머인사이트, 2022년 7월 조사). 폰 프로젝션 이용률을 따로 잰 국내 자료는 찾지 못했다
  - 근거: [B] [국산차 보유자 10명 중 7명 순정 내비게이션 이용 (컨슈머인사이트 22차 조사, 2022년 자료)](https://www.consumerinsight.co.kr/autoinsight/abview?no=3294&id=pr4_list) (2022-12); [B] ["순정 내비 불편해" 수입차 절반 이상 '스마트폰 보며 운전' (모터그래프, 네이버지도 18% 표기가 보도자료와 다름)](https://www.motorgraph.com/news/articleView.html?idxno=31192) (2022-12-12); [B] [국내 운전자 74%가 사용한다는 '이 앱' (컨슈머인사이트 인용, 표본·시점 미표기)](https://www.reportera.co.kr/car/relying-on-phone-navigation/?amp=1) (2024-03-23); [A] Pleos Connect phone projection 레퍼런스 구현 — `pleos-claude-kit/docs/connect/guide/glossary.md (Android Auto, Apple CarPlay, Phone Projection 항목)` (2026-10-24)
  - 검증 메모: 후속 조사를 추가한다. 컨슈머인사이트 23차 조사 리포트가 2024-03에 보도됐다(머니S 2024-03-05, 더팩트 2024-12-18). 표본은 기사 표기로 '3년 내 신차 구입자 중 스마트폰 내비 이용자 15,967명'이고 티맵 74%(+6%p), 카카오 12%(18%에서 감소), 네이버지도 7%다. 리포테라(2024-03-23)는 같은 조사로 보이는 자료에서 스마트폰 내비를 주로 쓰는 비율을 수입차 62%, 국산차 25%로 적었다. 국산 25%는 그대로이고 수입은 58%에서 62%로 올랐다. 조사 월은 기사에 없다(2023-07로 추정되지만 확인하지 못함). 'detail'의 '후속 조사는 표본·시점이 기사에 없다'는 리포테라에만 해당하고, 다른 보도는 23차와 15,967명을 밝힌다. 브랜드별 순정 비율(현대 74% 등)은 지금 찾을 수 있는 최신 값이 2022년 자료이므로 기획서에는 반드시 '(2022년 자료, 내비 기준)'을 붙인다.
  - 시사점: 현대차 신차 구입자의 74%가 순정 내비를 주로 쓴다는 값은 국산 브랜드에서 내장 경로가 이미 주력이라는 근거가 되지만 2022년·내비 한정이다. 기획서에는 '2022년 조사, 내비 기준'이라고 적고 Pleos 이후 이용률처럼 쓰지 않는다. 국내 CarPlay·Android Auto 이용률은 근거가 없으므로 가정으로 표시해야 한다.
  <details><summary>상세</summary>

  컨슈머인사이트 보도자료: 22차 연례 자동차 기획조사(2022-07 조사, 전체 약 10만 명 중 최근 3년 내 신차 구입자 24,619명 분석), 주행 중 가장 먼저 쓰는 내비 질문이다. 국산차 순정 68%·스마트폰 25%, 수입차 순정 38%·스마트폰 58%. 브랜드별 순정 내비 비율은 제네시스 81%, 현대 74%, 기아 69%, 쌍용 61%, 르노코리아 60%, GM 한국 23%, 수입차는 렉서스 50%, 폭스바겐 44%, BMW 43%, 메르세데스-벤츠 38%, 볼보 35%, 아우디 31%, 미니 19%다. 스마트폰 내비 분류에는 화면 미러링과 연동 시스템이 포함되지만 폰 프로젝션만 따로 떼어낸 수치는 없고, 현대·기아·제네시스의 스마트폰 내비 비율도 개별로 나오지 않는다. 스마트폰 내비 앱 점유율은 T맵 68%, 카카오내비 18%, 네이버지도 내비 7%, 원내비 2%다(모터그래프는 네이버지도를 18%로 적어 보도자료의 7%와 충돌, 보도자료 값을 따른다). 2023년 이후 후속 조사에서 T맵 74%, 카카오맵 12%, 네이버지도 7%라는 보도(2024-03-23)가 있으나 표본·시점이 기사에 없다. 모두 내비 한정 수치이고 2022년 자료라 Pleos Connect 이전 시점이다. 키트에 따르면 Pleos Connect에는 Android Auto(유선·무선)와 Wireless Apple CarPlay의 phone projection 레퍼런스 구현이 있으나, 그랜저·아반떼·투싼에서 실제로 켜져 있는지는 확인하지 못했다.

  </details>

### ⚠ 불확실

- **global-oem-apps-10** ⚠ 미확인 — 내장 앱이 미러링을 이기는 근거는 OEM 통합과 차량 신호 접근이고 지는 지점은 폰 앱의 폭이다. 2026년부터 Android Auto에도 정차 중 영상 앱이 들어오며 지원 제조사 목록에 현대·기아·제네시스가 있어, 정차 엔터테인먼트는 내장 앱만의 영역이 아니다
  - 근거: [B] [Android Auto video apps, Gemini updates (9to5Google)](https://9to5google.com/2026/05/12/android-auto-video-apps-gemini-updates/) (2026-05-12); [A] [Do more and have more fun with the next generation of Android in the car (Google)](https://blog.google/products-and-platforms/platforms/android/android-in-cars-updates) (2026-05-12); [B] [GM이 CarPlay를 뺀 이유(Barra 발언) (InsideEVs)](https://insideevs.com/news/798034/2027-cadillac-lyriq-apple-carplay-nacs/) (2026-06-08); [A] Pleos 단일 경로 정책과 Vehicle SDK 연결 구조 — `pleos-claude-kit/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide.md (경로 배타 정책), pleos-claude-kit/docs/connect-sdk-pleos/Vehicle/intro.md` (2026-10-24)
  - 검증 메모: 고칠 점은 다섯 가지다. (1) Android Auto 영상은 '2026년부터 들어온다'가 아니라 'Google이 2026-05에 연내 지원을 발표했고 첫 제조사 목록에 현대·기아·제네시스가 있음(차종 미공개, 2026-10-24 기준 출시 확인 못 함)'으로 쓴다. (2) Barra 발언은 'The Verge Decoder 팟캐스트(2025-10) 발언, InsideEVs 2026-06-08 재인용'으로 출처를 밝히고, 고객 반응은 '매우 투박하다(very clunky, 기사 의역)'로 고친다. (3) 키트 근거 경로는 pleos-claude-kit/docs/pleos-only/connect-sdk-pleos/Vehicle/intro.md다. (4) 폰 투영 앱도 Android Auto Car App Library(CarHardwareManager)로 속도·배터리 잔량·주행가능거리·주행거리계·EV 커넥터 종류·톨카드를 읽을 수 있다(OEM 제공 시, com.google.android.gms.permission.CAR_FUEL/CAR_SPEED/CAR_MILEAGE). 그러므로 시사점의 'EV' 신호는 Pleos 전용 차별 요소에서 뺀다. CarHardwareManager에 없는 도어·좌석 점유·공조·창문·조명 같은 Vehicle SDK 도메인을 차별 근거로 삼는다. (5) Pleos Connect의 Android Auto 구현이 CarHardwareManager 값을 실제로 넘겨주는지는 10/28 설명회 질문 목록에 추가한다.
  - 시사점: 영상·음악처럼 폰이 이미 잘하는 정차 엔터 기능은 Android Auto 영상 지원으로 차별성이 더 낮아진다. 기획서의 차량특화성은 좌석·도어·EV·공조처럼 Pleos Vehicle SDK로만 읽히는 신호와 결합한 기능으로 세워야 한다. '폰 앱이 Vehicle SDK를 못 쓴다'는 전제는 추론이므로 10/28 설명회에서 확인할 질문 목록에 넣는다.
  <details><summary>상세</summary>

  이기는 지점의 근거: (1) GM CEO Barra는 폰 미러링을 뺀 이유로 '매끄럽지 않고 오가는 것이 주의를 흐트러뜨릴 수 있다', 고객이 '매우 어색하다'고 했다고 InsideEVs(2026-06-08)가 전한다(OEM 주장이며 정량 근거 없음). (2) Pleos 키트는 phone projection을 phone 화면을 투영하는 레퍼런스 구현으로 설명하고 3rd party 앱은 Vehicle SDK로 Vehicle Service에 연결한다고 적는다. 폰 투영 앱이 Vehicle SDK를 호출할 수 있다는 근거는 키트에 없다(추론, 확인 필요). 내비 경로는 내비 앱과 Phone Projection이 하나의 경로만 갖는 단일 경로 정책 대상이다(navigation-app-development-guide.md). (3) 국내 국산차 순정 내비 68%(global-oem-apps-09, 2022년·내비 한정). 지는 지점의 근거: 9to5Google(2026-05-12)은 YouTube 같은 영상 앱이 올해 Android Auto로 오며(60fps FHD, 주행 시작 시 오디오 전용 전환) 지원 제조사 목록이 BMW, Ford, Genesis, Hyundai, Kia, Mahindra, Mercedes-Benz, Renault, Škoda, Tata, Volvo로 시작한다고 전했다. 차종별 지원 여부는 명시되지 않았다. 폰 연동 이용 시 만족도가 더 높고(J.D. Power 2024, 832·840점 대 평균 805점), 폰 연동이 사라지면 14%가 브랜드를 바꾼다는 응답이 있다(global-oem-apps-08). 내장 앱이 이긴다는 정량 비교(같은 기능의 내장 vs 미러링 이용 시간)는 찾지 못했다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **global-oem-apps-02** 자체 스토어를 큐레이션하는 브랜드는 BMW(ConnectedDrive Store, 전 세계 85개 이상, 2026-01)와 NIO(출시 때 40개 이상, 2023-09)이고, 외부 개발자가 입점하는 조건은 두 곳 모두 공개 자료에서 찾지 못했다
  - 근거: [A] [The BMW iX3 ushers in next-generation innovations at CES 2026](https://www.press.bmwgroup.com/global/article/detail/T0454649EN/the-bmw-iX3-ushers-in-next-generation-innovations-at-ces-consumer-electronics-show-2026) (2026-01-05); [B] [Nio brings in App Store for its vehicles, 1st available apps over 40 (CnEVPost)](https://cnevpost.com/2023/09/21/nio-brings-in-app-store-for-its-vehicles/) (2023-09-21); [A] [XPENG Google Play integration (2027년 초 적용 예정)](https://www.xpeng.com/pressroom/news/01a1446eef56a12024aa8a02829e01ed) (2026-10-15)
  - 시사점: Pleos App Market(18종)은 BMW(85개 이상)·NIO(40개 이상)와 같은 큐레이션형이고 규모는 아직 작다. BMW의 카테고리(음악·오디오, 엔터, 게임, 뉴스, 여행·로컬)와 '정차 때만 영상·통화' 규칙은 Pleos의 정차 중심 설계가 업계 관행이라는 근거로 쓸 수 있다. 앱 수 비교는 BMW 수치로만 하고 NIO는 2023년 값임을 밝힌다.

- **global-oem-apps-03** Tesla·Rivian·GM은 서드파티 앱을 차 안에 넓게 열지 않는 닫힌 모델이다. Tesla는 영상 서비스를 네이티브 앱이 아니라 브라우저로 붙이고, Rivian은 폰 미러링 없이 서드파티 API를 '계획'으로만 밝혔으며, GM은 신규 EV에서 폰 미러링을 뺐다(가솔린차는 발언이 엇갈린다)
  - 근거: [B] [Tesla adding more Tesla Theater apps (Not a Tesla App)](https://www.notateslaapp.com/news/4598/tesla-adding-more-tesla-theater-apps) (2026-08-21); [B] [Rivian's software boss is 'super excited' about third-party apps (TechCrunch)](https://techcrunch.com/2024/11/05/rivians-software-boss-is-super-excited-about-third-party-apps) (2024-11-05); [B] [Rivian EVs are getting a software update that warns you what's ahead (TNW)](https://thenextweb.com/news/rivian-os-2-no-carplay-dashboard-control-europe) (2026-08-24); [C] [Rivian Autonomy & AI Day 보도자료 (원문 403, 검색 요약으로만 확인)](https://businesswire.com/news/home/20251211701014/en/Rivian-Unveils-Custom-Silicon-Next-Gen-Autonomy-Platform-and-Deep-AI-Integration-at-Inaugural-Autonomy-AI-Day) (2025-12-11); [B] [The 2027 Cadillac Lyriq Is Keeping A Big Feature All Other GM EVs Have Already Lost (InsideEVs, 'Jun 8', 연도는 푸터 2026 기준)](https://insideevs.com/news/798034/2027-cadillac-lyriq-apple-carplay-nacs/) (2026-06-08); [B] [GM to offer new centralized platform, AI and autonomous features](https://www.repairerdrivennews.com/2025/10/28/gm-to-offer-new-centralized-platform-ai-and-autonomous-features/) (2025-10-28); [B] [GM to remove Android Auto, CarPlay from gas cars (9to5Google, GM 정정 반영)](https://9to5google.com/2025/10/22/gm-android-auto-carplay-remove-gas-cars-gemini-assistant/) (2025-10-24)
  - 시사점: 미러링을 빼는 OEM도 서드파티 개발자용 공개 SDK를 빠르게 열지는 않았다. 공개 SDK와 공모전을 여는 Pleos Playground는 상대적으로 열린 편이라는 점을 기획 배경에 쓸 수 있다. 다만 GM 가솔린차 정책은 출처가 엇갈리므로 단정하지 않는다.

- **global-oem-apps-04** 차 안에서 쓰는 Google 쪽 API는 카테고리가 좁다. Android for Cars App Library(템플릿)는 내비·POI·IoT·날씨 계열이고 영상·게임·브라우저는 정차 전용 별도 트랙이다. Car Hardware API로 속도·에너지 레벨·차종 등을 읽을 수 있으며, 공식 문서끼리 허용 카테고리 목록이 다르다
  - 근거: [A] [Android for Cars: supported app categories (Last updated 2026-10-22)](https://developer.android.com/training/cars) (2026-10-22); [A] [Build templated apps for cars (Last updated 2026-06-18) - navigation, POI, IoT, weather](https://developer.android.com/training/cars/apps) (2026-06-18); [A] [Other driving-related apps (Last updated 2025-09-05) - POI and IoT only](https://developer.android.com/design/ui/cars/guides/app-types/other-apps) (2025-09-05); [A] [Car Hardware APIs (Last updated 2026-09-08)](https://developer.android.com/training/cars/apps/library/car-hardware-api) (2026-09-08); [A] [Build parked apps for cars (Last updated 2026-10-23)](https://developer.android.com/training/cars/parked) (2026-10-23); [A] Pleos 앱 카테고리와 AAOS align 원칙 — `pleos-claude-kit/CLAUDE.md §4.7, pleos-claude-kit/docs/pleos-only/connect-sdk-pleos.md (API policy)` (2026-10-24)
  - 시사점: Google의 템플릿 API는 내비·POI·IoT·날씨에 한정되고 라이프스타일·엔터는 정차 전용 일반 앱 트랙이라, Pleos 카테고리가 더 넓다는 점을 근거로 쓸 수 있다. Car Hardware API가 속도·에너지 레벨을 AAOS 권한으로 여는 것은 속도 신호 접근 경로(research-digest 플랫폼 공백)를 10/28 설명회에서 질문할 때 참고 자료가 된다. 단 Pleos 심사에서 같은 권한이 허용된다는 뜻은 아니다.

- **global-oem-apps-05** 차 밖(클라우드) API는 Tesla Fleet API가 종량제 유료(명령 1,000건 $1, 데이터 500건 $1, 깨우기 50건 $1, 스트리밍 15만 신호 $1)이고, BMW CarData는 EEA 기업 한정에 고객 동의·유료로 열린다. 둘 다 차 안에서 돌아가는 앱 API가 아니다
  - 근거: [A] [Tesla Developer: Usage-based pricing](https://developer.tesla.com/) (2026-10-24); [A] [Fleet API Billing and Limits](https://developer.tesla.com/docs/fleet-api/billing-and-limits) (2026-10-24); [A] [What is Fleet API](https://developer.tesla.com/docs/fleet-api/getting-started/what-is-fleet-api) (2026-10-24); [A] [BMW CarData (BMW Group)](https://www.bmwgroup.com/en/innovation/connected-car/cardata.html) (2026-10-24); [A] [BMW Group launches BMW CarData (2017년 자료)](https://www.press.bmwgroup.com/global/article/detail/T0271366EN/bmw-group-launches-bmw-cardata:-new-and-innovative-services-for-customers-safely-and-transparently) (2017-05-30); [C] [Tesla Fleet API Pay Per Use Pricing (신호당 $0.0001 표기, Tesla 문서와 불일치, 검색 요약으로만 확인)](https://teslemetry.com/blog/tesla-fleet-api-pay-per-use) (2024-11)
  - 시사점: 클라우드 API는 앱 개발자가 데이터 이용료를 내는 구조(Tesla)이거나 지역·동의 조건이 붙는다(BMW). 한국 공모전 앱의 핵심은 차 안 Vehicle SDK(승인 없이 조회 가능)여야 하고, 클라우드 연동 아이디어는 비용·지역·동의 조건을 사업성 리스크로 적어야 한다.

- **global-oem-apps-06** 차량 특화 서드파티 앱 중 이용자 수가 공개된 사례는 Tesla용 Tessie(개발자 주장 약 40만 명)뿐이고, OEM 인카 앱스토어의 서드파티 앱 이용자·매출은 공개된 곳이 없다. 실패·위험 사례는 Tesla API 종량제 요금과 GM AppShop 무기한 연기다
  - 근거: [B] [Tesla releases API pricing, dev says would cost $60 million per year to run his 3rd party app (Electrek)](https://electrek.co/2024/11/28/tesla-releases-api-pricing-dev-says-would-cost-60-million-per-year-to-run-his-3rd-party-app/) (2024-11-28); [B] [TezLab launches new AI-powered car reports for Tesla and Rivian EVs (TechCrunch)](https://techcrunch.com/2024/10/09/tezlab-launches-new-ai-powered-car-reports-for-tesla-and-rivian-evs) (2024-10-09); [B] [Meet TezLab, the Fitbit for Tesla vehicles (2019년 자료)](https://techcrunch.com/2019/06/15/meet-tezlab-the-fitbit-for-tesla-vehicles/amp/) (2019-06-15); [C] [General Motors AppShop rollout postponed indefinitely (원문 403, 검색 요약으로만 확인, 2014년 자료)](https://gmauthority.com/blog/2014/07/general-motors-appshop-rollout-postponed-indefinitely/) (2014-07)
  - 시사점: 기획서에서 '서드파티 인카 앱의 성공 사례 수치'를 인용할 수 있는 근거는 사실상 없다. 사업성은 해외 성공 수치를 빌리지 말고 탑재 대수 × 가정 퍼널로 쓰고, 데이터 이용료가 수익을 잠식한 Tessie·Teslascope 사례를 클라우드 의존의 리스크 근거로 쓰는 편이 안전하다.

- **global-oem-apps-07** 인카 수익은 OEM의 기능·서비스 구독이 중심이고(Tesla FSD 유료 고객 약 148만 명, BMW 열선 시트 구독은 철회), 서드파티 수익 배분 조건이 공개된 곳은 Google Play뿐이다. 한국은 Google Play '나머지 시장' 구간(최초 100만 달러 15%, 초과 30%, 구독 15%)이고 Pleos 수수료는 미공개다
  - 근거: [B] [Tesla FSD reaches 1.48 million active subscriptions (Not a Tesla App, Tesla 2026 Q2 실적 인용)](https://www.notateslaapp.com/news/4483/tesla-fsd-reaches-148-million-active-subscriptions) (2026-07-23); [B] [BMW admits heated seat subscriptions were a mistake, but commits to ConnectedDrive (TechSpot)](https://www.techspot.com/news/111208-bmw-admits-heated-seat-subscriptions-mistake-but-commits.html) (2026-02-05); [A] [Google Play service fees (한국은 '나머지 시장' 구간)](https://support.google.com/googleplay/android-developer/answer/112622) (2026-10-24); [A] [Google Play on Android Automotive OS (Last updated 2026-09-15)](https://developer.android.com/training/cars/platforms/automotive-os/google-play) (2026-09-15); [A] Pleos 결제 시스템 마이그레이션(Payment SDK 준비 중) — `pleos-claude-kit/docs/pleos-only/in-prep/_payment-system-migration.md` (2026-10-24)
  - 시사점: 사업성에는 'Pleos 수익 배분·결제 조건은 미공개, 확정 시 적용'을 전제로 쓰고, Google Play 15%/30%는 참고 벤치마크임을 밝혀 인용한다(Pleos에 적용된다는 뜻이 아니다). 하드웨어 기능 잠금형 구독은 BMW 사례가 반면교사이므로 서비스 가치형 구독이나 B2B 제휴로 제안하는 것이 안전하다. FSD 같은 OEM 자체 서비스 수치는 서드파티 앱의 매출 근거로 쓰지 않는다.

### 조사 공백
- Li Auto, Xiaomi, BYD(중국 DiLink)의 인카 앱스토어 앱 수·서드파티 개방 범위를 공식 자료나 B등급 매체에서 찾지 못했다. NIO는 2023-09 출시 값(40개 이상)만 있고 현행 수치와 개발자 약관은 없다. Huawei HarmonySpace도 2021~2022년 자료뿐이다.
- Mercedes(MBUX 현행 앱 수, 서드파티 입점 조건), Toyota(Audio Multimedia는 Automotive Grade Linux 기반이고 Arene SDK 활용을 밝힌 2025-10 보도자료 요약만 확인, 앱스토어·개발자 프로그램은 찾지 못함), Volvo·Polestar(Google Play 앱 수 별도 공식 수치 없음, Volvo 공식 페이지 403), Kia·Genesis(Pleos 적용 시기는 기존 market-01 참고)는 이번 조사에서 새 근거를 찾지 못했다.
- GM 개발자 프로그램은 2013년 Remote API·In-Vehicle API, 2017년 NGI SDK(약 400개 차량 데이터 포인트) 자료만 있고 현행 여부를 확인하지 못했다. 2025-09-30 GM App Store 종료(2017~2020 NGI 차량) 보도는 단일 C등급 출처뿐이라 쓰지 않았다. GM OnStar 2026년 2분기 수치(재인식 매출 약 8억 달러, 연 30억 달러 전망, 이연 매출 63억 달러)는 GMAuthority·MarketBeat 열람 실패로 검색 요약만 확인해 인용하지 않았다.
- 국내 CarPlay·Android Auto 이용률을 직접 측정한 자료를 찾지 못했다. 컨슈머인사이트 2022 조사의 '스마트폰 내비'는 미러링과 연동을 포함하지만 분리되지 않고, 2023~2024 후속 보도는 앱 점유율만 있다. 2025~2026년 국내 조사 자료도 찾지 못했다.
- Pleos Connect 탑재 3개 차종(그랜저·아반떼·투싼)에서 Android Auto·CarPlay가 실제 활성화돼 있는지는 키트 레퍼런스 구현 언급 외에 확인하지 못했다. Android Auto 영상 앱 지원 제조사 목록에 현대·기아·제네시스가 있으나 Pleos 차종 지원 여부는 명시되지 않았다.
- McKinsey 2024 조사 원문과 표본·국가 구성을 열지 못했고(MacTech 재인용), Edison 2025~2026 접근율(40%·41%)은 검색 요약뿐이다. J.D. Power 2025 TXI 보도자료는 403으로 열지 못했다. 모두 미국·글로벌 수치다.
- Pleos App Market의 수수료·수익 배분 조건과 Payment SDK 사양은 여전히 공개되지 않았다. Google Play 수수료(한국은 최초 100만 달러 15%, 초과 30%, 구독 15%)는 일반 Google Play 정책이며 문서가 자동차를 언급하지 않아 AAOS 차량 내 앱에 같은 요율이 적용된다고 단정하지 못한다. Apple CarPlay 쪽 개발자 수익 구조는 조사하지 못했다.
- Tesla 오너스 매뉴얼과 Tesla FSD 2026년 2분기 원문 실적자료는 열지 못했고, Tesla Theater 구조는 Not a Tesla App 기사(오너 관찰)에 의존한다. Rivian Assistant의 서드파티 앱 연동(Google Calendar 첫 연동)은 Business Wire 원문 403으로 검색 요약(C)뿐이다.
- decisionCritical 상한(4건) 때문에 Android for Cars 허용 카테고리 문서 충돌(global-oem-apps-04, 4개 목록 대 POI·IoT만)과 GM 가솔린차 CarPlay 정책의 출처 충돌(global-oem-apps-03)은 비핵심으로 두었다. 둘 다 기획서에 사실로 인용하기 전에 fact-checker가 확인해야 한다. built-in.google/cars의 17개 로고와 Google 블로그의 16개 브랜드 불일치도 원인을 확인하지 못했다.
- 질문 해석: '앱은 몇 개인가'는 공식 수치가 있는 곳(Google Play 700개 이상, BMW 85개 이상, NIO 40개 이상 2023년)만 쓰고 나머지는 없다고 적었다. '이용자·매출이 공개된 차량 특화 앱·서비스'는 OEM 서비스(Tesla FSD)와 서드파티 앱(Tessie, TezLab)으로 나누었고, 서드파티 인카 앱스토어 앱의 이용자·매출은 공개 자료가 없다. 기존 digest와 겹치는 market-04·05·06은 새 수치(700개 이상, BMW 85개, FSD 세부)만 보강했다.
- 웹 조사는 가능했으나 일부 1차 자료(Volvo 공식, GM 뉴스룸, J.D. Power, Business Wire, GMAuthority, SSRS/Edison 원문)가 403·리다이렉트·타임아웃으로 열리지 않았다.

## 상위 아이디어의 경쟁·유사 기능 (`competitor-benchmark`)

### ✅ 확인된 사실

- **competitor-benchmark-03** 현대·기아·GM의 순정 후석 승객 알림은 모두 '시동을 끄고 운전석 도어를 열 때'(하차 시점)에 작동하고, 정차 후 다시 출발하는 시점에 빈자리를 확인하는 순정 기능은 찾지 못했다. 2위의 재출발 시점 차별점이 남는다.
  - 근거: [A] [2025 현대 투싼 - 후석 승객 알림 시스템 설정 및 작동](https://ownersmanual.hyundai.com/full_webhelp/NX4/2025/ko_KR/id229JA800L5Z.html) (2025); [A] [현대 매뉴얼 - 후석 승객 알림 작동(1차·2차, 10분·25초·8회)](https://ownersmanual.hyundai.com/docview/webhelp/doc/eb7ba0ca-1c40-4bc2-b5db-e7def42b4b26/id1c5824e4350.html) (조회 2026-10-24); [A] [2026 기아 타스만 - 후석 승객 알림 시스템](https://ownersmanual.kia.com/full_webhelp/TK1/2026/ko_KR/topics/t00375.html) (2026); [B] [GMC Acadia 'kid in the back' reminder tied to doors, not seat](https://www.repairerdrivennews.com/2016/06/14/gmc-acadia-kid-in-the-back-reminder-tied-to-doors-not-seat-tech-coming-to-more-gm-cars/) (2016-06-14); [B] [Hyundai's new rear seat reminder is a little different from Nissan's and GM's](https://kidsandcars.org/news/post/hyundais-new-rear-seat-reminder-is-actually-a-little-different-from-nissans-and-gms) (2017-10-03)
  - 검증 메모: 표현만 다듬는다. GM의 1차 알림 시점은 '운전석 도어를 열 때'가 아니라 '시동 OFF 때'이므로 '현대·기아는 시동을 끄고 운전석 도어를 열 때, GM은 시동을 끌 때(모두 하차 시점)'로 쓴다. 기획서에서 순정 비교를 할 때 현대 뒷좌석 안전벨트 미착용 경고(시동 ON·주행 중 벨트 체결 상태 경고, 탑승 감지 아님)와도 구분해 두면 반론에 대비할 수 있다. 참고로 Pleos 키트 API_INDEX에는 getCurrentSeatOccupancy/registerSeatOccupancy(CAR_SEATS 권한)가 있어 좌석 점유 신호 자체는 SDK에 있다. 차종별 지원 여부는 checkSeatOccupancyCapabilities로 확인해야 한다.
  - 시사점: 기획서에 '순정 후석 알림은 하차 시점, 이 앱은 재출발 시점'이라고 구분해 쓸 수 있다. 비교 대상으로 현대 투싼 2025 매뉴얼처럼 같은 차종의 순정 기능을 직접 인용하면 설득력이 높다. 순정 알림은 도어 이력(또는 2차 움직임 감지)을 쓰므로 '좌석 점유 스냅샷과 비교'는 신호 방식도 다르다. 순정 기능이 같은 시점을 다루는 차종·연식이 있는지는 출시 전에 현대·기아 최신 매뉴얼로 다시 확인한다.
  <details><summary>상세</summary>

  [현대 투싼 2025 한국어 매뉴얼] 뒷좌석 도어(또는 트렁크)를 열고 닫은 뒤 시동을 끄고 운전석 도어를 열면 클러스터에 '뒷좌석 탑승자 또는 소지품을 확인하십시오'를 표시한다. 실제 탑승자를 감지하지 못하고 도어 개폐 기록만 쓰며, 기록은 시동 OFF 후 도어 잠금 때 초기화된다. 이 페이지에는 2차 알림 설명이 없다. [현대 어드밴스드(차종 미표기 매뉴얼)] 1차 알림 후 운전석 도어를 닫고 전체 도어를 잠그면 실내 움직임을 10분간 감지하고, 약 25초 경고음·비상등, 블루링크 가입자에게 문자, 최대 8회 반복한다. 센서 적용 차량 한정이고 움직임이 작은 영유아·동물, 가려진 탑승자는 감지가 어렵다. [기아 2026 타스만 매뉴얼] 같은 구조다(1차 알림은 뒷좌석 도어를 연 뒤 주행하고 시동 OFF 후 운전석 도어를 열 때, 2차는 약 10분 움직임 감지). [GM] 2016년 기사에 따르면 시동 전 10분 이내 또는 주행 중에 뒷문을 열고 닫았으면 시동 OFF 때 5회 차임과 'Rear Seat Reminder / Look in Rear Seat' 메시지를 낸다(2016년 자료). 2017년 Kids and Cars 글은 GM·닛산이 뒷문 활동을 감시하고 현대는 뒷좌석 움직임 센서를 쓴다고 비교한다(2017년 자료).
  [한계] 열람한 매뉴얼에서 재출발 확인 기능을 찾지 못했다는 뜻이며 모든 차종·브랜드에 없다는 증명은 아니다. 한 검색 요약은 2차 알림 감지 시간이 '8시간'인 현대 매뉴얼이 있다고 했으나 직접 확인하지 못했다.

  </details>

- **competitor-benchmark-07** 4위 '차량 이상 징후 일지'의 '알림 이력·정비 연결'은 현대 리모트 케어(선제케어)에 이미 있다. 남는 차별점은 순정의 지원 범위·가입 조건 한계와 인카 '미해결 카드'·가족 인수인계에 좁혀진다.
  - 근거: [A] [현대 리모트 케어 - 선제케어](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care/preemptive-care) (조회 2026-10-24); [A] [현대 리모트 케어 - 데이터 기반 정비(대상 차종)](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care/data-driven-maintenance) (조회 2026-10-24); [A] [현대 리모트 케어 개요](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care) (조회 2026-10-24); [B] [현대자동차 ... 선제 케어 서비스(한국경제)](https://www.hankyung.com/article/2025102120051) (2025-10-22); [B] [제네시스, 車고장 '선제 케어 서비스' 출시](https://dealsite.co.kr/articles/132134) (2024-11-25); [A] [현대 블루링크 차량 관리 - 차량 진단](https://www.hyundai.com/kr/ko/digital-customer-support/app/myhyundai/bluelink/bluelink-vehicle-care) (조회 2026-10-24); [A] [기아 커넥트 - 스마트 원격진단](https://www.kia.com/kr/kia-connect/service/vehicle-management/remote) (조회 2026-10-24)
  - 검증 메모: 세부 출처 표기만 고친다. (1) '월간 리포트(이메일)'는 블루링크 차량 관리 웹페이지(bluelink-vehicle-care)가 아니라 블루링크 취급설명서(ownersmanual.hyundai.com/ivi/STD_GEN5/.../009_BlueLink_6.html)에 있으므로 그 매뉴얼을 출처로 단다. (2) 블루링크 매뉴얼의 제한 문구는 '법인 차량, 임대 차량, 렌터카는 서비스 이용에 제한이 있을 수 있습니다'이다. '리스'라는 단어는 없으므로 기획서에는 원문대로 '법인·임대·렌터카'로 인용한다. (3) 인카 경고 이력이 없다는 점은 검증되지 않았으므로 차별점 근거로 쓰기 전에 매뉴얼로 확인한다.
  - 시사점: 4위는 '순정이 못 하는 기록·정비소 연결'로 쓰면 안 되고, 기획서에서 현대 리모트 케어(선제케어·데이터 기반 정비)를 정면 비교해야 한다. 차별점 후보는 (1) 블루링크 미가입·비대상 차종·법인·리스·렌트처럼 순정이 닿지 않는 이용자(블루링크 매뉴얼의 법인·리스·렌트 제한 문구가 근거), (2) 정차 시 인카 미해결 카드와 가족 인수인계 메모다. '순정이 오일·브레이크액·워셔액 레벨 경고를 놓친다'는 직접 근거는 없다. 현대가 앱·서비스를 빠르게 넓히는 중이라 선점 경쟁 위험(market-03)이 커졌다.
  <details><summary>상세</summary>

  [현대 선제케어] 주행 중 차량 데이터를 실시간 분석해 이상 징후를 감지하고, 마이현대 앱 또는 문자로 상황과 조치 방법을 먼저 알리며, 필요하면 상담사가 전화하고 긴급출동·견인·A/S를 연계한다. 알림 이력은 마이현대 앱 알림함에서 확인하고, 긴급 고장은 상담 후 정비를 예약할 수 있다. 한계로 '원격으로 확인 할 수 있는 데이터에 한해 … 모든 상황에 대해 알림을 제공하지는 않는다'고 적는다. 블루링크 가입·앱 등록·OTA 최신 버전이 필요하다. 출시 시점은 페이지에 없고, 한국경제 2025-10-22는 현대차가 원격진단 플랫폼으로 '선제 케어 서비스'를 제공하며 긴급도·위험도 등급별로 SMS·앱 푸시로 알린다고 보도한다.
  [대상 차종 충돌] 같은 현대 사이트 안에서 선제케어 페이지는 아이오닉 9·5·5N·6·6N, 코나 EV, 넥쏘만 나열하고, 리모트 케어 개요는 4개 차종만 적으며, 데이터 기반 정비 페이지는 그랜저 GN7·투싼 NX4 PE·싼타페·팰리세이드를 포함해 훨씬 넓게 나열한다.
  [그 밖의] 제네시스 선제 케어 서비스는 2024-11-25 출시(안심 케어=고장, 스마트 케어=단순 운행 이상, 앱·문자). 블루링크 '차량 진단'은 엔진·변속 경고등 점등 시 원격 1차 진단과 정비소 길 안내·견인·정비 예약(상담원)이고, 월간 리포트는 이메일이다. 기아 커넥트는 차량 진단과 스마트 원격진단(전화 예약 후 상담, EV9 포함 일부 신규 차종)을 제공한다. 클러스터 경고는 해소되면 사라지는 것으로 보이며 인카 경고 이력 목록은 매뉴얼에서 찾지 못했다(검색 기준, 직접 열람 못함).

  </details>

- **competitor-benchmark-08** 5위 '계기판 운행일지'의 규정 근거는 법인세법 시행령 제50조의2와 국세청고시 제2025-8호이며, 운행기록부 서식은 계기판 누적거리와 출퇴근용·일반업무용 거리 칸을 두지만 출발지·도착지·방문목적 칸은 없다.
  - 근거: [A] [법인세법 시행령 제50조의2(업무용승용차 관련비용 등의 손금불산입 특례)](https://www.law.go.kr/LSW/lsLawLinkInfo.do?lsJoLnkSeq=1000119197&lsId=003608&chrClsCd=010202&print=print) (2026-10-01); [A] [국세청고시 제2025-8호 업무용승용차 운행기록 방법에 관한 고시(별지 서식 포함)](https://www.law.go.kr/LSW/flDownload.do?flSeq=167393171) (2025-04-01); [A] [국세청 자주묻는Q&A - 업무용승용차 운행기록·1,500만원·800만원](https://call.nts.go.kr/call/qna/selectQnaInfo.do?mi=1647&ctgId=CTG12028); [B] [업무용 차량 운행일지 작성 가이드(세무 실무)](https://www.heumtax.com/contents/posts/driving-log-guide) (2025-03-31)
  - 검증 메모: 주장 자체는 맞다. 기획서에 인용할 때 주의할 점만 적는다. (1) 고시 전문은 위임 근거를 '시행령 제50조의2제5항'으로 적는데, 이는 개정 전 번호다. 현행(2026-10-01 시행) 시행령에서 운행기록 작성·비치 의무는 제6항, 미작성 시 비율은 제7항이므로 현행 항 번호로 쓴다. (2) 현행 제4항 단서에 따라 전용 번호판(연두색)을 달지 않으면 업무용 사용금액이 0원이라는 요건이 하나 더 있다. (3) heumtax 원문 표현은 '복식부기자', '전문직 종사자'이다. 개인사업자의 법적 근거는 법인세법이 아니라 소득세법 쪽이며, 이번에 A등급 원문으로 확인하지 못했다. 타깃에 개인사업자를 넣으려면 소득세법 조문을 따로 확인한다.
  - 시사점: 5위 카드의 '주행 전후 계기판 거리 + 출퇴근/업무/개인 분류'는 서식 칸(출퇴근용·일반업무용, 나머지는 개인 사용)과 맞는다고 기획서에 법령 번호와 함께 쓸 수 있다. 도착지 자동 기록은 서식 필수 칸이 아니라 '업무목적 소명'용 보조 자료로 설명해야 한다. 서식의 '사용자'는 운전자가 아닌 이용자이므로 카드의 운전자 프로필은 사용자 구분과 연결해야 하고, 하루 합계만 적어도 되므로 운행별 분할은 필수가 아니다. 타깃은 법인·복식부기 사업자 등이며, 영업직 직장인이 본인 차로 업무할 때의 의무는 확인하지 못했다.
  <details><summary>상세</summary>

  [법인세법 시행령 제50조의2(국가법령정보센터, 시행 2026-10-01, 2026-09-30 일부개정)] 제6항: 업무전용자동차보험 특례를 받으려는 내국법인은 업무용승용차별로 운행기록 등을 작성·비치하고 세무서장이 요구하면 즉시 제출한다. 제7항: 운행기록을 쓰지 않으면 관련비용 1,500만원 이하는 업무사용비율 100%, 초과 시 1,500만원÷관련비용이다. 제10항: 감가상각비는 연 800만원 한도다. 제15항: 제42조제2항 요건을 모두 갖춘 내국법인은 제7·10·11·13항의 1,500만원을 500만원, 800만원을 400만원으로 낮춰 적용한다(요건 내용은 확인 못함). 제4항(요약 기준): 업무전용자동차보험에 가입하지 않으면 업무용 사용금액이 0원이다.
  [국세청고시 제2025-8호(2025-04-01), 직접 열람] 제2조: 별지 서식 '업무용승용차 운행기록부'를 쓰되 차종·자동차등록번호·사용일자·사용자·운행내역이 포함되면 별도 서식도 가능하다. 제3조: 법인은 과세관청이 요청하면 업무용승용차 관리규정·출장명령서 등으로 업무목적을 소명해야 한다. 서식(2016-04-01 제정) 칸: 차종, 등록번호, 사용일자, 사용자(운전자가 아닌 차량이용자의 부서·성명), 주행 전·후 자동차 계기판의 누적거리, 주행거리, 출퇴근용(원격지 출·퇴근 포함), 일반 업무용(사업장·거래처 방문, 회의, 판촉, 교육·훈련), 비고, 사업연도 총주행거리·업무용 사용거리·업무사용비율. 같은 날 동일인이 2회 이상 쓰면 주행 전·후 거리를 생략하고 주행거리 합계만 적을 수 있다.
  [적용 대상] 세무 실무 글(heumtax, 2025-03-31)은 법인사업자와 성실신고확인대상자·복식부기의무자·전문직 개인사업자가 운행일지·업무전용보험 대상이라고 설명한다. 국세청 Q&A는 관련비용이 1대당 연 1,500만원 이하이면 전용보험만으로 전액 인정, 초과 시 운행기록이 필요하다고 안내한다(게시일 미표기).

  </details>

### ✏ 반박되어 고친 내용

- **competitor-benchmark-01** 현대·기아 순정 알림 중 '서비스 인터벌'(설정 메뉴)과 기아 'Service in' 메시지는 사용자가 정하거나 정해진 주행거리·기간 기준이다. 그러나 엔진오일은 일부 차량에 운전 이력 기반 Oil Life Management System(잔여 오일 수명 표시, 가혹 주행 시 조기 알림)이 있다(현대 2025 투싼 미국 매뉴얼, 기아 2026 K5 미국 매뉴얼, 현대 호주 인포테인먼트 'Oil change reminder'). 따라서 '실제 사용량 기준'은 엔진오일에서는 현대·기아 대비 차별점이 아니다. 차별점은 엔진오일 외 소모품(와이퍼 작동 시간, 공조 풍량에 따른 필터 등)을 사용량 기준으로 알리는 것으로 좁혀 써야 한다. 국내 차량 탑재 여부는 국내 매뉴얼로 다시 확인한다. '업계 최초' 표현은 계속 쓰지 않는다. BMW CBS 설명은 aa1car가 인용한 2006·2007·2008년 기술 공지 기반(2006~2008년 자료)이라고 밝힌다.
  - 반박된 원 주장은 인용하지 않는다.
  - 근거: [A] [Hyundai 인포테인먼트 차량 설정 - Service interval (Distance/Interval/Reset)](https://ownersmanual.hyundai.com/ivi/ccNC/AVNT/AUS/English/Settings_vehicle.html) (조회 2026-10-24); [A] [Kia 매뉴얼 - 'Service in' 메시지(1,500km 또는 30일)](https://ownersmanual.kia.com/full_webhelp/MQ4KH/2024/en_RG/topics/chapter5_14_2.html) (2024); [A] [현대 블루링크 취급설명서 - 차량 진단·월간 리포트(소모품 교환 알림 항목 없음)](https://ownersmanual.hyundai.com/ivi/STD_GEN5/AVNT/KOR/Korean/009_BlueLink_6.html) (조회 2026-10-24); [A] [현대 리모트 케어 - 데이터 기반 정비(소모품 언급 없음)](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care/data-driven-maintenance) (조회 2026-10-24); [A] [기아 커넥트 차량관리 메뉴(소모품 교환 알림 없음)](https://www.kia.com/kr/kia-connect/service/vehicle-management) (조회 2026-10-24); [B] [BMW Condition Based Service System (aa1car)](https://www.aa1car.com/library/bmw_condition_based_service_system.htm)
  - 시사점: 기획서에는 '현대·기아 순정 알림은 주행거리·기간 기준'까지만 사실로 쓰고, '업계 최초 사용량 기반'은 쓰지 않는다. 브레이크 사용은 BMW도 패드 마모 계산에 쓴다고 서술돼 차별점이 약하다. 와이퍼 작동 시간·공조 풍량 같은 신호는 서술을 못 찾아 차별점으로 내세울 수 있다. 현대·기아가 서비스 인터벌을 사용 조건 기반으로 확장하면 겹칠 위험이 있다.
  <details><summary>상세</summary>

  [현대·기아] 현대 인포테인먼트 설정(Settings▶Vehicle▶Convenience)의 '서비스 인터벌'은 Enable / Distance(주행거리 후 점검 알림) / Interval(시간 경과 후 점검 알림) / Reset 네 항목이다. 기아 매뉴얼은 남은 거리·시간이 1,500km 또는 30일에 이르면 시동 ON마다 'Service in' 메시지를 몇 초 표시하고, 배터리 분리·방전 시 거리·일수가 부정확해질 수 있다고 적는다(모델·연식 미표기). 현대 블루링크 매뉴얼은 차량 진단(엔진·변속 경고등 점등 시)과 월간 리포트(점검 결과+주행 정보 이메일)를 안내하며 소모품 교환 알림 항목이 없다. 현대 리모트 케어/데이터 기반 정비 페이지는 소모품(엔진오일·브레이크 패드·에어컨 필터·와이퍼)을 언급하지 않는다. 기아 커넥트 '차량관리' 메뉴(차량 진단·무선 업데이트·배터리 방전 알림·안전 운전 습관·스마트 원격진단)에도 소모품 교환 알림이 없다.
  [BMW CBS] 업계 기술 사이트 aa1car(발행일 미표기, 2006·2008년 기술 공지를 인용)는 에어컨 필터가 기본 약 32,000마일 추정에서 rain sensor·외기온·팬 속도·내외기 순환·주행 속도·히터 사용으로 보정되고, 브레이크 패드는 마모 센서와 주행거리·속도·브레이크 사용으로 계산되며, 엔진오일은 연료 소비·오일 품질로 정해지고, 브레이크액은 24개월 시간 기준이라고 서술한다. 와이퍼 감시는 서술이 없다. BMW 공식 기술 문서(bmwtechinfo SIB PDF는 404, BMW USA 정비 PDF는 시간 초과)는 열람하지 못해 BMW 쪽은 2차 자료로만 확인했다. Mercedes ASSYST PLUS가 운전 습관을 반영한다는 서술은 2차 자료뿐이고 공식 매뉴얼에서는 확인하지 못했다.
  [판정] '못 찾음'은 '없음'의 증명이 아니다. 현대·기아가 앱·클러스터 외 경로로 소모품 알림을 제공할 가능성은 남는다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **competitor-benchmark-02** 소모품 교환 알림을 주는 서드파티 앱과 마이현대는 누적 주행거리와 날짜 주기를 기준으로 하고, 와이퍼·공조·브레이크 사용량으로 교체 시점을 계산하는 앱은 찾지 못했다.
  - 근거: [C] [마이클 - 차량관리, 엔진오일, 타이어, 세차, 예약 (App Store)](https://apps.apple.com/kr/app/%EB%A7%88%EC%9D%B4%ED%81%B4-%EC%A0%95%EB%B9%84%EC%86%8C-%EC%98%88%EC%95%BD-%EC%97%94%EC%A7%84%EC%98%A4%EC%9D%BC-%ED%83%80%EC%9D%B4%EC%96%B4/id1004325731) (2026-10); [B] [현대차, 통합 고객 서비스 앱 '마이현대 2.0' 출시](https://www.dailian.co.kr/news/view/1033887/) (2021-09-16)
  - 시사점: 앱 시장의 기존 방식(주행거리·날짜)과 구분되는 '실제로 쓴 만큼'을 앱 비교표에 쓸 수 있다. 마이클의 평점·평가 수는 앱스토어 표기이므로 정비·교환 알림 수요를 보여 주는 정성 참고로만 쓰고 시장 규모 수치로 인용하지 않는다.

- **competitor-benchmark-04** 국내에서 승용차 후석 방치 사고를 집계한 공식 통계는 찾지 못했고, 국내 법적 의무는 통학버스 하차 확인 장치(2019-04-17 시행)에 한정된다. 통계로 인용할 수 있는 것은 미국 자료뿐이다.
  - 근거: [B] [통학버스서 어린이 하차 확인 안하면 범칙금 13만원](https://m.go.seoul.co.kr/news/2019/04/10/20190410013015) (2019-04-10); [A] [강남구 - 어린이 통학버스 하차 확인 의무 안내](https://www.gangnam.go.kr/board/article/2522/view.do?mid=fm0503) (2019-04-29); [B] [NoHeatStroke.org - 미국 아동 차량 열사병 사망 통계](https://www.noheatstroke.org/) (2026-09-28); [B] [Hartford advocates push hot car alert systems](https://www.wfsb.com/2026/06/08/hartford-advocates-push-hot-car-alert-systems/) (2026-06-08); [C] [French family forgets child at motorway stop](https://www.thelocal.fr/20150809/french-family-forgets-child-at-motorway-layby) (2015-08-09); [C] [Parents in Croatia drive 125 miles before realising child left at rest stop](https://jamaicaobserver.com/2024/08/02/parents-croatia-drive-125-miles-realising-child-left-rest-stop) (2024-08-02)
  - 시사점: 기획서 2번(기획 배경)에 국내 수치를 쓸 근거가 없다. 미국 통계는 '해외 사례'로 출처·국가를 밝혀 보조로만 쓰고, 국내 페인포인트는 오너 커뮤니티·인터뷰로 보강한다. 휴게소 재출발 사례는 해외 보도(C등급 검색 요약)뿐이라 정성 근거로만 쓴다.

- **competitor-benchmark-05** Tesla Dog Mode, 기아 Pet Mode, 현대 Pet mode는 모두 '차를 세우고 반려동물을 둔 채 내릴 때 공조를 유지'하는 주차 중 기능이고, 반려동물 탑승 중 창문 열림·하차 시 공조 상태·리드줄 안내를 감시하는 기능은 찾지 못했다. 3위는 방향이 다르다.
  - 근거: [A] [Kia 매뉴얼 - Pet mode (if equipped)](https://ownersmanual.kia.com/full_webhelp/OV1K/2026/en_RG/topics/t00348.html) (2026); [A] [Hyundai 매뉴얼(EU) - Pet mode](https://ownersmanual.hyundai.com/ivi/ccNC_Lite/AV/EUR/English/Petmode.html) (조회 2026-10-24); [B] [Tesla Dog Mode: What It Does and How It Works](https://www.notateslaapp.com/news/603/tesla-dog-mode-everything-you-want-to-know) (2023-07-14); [C] [Tesla Model Y Owner's Manual - Keep Climate On, Dog, and Camp (직접 열람 실패, 검색 요약 기준)](https://www.tesla.com/ownersmanual/modely/en_us/GUID-4F3599A1-20D9-4A49-B4A0-5261F957C096.html) (2026)
  - 시사점: 'Tesla Dog Mode와 같다'는 지적에는 '순정 펫 모드는 주차 중 공조 유지, 이 앱은 주행 중 창문 열림과 하차 순간 경고'로 구분해 답할 수 있다. 순정 펫 모드는 반려동물을 두고 내리는 것을 전제로 하므로, '두고 내리지 마세요'라는 문구는 '공조 없이 두고 내리지 않게'처럼 순정 모드와 충돌하지 않게 다듬어야 한다. 펫 모드는 전기차 위주이고 Pleos 주력 차종(내연·하이브리드 포함)에는 순정 대안이 약할 가능성이 있다(추정).

- **competitor-benchmark-06** 3위의 두 경고(주행 중 창문 열림, 하차 전 리드줄)와 맞는 국내 정부 안전 수칙은 있으나, 차 안 불편을 측정한 통계는 아니다. 동물을 안고 운전하는 것은 도로교통법 제39조제5항 위반이다.
  - 근거: [A] [국토교통부 정책뉴스 - 반려동물과 함께하는 자동차 안전수칙](https://m.korea.kr/news/policyNewsView.do?newsId=148885333) (2021-03-23); [A] [농림축산식품부 정책뉴스 - 동물을 안고 운전해도 될까?](https://www.korea.kr/news/policyNewsView.do?newsId=148862582) (2019-07-11); [A] [2026 기아 타스만 - 후석 승객 알림(동물 감지 한계)](https://ownersmanual.kia.com/full_webhelp/TK1/2026/ko_KR/topics/t00375.html) (2026)
  - 시사점: 기획서 2번에 '정부 안전 수칙이 권고하는 행동(창문 환기 수준, 하차 전 목줄)을 차가 대신 상기시켜 준다'는 근거로 쓸 수 있다. 다만 이 수칙들은 불편의 크기가 아니라 권고이므로 페인포인트 단정에는 쓰지 않는다. 동물 감지가 약하므로 펫 자리를 수동 지정하는 설계의 근거가 된다.

- **competitor-benchmark-09** 운행일지 자동 작성 앱·서비스는 이미 있다(스마트폰 GPS 앱, LG U+ 단말 서비스, 국세청 지방청 엑셀 프로그램). 이들은 GPS·단말 기반이고, 서식이 요구하는 '자동차 계기판 누적거리'를 차량 신호로 직접 쓰는 순정 연계는 찾지 못했다.
  - 근거: [C] [차량운행일지 - 자동운행기록 작성앱 (App Store)](https://apps.apple.com/kr/app/%EC%B0%A8%EB%9F%89%EC%9A%B4%ED%96%89%EC%9D%BC%EC%A7%80-%EC%9E%90%EB%8F%99%EC%9A%B4%ED%96%89%EA%B8%B0%EB%A1%9D-%EC%9E%91%EC%84%B1%EC%95%B1/id1105310530?l=ko) (2025-10-13); [A] [차량 관제 DX 설루션 U+커넥트 출시(LG유플러스)](https://lg.co.kr/media/release/28492) (2024-12-23); [A] [LG U+ B2B 인사이트 - 법인차 운행일지 자동 생성](https://www.lguplus.com/biz/insight/story/900) (2026-04-21); [A] [부산지방국세청 - 차량운행기록부 작성 프로그램 게시(소규모 사업자용)](https://www.nts.go.kr/busannts/na/ntt/selectNttInfo.do?nttSn=1325716&mi=5960) (2023-07-17); [A] [현대 블루링크 - 정기 점검 리포트(법인·리스·렌트 제한 문구)](https://www.hyundai.com/kr/ko/digital-customer-support/app/myhyundai/bluelink/bluelink-vehicle-care/regular-inspection-report) (조회 2026-10-24)
  - 시사점: '운행기록 앱 자체는 새롭지 않다'는 지적은 사실이다. 차별점은 '계기판 값을 차량에서 직접 받아 서식 칸에 맞춘다'(GPS 거리는 계기판 값과 어긋날 수 있다는 카드의 주장은 이번 조사에서 수치로 검증하지 못함)와 '차 화면에서 P 전환 시 1탭 분류'로 좁혀 써야 한다. 앱 평점·평가 수는 앱스토어 표기라 시장 규모 근거로 쓰지 않는다. 순정 서비스가 법인 차량 이용을 제한한다는 문구는 법인 타깃의 빈틈 근거가 될 수 있다.

- **competitor-benchmark-10** 종합: 상위 5개 아이디어의 기존 기능 대비 차별점은 2위(재출발 시점)와 3위(주행 중 창문·하차 경고)가 가장 분명하고, 1위(소모품)는 현대·기아 대비에서만 유효하며, 4위(이상 징후 일지)는 순정과 가장 많이 겹친다.
  - 근거: [A] [2025 현대 투싼 - 후석 승객 알림 시스템](https://ownersmanual.hyundai.com/full_webhelp/NX4/2025/ko_KR/id229JA800L5Z.html) (2025); [A] [현대 리모트 케어 - 선제케어](https://www.hyundai.com/kr/ko/service-membership/car-management-service/remote-care/preemptive-care) (조회 2026-10-24); [A] [국세청고시 제2025-8호 업무용승용차 운행기록 방법에 관한 고시](https://www.law.go.kr/LSW/flDownload.do?flSeq=167393171) (2025-04-01); [A] [국토교통부 정책뉴스 - 반려동물과 함께하는 자동차 안전수칙](https://m.korea.kr/news/policyNewsView.do?newsId=148885333) (2021-03-23)
  - 시사점: 점수 차가 작은 1~5위에서 '순정과 얼마나 겹치는가'를 선택 기준에 추가할 수 있다. 3·2위는 순정 공백이 비교적 뚜렷하고, 4위는 현대 리모트 케어와 정면 비교가 필요하다. 이 순위 판단은 조사자의 해석이며 순정 기능이 없다는 직접 증명은 아니다.

### 조사 공백
- 참조 파일 .claude/skills/pleos-research/references/axes.md는 없고 axes.json만 있어 axes.json과 _workspace/01_research/axes_supplement.json의 축 정의를 따랐다.
- BMW CBS 공식 문서는 열람하지 못했다(bmwtechinfo SIB PDF 404, BMW USA 정비 PDF 시간 초과). 사용 조건 반영(에어컨 필터 팬 속도·내외기 순환, 브레이크 사용)은 2차 자료 aa1car(발행일 미표기, 2006·2008 공지 인용)로만 확인했다. 와이퍼 교체 알림을 사용량으로 계산하는 순정 사례는 찾지 못했고 Mercedes ASSYST PLUS의 사용량 기반 계산은 공식 매뉴얼에서 확인하지 못했다.
- Tesla 공식 매뉴얼·지원 페이지와 GM Canada 지원 페이지는 403으로 열람하지 못해 Dog Mode 세부(창문 상태 등)는 검색 요약과 언론(notateslaapp 2023)으로만 확인했다. Rivian·Volvo·Polestar·중국 브랜드의 펫 모드는 확인하지 못했고, 기아 Pet Mode 매뉴얼(OV1K)의 차종과 현대·기아 펫 모드의 국내 판매 차종 적용 여부는 확인하지 못했다.
- '못 찾음'과 '없음'은 다르다. 순정 재출발 빈자리 확인, 사용량(와이퍼·공조·제동) 기반 소모품 알림, 인카 경고 이력 목록, 주행 중 창문 감시형 펫 기능은 열람한 자료에서 찾지 못했을 뿐 모든 차종·연식에서 없다는 증명이 아니다.
- 국내 후석 방치 사고의 공식 통계(소방청·도로교통공단·한국소비자원)와 국내 '휴게소에 아이를 두고 출발' 사례, 반려동물 동승 불편 통계, 소모품 교체 시기를 놓치는 비율을 다룬 국내 설문은 찾지 못했다. 미국 NoHeatStroke 수치만 확인했고 국내 수치로 쓸 수 없다.
- Euro NCAP 어린이 존재 감지(CPD) 평가와 NHTSA 후석 안전벨트 경고 최종 규칙(2025-01 확정, 준수 2027-09-01로 보도)은 원문을 열람하지 못해 이번 발견 사항에서 제외했다. 한 검색 요약이 말한 '2차 알림 감지 8시간 표기 현대 매뉴얼'도 직접 확인하지 못했다.
- 현대 리모트 케어 페이지 간 대상 차종 목록이 서로 달라(선제케어 EV 위주, 데이터 기반 정비는 그랜저 GN7·투싼 NX4 PE 포함) 어느 차종이 실제 대상인지 확정하지 못했다. 출시 시점도 명시되지 않았다. 기아의 선제케어 대응 서비스 출시 여부는 확인하지 못했다.
- 업무용승용차 운행기록부 의무가 영업직 직장인(근로소득자)이 본인 차를 업무에 쓸 때도 적용되는지, 소득세법 제33조의2·시행령 제78조의3의 개인사업자 적용 범위, 법인세법 시행령 제50조의2제15항이 가리키는 제42조제2항 요건은 직접 확인하지 못했다. 국세청 Q&A는 게시일이 없다. Pleos 탑재 차종 오너 중 법인·복식부기 개인사업자 비중 자료도 없다.
- 질문 해석: '후석 승객 방치 사고 통계'는 국내를 우선했으나 없어 미국 통계를 병기했다. 'GPS 앱 거리가 계기판 값과 어긋난다'는 카드의 주장은 정량 자료를 찾지 못해 검증하지 않았다. 앱스토어 평점·평가 수는 C등급 정성 참고로만 두었다.

## 파격 아이디어 4개의 경쟁·유사 사례 (`wow-benchmark`)

### ✅ 확인된 사실

- **wow-benchmark-06** W3: 차량 센서 기반 크라우드 도로 날씨·위험 경고는 해외 완성차에서 이미 상용이다. Ford Local Hazard Information은 와이퍼 작동·안개등·에어백·비상등 신호를 쓴다. Audi도 비·조명 센서와 와이퍼를 쓴다. Mercedes는 폭우·안개 경고를 제공하지만 와이퍼 트리거는 공식 페이지에서 확인하지 못했고, 언론 보도와 엇갈린다.
  - 근거: [A] [Ford press release: Connected Car Technology Now Warns Drivers of Dangers Around the Corner](https://www.fordpers.be/content/documents/news/2020/2020_01_13_local_hazard_information_en.pdf) (2020-01-13); [A] [Mercedes-Benz Hazard Warnings (Connectivity)](https://connectivity.mercedes-benz.com/products/hazard-warning) (2026-10-24); [A] [Audi: Precise data for greater safety (Local Hazard Information, 검색 결과 요약만 확인)](https://audi-mediacenter.com/en/press-releases/precise-data-for-greater-safetyaudi-warns-its-drivers-about-slippery-roads-13799) (2021-03-08); [B] [Mercedes Car-to-X 경고 목록과 트리거 (Automotive World, 403으로 열지 못하고 검색 요약만 확인)](https://www.automotiveworld.com/?p=342704) (2021)
  - 검증 메모: 표현을 조금 다듬을 필요가 있다. (1) Mercedes 공식 페이지는 와이퍼를 '부정'하지 않고 '언급하지 않을' 뿐이다. 그런데 2021년 보도 여러 건(driving.co.uk는 'the car maker says'로 제조사 설명을 인용, Automotive World 재게재 자료)은 '와이퍼 최고 속도'를 폭우 경고 트리거로 쓴다. 이 둘은 '충돌'이라기보다 '공식 데이터 상품 페이지에는 없고 보도에는 있다'로 적는 편이 정확하다. 보도 원문은 이번에도 403과 404로 열지 못해 검색 요약으로만 확인했다. (2) '반경 3km'는 열어 본 출처 가운데 evoindia(2021-10-08, 인도 S-Class 출시 기사)에만 있었다. Automotive World와 NewMobility의 내용으로 묶지 말고 출처를 따로 적는다. (3) Ford 80% 확대 계획은 Ford of Europe 기준이라고 밝힌다. 시사점의 'Mercedes 와이퍼 트리거는 공식 근거가 없다'는 '공식 데이터 상품 페이지로는 확인되지 않는다'로 낮춰 쓴다.
  - 시사점: '와이퍼·안개등 신호로 도로 날씨를 모은다'는 아이디어 자체는 새롭지 않다. W3의 차별점은 신호가 아니라 배포 방식이다. 자사 차량에 갇힌 서비스가 아니라 개방형 앱마켓 앱이라는 점, 그리고 국내 도로를 대상으로 한다는 점이다. 현대·기아 국내 차량에 같은 기능이 있는지는 이번 검색에서 확인하지 못했다(gaps). Ford는 다브랜드 클라우드로 네트워크 효과를 해결했는데 W3는 Pleos 탑재 규모가 작아(market-01) 초기 가치가 약하다. 기획서에는 '해외 OEM이 상용화했고, 국내 개방형 앱마켓 구현'이라고 쓰고, 최초라는 표현은 쓰지 않는다. Mercedes 와이퍼 트리거는 공식 근거가 없으므로 Ford 사례를 대표 근거로 쓴다.
  <details><summary>상세</summary>

  Ford 공식 보도자료(2020-01-13, 2020년 자료): 시스템의 트리거는 '앞 차량에서 벌어지는 일'이며 '에어백 작동, 비상등 점멸, 또는 와이퍼 작동'일 수 있다. 센서는 긴급 제동·안개등·트랙션 컨트롤을 감시해 악천후나 도로 상태를 판단한다. HERE Technologies가 여러 브랜드의 정보를 모으는 클라우드 플랫폼을 운영하고, 정보는 다른 제조사 차량에도 전달된다. 2020년 말까지 승용 라인업의 80% 이상에 확대할 계획이었고, 고객은 일부 데이터 공유를 옵트인·옵트아웃할 수 있으며 첫 1년 뒤에는 이용료가 붙는다. Audi 공식 보도자료(2021-03-08, 검색 요약으로 확인, 원문 페이지는 열지 못함): ESC·비 센서·조명 센서·와이퍼·헤드라이트·비상 호출·에어백을 분석하고, 휠 슬립 기반 마찰계수 추정을 더했다. 2021년 유럽 폭스바겐그룹 차량 170만 대 이상이 데이터를 낼 것이라는 업계 매체 보도가 있다. Mercedes 공식 Hazard Warnings 페이지(B2B 데이터 상품, 열람일 2026-10-24): 데이터 항목에 Heavy rain·Fog/reduced visibility·Slippery road·Crosswind 등이 있고 ABS·ESP·타이어 센서를 근거로 든다. 이 페이지는 와이퍼와 안개등을 언급하지 않으며 소유자 동의를 받은 차량에서만 수집한다고 밝힌다. 반면 업계 매체(Automotive World, NewMobility 2021; 검색 요약으로만 확인)는 Mercedes Car-to-X에서 '와이퍼 최고 속도'가 폭우 경고를 일으키고 반경 3km 안의 차량에 알린다고 쓴다. 두 출처가 충돌하므로 둘 다 남긴다.

  </details>

### ⚠ 불확실

- **wow-benchmark-04** ⚠ 미확인 — W2: 운전 습관과 차량 상태에 따라 캐릭터가 자라는 '다마고치형' 제품은 국내외 검색에서 찾지 못했다. 가장 가까운 사례는 점수·레벨·배지형 운전 앱이며 이 영역은 이미 붐빈다. 효과 연구도 점수표가 게임화보다 크다고 본다.
  - 근거: [B] [티맵모빌리티 자동차보험 비교 이벤트 (UBI 국내 1위, 가입자 500만 육박)](https://www.fnnews.com/news/202606261004583062) (2026-06-26); [B] [A new app from Carrot rewards safe drivers with cash incentives](https://cioworldasia.com/2022/06/13/a-new-app-from-carrot-rewards-safe-drivers-with-cash-incentives/) (2022-06-13); [A] [CoastMaster: An Ambient Speedometer to Gamify Safe Driving (AutomotiveUI '16)](https://www.medien.ifi.lmu.de/pubdb/publications/pub/steinberger2016autoui/steinberger2016autoui.pdf) (2016-10); [A] [Impacts of different feedback features on driver behaviour: multiphase naturalistic study (Transportation Research Part F, Vol.120)](https://trid.trb.org/View/2707998) (2026-05); [B] [Ford debuts SmartGauge with EcoGuide (검색 결과 요약만 확인)](https://www.motorauthority.com/news/1030484_ford-debuts-smartgauge-tech-on-new-fusion-hybrid) (2009)
  - 검증 메모: detail의 'Tmap은 운전점수 할인을 자동차 용품·생명보험으로 넓힐 계획'을 fnnews 원문대로 '자동차 용품 구매와 건강보험 등 각종 생활보험으로 확대할 예정'으로 고친다. '가장 가까운 사례'에 다음을 더한다. 'Allstate 특허 US9858832B1(2013 우선, 2018 등록, 존속 추정 2033)은 운전 데이터·점수로 게임 캐릭터의 건강·행복·배고픔 같은 속성을 바꾸는 개념을 담고 있다. 제품화 근거는 없다.' 시사점에는 '애착형 캐릭터'를 차별점으로 내세우기 전에 이 특허의 청구항과 겹치는지 검토해야 한다는 리스크를 적는다. 캐롯 등급 5단계와 레벨 1~30은 이제 이코노미스트 2022-05-18 기사로 확인됐으므로 '검색 요약만 확인' 표시를 지워도 된다. 기획서에는 '유사 제품은 검색한 범위에서 확인하지 못했다. 다만 보험사(Allstate) 특허로 개념은 공개돼 있다'로 쓴다.
  - 시사점: 차별점은 '운전 점수'가 아니라 '차량 상태까지 포함한 애착형 캐릭터와 정차 회고'로 좁히는 것이 사실에 맞다. 점수·배지·리더보드형은 Tmap·캐롯이 이미 점유했다. 다만 효과 연구는 점수표의 효과가 가장 크다고 보므로, '점수표는 금방 질린다'는 W2의 전제는 국내 근거가 없을 뿐 아니라 이 연구와도 맞지 않는다. 정차 후 회고 카드에는 숫자 요약(급제동 횟수, 연속 운전 시간)을 반드시 남겨 두는 편이 근거에 맞다. 기획서에는 '유사 제품 없음'이 아니라 '검색한 범위에서 확인하지 못함'이라고 쓴다.
  <details><summary>상세</summary>

  가장 가까운 사례: (1) Ford SmartGauge EcoGuide(2010년형 Fusion Hybrid, 2009년 자료): 효율적으로 운전하면 계기판 덩굴과 잎이 풍성해지고 급가속·급제동은 잎을 떨어뜨린다(검색 요약만 확인). (2) 캐롯손해보험 '캐롯 멤버스 오토'(2022-05 출시): 미션, 진행도, 레벨, 리더보드, '레벨과 배지가 있는 가상 캐릭터', 보험료 납부에 쓰는 보상. 영문 기사가 가상 캐릭터를 언급하지만 국내 검색에서 캐릭터 기능은 확인하지 못했다. 등급 5단계·레벨 1~30은 검색 요약만 확인했다. (3) Tmap 운전점수 기반 UBI는 가입자가 500만 명에 육박한다(fnnews 2026-06-26). Tmap은 운전점수 할인을 자동차 용품·생명보험으로 넓힐 계획이다. (4) 학술 검토(AutoUI'16, 2016년 자료): 운전 관련 스마트폰 앱 21개를 검토했더니 14개가 안전운전을 표방하고 11개가 게임 요소, 10개가 실시간 피드백을 쓰며, 네 기준(앰비언트·게임화·안전운전·실시간 피드백)을 모두 충족한 앱은 없었다. (5) 효과 근거(Transportation Research Part F, 2026-05): 운전자 175명, 21개월, 트립 73,869건. 스마트폰 앱 피드백이 과속 시간과 급제동을 줄였다. 효과가 가장 큰 기능은 점수표(scorecard)였고, 경쟁·도전 같은 게임화가 두 번째였다. 급가속은 오히려 늘었다. 이 검색은 영어·한국어 앱스토어·뉴스 검색이라 '없다'는 단정은 못 한다.

  </details>

- **wow-benchmark-09** ⚠ 미확인 — W4: 위치 기반 오디오 투어는 성숙한 시장이고 차량 내장 앱으로도 들어왔다(Autio는 2026-08 Ford·Lincoln 차량에 탑재, Mercedes는 독일 아우토반 Tourguide). 그러나 내비 경로와 남은 시간에 맞춰 이야기가 지점 도착에 맞춰 끝나도록 재생 시점을 계산하는 서비스는 찾지 못했다.
  - 근거: [A] [VoiceMap: How does a VoiceMap tour work?](https://support.voicemap.me/how-does-a-voicemap-tour-work/) (2026-10-24); [B] [Autio vs Shaka Guide 2026 (Autio 자사 블로그, 경쟁사 수치는 자체 주장)](https://autio.com/here-and-there/autio-vs-shaka-guide-2026) (2026-04-30); [B] [Autio gains dashboard placement in five Ford and Lincoln models](https://ppc.land/autio-gains-dashboard-placement-in-five-ford-and-lincoln-models/) (2026-08-05); [B] [Mercedes Rolls Out Custom Voice AI Tourguide Feature From Cerence](https://voicebot.ai/2022/07/18/mercedes-rolls-out-custom-voice-ai-tourguide-feature-from-cerence/) (2022-07-18); [B] [나의 관광 오디오 가이드 '오디'랑 어디로 떠나볼까? (정책브리핑)](https://v.daum.net/v/6zfkIWrRzT?f=p) (2025-02-04); [A] [오디 - 국내 여행 오디오 가이드 (한국관광공사 앱스토어 소개)](https://apps.apple.com/kr/app/id739645554) (2026-10-24)
  - 검증 메모: Autio 관련 'Bluetooth·CarPlay 연결'은 인용 출처에 CarPlay가 없으므로 'Bluetooth 연결'로 고친다. 오디는 '콘텐츠 규모 수치는 찾지 못했다'에 '앱스토어 소개에 국내외 100만 명 이상 사용(자체 수치)이라는 문구가 있다'를 더한다. 선행 사례로 다음을 추가한다. 'holoride(Audi 분사, 2019 Porsche 발표)는 내비 데이터로 계산한 여정 시간에 맞춰 콘텐츠 길이를 바꾸는 elastic content를 공개적으로 내세웠다. 2022년 제품을 출시했고 2024-05 파산을 신청했다. 길이 맞춤 기능의 출시 여부는 미확인이다. 남은 이동 시간에 맞춰 오디오 콘텐츠를 고르거나 늘이고 줄이는 특허와 출원도 2008년부터 있다.' 따라서 기획서의 차별점은 '여정 시간 맞춤'이 아니라 '내비 경로상 개별 지점 도착 시각에 이야기 끝을 맞추는 재생 스케줄링'으로 더 좁혀 쓰고, '확인하지 못함'과 '관련 특허 존재'를 같이 적는다. 시사점의 플랫폼 서술도 고친다. 프로젝트 CLAUDE.md는 NaviHelper 현재 위치(37.4127, 127.0947)를 '고정'이 아니라 '에뮬레이터 기본값'이라고 적는다. 'NaviHelper 이벤트가 내비 앱 구현에 달려 있다'는 research-digest platform-05가 [unverified]로 둔 항목이므로 '에뮬레이터 실측 필요'로 표시한다.
  - 시사점: W4의 '경로 맞춤 오디오 가이드'에서 지킬 수 있는 차별점은 '내비 경로·남은 시간으로 이야기가 지점에 맞춰 끝나는 재생 시점 동기화' 하나로 좁혀진다. 차량 내장형 이야기 앱이 없다거나 국내 오디오 가이드가 없다는 주장은 Autio(Ford 탑재)와 오디(공식 앱)가 있어 쓸 수 없다. 이 동기화 서비스를 찾지 못한 것은 검색 범위의 한계이므로 기획서에는 '확인하지 못함'으로 쓴다. Pleos에서는 NaviHelper 이벤트가 내비 앱 구현에 달려 있고 에뮬레이터 현재 위치가 고정이라 이 차별점의 시연 가능성이 가장 큰 리스크다(research-digest platform-05, 프로젝트 CLAUDE.md).
  <details><summary>상세</summary>

  VoiceMap(공식 지원 페이지): 도착하면 오디오가 자동 재생되고 화면을 켜 둘 필요가 없다. 대부분 도보 투어이지만 운전·자전거·보트 투어도 있고, 정류장 사이 턴바이턴 안내와 오프라인 사용을 제공한다. 앱스토어 소개에는 약 2,000개 투어·80개국이 적혀 있다(검색 요약만 확인). Autio(자사 블로그 2026-04-30, 경쟁사 비교 글이라 주장은 자체 보고): 스토리 25,000개 이상, 설치 90만 이상, 백그라운드에서 GPS로 자동 재생, 블루투스·CarPlay 연결. Shaka Guide는 약 90개 투어, 설치 50만 이상, 투어당 15~25달러이며 GPS로 내레이션이 시작된다. 내비게이션 연동 여부는 두 앱 모두 명시되지 않았다. Autio 차량 탑재(ppc.land 2026-08-05): 2025년형 이후 Ford Explorer·Expedition, Lincoln Aviator·Navigator, 2024년형 이후 Lincoln Nautilus에서 Premium Connectivity 요금제로 쓴다. 정차 중에는 지도의 지점을 눌러 재생하고 주행 중에는 도시·경관도로·랜드마크를 지날 때 자동으로 시작한다. 미국 라이브러리 12,000개 이상이고 '무엇이 재생될지는 플레이리스트가 아니라 위치가 정한다'고 쓴다. 기사에 내비 연동은 언급이 없다. Mercedes Tourguide(Voicebot 2022-07-18, 독일 한정): '헤이 메르세데스, 투어가이드 시작'으로 켜면 아우토반의 갈색 관광 표지판 약 3,400개를 지날 때 해설이 시작되고, 소개받은 장소를 바로 내비 목적지로 지정할 수 있으며 90분간 활동이 없으면 꺼진다. Cerence Tour Guide(2021, 검색 요약만 확인)에는 경로가 정해진 경우의 Planned Tour 모드가 있으나 경로 타임라인에 맞추는 규칙은 공개 자료에 없다. 국내: 한국관광공사 '오디(Odii)'는 무료 관광 오디오가이드이고 도착하면 GPS로 자동 해설이 나오며 관광택시 연계도 있다(정책브리핑 2025-02-04). 앱스토어 평점은 4.5(98건)이며 콘텐츠 규모 수치는 찾지 못했다. 이야기를 '도착 시각에 맞춰 끝내는' 서비스를 찾으려 두 번째 검색을 했으나 위치 반경(WayTale 250m)에 따른 트리거만 나왔다.

  </details>

### ✏ 반박되어 고친 내용

- **wow-benchmark-01** '둘 다 양산 서비스로 자리 잡았다는 근거는 찾지 못했다'를 다음처럼 고친다. 'Ford Mach-E 방탈출(2022-01)은 작동하는 콘셉트이고 양산 근거는 없다. Tesla의 Beach Buggy Racing 2(2019-06)는 OTA로 고객 차량에 배포된 양산 기능이며 실제 핸들·브레이크를 입력으로 쓰고 P단에서만 작동한다. 2026년 현재도 제공되는지는 확인하지 못했다.' TopGear 관련 문장도 고친다. 'TopGear는 주행 중에는 히터와 스피커만 제어한다고 쓰고, 정차 중에만 열리는 과제를 따로 말하지는 않는다.' 시사점에도 반영해야 한다. 차 조작을 게임 입력으로 쓰는 방식은 콘셉트 수준을 넘어 2019년부터 양산 선례(Tesla, 정차 한정)가 있다. 따라서 기획서는 'Ford 콘셉트 방향을 구현한다'보다 'Tesla가 정차 중 게임 입력으로 양산했고 Ford가 기능 학습용 콘셉트로 시연한 방향을, 차량을 제어하지 않는 읽기 전용 신호로 개방형 앱마켓에서 구현한다'고 쓰는 편이 정확하다. Ford 튜토리얼 특허와 Audi e-tron 영상 18편은 근거에서 빼거나 '미확인'으로 표시한다.
  - 반박된 원 주장은 인용하지 않는다.
  - 근거: [B] [Ford Mustang Mach-E escape room (TopGear Philippines)](https://www.topgear.com.ph/news/technology-news/ford-mustang-mach-e-escape-room-a4354-20220128) (2022-01-28); [B] [Ford Mustang Mach-E escape room game (Electrek)](https://electrek.co/?p=223127) (2022-01-27); [B] [Tesla drivers can play 'Beach Buggy Racing 2' starting today (Engadget)](https://www.engadget.com/2019/06/18/tesla-racing-game/) (2019-06-18); [A] Pleos SDK 가용성 — `pleos-claude-kit/CLAUDE.md §2.2 (Vehicle Control set* 8개 '준비 중')` (2026-10-24)
  - 시사점: W1은 '차 조작을 게임으로 쓰는 세계 최초'라고 쓸 수 없다. 기획서는 'Ford가 2022년 콘셉트로 시연한 방향을, 양산 차량의 앱마켓에서 읽기 전용 신호로 구현한다'는 식으로 쓰는 편이 안전하다. 남는 차별점은 세 가지다. (1) 정차·충전 시간에 맞춘 판 길이, (2) 좌석 점유 기반 동승자 협동 모드, (3) 차량을 제어하지 않아 보안·안전 부담이 작다는 점이다. Ford가 '충전소·기능 학습'을 직접 겨냥했으므로 페르소나 근거로는 쓸 수 있지만, 완성차가 마음먹으면 따라올 수 있는 영역이라는 경쟁 리스크도 같이 적어야 한다.
  <details><summary>상세</summary>

  Ford 콘셉트(2022-01-27~28 보도): 게임은 Apple CarPlay에 연결한 iOS 기기나 Mach-E 화면에서 시작하고, 과제는 차량 스피커로 안내한다. 전동 시트·핸즈프리 테일게이트·에어컨·헤드라이트는 '게임이 작동시키거나 플레이어가 트리거로 쓴다'고 적혀 있다. 보도는 이를 '아직 순전히 작동하는 콘셉트(purely a working concept)'라고 하고, Electrek은 '초기 작업 중인 게임, 주로 재미용'이라고 쓴다. Ford는 이 방식을 '차 기능을 익히게 하는 수단'으로 소개했다. 충전소처럼 움직이지 않는 시간에 쓸 만하다고 보았고, 상호작용형 사용설명서나 운전 코칭으로 넓힐 수 있다고 했다. 일부 과제는 정차 중에만 열린다고 보도한 매체가 있으나 TopGear는 이를 명시하지 않아 보도마다 다르다. Tesla(2019-06-18 Engadget): Beach Buggy Racing 2는 핸들과 브레이크 페달을 입력으로 받고 스크롤 휠이 가속에 연결되며, '주차(P) 상태에서만' 작동한다. 차 기능을 게임으로 익히게 하는 온보딩이 실제로 출시된 사례는 찾지 못했다. Ford의 차량 내 튜토리얼 특허(검색 결과 요약에서만 확인)와 Audi e-tron의 영상 18편 안내 정도다. Pleos 쪽 차이: 차량 제어(set* 계열)는 콘솔에서 '준비 중'이라 앱은 사람이 조작한 결과를 읽기만 할 수 있다(키트). Ford 콘셉트는 게임이 차를 작동시키는 방향이라 W1과 다르다.

  </details>

### ○ 미검증 참고(정성 근거로만)

- **wow-benchmark-02** W1: 정차 중 인카 게임은 완성차 채택 단계에 들어섰다. BMW·MINI 50만 대 이상에 OTA로 배포됐고 폰을 컨트롤러로 쓴다. 다만 이용자 수나 이용 시간은 공개된 곳이 없다. 별도 하드웨어를 요구한 인카 VR 엔터(holoride)는 2024-05 파산을 신청했다.
  - 근거: [A] [BMW Group: In-car gaming (AirConsole, UNO Car Party!)](https://www.bmwgroup.com/en/news/general/2024/in-car-gaming.html) (2024-10-21); [B] [holoride insolvent (UploadVR)](https://uploadvr.com/holoride-insolvent) (2024-05-10); [B] [BMW partners with AirConsole (TechCrunch, 검색 결과 요약만 확인)](https://techcrunch.com/2022/10/11/bmw-partners-with-airconsole-to-bring-in-car-gaming-in-2023) (2022-10-11); [B] [Tesla under investigation for 'Passenger Play' (Engadget, 검색 결과 요약만 확인)](https://www.engadget.com/tesla-passenger-play-games-driving-investigation-nhtsa-161515954.html) (2021-12)
  - 시사점: '정차 중 게임'이라는 방향은 완성차가 이미 채택한 흐름이라 시장성의 정성 근거로 쓸 수 있다. 하지만 이용 실적 통계가 없어 W1의 리텐션(충전할 때마다 한 판)과 테마 팩 유료 모델은 가정으로 써야 한다. 기존 AirConsole 게임은 폰이 컨트롤러이고 차는 화면이다. W1은 차 자체가 컨트롤러라는 점이 구조적 차이다. holoride 사례는 인카 엔터의 유료화와 별도 하드웨어 모델이 어렵다는 선례다. 주행 중 게임 허용은 규제 리스크이므로 W1의 'P에서만 시작, 규제가 걸리면 즉시 숨김' 설계가 타당하다.

- **wow-benchmark-03** W2: NIO NOMI는 감정 엔진을 내세운 대시보드 위 캐릭터이지만, 공식 설명상 음성 비서다. 운전 행동이나 차량 상태를 캐릭터의 기분으로 반영한다는 설명은 찾지 못했다.
  - 근거: [A] [The History Behind NOMI (NIO 공식 블로그)](https://www.nio.com/blog/history-behind-nomi) (2021-11-11); [B] [Nio says users have communicated with NOMI over 200 million times (CnEVPost)](https://cnevpost.com/2021/05/17/nio-says-users-have-communicated-with-nomi-virtual-assistant-over-200-million-times) (2021-05-17); [B] [NIO launches NOMI GPT large model (Gasgoo, 검색 결과 요약만 확인)](https://autonews.gasgoo.com/china_news/70032446.html) (2024-04)
  - 시사점: '차 안에 사는 캐릭터'라는 콘셉트 자체는 NIO가 선점했다. W2를 차별화하려면 NOMI식 음성 비서 캐릭터가 아니라 '운전 습관과 차량 상태(워셔액·타이어·SOC)가 캐릭터의 기분이 되는 구조'로 정의해야 한다. 기획서에서 NOMI와 겹치지 않게 선을 그어 둔다. W2는 Gleo 음성 연동이 승인 전이라 음성 대화로 NOMI와 맞서는 구도는 성립하지 않는다.

- **wow-benchmark-05** W2: 국내에서 주행 행동 데이터(급가속·급감속·과속)는 동의 기반 UBI로 이미 상용화돼 있다. 다만 금융당국·개인정보위의 차량 주행 데이터 전용 기준 원문은 이번 조사에서 찾지 못했다. Pleos는 운전 중 행동 데이터를 마케팅에 쓰지 않도록 구분하라고 요구한다.
  - 근거: [B] [현대해상, 커넥티드카-UBI 특약 (이코노믹리뷰/ekn)](https://m.ekn.kr/view.php?key=437103) (2019-06-12); [B] [티맵모빌리티 UBI 국내 1위, 가입자 500만 육박 (파이낸셜뉴스)](https://www.fnnews.com/news/202606261004583062) (2026-06-26); [A] 보안·개인정보 설계: 운전 중 행동 데이터 마케팅 목적 사용 구분 — `pleos-claude-kit/docs/pleos-only/vehicle-app-planning-guide/security-privacy.md (22행)` (2026-10-24)
  - 시사점: W2가 보험 제휴를 수익 모델에서 뺀 것은 Pleos 규칙과 맞는다. 다만 국내 시장에서 주행 행동 데이터의 현금화 경로는 UBI가 사실상 전부이므로 W2의 수익은 꾸미기·IP에 한정된다. 기획서에는 점수 계산을 기기 안에서 하고 서버 전송을 최소화하는 설계를 명시한다. 데이터가 동의 기반으로 이미 오가고 있다는 점은 사용자 수용성의 정성 근거로만 쓴다.

- **wow-benchmark-07** W3: 와이퍼 데이터를 기상 관측에 쓰려는 공공·민간 시도는 일본(Toyota×Weathernews 2019 실증)과 한국(기상청 차량 OBD 수집 2016)에 있었다. 하지만 국내 기상청·도로공사의 상용 차량 센서 기반 노면·날씨 경고는 찾지 못했다. 국내 내비 연동 경고는 환경부 홍수 정보 기반(2024-07)이다.
  - 근거: [A] [Weathernews: 와이퍼 데이터 실증 시험 (Toyota 커넥티드카)](https://global.weathernews.com/news/12918/) (2019-11); [B] [기상청 차량 OBD 활용 기상 데이터 수집 개발 (디지털타임스)](https://www.dt.co.kr/article/11010728) (2016-04-11); [B] [현대차·기아 내비게이션 고도화로 도로침수 참사 예방, 민·관 업무협약](https://v.daum.net/v/20240710174318844) (2024-07-10); [C] [Waze 포럼: Posting weather hazards (검색 결과 요약만 확인)](https://www.waze.com/discuss/t/posting-weather-hazards/184530) (2026-10-24)
  - 시사점: 국내에서 차량 센서 기반 도로 날씨는 공공 상용 서비스 공백일 가능성이 있다. 다만 '찾지 못했다'는 단계이고, 기상청이 2016년에 같은 방향을 시도했다가 상용화 장애를 스스로 짚었다는 점이 기획서의 리스크 서술 근거가 된다. 협력 상대(기상청·도로공사)와 B2B 데이터 판매 모델은 가능성이지 검증된 시장이 아니다. 위치 크라우드소싱의 국내 법적 요건(위치정보법 동의·사업 신고, 개인정보보호법)은 기존 리서치 regulation-safety-05·06을 재사용한다.

- **wow-benchmark-08** W3 근거 통계: 우천 시 교통사고 치사율은 맑은 날보다 높다. 2023~2025년 전국 100건당 1.58명 대 1.22명(약 1.3배), 2021~2025년 고속도로 4.7명 대 3.4명(약 1.4배)이다. 안개 구간 통계는 찾지 못했다.
  - 근거: [B] [우천 시 교통사고 치사율 (세계일보, TS TAAS 인용)](https://www.segye.com/newsView/20260707519306) (2026-07-07); [B] [고속도로 비 오는 날 치사율 (경향신문, 한국도로공사 분석 인용)](https://www.khan.co.kr/article/202607091638021) (2026-07-09)
  - 시사점: W3의 페인포인트 서술에 '비 오는 날 치사율이 맑은 날의 약 1.3배(전국), 고속도로 약 1.4배'를 출처와 기간을 붙여 인용할 수 있다. 사고 감소 효과는 주장하지 말고 '경고 필요성'의 근거로만 쓴다. 안개와 갑작스러운 강우 진입 구간의 통계는 별도로 찾아야 한다.

- **wow-benchmark-10** 근거 통계: 주행 중 오디오 이용은 미국 Edison(2025)과 5개국 Futuresource(2026-02) 수치를 인용할 수 있다. 반면 충전 대기 시간, 충전 중 차 안 체류 비율, 인카 게임 이용 실적은 신뢰할 공개 수치를 찾지 못했다.
  - 근거: [B] [Radio's Reach Remains Strong as Digital Audio & Podcasts Grow (Edison Infinite Dial 2025 보도)](https://radioink.com/2025/03/21/radios-reach-remains-strong-as-digital-audio-podcasts-grow/) (2025-03-21); [B] [How smartphones are steering the future of in-car audio (Futuresource)](https://www.futuresource-consulting.com/the-source/industry-pulse/how-smartphones-are-steering-the-future-of-in-car-audio/) (2026-02-17); [B] [CARIAD in-car gaming study (Newzoo 공동 조사)](https://cariad.technology/de/en/news/stories/in-car-gaming-study.html) (2023-08-31); [C] [EV Powered: drivers wait 30 minutes to charge (출처 불명 45% 수치, 정성 참고만)](https://evpowered.co.uk/feature/drivers-wait-30-minutes-to-charge-then-spend-it-on-quick-games/) (2025)
  - 시사점: W4의 '주행 중 오디오가 허용되는 몇 안 되는 콘텐츠'는 규제 근거(regulation-safety-08)이고, 수요 근거로는 미국·5개국의 차 안 오디오 이용 수치만 쓸 수 있다. 한국 수치가 아니므로 '해외 조사'라고 밝혀 쓴다. W1의 충전 대기 시간과 인카 게임 이용 실적은 수치 근거 없이 정성(완성차 채택, 충전 중 활용 여지)으로만 쓰고, 기획서에 임의 수치를 넣지 않는다.

### 조사 공백
- 충전 대기 시간과 충전 중 차 안 체류 비율에 대한 국내 공식 통계를 찾지 못함. 급속충전 30~40분은 계산값 성격이고 실측 평균이 아님. 'EV 운전자 45% 차 안 체류'는 출처 불명(C)이라 수치로 쓰지 않음.
- 인카 게임의 실제 이용 실적(BMW·VW AirConsole, Tesla Arcade 이용자 수·이용 시간)은 공개된 값을 찾지 못함. DTS/Xperi 미국 설문의 46%(밀레니얼)·38%(Gen Z) 수치는 원문(403)을 열지 못해 검증하지 못함.
- 현대·기아·제네시스 국내 차량에 유사 기능(센서 기반 도로 위험·날씨 경고, 주행 연동 오디오 투어, 운전 반영 캐릭터)이 있는지 한국어 검색에서 확인하지 못함. 없다는 뜻이 아니라 확인하지 못했다는 뜻이다.
- Mercedes Car-to-X의 와이퍼 트리거는 공식 페이지(connectivity.mercedes-benz.com)가 ABS·ESP·센서만 적고 와이퍼는 적지 않는데, 업계 매체는 와이퍼 최고 속도가 폭우 경고를 일으킨다고 쓴다. 오너 매뉴얼 페이지는 접속 실패로 열지 못했다. Audi 공식 보도자료와 Automotive World 원문도 직접 열지 못하고 검색 요약으로만 확인함.
- '내비 경로·남은 시간에 맞춰 이야기가 지점 도착에 끝나도록 재생 시점을 맞추는 서비스'는 검색 두 번에서 찾지 못했다. 이는 부재의 증거가 아니라 검색 한계이다. VoiceMap·Autio·Shaka의 내비 연동 여부는 자료에 명시가 없다.
- Autio·Shaka Guide 설치 수와 콘텐츠 수는 Autio 자사 비교 블로그의 주장이고, 오디(Odii) 콘텐츠 규모(관광지·오디오 수)는 공식 수치를 찾지 못함. VoiceMap 투어 수(약 2,000개)는 앱스토어 소개 검색 요약만 확인함.
- 기상청 차량 OBD 기상 수집(2016)의 후속 결과와 한국도로공사 C-ITS 안개·노면 정보 서비스의 상세는 찾지 못함. 강원대 레인센서·부산 버스 시범은 검색 요약만 확인함.
- 금융감독원·금융위원회 UBI 가이드라인과 개인정보보호위원회의 차량 주행 데이터 전용 기준 원문은 검색에 나오지 않음. 위치정보법·개인정보보호법의 일반 요건은 기존 리서치(research-digest regulation-safety-05·06)와 겹쳐 다시 조사하지 않음.
- holoride의 2024-06 이후 상태(청산·인수 여부)를 확인하지 못함. NOMI 이용 소유자 비율과 2022년 이후 NOMI 이용 통계도 NIO 공개 값을 찾지 못함.
- 질문 해석: 'AirConsole car'는 BMW·MINI 중심으로 좁혔고 Mercedes와 AirConsole 제휴는 확인하지 못함(Mercedes는 Boosteroid 클라우드 게임 보도만 확인). 'Tesla Arcade 핸들·페달'은 Beach Buggy Racing 2로 좁혔고 Cybertruck 지원은 확인하지 못함. Pleos 신호(앰비언트 색·창문 area·좌석 점유 등)의 차종별 지원 여부는 platform 축 소관이라 이번 축에서 조사하지 않음.

## 누락 검토가 찾은 항목

- **Pleos 탑재 설치기반(MAU 퍼널의 분모)과 향후 12개월 확대 로드맵** (market) — 기획서 6번(타깃 규모·예상 MAU)의 분모가 비어 있다. 지금 있는 것은 탑재 3개 차종 이름과 2030년 2천만 대 목표뿐이고, 신형(Pleos 탑재) 트림만의 월별 출고 대수는 없다. 구형 재고 구분, 투싼 출고 시점, 기아·제네시스 적용 시기, 기존 차량 OTA 적용 여부도 미확인이다. 공식 수치가 없으면 MAU는 보수·기준·낙관 3개 범위의 명시적 가정으로 쓰고 각 단계에 가정 표시를 붙여야 한다.
- **탑재 3개 차종의 파워트레인 구성과 EV 신호 대상 규모** (platform + market) — user-needs와 tech-trends는 국내 전기차 109만 대와 충전 불편을 근거로 쓴다. 그런데 Pleos 탑재 차종(그랜저·아반떼·투싼)에 EV/PHEV 트림이 있는지, 비중이 얼마인지는 어느 축에서도 확인하지 않았다. 내가 아는 한 세 차종은 내연·하이브리드 중심이라, 충전·SOC 기반 아이디어의 실제 대상이 전체 EV 시장보다 훨씬 작을 수 있다. 연료·주행거리·EV 배터리 신호가 차종별로 지원되는지(capability)도 함께 정리해야 한다.
- **MAU 전환 퍼널 벤치마크(탑재 차량에서 월 1회 이상 실행까지)** (market) — 탑재 차량에서 Pleos 계정 연동, 앱마켓 이용, 서드파티 앱 설치, 월 1회 이상 실행으로 이어지는 단계별 전환율의 앵커가 하나도 없다. Pleos App Market MAU는 비공개다. 대용 지표로 Bluelink 같은 현대차 커넥티드 서비스의 가입·활성률, 국내 Android Auto/CarPlay 사용률, 내비·음악 앱 MAU(민간 집계)가 있는지 찾아 상한선으로 써야 한다. 없으면 없다고 기록하고 가정으로 표시한다.
- **Retention 근거(인카 앱 재방문 빈도와 습관 트리거)** (market + user-needs) — 기획서 6번과 사업성 심사 항목이 Retention을 요구하는데 조사 전체에 인카 앱 D30·월간 재방문 수치나 유사 사례가 없다. user-needs-04의 이용시간 자료(쏘카 설문)는 공유차량 회사 이용자 중심의 표본이라 대표성이 낮고 2023년 자료다. Retention은 벤치마크 대신 시동·충전·정차 같은 반복 이벤트에 연결된 트리거 설계로 논증하고 가설과 검증 계획으로 표시해야 한다. 그러려면 인카 앱 이용 빈도의 공개 자료를 최소한 탐색해야 한다.
- **대체재와 순정 기능 중복도(Phone Projection, 내장 Navigation, Bluelink, Car Pay, Gleo 기본 기능)** (market) — 경쟁 조사는 Pleos App Market 18종과 해외 앱스토어만 다루고, 사용자가 이미 쓰는 대체재는 비교하지 않았다. 키트 문서(glossary, navigation guide)에 Phone Projection(Android Auto/CarPlay 레퍼런스) 언급이 있어 폰 앱이 경쟁 대체재가 될 수 있다. 탑재 3개 차종에서 이것이 실제로 켜져 있는지는 미확인이다. 순정 내비의 충전·주차 안내와 Car Pay가 이미 겹치는 영역도 목록화해야 차별성과 사업성 논리가 선다.
- **속도 신호 접근 가능성(PERF_VEHICLE_SPEED)과 공모전 사이트 문구 불일치** (platform) — platform 축이 스스로 이 경로가 막히면 속도 기반 기능이 성립하지 않는다고 적었다. 공식 예시 시나리오 '안전운전 점수'가 바로 이 영역이라 아이디어 풀 전체에 영향이 크다. 공모전 사이트는 차량 속도 변경 가능이라 하고 SDK 문서는 속도 카드가 없다고 해 서로 어긋난다. 읽기 전용 점검(car_service 속성 목록 확인)과 10/28 설명회 질의로 아이디어 순위 확정 전에 결론을 내야 한다.
- **10/28 온라인 설명회 질의 목록과 아이디어 확정 시점의 의존성** (platform) — 준비 중 SDK(FusedLocation·TTS/STT·Vehicle Control)의 공모전 사용 가능 여부, Gleo schema 승인 절차와 소요 기간, 심사 환경(에뮬레이터 기준인지 실차 기준인지), 개발자 수익배분은 공식 답변 전에는 풀 수 없다. 기준일이 설명회보다 4일 앞이고 접수 마감은 11/15다. 리서치가 우선순위 질문 5~6개를 미리 정리하고, 아이디어는 준비 중 SDK에 의존하지 않는 안을 기본으로 삼아야 한다.
- **에뮬레이터 실측이 필요한 핵심 가정(NaviHelper 이벤트, Gleo schema 호출, 좌석 점유·EV 신호 값)** (platform) — platform-05와 platform-06은 [unverified]이고 gaps에서도 NaviHelper 위치·TBT·도착 이벤트 수신과 Gleo의 제3자 앱 호출이 에뮬레이터에서 나오는지 확인하지 못했다고 한다. 2차 PT는 실제 동작 시연이 필수다. 시연 불가능한 핵심 흐름을 가진 아이디어는 후반에 탈락하므로, 아이디어 심사 전에 1일 이내 스파이크로 우선 검증해야 한다.
- **기획 배경 인용 수치의 판정 충돌과 출처 품질** (fact-check (user-needs + tech-trends)) — tech-trends-06은 [refuted]인데 같은 109.5만 대 수치가 user-needs-01에서는 [confirmed]다. 어느 하위 주장(충전기 51만 기, 2030년 123만 기, 최초 100만 돌파 표현 등)이 반박됐는지 구분이 없어 기획서에 쓰면 오류가 될 수 있다. user-needs-05는 GS차지비(충전사업자) 설문과 환경공단 설문이 상위 불편을 다르게 꼽는데 표본·방법 비교가 없고, GS차지비 수치는 뉴스1 본문에 없다는 gaps 기록과 충돌한다. 통계청 통근 시간(2024년 12월 발표)은 시의성이 낮다. 인용 전에 국토부·환경공단 원문과 판정 정정이 필요하다.
- **2천만 대 목표의 정의와 축 간 판정 불일치** (market (fact-check)) — market-01은 [confirmed], tech-trends-01은 같은 내용을 [unverified]로 두었고, tech-trends 쪽 gaps는 기획서 6번에 그대로 인용될 수치라 핵심 검증이 필요하다고 인정했다. 누적인지 연간인지, 글로벌인지 국내인지, 현대·기아·제네시스 합산인지, 신차 판매만인지 기존 차량 OTA 포함인지가 정의되지 않았다. 국내 비중을 모르면 타깃 규모 문장에 쓸 수 없으므로 현대차그룹 보도자료 원문으로 정의를 확정해야 한다.
- **Pleos 기준 '사용 가능한 정차 시간'(P단 체류) 정량 근거** (user-needs + regulation-safety) — regulation-safety-02와 platform-07에 따르면 Pleos는 P단만 제한 없는 상태로 보고 신호대기·공회전(D)은 주행과 같이 제한한다. 그래서 영상·대용량 UI·설정을 쓸 수 있는 실제 창은 법적 '정지' 시간이 아니라 P단 체류 시간(충전 중, 주차 직후, 출발 전, 픽업 대기)이다. user-needs의 정차 관련 gaps는 일반 정차·충전 대기 통계 부재만 다루고 이 구분과 연결하지 않았다. 이 시간대 규모 추정, 또는 아이디어를 주행 중 허용 카테고리(음성·내비·모빌리티)에 맞추는 대안 근거가 필요하다.
- **탑재 차종 이용자 프로필(개인·법인·렌터카·택시 비중, 연령대)** (market + user-needs) — 타깃 규모와 페르소나(고령, 업무 운전, 가족 동승)를 탑재 차종의 실제 구매·이용층과 연결한 자료가 없다. 사업용 주행거리가 비사업용의 3.1배라는 통계와 배달·대리·영업 운전 불편은 Pleos 탑재 차량이 해당 층에 팔리는지 확인되기 전에는 타깃 근거가 될 수 없다. 렌터카·법인 차량은 Pleos 계정 로그인과 개인정보 동의 흐름도 달라진다.
- **Pleos 오너의 1차 정성 근거(오너 커뮤니티, App Market 리뷰, Gleo 사용 후기)** (user-needs) — Pain point 명확성이 창의성 심사 항목인데 현재 근거는 거시 통계 중심이고 정성 근거는 클리앙 게시글 2건뿐이다. 보배드림과 앱 스토어 리뷰는 열람하지 못했다. 신형 그랜저·아반떼·투싼 오너가 Pleos Connect와 앱마켓·Gleo에서 겪는 불만과 빠진 기능이 가장 직접적인 아이디어 소스다. 마감까지 시간이 있으므로 소규모 설문·인터뷰(N=30~50) 계획도 검토할 수 있다.
- **외부 네트워크·통신 요금·계정·백그라운드 정책(재방문 장치의 기술적 전제)** (platform) — 외부 API(충전·주차 공공데이터, 날씨 등) 호출 가능 여부, 차량 통신 무료 제공 기간과 이후 요금, Pleos Account 프로필 동기화, 스마트폰 연동, 알림·푸시, 백그라운드 실행 한계가 리서치 요약에 없다. 키트 문서(platform-api-architecture, app-security-guide, connect.md)에 일부 내용이 있어 추출하면 된다. Retention 설계와 서버·제휴 의존 아이디어의 실현성이 이 정보에 달려 있다.
- **결제·콘텐츠 저작권·공공데이터 이용허락 규제(아이디어 유형별)** (regulation-safety) — regulation-safety 축은 도로교통법·위치정보법·개인정보보호법만 다뤘다. 충전·주차 공공데이터의 상업적 이용 조건, 음원·영상 등 미디어 콘텐츠 라이선스, 인카 결제·커머스(전자금융 관련), 만 14세 미만 동승자 대상 서비스의 추가 제약은 조사하지 않았다. 아이디어가 이 유형으로 가면 심사의 안전·정책 렌즈에서 감점될 수 있다.
- **사업성(수익화·제휴) 근거와 공모전 내 경쟁 포화도** (market) — 개발자 수익배분과 Pleos Payment은 미공개이고 인카 구독·커머스 성과 수치도 없어 매출 추정은 근거가 없다. 사업성은 매출 숫자 대신 B2B 제휴 경로(보험 UBI, 충전·주차 사업자, 정비 등)와 확대 계획으로 쓰되 그 경로의 실재성을 확인해야 한다. 또한 공식 예시 3종(음성 메모, 안전운전 점수, 배달기사 동선)과 니즈 예시 4종에 응모작이 몰릴 가능성, 유사 공모전 수상작 패턴은 조사하지 않았다.
- **동승자·뒷좌석 타깃의 실제 표시·입력 경로** (platform + regulation-safety) — 공식 니즈 예시에 '패밀리&동승자 케어'가 있으나 user-needs-08은 동승 불편 측정 자료가 없다고 하고, regulation-safety gaps는 Pleos 뒷좌석 디스플레이 지원 여부를 확인하지 못했다. 동승자가 앞좌석 중앙 화면만 쓴다면 주행 중에는 운전자에게 보이는 위치라 규제가 그대로 적용되고, 좌석 점유 신호만 맥락 입력이 된다. 이 경로가 확정되지 않으면 동승자 아이디어는 정차 전용으로 한정해야 한다.
- **[보강] 상위 5개 아이디어 핵심 신호의 탑재 3차종·트림별 실제 지원 범위 (뒷줄 SeatOccupancy, 뒷창문·창문 잠금, Safety 경고 4종, Wiper 상태)** (platform) — 기획서 6번 타깃 규모의 분모는 '아이디어 핵심 신호를 실제로 지원하는 트림 수'인데 아직 없다. 경쟁 조사에서 순정 후석 알림이 하차 시점 로직으로 동작한다는 점은 확인됐다. 하지만 그 알림이 어떤 센서(좌석 점유, 도어 열림 로직, 초음파 등)에 의존하는지, 앱이 읽는 SDK 점유 신호와 같은 것인지는 모른다. 2위의 '재출발 시점' 차별점은 뒷좌석 점유를 읽을 수 있어야 성립한다. 에뮬레이터의 capability 응답은 양산 차 지원을 보장하지 않는다. 순위를 바꿀 수 있는 항목이라 기획서 문장용 통계보다 먼저 확인해야 한다. 차종·트림별 capability 문서 또는 10/28 설명회 질의로 확인한다.
- **[보강] 탑재 3차종(그랜저·아반떼·투싼) 단위의 순정 기능 대조 (오너 매뉴얼 기준: 후석 승객 알림, Pet Mode, 리모트 케어·선제케어, 클러스터 정비·소모품 알림, 폰 프로젝션 활성 여부)** (competitor-benchmark) — 경쟁 조사의 차별점 판단은 브랜드 단위 웹페이지와 '찾지 못함'이라는 부정 증거에 기대고 있다. 현대·기아 Pet Mode의 국내 판매 차종 적용 여부, 리모트 케어 페이지 간 대상 차종 목록 충돌, 기아 대응 서비스 출시 여부가 해소되지 않았다. 못 찾은 것이 없다는 뜻은 아니므로, 기획서 3~4번(차별성)에 '순정에 없다'고 쓰려면 3개 차종의 오너 매뉴얼과 앱 기능을 직접 확인해야 한다. 이 확인은 1위의 '업계 최초 불가' 정정과도 이어지고, 4위가 순정과 가장 많이 겹친다는 판단을 차종 단위로 확정하는 데도 필요하다.
- **[보강] 아이디어별 이용 트리거 빈도(일·계절·연 단위)와 Retention 논거** (user-needs) — 사업성 심사가 Retention Rate를 요구하는데 근거가 없다. 인카 앱 재방문 수치가 공개되지 않는다는 점은 확인됐으므로, 대신 아이디어마다 사용 빈도 구조를 정량화해야 한다. 2위는 2시간 넘는 가족 장거리 이동(명절·휴가)에서만 켜지고, 1위는 계절 단위 교체 카드, 5위는 출퇴근마다 쓰는 일 단위 구조다. 월 1회 이상 실행이라는 MAU 정의와 Retention 서사가 아이디어마다 달라지므로 순위 변수가 될 수 있다. 국민여행조사, 도로공사 휴게소 이용·명절 통행량, 반려견 동반 주행 빈도 같은 빈도 자료가 필요하다. 자료가 없으면 가설로 표시한다.
- **[보강] Pleos 탑재 설치기반(타깃 규모·MAU 퍼널의 분모)의 A등급 재확인: 신형 트림 월별 출고, 구형 재고 분리, 기아·제네시스 적용 시기** (market) — 앞선 누락 검토에서 지적됐고 이번 두 축으로도 채워지지 않았다. 현재 분모는 9월 언론 보도(B등급)의 그랜저·아반떼 판매 대수뿐이고, 그 판매량이 Pleos 탑재분인지 구분되지 않는다. 현대차 월간 판매실적 공식 자료와 신형 출시 이후 판매량으로 교체하고, 구형 재고와 투싼 출고 시점을 분리해야 한다. 공식 수치가 없는 항목은 보수·기준·낙관 3개 범위의 가정으로 표시한다. 2천만 대 목표는 정의(누적/연간, 글로벌/국내)가 확정되기 전에는 6번에 인용하지 않는다.
- **[보강] MAU·Retention의 대용 지표 세트와 상·하한 계산 규칙 (Bluelink 가입·유료 전환, 국내 차량 관련 앱 MAU, Tessie 이용자÷Tesla 보유 대수, J.D. Power TXI 이용률)** (market) — global-oem-apps 축은 OEM 인카 앱스토어 서드파티 앱의 이용자·매출이 공개된 곳이 없다고 확인했다. 직접 앵커가 없으므로 대용 지표로 전환 퍼널의 상한선을 잡아야 한다. 후보는 현대 커넥티드 서비스(Bluelink·마이현대)의 가입자 수와 활성률, 모바일인덱스·와이즈앱의 차량 관리·내비 앱 MAU, 차량 특화 서드파티 앱(Tessie, TezLab)의 이용자 수를 해당 차량 보유 대수로 나눈 침투율이다. Tessie 약 40만 명은 개발자 주장이고 인카 앱이 아니라 폰 앱이라는 한계가 있어 상한 참고로만 쓸 수 있다. J.D. Power 2025 TXI는 403이라 원문을 못 열었다. 기획서 6번에서 가정과 근거를 분리해 쓰려면 이 세트가 필요하다.
- **[보강] Pleos·42dot 앱마켓 운영 방침과 개발자 지표 정책 (수수료·정산, 서드파티 입점 큐레이션 기준, 자사·OEM 기능과 중복되는 앱 허용 여부, 개발자 이용 통계 제공)** (platform) — 해외에서는 BMW·NIO가 큐레이션형, Google이 개방형이고 서드파티 수익 배분 조건이 공개된 곳은 Google Play뿐이다. Pleos의 방식은 알 수 없다. 42dot이 차량 관리 카테고리 추가를 예고했다는 점은 기회이자, 같은 회사가 순정 기능으로 편입해 버릴 수 있다는 위험이다. 해외에는 Tesla Dog Mode의 순정 편입, BMW 구독 철회, GM AppShop 무기한 연기, Tesla API 유료화 같은 선례가 있다. 또 앱 마켓이 개발자에게 MAU·재방문 통계를 주는지가 정해져야 기획서의 Retention 측정 계획을 쓸 수 있다. 10/28 설명회 질문 목록에 추가해야 하고, 사업성의 확대 계획에도 필요하다.
- **[보강] 업무용승용차 운행기록부(5위)의 대상 규모, 미작성 불이익 금액, 차량 자동 기록의 증빙 인정 여부** (regulation-safety) — 법인세법 시행령 제50조의2와 국세청고시, 서식 구조는 확인됐지만 사업성에 필요한 세 가지가 비어 있다. 첫째는 의무 대상이 법인·복식부기 개인사업자인지, 영업직 근로소득자가 본인 차를 쓸 때도 해당하는지다. 이에 따라 타깃 정의가 달라진다. 둘째는 미작성 시 비용 인정 한도(연 1,500만 원으로 알려진 값)를 원문으로 확인해 지불 의사의 크기를 제시하는 것이다. 셋째는 위치 없이 계기판 거리와 출퇴근 구분만 남긴 차량 신호 기반 기록이 국세청이 인정하는 증빙 방식인지다. 국세청 업무용승용차 비용 명세서 제출 법인·개인 수 같은 모수 통계도 찾아야 한다. 기획서 6번 타깃 규모에 들어갈 수 있는 근거다.
- **[보강] 상위 5개 사업모델의 가격·제휴 단가 근거 (운행일지 서비스 요금, 정비 예약·소모품 쿠폰 수수료, 휴게소·키즈 제휴, 펫 보험·동물병원 제휴)** (market) — 5개 아이디어의 사업 모델이 모두 제휴나 구독 가정이고 수치 근거가 없다. 인카 서드파티 수익 배분 선례도 공개된 것이 없어 매출은 국내 유사 서비스 가격으로 대체해야 한다. 경쟁 조사가 찾은 LG U+ 단말 서비스, GPS 운행일지 앱, 국세청 엑셀 프로그램의 요금과 구독 구조가 5위 '월 30건 무료, 개인사업자 구독' 가격 가정의 앵커가 된다. 정비 예약 플랫폼 수수료, 휴게소 사업자(도로공사) 제휴 구조, 펫 보험 연계 모델은 제휴처가 실제로 있는지부터 확인해야 한다. 근거가 없으면 매출 추정 없이 확대 경로로만 적는다.
- **[보강] 국내 후석 방치·차량 내 갇힘·반려동물 차량 내 방치·주행 중 창문 이탈 사고의 대체 출처와 사례 집계 (2·3위 기획 배경)** (user-needs) — 경쟁 조사는 국내 승용차 후석 방치 공식 통계를 찾지 못했고, 인용 가능한 것은 미국 NoHeatStroke 수치뿐이라 국내 기획 배경으로 쓸 수 없다. 심사위원도 1~3위의 페인포인트 근거 부족을 약점으로 지적했다. 소방청 구조활동 통계에 차량 내 갇힘이나 문 개방 출동 항목이 있는지, 한국소비자원 위해정보, 뉴스 집계(빅카인즈 등)로 사례 건수를 세는 방법을 시도할 수 있다. 그래도 없으면 '국내 공식 통계 없음'을 명시하고 정성 사례만 쓴다. 휴게소에 아이를 두고 출발한 국내 사례는 별도로 확인하지 못했다.
- **[보강] 소모품 '사용량 기반 마모'의 공학적 근거와 국내 정비 지연 지표 (1위 핵심 전제)** (user-needs) — 1위의 핵심 전제는 와이퍼·풍량·제동 사용량이 주행거리·달력보다 교체 시점을 더 잘 설명한다는 것인데 검증된 근거가 없다. 사용 조건을 반영하는 순정 선례는 BMW CBS가 2차 자료 한 건(발행일 미표기)으로만 확인됐고, 와이퍼 사용량 기반 계산은 찾지 못했다. 탑재 차종 오너 매뉴얼에 가혹 조건 교체 주기 단축 기준(에어컨 필터·브레이크 패드·와이퍼)이 이미 있는지가 차별점을 강화하거나 약화한다. 부품사의 와이퍼·필터 수명 기준과 한국교통안전공단 정기검사의 제동·와이퍼 관련 부적합 비율 같은 간접 지표도 필요하다. 이것이 없으면 기획서에서 '점검 권장' 수준 이상의 주장을 정당화할 수 없다.
- **[보강] 안전 경고형 앱(2·3·4위)의 책임·오탐·미탐 설계와 심사 정책 근거 (후석 감지 원문, Pleos 심사 가이드의 안전 기능 대체 조항)** (regulation-safety) — 순정 후석 승객 알림이 이미 있다는 점이 확인됐으므로, 앱의 경고는 순정을 보조하는 기능이라는 포지션과 면책 설계가 필요하다. 앱이 포그라운드가 아닐 때 경고가 불가능한 AAOS 백그라운드 제약도 미확인이다. Euro NCAP 어린이 존재 감지와 NHTSA 후석 안전벨트 경고 최종 규칙은 원문을 못 열어 인용에서 제외된 상태다. 오탐·미탐 시 책임과 순정 경고와의 중복·충돌은 안전·정책 렌즈의 감점 요인이 될 수 있다. Pleos 심사 가이드에서 안전 관련 알림을 다루는 조항을 직접 찾아 기획서 리스크 항목에 반영해야 한다.
- **[보강] 폰 프로젝션 대체 위험의 현재성: 국내 2024~2026 이용 자료와 신형 무선 연결 탑재차의 이용 행태** (global-oem-apps) — 폰 미러링은 내장 앱의 가장 강한 대체재인데 국내 직접 수치는 컨슈머인사이트 2022년 7월 조사뿐이다. 이 조사는 2019~2022년 구입 차량 기준이고 스마트폰 내비에 미러링과 연동이 분리되지 않아, 2026년 신형 Pleos 탑재차 이용자에게 그대로 옮기기 어렵다. 현대차 공식 Android Auto·CarPlay 지원 차종 발표, 컨슈머인사이트 후속 조사, 오너 커뮤니티의 신형 3차종 사용 후기로 보강해야 한다. 3차종에서 폰 프로젝션이 켜져 있는지가 확인되지 않으면 사업성 논리에서 대체재 대응 문단을 쓸 수 없다.
