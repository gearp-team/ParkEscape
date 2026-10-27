# 차 방탈출 기획 문서 (v4)

> 기준일: 2026-10-27 · 선택안: W1 "차 방탈출: 차 전체가 게임 패드"(수정 방향 `_workspace/02_ideation/02_selected.json` 반영) · 카테고리: Entertainment
> v4: v3(`03_planner_plan_v3.md`)에 구현 1~2주차(F1·F2-ui)에서 결정·실측된 사항을 반영한 판이다. 근거는 `_workspace/05_build/05_qa_F1.md`, `05_qa_F2-ui.md`, `F2ui_rehearsal.md`, 프로젝트 `CLAUDE.md` "실제로 확인한 함정" 절이다. 고친 곳은 문서 끝 "변경 기록"의 v4 항목에 있다.
> 앱 이름은 "차 방탈출"(영문 식별자 후보 `CarEscape`)로 쓴다. 패키지·팀명 등 임시 식별자는 Pleos Playground 등록 전에 프로젝트 `CLAUDE.md` 규칙대로 바꾼다.
> 근거 표기: 리서치 요약(`docs/research-digest.md`)의 ✅ 항목만 사실로 인용한다. ○ 미검증은 정성 근거로만, ✏ 반박 항목은 고친 내용만 쓴다. 웹에서 보충한 것은 출처 URL을 단다. [가정]은 근거가 없는 설계 판단이다. 용어: 차량 상태의 "D단 정지"(기어 D, 0km/h, 신호 대기)와 조례의 "엔진 공회전"(원동기를 켠 채 주·정차)을 구분해 쓴다.

## 0. 핵심 결정 요약
- 타깃: 국내 Pleos Connect 탑재 3차종(더 뉴 그랜저·디 올 뉴 아반떼·디 올 뉴 투싼, 내연·하이브리드) 오너가 **기어 P로 서서 기다리는 5~10분**(학원·학교 픽업 대기, 휴게소에서 동승자 기다리기, 약속 장소 조기 도착). 2차 타깃은 유럽 아이오닉 3와 향후 EV의 충전 대기.
- 해결하는 불편: 차 안에서 기다리는 시간에 할 일이 휴대폰뿐이고, 수천만 원짜리 차는 그 시간에 아무 역할도 하지 않는다.
- 차량 특화 핵심: 공조 설정(목표 온도·풍량·내외기)·실내 조명(앰비언트 색·밝기, 독서등)·핸들 각도·브레이크 페달·좌석 점유를 **읽기만 해서** 사람이 한 조작을 퍼즐 정답으로 판정한다. CarUxRestrictions가 `0x0`(정차)일 때만 시작·이어하기가 열리고, 앱은 `distractionOptimized`를 선언하지 않아 주행 전환 시 시스템이 "주행 중에는 사용할 수 없습니다." 차단 화면으로 앱을 덮는다(에뮬레이터 실측). 차단 화면의 흐린 배경은 전환 순간 화면을 판독할 수 없게 흐린 시스템 정지 이미지라 앱이 바꿀 수 없다. 이때 액티비티는 그대로 살아 있고 `onPause`/`onStop`이 오지 않으므로, 앱은 `CarUxRestrictionsManager`를 직접 구독해 `requiresDO=true`가 되는 순간(홈 차단과 같은 순간, 실측 −2~+21ms) 일시정지 화면으로 바꾸고 진행을 저장한다(구현 실측: 콜백 → 리스너 해제 1ms → 저장 8~11ms → S-P 26~42ms). 정차하면 차단 화면만 걷히고, P 확인을 거쳐 이어한다.
- MVP: F1 정차 전용 세션 게이트(구현·QA 통과), F2 생성형 퍼즐 판(지시형·암호형·연쇄형 단서, 신호 판정, 원상 복구 체크리스트, 판 길이별 개인 최고 기록. 데이터·엔진·화면 구현·QA 통과), F3 좌석도 참가자 선택과 자리 단서(좌석 점유 신호는 초기 선택값. 마무리 중).
- 가장 큰 리스크: 퍼즐 신호 8종과 좌석 점유는 에뮬레이터에서 모두 수신을 확인했으나(`spike_w1.md`, F2-data QA), Vehicle 2.3.2 SDK 결함 2건(독서등 해제 API가 재등록을 하고, 앰비언트 색 리스너가 밝기 구독을 막음) 때문에 조명 3종은 앱 수명 동안 공유 등록으로 우회한다. 좌석 점유 값의 극성·핸들 각도의 단위와 부호·앰비언트 프리셋 팔레트는 실차에서 다를 수 있다. 실차 동작과 "Vehicle State" 승인 범위는 10/28 설명회 확인 항목이다.

## 1. 기획서 초안 (접수양식)

### 1-1. 한 줄 소개
P단으로 서 있는 5~10분 동안 공조 다이얼·실내 조명·핸들·브레이크가 게임 패드가 되는 정차 전용 방탈출 게임. 앱은 차를 제어하지 않고, 사람이 직접 한 조작을 차량 신호로 읽어 퍼즐을 판정한다.

### 1-2. 기획 배경

**불편.** 차 안에서 기다리는 시간은 자주 생기지만(픽업 대기, 휴게소, 조기 도착, 충전) 그 시간을 채우는 것은 휴대폰뿐이다. 차에는 대형 화면과 수십 개의 조작부가 있는데 정차 중에는 아무 역할이 없다. 국내 차 안 대기 시간과 충전 중 체류 비율은 신뢰할 공개 통계가 없어(wow-benchmark-10 ○) 이 문서는 수치를 지어내지 않고, 아래의 확인된 사실과 정성 흐름을 나눠 근거로 삼는다.

**확인된 근거.**
1. 도로교통법 제49조제1항제11호·제11호의2는 운전 중 영상 표시·조작을 금지하고 정지 중에는 허용한다(regulation-safety-01 ✅). Pleos 심사 가이드는 게임을 포함한 미디어 앱을 "정차 중에만 사용 가능, 운전 중 실행·사용 불가"로 정하고(`distribute/app-review-guide/review-driver-distraction-elements.md`), 이 에뮬레이터는 정차(P)에서 `0x0`, D단 정지와 주행에서 `0x50`을 준다(platform-07 ✅). 즉 "정차 전용 게임"은 법과 Pleos 정책상 게임이 쓸 수 있는 형태다.
2. Pleos App Market은 2026-09 기준 국내 18종으로 내비·음악·영상 대형 서비스 중심이고, 확인된 앱 가운데 게임은 보이지 않는다(전체 목록은 미확인). 같은 발표에서 42dot은 게임·차량 관리·라이프스타일 앱을 순차 추가하겠다고 밝혔다(market-03 ✅). 플랫폼이 게임을 다음 영역으로 지목했다는 기회이자, 다른 게임사가 곧 들어온다는 경쟁 신호다(7절).
3. 정차 중 인카 게임은 이미 완성차 채택 단계다. Tesla는 2019-06 실제 핸들·브레이크를 입력으로 쓰는 P단 전용 레이싱 게임(Beach Buggy Racing 2)을 OTA로 고객 차량에 배포했고, Ford는 2022-01 Mustang Mach-E 방탈출을 작동하는 콘셉트로 시연했다(wow-benchmark-01 ✏ 정정 내용).

**정성 근거(미검증, 흐름으로만 인용).** 정차 중 엔터테인먼트는 Google과 주요 완성차가 "정차 때만 영상·게임, 주행 전환 시 축소"로 수렴하고 있다(tech-trends-08 ○). BMW·MINI는 2024-08 AirConsole 정차 게임을 배포했고 BMW 발표 기준 50만 대 이상이라고 하나 이용 실적은 공개되지 않았다(wow-benchmark-02 ○).

**기존 해결책의 한계.**
- 휴대폰: 차와 무관하다. 차의 화면·조작부·동승자 좌석을 쓰지 않는다.
- Tesla(2019): 입력이 핸들·브레이크(와 스크롤 휠) 위주이고, 제조사가 고른 게임만 들어가는 폐쇄 플랫폼이다.
- Ford(2022): 게임이 시트·테일게이트·공조·헤드라이트를 작동시키기도 하는 콘셉트라 차량 제어 권한이 필요하고, 양산되지 않았다.
- BMW AirConsole(2024): 폰이 컨트롤러라 차는 화면에 그친다.

**이 앱의 위치.** Tesla가 정차 중 게임 입력(핸들·브레이크)으로 양산했고 Ford가 기능 학습용 콘셉트로 시연한 방향을, **차량을 제어하지 않고 실내 조작 여러 개를 읽기만 하는 방식으로 개방형 앱마켓에서 구현한다.** 차별점은 네 가지다. (1) 제어(`set*`) 없이 승인된 조회 SDK만 써서 보안·안전 부담이 작다. (2) 핸들·브레이크 두 개가 아니라 조명·공조·핸들·브레이크·좌석 점유를 엮는 퍼즐이다. (3) 좌석 점유 신호로 동승자 좌석을 미리 잡아 주는 협동이다. (4) 정차 시간에 맞춘 판 길이(3·5·10분, EV는 충전 진행 연동)다.

### 1-3. 주요 기능 및 차량 특화 요소

| 기능 | 쓰는 차량 신호(Vehicle SDK 조회) | 정차에서의 경험 | D단 정지·주행에서의 경험 |
|---|---|---|---|
| 정차 전용 세션 게이트 | AAOS `CarUxRestrictionsManager`의 `activeRestrictions`·`isRequiresDistractionOptimization()` | 규제 `0x0`이고 DO 불요일 때만 시작·이어하기 버튼이 산다. 시작할 때도 이어할 때도(앱 전환에서 돌아올 때도) "기어 P·주차 브레이크 확인" 버튼을 거친다. 저장된 판이 있을 때 새로 시작하면 덮어쓰기 확인을 거친다 | 앱은 `distractionOptimized`를 선언하지 않는다. 기어를 넣으면 시스템이 앱 영역을 "주행 중에는 사용할 수 없습니다." 차단 화면으로 덮는다(에뮬레이터 실측: 앱은 종료·백그라운드 없이 그대로 살아 있고 `onPause`/`onStop`이 오지 않는다). 그래서 앱은 규제 콜백(`requiresDO=true`)을 직접 받아 그 순간 타이머 정지·진행 저장·리스너 해제(조명 3종은 SDK 결함 때문에 공유 등록을 유지하되 값을 쓰지 않는다)·일시정지 화면(S-P) 전환을 한다. 차단 화면의 흐린 배경은 전환 순간 화면(퍼즐 화면)을 판독할 수 없게 흐린 시스템 정지 이미지라 앱이 바꿀 수 없고, 앱은 같은 순간 S-P로 바꿔 정차해 차단이 걷히면 S-P가 보이게 한다. 주행 중 재개·알림은 없다. 정차하면 S-P의 "이어하기" → P 확인으로 돌아온다 |
| 생성형 퍼즐 판 | 공조 목표 온도·풍량·내외기(CAR_CLIMATE), 앰비언트 색·밝기·독서등(CAR_LIGHTS), 핸들 각도·브레이크 페달(CAR_DRIVE) | 판마다 퍼즐 조합·순서·목표값이 시드로 바뀐다. 단서는 지시형("냉각 코일을 22℃로")만이 아니라 암호형(판 전체 암호표를 풀어야 목표 색·자리가 나온다), 연쇄형(숫자 목표에만. 앞 퍼즐의 답으로 다음 목표값을 계산한다)이 섞인다. 차의 실내 조작(다이얼·버튼·설정 메뉴)으로 신호를 바꾸면 그 순간 판정된다(실측 반응: 핸들 ≤ 0.2초, 나머지 ≤ 20ms). 홈에는 이 차에서 만들 수 있는 판 길이만 활성으로 보인다 | 해당 없음(시스템 차단) |
| 좌석도 참가자 선택과 자리 단서 | 좌석 점유(CAR_SEATS) | S1 좌석도(운전석·조수석·2열 왼쪽·2열 오른쪽)에서 참가자를 탭으로 고른다. 좌석 점유 신호는 초기 선택값으로만 쓴다(운전석에는 반드시 사람이 있다는 전제로 극성을 보정). 확정된 자리에서만 할 수 있는 단서(조수석·뒷좌석 독서등)를 판에 섞는다 | 해당 없음 |
| 원상 복구 체크리스트·기록 | 위 신호의 판 시작 시 스냅샷 | 판이 끝나면 바꾼 설정(온도·풍량·조명)의 원래 값을 보여 주고, 신호가 원래 값으로 돌아오면 체크가 자동으로 켜진다(온도 ±0.25℃, 앰비언트 색은 RGB 일치). 핸들은 퍼즐 안에서 중앙 복귀까지 판정한다. 판 길이별 개인 최고 기록을 남긴다 | 해당 없음(체크리스트 갱신을 멈추고 "새 판"은 비활성) |

**안전 설계(기능의 일부로 둔다).**
- 허용 원칙: **효과가 실내에만 머무는 조작**만 퍼즐로 쓴다. 앰비언트 색·밝기, 독서등, 공조 설정값(목표 온도·풍량·내외기), 정차 중 핸들 소폭 회전(각도 범위의 5~15%)과 중앙 복귀, 브레이크 페달(확인 입력), 좌석 점유(참가 인원 초기값).
- 제외: 창문·선루프 열기(주차장·충전소에서 열린 채 남을 위험), 도어·트렁크 열기(하차 동선·보행자 영향), 방향지시등·비상등·전조등·상향등 등 모든 외부 등화(다른 운전자에게 잘못된 신호), 와이퍼(마른 유리 손상), MVP의 등받이·시트 이동(뒷좌석 동승자 끼임. 이후 단계에서 뒷좌석 빈 것을 좌석 점유로 확인한 경우에만 검토), 앞유리 제상(차종에 따라 A/C·외기·풍량을 함께 바꿀 수 있어 확인 전까지 제외).
- 엔진 공회전: 내연·하이브리드 차량에서 엔진을 켠 채 서 있으면 지자체 공회전 제한 대상이 될 수 있다(부록 A). 앱은 시동을 다루지 않으므로, 비EV 설정에서는 판 시작 전에 안내를 한 번 띄우고 공조 퍼즐은 설정값만 판정하며 판당 최대 2개로 제한한다. 온도 목표는 쾌적 범위(18~28℃) 안에서만 낸다.
- 주행 중: DO 미선언으로 시스템이 앱을 차단 화면으로 덮고 터치도 앱에 전달하지 않는다(실측). 생명주기 콜백이 오지 않으므로 앱이 규제를 직접 구독해 그 순간 일시정지 화면으로 바꾸고 저장하며 리스너를 해제한다. 설정 패널·S3 "새 판"도 게이트가 닫히면 닫히거나 비활성이다. 주행 중 재개·헤드업 알림은 없다.

### 1-4. 주요 사용 시나리오

페르소나: 경기 남부에 사는 40대, 디 올 뉴 투싼 하이브리드 오너, 초등학생 자녀 둘. 평일은 혼자 학원 픽업을 다니고 주말에는 가족이 함께 이동한다.

**장면 1 — 평일 저녁, 학원 앞 주차장(혼자, 7분 일찍 도착).** 기어를 P에 두자 앱 홈에 "시작" 버튼이 산다. 5분 판(퍼즐 5개)을 고르고, 좌석도에 운전석만 표시된 것을 확인한 뒤 "P·주차 브레이크 확인"을 누른다. 비EV 설정이라 "주차장 같은 제한 장소에서 엔진을 켠 채 서 있으면 지역과 기온에 따라 공회전 과태료 대상이 될 수 있어요. 엔진을 끄고 전원만 켠 상태를 권장해요"가 한 번 뜬다. 첫 단서 "조타 키를 왼쪽 2번 구간에 맞추고 다시 중앙으로" — 핸들을 살짝 돌리자 화면 바늘이 따라 움직이고, 구간에 1초 머문 뒤 중앙으로 돌리자 잠금이 풀린다. 둘째는 암호형: 화면 암호표(◇ 보라·○ 파랑·△ 초록)에서 "산소 계통 = ◇"를 찾아 ◇가 보라임을 알아내고, 단서 카드의 "조명을 켠 채로, 조명 설정으로 다녀오면 P 확인을 한 번 눌러요" 안내대로 차량 설정에서 앰비언트 색을 보라로 바꾼 뒤 앱으로 돌아와 P 확인을 누르자 판정이 뜬다. 셋째는 연쇄형: "환기 팬 단수 = 조타 구간 번호 × 2" — 아까 2번 구간이었으니 풍량을 4단으로. 넷째 "냉각 코일 온도 = 20 + 조타 구간 번호" — 22℃. 마지막 "브레이크를 두 번 밟아 해치 봉인 해제" — 밟고 있던 발을 한 번 떼었다가 두 번 밟는다. 4분 12초에 탈출, 5분 판 최고 기록이 갱신된다. 복구 체크리스트가 "온도 24℃로, 풍량 2단으로, 앰비언트 원래 색으로"를 보여 주고 되돌릴 때마다 체크가 켜진다. 아이가 나오는 것이 보여 기어를 D로 넣자 시스템이 "주행 중에는 사용할 수 없습니다." 화면으로 앱을 덮고, 앱은 그 순간 퍼즐 화면을 일시정지 화면으로 바꾸고 진행을 저장한다.

**장면 2 — 주말 휴게소(가족, 배우자가 매점에 간 10분).** 좌석도에 뒷좌석 두 자리가 좌석 점유 신호로 미리 표시되어 확인만 하면 참가자 3명으로 시작한다. 10분 판(퍼즐 8개)에는 "2열 오른쪽 승무원: 독서등으로 신호를 보내라" 같은 자리 전용 단서가 섞여 아이들이 자기 자리 독서등을 켜고 끈다. 운전자는 공조·핸들 단서를 맡는다. 같은 테마지만 어제와 다른 조합·순서·목표값이고, 암호표도 판마다 다르다.

**장면 3 — 2차 타깃, EV 충전 대기(PT 단계 기능).** 유럽 아이오닉 3 오너가 급속충전기에 연결한다. 앱이 EV 설정에서 충전 중 SOC 변화를 읽어 남은 충전 시간에 맞춘 판 길이를 제안한다. 충전이 끝나 기어를 D로 넣으면 시스템이 앱을 막고 진행은 저장된다.

### 1-5. 개발 계획

- **구현 방식.** Pleos Connect(AAOS) 네이티브 앱, Kotlin + Jetpack Compose, 단일 `:app` 모듈. Vehicle SDK 2.3.2의 조회 API(`check*Capabilities`/`get*`/`register*`)와 AAOS `CarUxRestrictionsManager`만 쓴다. SDK 호출은 `data/` 아래 Repository에 격리하고 영역마다 인터페이스 + Pleos 구현 + Fake 구현을 둔다(`AppContainer`가 `BuildConfig.USE_FAKE_SDK`로 선택). 외부 서버·계정·위치 정보는 쓰지 않는다. 진행 상태와 기록은 기기 내 DataStore에만 둔다(파일 손상 시 교체 처리기로 복구. 설정은 별도 파일). SDK 콜백은 바인더 스레드로 오므로 UI 상태로 옮길 때 메인 스레드로 넘긴다(실측). `MainActivity`는 `singleTop`이라 런처·`am start`로 다시 열어도 세션이 둘이 되지 않는다. 서체는 Asta Sans 가변 서체(SIL OFL 1.1, 라이선스 파일을 APK에 포함)다.
- **주행 규제 대응.** 액티비티에 `distractionOptimized`를 선언하지 않는다(미디어 앱은 정차 중에만 사용 가능하다는 심사 가이드에 맞춘다). 에뮬레이터 실측(2026-10-25 `_workspace/03_plan/do_test/README.md`, 2026-10-26 `_workspace/05_build/spike_w1.md`)에서 DO 미선언 앱은 D단 정지·주행(`0x50`)에서 종료되거나 백그라운드로 가지 않고, 홈 화면(TaskViewContainer)이 앱 영역을 흐린 배경 + "주행 중에는 사용할 수 없습니다." 차단 화면으로 덮으며 터치를 앱에 전달하지 않는다. 액티비티는 RESUMED 그대로라 `onPause`/`onStop`이 오지 않는다. 앱의 `CarUxRestrictionsManager` 콜백은 홈의 차단과 같은 순간에 오고(−2~+21ms), 앱은 그 뒤 26~42ms에 일시정지 화면을 그린다(F1·F2-ui QA 실측). 따라서 앱은 규제 Flow(`DrivingStateRepository.restrictions`)를 직접 구독해 `requiresDO=true`가 되는 순간 타이머 정지·진행 저장·차량 신호 리스너 해제·일시정지 화면 전환(정차 복귀 시 보이는 화면이 S-P가 되도록)을 한다. 차단 화면의 흐린 배경은 전환 순간 화면의 정지 이미지라 앱이 바꿀 수 없다(시스템 동작). 정차(`0x0`)로 돌아오면 차단 화면만 사라지고 같은 액티비티가 그대로 보이므로 "이어하기"는 앱 안의 P 확인 단계로 처리한다. 실차 동작은 10/28 설명회 확인 항목이다. "DO 선언 + 주행 중 안내 한 줄" 방식은 기능 플래그 `pleos.drivingNotice`(기본 꺼짐, 추가 매니페스트 소스 세트 병합)로 구현돼 있으며, 켠 빌드는 주행 중 S-H(아이콘 1개 + "정차하면 이어하기", 터치 요소 0)만 그린다(F1 QA 2회차 통과). 10/28 설명회에서 게임 앱의 DO 선언이 공식 허용된 경우에만 켠다.
- **쓰지 않는 것.** Vehicle 제어(`set*`, `CONTROL_CAR_*`), TextToSpeech, SpeechToText, FusedLocation(모두 콘솔 "준비 중"), NaviHelper(이 앱에 경로 기능 없음. 의존성과 Repository를 삭제했다), 숨김 API(`getGear()`), 창문·와이퍼·외부 등화·도어·시트 이동·제상 신호, 앰비언트 스위치(`getAmbientLightSwitch`. 에뮬레이터에서 VC와 무관하게 항상 OFF를 돌려줘 판정에 쓰면 L1·L2가 영원히 풀리지 않는다), 독서등·앰비언트의 `unregister*`(SDK 결함, 2-2).
- **개발 범위.**
  - MVP(1차 제출 2026-11-15, release APK + 시연 영상): F1 정차 전용 세션 게이트 / F2 생성형 퍼즐 판(신호 8종 × 단서 유형, 테마 1개 "우주선 비상 전원 복구") + 신호 판정 + 원상 복구 체크리스트 + 판 길이별 개인 최고 기록 / F3 좌석도 참가자 선택과 자리 단서. 10/27 기준 F1·F2(데이터·엔진·화면)는 구현과 QA를 마쳤고 F3는 마무리 중이다.
  - PT(2차 PT 2026-12-03~04): F4 오늘의 판(날짜 시드)·협동 기록 / F5 EV 충전 대기 판 길이(CAR_ENERGY, EV 설정 자동 판별. 삭제한 `VehicleRepository`를 git 기록에서 되살린다) / F6 협동 확장(좌석별 역할 분담, 동시 조작 단서, 효과음+오디오 포커스).
  - 이후: F7 신차 기능 익히기 모드 / F8 테마 팩 / F9 Gleo 음성 진입점(App Schema function 1개) / 뒷좌석 빈 것을 확인한 경우의 시트 퍼즐, 제상 연동 동작 확인 뒤 제상 퍼즐 검토.
- **SDK·API 사용 계획(요약, 전체는 3절).**
  - 주행 규제: `CarUxRestrictionsManager.registerListener` → `CarUxRestrictions.isRequiresDistractionOptimization()`, `getActiveRestrictions()`.
  - 공조(CAR_CLIMATE): `checkHvacTemperatureCapabilities`·`registerHvacTemperature`, `checkHvacFanSpeedCapabilities`·`registerHvacFanSpeed`, `checkHvacRecirculationCapabilities`·`registerHvacRecirculationState`, 스냅샷·재동기화용 `getHvacTemperature`·`getHvacFanSpeed`·`getHvacRecirculationState`.
  - 실내 조명(CAR_LIGHTS): `checkAmbientLightBrightnessCapabilities`·`registerAmbientLightBrightnessListener`(**먼저 등록**), `checkAmbientLightBrightnessColorCapabilities`·`registerAmbientLightBrightnessColorListener`, `checkReadingLightStateCapabilities`·`registerReadingLightState`, 스냅샷·재동기화용 `getAmbientLightBrightness`·`getAmbientLightBrightnessColor`·`getCurrentReadingLightState`. 이 세 신호는 앱 수명 동안 한 번만 공유 등록하고 `unregister*`를 부르지 않는다(2-2). `getAmbientLightSwitch`는 호출하지 않는다.
  - 핸들·브레이크(CAR_DRIVE): `checkSteeringWheelAngleCapabilities`·`registerSteeringWheelAngle`·`getCurrentSteeringWheelAngle`(핸들 중앙 복귀 확정에 사용), `checkBrakePedalStatusCapabilities`·`registerBrakePedalStatus`.
  - 좌석(CAR_SEATS): `checkSeatOccupancyCapabilities`·`getCurrentSeatOccupancy`·`registerSeatOccupancy`.
  - PT에서 추가(CAR_ENERGY): `checkEvBatteryStateOfChargeCapabilities`·`registerEvBatteryStateOfCharge`·`getEvChargeSwitchState`.
- **매니페스트.** MVP 퍼미션은 `pleos.car.permission.CAR_CLIMATE`, `CAR_LIGHTS`, `CAR_DRIVE`, `CAR_SEATS` 4개(모두 normal). 골격에 있던 `CAR_ENERGY`·`NAVI_ROUTE`와 액티비티의 `distractionOptimized=true` 메타데이터는 F1에서 삭제했고, release APK에서 DO 0건·pleos 퍼미션 4개를 확인했다(`check_permissions.py` PASS). 도메인별 필요 이유는 3절 끝에 적었다.
- **검증 환경.** Pleos Connect 에뮬레이터 v2.1 + Virtual Console로 모든 퍼즐 신호를 재현한다(5절. 신호 8종과 좌석 점유 모두 VC 조작에 콜백이 오는 것을 실측하고, 시연 대본 전 단계를 리허설했다). 디버그 빌드 전용 고정 시드로 시연 판을 재현한다. Fake 구현으로 SDK 없이도 같은 흐름이 돈다(6초 간격 자동 풀이).

### 1-6. 기대효과 및 발전 가능성

**타깃 규모(근거).** Pleos Connect 탑재 차종은 국내 현대차 3종(더 뉴 그랜저 2026-05 첫 적용, 디 올 뉴 아반떼 2026-08 계약 시작·전 트림 기본, 디 올 뉴 투싼 2026-10-21 계약 시작·기본 트림부터)이다(market-01 ✅). 더 뉴 그랜저는 출시 첫날 계약 10,277대(가솔린 58%·하이브리드 40%)였고, 2026-09 국내 판매는 그랜저 8,898대·아반떼 7,305대(합 16,203대)다(market-02 ✅). Pleos 앱마켓의 MAU·누적 탑재 대수는 공개된 값이 없다(market-02 ✅). 따라서 1차 타깃은 **내연·하이브리드 차량의 정차 대기**이고, EV 충전 대기는 유럽 아이오닉 3와 EV 확대에 맞춘 2차 타깃이다. 참고로 국내 등록 자동차는 약 2,668만 대, 전기차는 약 110만 대(4.1%, 반기 21.8% 증가)로(user-needs-01 ✅) EV 2차 타깃의 모수는 커지고 있지만 Pleos 탑재 EV는 아직 국내에 없다.

**예상 MAU(계산식).** 근거 수치와 가정을 나눈다.

```
[기본 시나리오] 탑재 추정 대수(2027-03 시점) [가정 A]
  = 그랜저 9,000대/월 × 10개월 + 아반떼 7,000대/월 × 7개월
  ≈ 90,000 + 49,000 ≈ 약 139,000대 ≈ 약 14만 대
  근거: 2026-09 월 판매(그랜저 8,898·아반떼 7,305, market-02 ✅)를 반올림.
  개월 수: 그랜저 2026-05 출시 → 2026-05~2027-02 10개월, 아반떼 2026-08 계약·가솔린 3분기 출고 → 2026-08~2027-02 7개월(market-01·02 ✅).
  단서: 월 판매가 유지되고 전량 탑재라는 가정이며, 그랜저 트림별 적용 여부와 9월 아반떼 판매의 구형 재고 포함 여부가 미확인이라 상한값이다(market-02 ✅ 상세).
앱마켓 활성화율 [가정 B] = 30%
  (앱마켓은 Pleos 계정 QR 로그인과 정차 중 설치가 필요하다는 사실(market-03 ✅)에서 보수적으로 잡은 가정. 수치 근거 없음)
앱 설치율 [가정 C] = 10%          (무료, 신규 카테고리. 근거 없음)
월 1회 이상 플레이 비율 [가정 D] = 50%  (정차 대기는 주 단위로 생기고 판이 3~10분으로 짧음. 근거 없음)

MAU ≈ 139,000 × 0.30 × 0.10 × 0.50 ≈ 약 2,100 (2027년 1분기, 국내)

[상향 시나리오] ≈ (139,000 + 투싼 20,000) × 0.50 × 0.20 × 0.60 ≈ 약 9,500
  투싼 20,000대 = 5,000대/월 × 4개월(2026-11~2027-02) [가정] — 판매 실적이 없고 출고 일정도 미확인이라(market-01 ✅ 상세) 기본 시나리오에서는 뺐다. 5,000은 아반떼 월 판매의 약 70%로 둔 임의 값이다.
```

이 숫자는 작다. 작은 이유는 탑재 대수가 아직 작고 앱마켓 사용자 통계가 없어서이며, 과장하지 않는 쪽을 택했다. 시점과 무관한 규모 예시로, 탑재 100만 대에 같은 비율(1.5%~6%)을 적용하면 MAU 1.5만~6만이다. 현대차그룹의 장기 목표는 2030년까지 약 2천만 대 적용이다(market-01 ✅, 장기 계획).

**Retention 근거.** 인카 게임의 이용 실적은 공개된 값이 없으므로(wow-benchmark-10 ○) 목표값은 가정이고, 대신 "다시 할 이유"를 장치로 설계했다.
- MVP 반복 장치 1 — **생성형 판**: 신호 8종(공조 3·조명 3·핸들·브레이크)에 단서 유형(지시형·암호형, 숫자 신호는 연쇄형까지)을 조합해 판마다 조합·순서·목표값·암호표를 시드로 뽑는다. 암호형은 화면의 암호표를 풀어야 목표 색·숫자가 나오고, 연쇄형은 앞 퍼즐의 답으로 다음 목표값을 계산해야 해서 같은 신호라도 "숫자만 다른 같은 조작"이 되지 않는다. 테마 콘텐츠를 더 만들지 않아도 반복된다.
- MVP 반복 장치 2 — **판 길이별 개인 최고 기록**: 3·5·10분 판마다 최고 기록을 S0·S3에 보여 준다. 추리형 단서가 있어 기록 단축에 실력 요소가 생긴다.
- MVP: 짧은 판이 대기 시간 단위와 맞아 "기다릴 때마다 한 판"이 성립한다. 좌석도 참가자 선택과 자리 단서는 동승자가 있을 때 다른 판이 되게 한다.
- PT: 날짜 시드 "오늘의 판"(하루 한 번 모두에게 같은 판), 협동 기록.
- 이후: 신차 기능 익히기 모드(출고 직후 유입), 테마 팩(월 1개).
- 측정: 서버 없이 기기 내 이벤트(판 시작·완료·복구 완료·재플레이 간격)만 기록한다. 목표 D30 재플레이 20%[가정].

**확대 계획.** (1) 기아 2027년 적용(임원 발언, 수치 아님)과 유럽 아이오닉 3로 차종·지역 확대 시 F5(EV 충전 연동)가 1차 기능이 된다. (2) 신차 온보딩(기능 익히기 게임)을 딜러·출고 안내에 붙이는 B2B. (3) 수익 모델은 기본 테마 무료 + 테마 팩 유료·IP 협업 테마이나, Payment SDK가 준비 중이라 공모전 단계에서는 시연하지 않는다(market-03 ✅). (4) 마켓 입점은 공모전 수상과 별개로 보장되지 않으므로 전제로 두지 않는다. 국내 게임 출시에는 게임물 등급분류(GRAC)와 IARC 연령 등급이 필요하므로(`distribute/app-submission-guide.md`) 입점 준비 단계에 넣는다.

**카테고리.** Entertainment("오락 및 여가 제공", `vehicle-app-planning-guide/app-category.md`). 게임은 미디어 앱 규칙(정차 전용, 주행 시 사용 불가)을 받으며 Entertainment에는 주행 중 예외가 없다. 이 앱은 처음부터 정차 전용으로 설계하고 DO를 선언하지 않아 카테고리와 기능이 일치한다. Kids(아동 전용 아님), Lifestyle(일상 관리 아님)은 맞지 않는다.

### 1-7. 참고자료
- 시연 영상 링크: {1차 제출 시 삽입}
- GitHub 저장소(소스 + `apk/팀명_앱이름_release.apk`): {1차 제출 시 삽입}
- 근거 출처
  - Pleos 공식 문서: 앱 카테고리(`vehicle-app-planning-guide/app-category.md`), 주행 규제 정책(`vehicle-app-planning-guide/driving-restiction-policy.md`), 운전자 주의 분산 심사(`distribute/app-review-guide/review-driver-distraction-elements.md`), 앱 제출 가이드(`distribute/app-submission-guide.md`), Vehicle SDK 퍼미션(`connect-sdk-pleos/Vehicle/vehicle-permission.md`) — 원문 URL은 각 파일 첫 줄 `source:`
  - 도로교통법 제49조제1항제11호·제11호의2(regulation-safety-01)
  - 국토교통부 자동차 등록 통계 보도(2026-07-30, user-needs-01), 현대차 차종별 판매·계약 보도(market-01·02), 포티투닷 앱마켓 발표(2026-09-07, market-03)
  - Engadget "Tesla drivers can play Beach Buggy Racing 2"(2019-06-18), TopGear Philippines·Electrek Ford Mach-E escape room(2022-01), BMW AirConsole(2024-08)(wow-benchmark-01·02)
  - 공회전: 서울특별시 환경 누리집 "자동차 공회전 제한" 안내(https://news.seoul.go.kr/env/archives/509990, 수정일 2023-11-02), 서울시 미디어허브(https://mediahub.seoul.go.kr/archives/171578), 인천시 공회전 제한 강화 보도(https://www.inews24.com/view/1798423)
  - 에뮬레이터 실측: `_workspace/03_plan/do_test/README.md`(2026-10-25), `_workspace/05_build/spike_w1.md`(2026-10-26), QA `_workspace/05_build/05_qa_F2-data.md`·`05_qa_F2-engine.md`(2026-10-26), `05_qa_F1.md`·`05_qa_F2-ui.md`·`F2ui_rehearsal.md`(2026-10-27), 프로젝트 `CLAUDE.md` "실제로 확인한 함정"
  - SDK 결함 근거: `pleos-claude-kit/sdk-artifacts/sources/Vehicle-2.3.2/ai/pleos/playground/vehicle/api/implement/LightImpl.kt`(`unregisterReadingLightState`가 `addListener`를 호출), `…/vehicle/callback/PropertyCallback.kt`(`isEmptyPleosPropertyId`가 속성 이름을 `startsWith`로 비교)
  - 서체: Asta Sans(SIL OFL 1.1, `assets/licenses/AstaSans-OFL.txt`)

### 1-8. 숏폼 링크
{자리만 둔다}

## 2. 기능 명세

| id | 기능 | 단계 | 사용자 가치 | 화면·동작 | API·퍼미션 | Repository | 주행/정차 동작 | 실패·미지원 시 | Fake 데이터 | 완료 조건 |
|---|---|---|---|---|---|---|---|---|---|---|
| F1 | 정차 전용 세션 게이트 | MVP(구현·QA 통과) | 정차일 때만 게임이 열리고, 출발하면 시스템이 막는 순간 저장돼 다시 정차했을 때 그 자리부터 한다 | S0 홈: 판 길이(3·5·10분) 선택(이 차의 지원 신호 풀로 만들 수 있는 길이만 활성. 가능 여부는 참가자를 운전석만으로 보수적으로 계산한다), 각 칸에 최고 기록("기록 없음"), "시작"/"이어하기"(저장된 세션 있을 때. 저장된 판이 있는데 새로 시작하면 "저장된 판을 지우고 새 판을 시작할까요?" 확인창 → 확인 → S1 → P 확인 시점에 기존 판 삭제. 취소하거나 S1에서 홈으로 돌아오면 저장 판 유지), 설정 버튼(앱 안 패널: "내 차는 전기차" 토글, 공회전 안내 다시 보지 않기. S0에서만 열리고 게이트가 닫히면 닫힌다). S1 준비: "기어 P·주차 브레이크 확인" 버튼(시작·이어하기·앱 전환 복귀 모두 필수. 이어하기일 때는 참가 자리 변경 불가), 비EV 설정이면 공회전 안내 카드 1회, 참가자 좌석도(F3). S-P 일시정지: 게이트가 닫히는 순간 S2 대신 그리는 화면("이어서 할까요? N분 판 퍼즐 i/K · 남은 시간", "이어하기"·"홈" 버튼, "저절로 다시 시작하지 않아요". 퍼즐·암호표 없음. 게이트가 닫힌 동안 두 버튼은 앱에서도 비활성. 차단이 걷힌 뒤 보이는 화면이다) | `CarUxRestrictionsManager.registerListener` → `CarUxRestrictions.isRequiresDistractionOptimization()`, `getActiveRestrictions()` (AAOS, 퍼미션 없음). 액티비티에 `distractionOptimized` 미선언(기본 빌드) | `DrivingStateRepository`(기존) | 정차 = `requiresDO == false && activeRestrictions == 0`일 때만 "시작"·"이어하기"·P 확인 활성. 게이트가 닫히면(D단 정지 `0x50`, 주행, 불명 `0x1ff`) 규제 콜백에서 즉시 타이머를 멈추고 진행(시드·완료 퍼즐·경과 시간·스냅샷·참가자)을 DataStore에 저장하며 차량 신호 리스너를 해제하고(조명 3종은 SDK 결함 때문에 공유 등록을 유지하되 값을 쓰지 않는다, 2-2), 재생 중인 효과음이 있으면 멈추고 오디오 포커스를 반환하며(PT F6), S2를 S-P로 바꾸고 설정 패널을 닫는다. 실측(F1·F2-ui QA): 콜백 → 해제 1ms → 저장 8~11ms → S-P 26~42ms. S-P의 "이어하기"·"홈" 버튼은 게이트가 닫힌 동안 앱에서도 비활성이다(차단 화면이 터치를 전달하지 않는 것을 실측했으므로 이중 안전장치). 이것이 유일한 1차 경로다: DO 미선언 앱은 주행 전환 시 시스템 차단 화면에 덮일 뿐 액티비티가 RESUMED로 남아 `onPause`/`onStop`이 오지 않는다(에뮬레이터 실측). `onPause`/`onStop` 저장은 앱 전환·종료 같은 다른 경우를 위한 보조이며, 창 크기 변경으로 오는 `onStop`(`isChangingConfigurations`)은 일시정지로 보지 않는다. 게이트가 다시 열리면 차단 화면만 걷히고 S-P가 그대로 보이며, S-P의 "이어하기" → S1 P 확인을 거쳐야 재개된다(자동 재개 없음). 주행 중 재개·알림 없음 | 규제 값을 못 받으면 `FullyRestricted`로 간주해 버튼 비활성. DataStore 손상 시 파일을 교체하고 저장 진행을 버린 뒤 홈으로. 기능 플래그 `pleos.drivingNotice`(기본 꺼짐. 추가 매니페스트 소스 세트 병합으로 `distractionOptimized`를 넣는다)를 켠 빌드는 게이트가 닫힌 동안(첫 규제 콜백 전 UNKNOWN 포함) 모든 화면 대신 S-H(아이콘 1개 + "정차하면 이어하기" 한 줄, 터치 요소 0)를 그리고 설정 패널을 닫는다(F1 QA 2회차 실측: 주행 중 clickable 요소 0개). 10/28 설명회에서 게임 앱의 DO 선언이 공식 허용될 때만 켠다 | `FakeDrivingStateRepository` + 디버그 인텐트 `--es fakeGate park|idle|drive`로 게이트를 바꾼다(자동 순환 없음) | `vc.sh park`에서 "시작" 활성. 판 도중 `vc.sh drive 40`이면 규제 콜백 뒤 100ms 안에 S-P를 그리고 저장한다(logcat의 콜백·DRAW·저장 로그로 확인. 차단 화면의 흐린 배경은 확인 대상이 아니다). 차량 신호 리스너가 해제된다(로그의 `removeListener`로 확인. 조명 3종 제외). `vc.sh park`로 돌아오면 차단 화면이 사라지고 S-P가 보이며, "이어하기" → P 확인을 거쳐 같은 퍼즐 번호·남은 시간에서 재개된다(그 퍼즐의 판정 진행은 처음부터). `vc.sh idle`에서는 "시작"·"이어하기"·P 확인이 비활성이다. 차량 설정 앱으로 갔다 돌아오면 S1 P 확인이 바로 뜬다. 앱을 강제 종료했다 켜도 S0 "저장된 판 … 이어하기"로 같은 세션이 복원된다. 가로·세로 전환과 창 크기 변경(액티비티 재생성)에서 일시정지 없이 판이 이어진다. 플래그 빌드는 `drive 40`에서 S-H만 보이고 눌리는 요소가 0개다 |
| F2 | 생성형 퍼즐 판 + 신호 판정 + 원상 복구 체크리스트 + 개인 최고 기록 | MVP(구현·QA 통과) | 판마다 다른 퍼즐을 차의 실내 조작으로 풀고, 추리가 섞여 기록을 줄일 여지가 있으며, 끝나면 바꾼 설정을 원래대로 돌리도록 안내받는다 | S2 진행: 단서 카드(한 줄, 큰 글자, 어절 단위 줄바꿈; 암호형은 **판 전체 암호표** 카드가 함께(진짜 1 + 미끼 2, 기호 12개 이하·중복 없음. S2에만 있고 S-P에는 없다); 차량 설정 메뉴로 가야 하는 단서(L1·L2)에는 "조명을 켠 채로, 조명 설정으로 다녀오면 P 확인을 한 번 눌러요" 안내), 현재 신호 게이지(지시형만 목표 구간 표시. 암호형·연쇄형은 목표를 드러내지 않는다. 온도 게이지 눈금은 capability 범위 그대로), 진행 n/K, 타이머, 힌트 버튼(2장: 첫 장은 단서 풀이 방향, 둘째 장은 조작 위치. 연쇄형 힌트는 값을, L3 암호형 힌트는 좌석을 밝히지 않는다. 새 판에서 초기화), 봉인 카드는 "풀린 퍼즐" 줄과 "남은 N초"(30초 창과 8초 간격 중 먼저 끝나는 쪽). S3 결과: 탈출 성공/시간 초과, 소요 시간, 판 길이별 최고 기록("새 최고 기록 · 이전 M:SS"), 복구 체크리스트(원래 값·현재 값·자동 체크. 허용 오차: C1 ±0.25℃, L1 RGB 일치(알파 무시), C2·L2 정수 일치, C3·L3 상태 일치, D1 중앙 ±5%. 원래 값을 모르는 신호는 줄을 만들지 않는다), "새 판"·"홈"(게이트가 닫히면 "새 판" 비활성, 체크리스트는 "정차하면 다시 확인해요"). 디버그 빌드 전용: `--es seed <값>`으로 시드 고정(시연 모드), 시드 시작 시 현재값이 5-1 0단계와 다르면 항목별 경고(취소/그대로 시작). release는 시드를 무시한다 | 2-1 신호 표의 API. 퍼미션 CAR_CLIMATE, CAR_LIGHTS, CAR_DRIVE | `ClimateRepository`, `CabinLightRepository`, `DriveInputRepository`, 도메인 `PuzzleGenerator`, `PuzzleJudge`, `RecordStore` | 정차에서만 S2·S3 표시. 게이트가 닫히면(규제 콜백, `PausedByGate`) 타이머 정지·저장·리스너 해제·S-P 전환. 게이트가 열린 채 진짜 앱 전환으로 `onStop`이 오면(`PausedByApp`) 타이머 정지·저장·리스너 해제를 하고 돌아왔을 때 S1 P 확인을 바로 띄운다. **재개 조건은 둘 다 "게이트 열림 + S1 P 확인"**이며, 확인 뒤 리스너를 재등록하고 `get*`으로 현재값을 다시 읽어 판정한다. 판정 진행(B1 누름 횟수, D1 칸 유지 시간)은 저장하지 않으므로 재개하면 그 퍼즐은 처음부터다. S3에서 게이트가 닫히면 결과 리스너를 해제하고 갱신을 멈추며, 정차로 돌아오면 다시 읽어 체크한다 | 판 시작 시 각 신호의 `check*Capabilities`로 `accessType`이 READ/READ_WRITE이고 area가 지원되며 `get*`로 읽은 현재값이 null이 아닐 때만 풀에 넣는다(풍량 꺼짐 0처럼 범위 밖이라 null로 정규화되는 값도 제외). 풀·참가 좌석·현재값·EV 설정으로 만들 수 있는 판 길이만 S0에서 활성이고(2-1 시작 가능 규칙), 3분 판도 만들 수 없을 때만 "이 차에서는 지원하는 조작이 부족해요"로 시작 불가. 시작 시점에 실제 참가자로 생성이 실패하면 `BOARD_UNAVAILABLE` 안내로 홈에 돌아간다(안전망). 요구 수보다 적은 짧은 판은 만들지 않는다. 판정 중 null은 불일치다. 리스너 `onFailed`나 60초 동안 신호 없음이면 그 퍼즐을 건너뛰기 허용(봉인 제외. 건너뛴 판은 기록 제외). 끝난 판의 세션은 지우므로 S3에서 프로세스가 죽으면 S0로 간다(체크리스트 복원 없음) | `Fake*Repository` 기본값은 5-1 0단계와 같다(온도 24, 풍량 2, 앰비언트 Cyan·100, 참가자 운전석 + 2열 오른쪽). 자동 풀이는 6초 간격으로 퍼즐을 풀고(5분 판 33초 탈출), 결과 화면에서 6초 뒤 원래 값으로 되돌린다. 주행 전환은 `--es fakeGate`로 따로 찍는다 | 판은 시드·참가 좌석·지원 신호 풀·시작 때 현재값·EV 설정 다섯 입력으로만 정해진다: 다섯 입력이 같으면 같은 판(조합·순서·목표값·암호표·단서 문장)이고, 시드가 다르면 하나 이상이 다르다(단위 테스트는 다섯 입력을 고정해 검증. 고정값 테스트와 골든 보드 포함. 단서 문장 결정성도 단언). S0에서 활성인 판 길이는 생성이 반드시 성공한다(available ⇔ 생성 성공, 작은 풀로 단언). 판에 같은 신호가 연달아 나오지 않고 같은 신호는 판당 2개 이하다. 2-1의 신호 8종이 Virtual Console 조작으로 각각 판정되고, 암호형·연쇄형 단서가 각각 1개 이상 생성되며, 연쇄형의 계산 결과가 2-1의 제약을 모두 지키고 항등식이 없다(단위 테스트). 암호형·연쇄형의 문장·힌트·게이지에 목표값이 드러나지 않는다. 설정 앱으로 갔다 돌아오면 S1 P 확인이 바로 뜨고, 확인 뒤 S2가 복원되며 바뀐 값이 `get*`로 판정된다. 복구 체크리스트가 신호 복귀로 자동 체크된다. 판정 반응이 신호 변경 후 1초 이내다(실측 여유: 핸들 ≤ 0.2초, 나머지 ≤ 20ms). 최고 기록이 재부팅 뒤에도 남는다(기기 재부팅 확인은 리허설 항목) |
| F3 | 좌석도 참가자 선택과 자리 단서 | MVP(마무리 중) | 동승자가 있으면 자기 자리에서만 풀 수 있는 단서가 섞여 함께 한다. 좌석 점유 신호가 좌석도를 미리 채워 준다 | S1 준비 화면의 좌석도(운전석·조수석·2열 왼쪽·2열 오른쪽. 차 그림 위 원형 버튼과 오른쪽 목록 행 모두 탭 가능, 운전석은 비활성)에서 참가자를 **탭으로 고르는 것이 기본 동작**이다. 좌석 점유 신호는 초기 선택값으로만 쓴다: 판을 하는 동안 운전석에는 반드시 사람이 있으므로 `ROW_1_LEFT` 값이 `false`면 모든 좌석 값을 뒤집어(극성 보정) "점유"로 해석한다. 운전석 원시값을 모르면(null) 극성을 알 수 없으므로 초기 선택은 운전석만이다. 운전석은 항상 선택·해제 불가. 2열 가운데는 독서등 capability가 없어 선택지에 없다. 이어하기일 때는 참가 자리를 바꿀 수 없다. S2 단서에 "2열 오른쪽 승무원" 같은 자리 표기 | `checkSeatOccupancyCapabilities`, `getCurrentSeatOccupancy(area)`, `registerSeatOccupancy(area, SeatOccupancyListener)` — CAR_SEATS. 자리 단서는 `registerReadingLightState(해당 SeatArea, LightStateListener)` — CAR_LIGHTS(앱 수명 동안 공유 등록, 2-2). 실측: 에뮬레이터는 VC "점유"가 SDK `false`로 오고(SDK가 원시값 `== 1`을 `true`로 바꾸며 AAOS `VehicleSeatOccupancyState`는 VACANT=1·OCCUPIED=2로 보임[추론]), `ROW_2_CENTER`는 항상 `true`다. 실차 해석은 미확인 | `SeatRepository` | 정차에서만. 좌석 점유는 S1에서만 읽고(초기 선택값), 판 중 변화는 다음 판부터 반영 | 점유 capability NONE·area 미지원·값 불명(UNKNOWN, 운전석 null)이면 초기 선택은 운전석만이고 좌석도 탭으로 고른다. 독서등 capability가 없는 좌석은 선택 불가. 선택이 운전석뿐이면 자리 단서를 풀에서 뺀다 | `FakeSeatRepository`: 운전석 + 2열 오른쪽 점유(보정 후 값) | `vc.sh set seat-occupancy true rear_right`로 켜면(0단계에서 `driver`도 true) S1 좌석도의 초기 선택에 2열 오른쪽이 들어오고, 원이나 목록 행을 탭해 해제·추가할 수 있다. 확정된 참가 좌석 가운데 현재 독서등이 OFF인 좌석에만 자리 단서가 나오고 해당 area 독서등 ON으로 판정된다. 점유 신호가 없어도 좌석도 탭만으로 같은 판이 된다. 세로 1단에서 좌석 원(72dp 이상, 간격 8dp 이상)이 겹치지 않는다 |
| F4 | 오늘의 판·협동 기록 | PT | 하루 한 번 모두가 같은 판을 풀고, 참가자 수별 기록이 남는다 | S0에 "오늘의 판"(날짜 시드 `day-YYYY-MM-DD`, 하루 1회), 참가자 수별 기록 | SDK 추가 없음. DataStore | `RecordStore` 확장 | 정차에서만 표시 | — | 기록 3건 | 같은 날 두 기기에서 같은 시드가 생성된다(단위 테스트. 판 자체는 참가 좌석·차종 지원 범위·시작 때 현재값·EV 설정에 따라 다를 수 있다) |
| F5 | EV 충전 대기 판 길이 | PT | 충전이 끝나는 시점에 맞춰 판이 끝난다 | S1에서 EV 설정이면 "충전 중 · 약 N분" 제안 길이 표시 | `checkEvBatteryStateOfChargeCapabilities`, `registerEvBatteryStateOfCharge(EvBatteryStateOfChargeListener)`, `getEvChargeSwitchState` — CAR_ENERGY(PT에서 매니페스트 추가) | `VehicleRepository`(F1에서 삭제한 것을 git 기록에서 되살려 `evStateOfCharge`를 확장) | 정차에서만. SOC 변화율로 남은 시간 추정(충전 목표 80% 가정) | SOC capability NONE이면 EV 설정을 자동으로 끄고 수동 길이 선택으로 | `FakeVehicleRepository`(SOC 40→45, git 기록에서 복원) | VC에서 SOC를 올리면 제안 길이가 줄어든다 |
| F6 | 협동 확장 + 효과음 | PT | 동승자가 역할을 나눠 같이 푼다 | 좌석별 역할 카드, 동시 조작 단서("운전석 풍량 4와 2열 독서등을 동시에") | F2·F3 API 재사용. 효과음은 `AudioFocusRequest`(`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) | 기존 | 정차에서만. 게이트가 닫히면 규제 콜백에서 즉시 정지하고 포커스를 반환한다(주행 전환 때 `onPause`가 오지 않으므로 포커스 LOSS에만 기대지 않는다). 포커스 LOSS·LOSS_TRANSIENT 시에도 정지 | 포커스 못 받으면 무음 진행 | — | 동시 조작 단서가 두 신호 모두 충족될 때만 판정된다 |
| F7 | 신차 기능 익히기 모드 | 이후 | 출고 직후 차 기능 위치를 게임으로 익힌다 | 기능 위치 안내 + 퍼즐 | F2 API 재사용 | 기존 | 정차에서만 | — | — | — |
| F8 | 테마 팩 | 이후 | 새 이야기 | 테마 선택 | 없음 | 기존 | — | — | — | — |
| F9 | Gleo 음성 진입점 | 이후 | "방탈출 시작해 줘" | `assets/schema.json` function 1개(`START_ESCAPE_ROOM`), 응답 브로드캐스트 | Gleo Interface(스키마 승인 필요) | — | 정차에서만 시작 | 승인 전·에뮬레이터 미호출 시 adb intent로 모사 | — | — |

### 2-1. 퍼즐 풀(F2·F3가 쓰는 신호 8종 × 단서 유형)

**판 생성 규칙.**
- 판 길이는 시간 제한이자 퍼즐 수 K의 기준이다. 퍼즐당 예상 소요[가정, 리허설로 보정 중: 시연 5분 판을 VC로 1:05~1:31에 끝냈으므로 사람 조작 기준은 플레이테스트로 다시 잰다]: 지시형 30~45초, 암호형·연쇄형 60~90초, 봉인(B1) 15초. K = 3분 3개(지시 1·추리 1·봉인 1) / 5분 5개(지시 2·추리 2·봉인 1) / 10분 8개(지시 3·추리 4·봉인 1). 판은 항상 K개다. 요구 수보다 적은 "짧은 판" 폴백은 쓰지 않는다.
- 판을 정하는 입력은 다섯이다: **시드**, **참가 좌석**(F3 좌석도에서 확정한 결과), **지원 신호 풀**(capability로 걸러지고 `get*` 현재값이 null이 아닌 신호 목록), **시작 때 현재값**(온도 2℃ 차이·내외기 반대 전환·독서등 OFF 좌석 같은 규칙이 현재값에 따라 판을 바꾼다), **EV 설정**(비EV 공조 상한). 다섯 입력이 같으면 같은 판(조합·순서·목표값·암호표·단서 문장)이다. 시드 = 랜덤(MVP 기본), 디버그 빌드에서는 인텐트 extra `seed`로 고정(시연 모드), PT에서는 날짜 시드 `day-YYYY-MM-DD`(F4). 시연 모드는 지원 신호 풀을 8종 전부로 고정하고(capability 결과와 무관. 판정은 실제 신호), 참가 좌석은 좌석도 확정값, 현재값은 5-1 0단계 초기값, EV 설정은 꺼짐(비EV)을 따른다.
- 현재값을 모르는 신호: 판 생성 때 `get*` 현재값이 null인 신호(에뮬레이터의 풍량 꺼짐 0처럼 capability 범위 밖이라 데이터 계층이 null로 내보내는 값 포함)는 그 판에서 뺀다. 판정 중에 오는 null은 불일치로 본다(판정기는 "꺼짐"과 "오류·미지원"을 구분하지 않는다).
- 판의 제약: 마지막 퍼즐은 항상 B1 브레이크 봉인 해제 1개(브레이크 미지원이면 화면 버튼 "봉인 해제"로 대체하되 봉인 1개로 센다). D1 핸들은 판당 최대 1개. L3 자리 단서는 좌석도에서 확정된 동승 좌석 가운데 현재 독서등이 OFF인 좌석에만. 비EV 설정에서는 공조 신호(C1~C3) 퍼즐 판당 최대 2개. B1을 뺀 서로 다른 신호가 2종 이상. 같은 신호가 연달아 나오지 않는다. 같은 신호는 판당 최대 2개이며, 두 번째 목표가 그 신호의 제약(C1은 현재값과 첫 목표 모두에서 2℃ 이상 떨어진 값, C2는 목표 후보 2개 이상, L2는 범위 폭이 있을 때) 안에서 실제로 존재할 때만 2개로 센다.
- **시작 가능 규칙(판 길이별 실현 가능성).** 시작 가능 여부는 "지원 신호 개수"가 아니라 위 제약을 모두 지키면서 판 길이별 K개를 채울 수 있는지로 정한다. 실현 가능성은 생성기와 같은 판정 함수로 세어 S0가 가능하다고 한 길이는 생성이 반드시 성공한다. S0의 가능 길이는 참가자를 운전석만으로 보수적으로 계산한다(참가자가 늘면 L3 후보만 늘어 가능 길이가 줄지 않는다). 홈(S0)은 가능한 판 길이만 활성으로 보여 준다. 3분 판도 만들 수 없을 때만 "이 차에서는 지원하는 조작이 부족해요"로 시작 불가다. 시작 시점에 실제 참가자로 생성이 실패하면 `BOARD_UNAVAILABLE` 안내로 홈에 돌아간다(안전망). 최소 구성 예시: 비EV에서 C1·C2·C3·D1·B1이 남으면 3분 판(공조 1~2 + D1 + B1)만 가능하고, 5분 판은 공조 2 + D1 + B1 = 4개라 불가다. 신호 하나 + B1(예: 앰비언트 색 + 브레이크)은 "서로 다른 신호 2종" 조건에 걸려 3분 판도 불가다.
- 단서 유형: **지시형**(목표값을 그대로 말한다. 게이지에 목표 구간이 보인다), **암호형**(판마다 섞이는 암호표 카드 — 기호↔색, 기호↔숫자, 기호↔자리 — 를 풀어야 목표값이 나온다. 암호표는 판 전체 것을 보이며 진짜 1 + 미끼 2, 기호는 겹치지 않고 모두 표에서 뜻을 찾을 수 있다. 게이지·힌트에 목표를 드러내지 않는다), **연쇄형**(앞 퍼즐의 답으로 다음 목표값을 계산한다. 예: "냉각 코일 온도 = 20 + 조타 구간 번호"). **연쇄형은 숫자 목표(C1·C2·L2)에만 나온다.** C3·L1·L3·D1은 지시형·암호형만, B1은 봉인 문장만 쓴다. 목표값은 capability의 `AreaBounds` 안에서 뽑는다.
- 연쇄형 규칙: 입력값은 앞 퍼즐의 답(D1 칸 번호 1~3, C2 풍량 단수, C1 온도 정수, L2 밝기 단계, B1은 입력으로 쓰지 않음)이다. 연쇄식은 한 번의 연산이고, 상수와 배수는 생성기가 **계산 결과가 목표 신호의 `AreaBounds` 안이고, 아래 신호별 제약(C1 쾌적 범위·양 끝 회피, 현재값 차이 조건, 비EV 풍량 상한)을 모두 지키도록** 고른다. **식에 보이는 수가 곧 답이 되는 식은 쓰지 않는다**: ×1 + 0(항등), ×0, 입력 답이 0이라 상수가 답이 되는 식, 입력 답이 1이라 배수가 답이 되는 식, 결과가 입력과 같은 식, 빼기 결과가 문장 속 수와 같은 식(예: 2 − 1 = 1. 부호를 뗀 상수와 비교해 거른다). 그런 식만 가능하면 그 퍼즐을 지시형으로 바꾼다(QA 탐침: 연쇄형 1,722개 중 항등식 0건, 지시형 대체 68건). 앞 퍼즐을 건너뛰었으면(60초 무신호) 연쇄 입력값을 힌트 카드에 그대로 보여 준다(건너뛴 판은 기록 제외).
- 신호별 생성 제약(W1 실측·F2 구현 반영):
  - C1 목표 온도: `AreaBounds` 양 끝에서 1℃ 이상 안쪽이면서 쾌적 범위 18~28℃ 안의 정수만. 현재값과 2℃ 이상 차이. 에뮬레이터 capability는 15~30℃인데 VC는 16~30℃라 15℃는 재현할 수 없고, 극단 온도는 엔진 공회전·배터리 부담을 키운다. 화면의 온도 게이지 눈금은 capability 범위(에뮬레이터 15~30℃)를 그대로 보인다(리더 결정).
  - C2 풍량: 데이터 계층은 capability 범위 밖 값(에뮬레이터 꺼짐 = 0, 범위 1~8)을 null로 내보내고, 생성기는 현재값이 null인 신호를 그 판에서 뺀다(위 "현재값을 모르는 신호"). 목표는 현재값과 다른 값. 비EV 상한은 범위 상한의 절반(에뮬레이터 4).
  - L1 앰비언트 색: 콜백의 알파가 0x00으로 오므로 판정·복구 비교는 RGB만 쓴다. 시연 시드는 VC 화면 프리셋으로 재현되는 보라(`#aa00ff`, h 280°)·파랑(`#2979ff`, h 217.6°)만 목표로 낸다. 빨강·주황·노랑·초록은 `vc.sh set ambient-light-color '#ff8000'`처럼 명령으로만 재현된다(일반 판에서는 허용. 리허설의 세로 판에서 초록 `#00ff00`을 명령으로 풀었다). 앰비언트 스위치(`getAmbientLightSwitch`)는 판정에 쓰지 않는다. 에뮬레이터에서 VC 스위치를 Off·On·Auto로 바꿔도 항상 OFF를 돌려주고 리스너 API도 없어서, 스위치를 조건으로 걸면 L1·L2가 영원히 풀리지 않는다(`spike_w1.md` 앰비언트 스위치 절). 조명이 꺼진 채 색만 맞는 경우는 단서 문구 "조명을 켠 채로"로만 안내한다. 색 리스너는 밝기 리스너보다 **나중에** 등록한다(2-2 SDK 결함 2).
  - D1 핸들: SDK 값 기준으로 판정한다(왼쪽 음수. 에뮬레이터는 VC 값의 부호가 반대이고 크기는 핸들 회전각과 같다. 실차는 앞바퀴 각도일 가능성이 높다[추론]). 양쪽 범위를 각각 쓴다(왼쪽 |min|, 오른쪽 max). 값이 capability 범위를 넘을 수 있으므로(VC 500 → SDK −500) 칸 판정 전에 범위로 자른다. 값이 그대로여도 약 3.1초마다 같은 값이 다시 오므로 데이터 계층이 연속 중복(−0.0과 0.0 포함)을 거르고 판정은 값이 바뀔 때만 갱신한다. **중앙 복귀는 콜백 값이 아니라 `getCurrentSteeringWheelAngle`로 다시 읽은 값으로 확정한다**(등록 직후 첫 콜백이 낡은 값일 수 있다는 관찰, F2-data QA). 범위 폭이 비정상(10° 미만)이면 풀에서 뺀다.
  - B1 브레이크: 1% 이상이면 `true`로 온다. **150ms 이상 뗀 뒤의 밟음만 센다.** 상태를 모르는 첫 값(null)과 밟은 채 시작·재개한 첫 `true`는 기준으로만 쓰고 세지 않으며, 재개 직후 SDK가 주는 낡은 0~1ms 뗌(밟음 → 0ms 뗌 → 밟음)도 뗌으로 치지 않는다. 그래서 밟은 채 시작·재개해도 떼었다가 밟아야 1회다(에뮬레이터 실측: 봉인 1/2에서 주행 → 정차 → 밟은 채 재개 → 0/2). 짧은 떨림(밟음·뗌·밟음이 150ms 안)은 같은 규칙으로 걸러진다(경계 150ms는 뗌으로 인정).

| id | 단서 예(테마 "우주선 비상 전원 복구") | 신호·API | 리스너 콜백 | 판정 규칙 | 조건·원복 |
|---|---|---|---|---|---|
| C1 온도 다이얼 | 지시형 "냉각 코일을 22℃로" / 암호형 "냉각 코일을 ☆℃로"(암호표 ☆=22) / 연쇄형 "냉각 코일 온도 = 20 + 조타 구간 번호" | `checkHvacTemperatureCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Float>>, HvacTemperatureConfigArray?) -> Unit, …)`, `registerHvacTemperature(area: SeatArea, listener: HvacTemperatureListener)`, 스냅샷 `getHvacTemperature(area, TemperatureUnit, …)` | `HvacTemperatureListener.onHvacTemperatureUpdated(temperature: Float?)` (℃) | 목표값 ±0.5℃. area는 capability map의 키 가운데 `SeatValue.ROW_1_LEFT`를 포함하는 것을 쓴다(에뮬레이터는 좌석별 단일 area `ROW_1_LEFT`·`ROW_1_RIGHT`). 목표는 그 area의 `AreaBounds` 양 끝 1℃ 안쪽·18~28℃ 안의 정수 중 현재값과 2℃ 이상 차이 나는 값. 현재값 null이면 이 판에서 제외 | 설정값만 판정(실내 온도 아님). 원복: 스냅샷 값 ±0.25℃ |
| C2 풍량 | 지시형 "환기 팬을 4단으로" / 암호형 / 연쇄형 "환기 팬 단수 = 조타 구간 번호 × 2" | `checkHvacFanSpeedCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, …)`, `registerHvacFanSpeed(area: SeatArea, listener: HvacFanSpeedListener)`, 스냅샷 `getHvacFanSpeed` | `HvacFanSpeedListener.onHvacFanSpeedUpdated(speed: Int?)` | 목표값 일치. area는 capability map 키(에뮬레이터는 `ROW_1_LEFT+ROW_1_RIGHT` 묶음 1개)를 그대로 쓴다. 현재값이 null(범위 밖 꺼짐 0 포함)이면 이 판에서 제외. 판정 중 null은 불일치 | 비EV는 목표를 범위 상한의 절반 이하로(12V 배터리·소음). 원복: 스냅샷 값 일치 |
| C3 내외기 | 지시형 "외부 공기 차단 — 내기 순환" / 암호형 "격벽 상태 = ■"(암호표: ■=닫힘). 연쇄형 없음 | `checkHvacRecirculationCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, …)`, `registerHvacRecirculationState(area: SeatArea, listener: HvacRecirculationStateListener)`, 스냅샷 `getHvacRecirculationState` | `HvacRecirculationStateListener.onHvacAutoRecirculationStateUpdated(state: Boolean?)` (이름에 Auto가 들어 있지만 recirculation 등록에 쓰는 리스너 타입) | 현재값의 반대로 전환(true = 내기 순환). area는 capability 키(에뮬레이터는 5좌석 묶음). 현재값 null이면 제외 | 원복: 스냅샷 값 일치 |
| L1 앰비언트 색 | 암호형 "비상 조명을 산소 계통 색(◇)으로"(암호표: ◇=보라) / 지시형 "비상 조명을 보라색으로". 연쇄형 없음 | `checkAmbientLightBrightnessColorCapabilities(onComplete: (CapabilityConfig, List<SeatArea>) -> Unit, …)`, `registerAmbientLightBrightnessColorListener(area: SeatArea, listener: AmbientLightBrightnessColorListener)`(앱 수명 동안 1회, 밝기 다음에 등록, 해제 안 함), 스냅샷 `getAmbientLightBrightnessColor` | `AmbientLightBrightnessColorListener.onBrightnessColorUpdated(argbColor: Int?)` | RGB→HSV 변환(알파 무시), 목표 색상(빨강 0°·주황 30°·노랑 60°·초록 120°·파랑 240°·보라 280°) 중 하나와 색상차 ≤ 30°, 채도 ≥ 0.3. area는 capability 키(에뮬레이터는 5좌석 묶음). 앰비언트 스위치 상태는 보지 않는다. 멈춘 동안 들어온 값은 쓰지 않고 재개 시 `get*`로 다시 읽는다 | 실차 앰비언트는 프리셋 색이라 색상차 판정. 차종에 따라 설정 메뉴에서 바꾸므로 단서 카드에 "조명을 켠 채로, 조명 설정으로 다녀오면 P 확인을 한 번 눌러요"를 붙이고, 돌아와 P 확인 뒤 `get*` 재동기화로 판정한다(F2). 원복: 스냅샷 색과 RGB 일치(알파 무시. 에뮬레이터 기본 Cyan `#00e5ff`) |
| L2 앰비언트 밝기 | 지시형 "조명 출력을 최대로" / 암호형 / 연쇄형 "조명 단계 = 환기 팬 단수 − 1" | `checkAmbientLightBrightnessCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, …)`, `registerAmbientLightBrightnessListener(area: SeatArea, listener: AmbientLightBrightnessListener)`(앱 수명 동안 1회, **색보다 먼저** 등록, 해제 안 함), 스냅샷 `getAmbientLightBrightness` | `AmbientLightBrightnessListener.onBrightnessUpdated(brightness: Int?)` | 목표 = `AreaBounds` 안의 값(지시형은 max/min 중 현재값의 반대쪽. 에뮬레이터 0~100). 현재값 null이면 제외. 멈춘 동안 값은 쓰지 않는다 | 설정 메뉴 조작이면 L1과 같은 안내. 원복: 스냅샷 값 일치 |
| L3 독서등(자리 단서) | 지시형 "2열 오른쪽 승무원: 독서등으로 신호" / 암호형 "△ 자리 승무원: 독서등"(암호표: △=2열 오른쪽. 힌트는 좌석을 밝히지 않는다). 연쇄형 없음 | `checkReadingLightStateCapabilities(onComplete: (CapabilityConfig, Map<SeatArea, List<LightState>>) -> Unit, …)`, `registerReadingLightState(area: SeatArea, listener: LightStateListener)`(앱 수명 동안 좌석별 1회, 해제 안 함), 스냅샷 `getCurrentReadingLightState` | `LightStateListener.onLightStateUpdated(state: LightState?)` | `LightState.ON`. area는 F3에서 확정된 참가 좌석(운전석 제외) 중 capability map 키에 해당 `SeatValue`를 포함하고 **현재 OFF인** 좌석만(에뮬레이터는 `ROW_1_LEFT`·`ROW_1_RIGHT`·`ROW_2_LEFT`·`ROW_2_RIGHT` 단일 area 4개, 가운데 없음). 참가 좌석이 운전석뿐이면 L2 또는 C2로 대체. 멈춘 동안 값은 쓰지 않는다 | 원복: OFF |
| D1 핸들 다이얼 | 지시형 "조타 키를 왼쪽 2번 구간에 맞추고 중앙으로" / 암호형 "조타 키를 ○ 구간으로"(암호표: ○=오른쪽 1번). 연쇄형 없음 | `checkSteeringWheelAngleCapabilities(onComplete: (CapabilityConfig, AreaBounds<Float>) -> Unit, …)`, `registerSteeringWheelAngle(listener: SteeringWheelAngleListener)`, 스냅샷·복귀 확정 `getCurrentSteeringWheelAngle` | `SteeringWheelAngleListener.onSteeringWheelAngleUpdated(angle: Float?)` (왼쪽 음수) | 목표 구간 = 한쪽 범위(왼쪽 |`AreaBounds.minValue`|, 오른쪽 `maxValue`)의 5~15%를 3칸으로 나눈 것(1번 5~8.3%, 2번 8.3~11.7%, 3번 11.7~15%. 에뮬레이터 왼쪽 2번 칸 = SDK −39~−55). 단서가 방향과 칸 번호를 지정하고, 칸 번호가 연쇄형 입력값이 된다. 값은 범위로 자른 뒤 판정하고, 같은 값의 반복 콜백(3.1초 주기)은 무시한다. 목표 칸 안에 1초 이상 머문 **뒤 중앙(±5%)으로 돌아와야** 퍼즐이 끝나며, 중앙 복귀는 `getCurrentSteeringWheelAngle`로 다시 읽은 값으로 확정한다(화면 안내: "목표 칸에 1초 머문 뒤 핸들을 가운데로 돌리면 다음 단서로 넘어가요"). 재개하면 칸 유지 시간은 처음부터 | 정차 중 소폭 회전만, 판당 1개. 범위 폭 10° 미만이면 풀에서 제외. 판 끝 체크리스트에는 "핸들 중앙(±5%)" 확인 행만 남는다 |
| B1 브레이크 봉인 해제(항상 마지막) | "브레이크를 두 번 밟아 해치 봉인 해제" (시연 시드는 2회 고정, 일반은 2~4회) | `checkBrakePedalStatusCapabilities(onComplete: (CapabilityConfig) -> Unit, …)`, `registerBrakePedalStatus(listener: BrakePedalStatusListener)` | `BrakePedalStatusListener.onBrakePedalStatusUpdated(status: Boolean?)` | 150ms 이상 뗀 뒤의 `false→true` 전환 횟수 = 목표. 첫 값(null·true)과 낡은 0~1ms 뗌은 기준으로만 쓰고 세지 않는다(밟은 채 시작·재개하면 떼었다가 밟아야 1회). 누름 사이 간격 8초 이내, 전체 창 30초(창을 넘기면 횟수 초기화. 화면의 "남은 N초"는 둘 중 먼저 끝나는 쪽). `vc.sh`로 두 번 누르기는 약 6.3초(누름 간격 약 4.2초), 네 번은 약 14.7초라 창 안에 든다(실측). 재개하면 횟수는 0부터 | 원복 불필요. 브레이크 미지원이면 화면 버튼 "봉인 해제" |

제외 목록(퍼즐로 쓰지 않음): Window·Wiper 전체(CAR_WINDOWS 미사용), TurnSignal·외부 등화(Light 도메인의 Low/High Beam·Fog·Tail Lamp API는 호출하지 않음), Door·Trunk, Seat 위치·등받이(MVP), Safety·Telltale, HVAC 제상(`registerHvacDefrosterState` — 차종에 따라 A/C·외기·풍량을 함께 바꿀 수 있어 부록 A의 "A/C 가동을 요구하지 않는다"와 어긋날 수 있다. 탑재 3차종의 제상 연동 동작을 확인한 뒤 이후 단계에서 검토), 앰비언트 스위치(`getAmbientLightSwitch` — 에뮬레이터에서 항상 OFF), 실내등(`registerCabinLightState` — `unregisterCabinLightState`에 독서등과 같은 결함이 있고 퍼즐에 쓰지 않는다).

### 2-2. 상태 모델(개발자용)
- `GameSession(seed, lengthMinutes, puzzles: List<Puzzle>, currentIndex, elapsedMs, snapshot: Map<SignalKey, Any>, participants: Set<SeatArea>, status: Preparing|Playing|PausedByGate|PausedByApp|Escaped|TimedOut)`
- 전이: Preparing →(S1 P 확인)→ Playing →(마지막 퍼즐 판정)→ Escaped / (타이머 0)→ TimedOut → S3. 일시정지는 원인별로 둘이지만 **재개 조건은 둘 다 "게이트 열림(`0x0`) + S1 P 확인"**이다(리더 결정: 안전 쪽 단순안).
  - `PausedByGate`: 규제 콜백(`requiresDO=true` 또는 `activeRestrictions != 0`)으로 멈춤. S-P를 그리고, 돌아올 때는 게이트 열림 + S-P/홈 "이어하기" + S1 P 확인을 거쳐야 Playing이다. 게이트가 열렸다는 이유만으로 자동 재개하지 않는다.
  - `PausedByApp`: 게이트는 열린 채 **진짜 앱 전환**으로 `onStop`이 온 경우(차량 설정 앱으로 앰비언트 색을 바꾸러 가는 등). 타이머 정지·저장·리스너 해제를 같이 하고, `onStart`에서는 S-P를 거치지 않고 S1 P 확인을 바로 띄운다. 확인 뒤 Playing으로 돌아가 리스너 재등록 + `get*` 재동기화로 바뀐 값을 판정한다. 떠나 있는 동안 게이트가 닫혔는지는 판별하지 않는다(실측상 화면이 보이는 동안만 구독하는 Flow는 돌아왔을 때 현재값만 다시 주고, 프로세스 범위 리스너는 동결로 이벤트가 3~6초 늦게 몰려와 순서가 보장되지 않는다). 다만 돌아온 시점에 게이트가 닫혀 있으면 P 확인이 거부된다. 두 원인의 구분은 저장 필드와 S3 통계(일시정지 원인) 표시에만 쓴다.
  - **창 크기 변경(세로 1단 ↔ 가로 2단)으로 오는 `onStop`은 일시정지가 아니다**(`isChangingConfigurations`). 액티비티가 다시 만들어져도 타이머·리스너는 그대로이고 규제 Flow만 다시 구독되어 현재값을 다시 받는다(실측: 판 도중 창을 나눠도 일시정지 없이 이어지고, 그 상태에서 주행 전환 시 28ms에 S-P). Pleos에서는 HOME 키로 앱이 멈추지 않으므로(`onStop` 없음) 앱 전환은 차량 설정 앱 같은 다른 앱 실행으로만 생긴다.
  - 프로세스 재생성: Playing 중 프로세스가 죽었다 복원되면 `PausedByApp`으로 두고 S0 "저장된 판 … 이어하기" → S1 P 확인을 받는다. 저장 세션의 버전이 다르거나 JSON이 손상되면 버리고 지운 뒤 홈으로 간다. 끝난 판(Escaped·TimedOut)의 세션은 지우므로 S3에서 죽으면 S0로 간다.
  - 덮어쓰기: 저장된 판이 있을 때 S0에서 새 판을 시작하면 확인창을 띄운다. 확인 → S1 → P 확인 시점에 기존 판을 지운다. 취소하거나 S1에서 홈으로 돌아오면 저장 판이 남는다.
- 저장 범위: 세션(시드·판·현재 퍼즐 번호·경과 시간·스냅샷·참가자·상태·일시정지 원인)과 최고 기록만 저장한다. **판정 진행(B1 누름 횟수, D1 칸 유지 시간·복귀 여부, 건너뛰기 타이머, 힌트 펼침)은 저장하지 않는다.** 재개하면 현재 퍼즐은 처음부터이고, B1은 150ms 이상의 실제 뗌을 본 뒤부터 다시 센다. 설정("내 차는 전기차", 안내 다시 보지 않기)은 별도 DataStore 파일(손상 처리기 포함)이다.
- 저장 시점: 전이마다, 퍼즐 판정마다, `onPause`/`onStop`마다. 주행 전환에서는 `onPause`/`onStop`이 오지 않으므로(에뮬레이터 실측) 규제 콜백의 전이 저장이 1차 경로다. 프로세스가 죽으면 마지막 저장 이후의 경과 시간(실측 약 14초)은 사라진다(사용자에게 유리한 쪽). 복원: 앱 재실행·창 크기 변경(액티비티 재생성)·프로세스 재생성·시스템 차단 뒤 모두 같은 세션으로. 최고 기록은 더 짧을 때만 갱신한다.
- 리스너 수명: 원칙은 Playing(과 S3 결과 갱신) 동안만 등록하고 두 Paused에서 전부 해제하는 것이다(주행 중 신호 수신 없음, CPU·최소 수집 원칙). **예외: 독서등(L3)·앰비언트 색(L1)·밝기(L2)는 Vehicle 2.3.2 SDK 결함 2건 때문에 앱 수명 동안 한 번만 공유 등록하고(`shareIn(Lazily, replay = 1)`, 처음 쓰는 판에서 지연 시작) `unregister*`를 부르지 않는다.** (결함 1) `unregisterReadingLightState`가 `removeListener` 대신 `addListener`를 불러 해제가 되지 않고 구독할 때마다 리스너가 쌓인다(`LightImpl.kt`. `unregisterCabinLightState`도 같다). (결함 2) `PropertyCallback.isEmptyPleosPropertyId`가 속성 이름을 `startsWith`로 비교해 `INTERIOR_LIGHTS_AMBIENT_BRIGHTNESS`가 `…_BRIGHTNESS_COLOR` 리스너까지 자기 것으로 세므로, 색 리스너가 먼저 있으면 밝기 속성을 차량 서비스에 구독하지 않는다. 그래서 **밝기를 색보다 먼저** 등록하고, 둘 다 해제하지 않는다. 멈춘 동안 이 세 신호의 값은 판정에 쓰지 않으며(ViewModel이 구독하지 않음), 재개 때 replay와 `get*`으로 현재값을 다시 받는다. 세 신호는 바뀔 때만 오는 이벤트라 주행 중 CPU 부담은 무시할 수준이다. 나머지 신호(C1·C2·C3·D1·B1)는 Paused에서 해제한다(실측: 주행 전환 로그에 `removeListener` 4~5건). 규제 Flow 구독은 화면이 STARTED인 동안 유지한다(S0·S1 버튼 활성화와 S2·S3의 게이트 닫힘 처리에 쓴다. 돌아오면 2ms 안에 현재값을 다시 받는다, 실측). 타이머는 화면에서 `SystemClock.elapsedRealtime`을 넘긴다.
- 매니페스트 변경: 액티비티의 `distractionOptimized=true` 메타데이터를 기본 매니페스트에서 **삭제했다**(F1). 기능 플래그 `pleos.drivingNotice=true` 빌드에서만 추가 매니페스트 소스 세트(`src/drivingNotice/AndroidManifest.xml`)를 병합해 넣는다(플레이스홀더 방식이 아니다. 기본 빌드 0건·플래그 빌드 1건을 debug·release 모두 확인). `MainActivity`는 `launchMode=singleTop`이고 디버그 인텐트(`seed`, `fakeGate`)는 `onNewIntent`로 받으며 release는 `BuildConfig.DEBUG`로 막는다.
- 기존 골격과의 관계: 차량 배터리(`VehicleRepository`)·내비(`NaviRepository`) Repository와 NaviHelper 의존성은 F1에서 **삭제했다**(release dex에 NaviHelper 0건). PT F5에서 `VehicleRepository`·`FakeVehicleRepository`를 git 기록에서 되살려 `evStateOfCharge`를 확장한다. 디버그 전용 `SpikeActivity`·`DataCheckActivity`·스파이크 퍼미션은 release에 들어가지 않는다(실측 확인).
- 이후 스파이크 후보: 기어를 직접 읽는 AAOS `GEAR_SELECTION`(`CAR_POWERTRAIN`, normal)은 마켓 승인 여부가 미확인이라(platform-03 ✅ 검증 메모) MVP에 넣지 않고, 승인이 확인되면 P 확인 버튼을 자동 확인으로 바꾸는 데 쓴다.

## 3. SDK·퍼미션 대응표

모든 API 이름·퍼미션은 `pleos-claude-kit/API_INDEX.md`에서 `grep -n`으로 확인한 값이다(2026-10-24). 승인 상태는 키트 §2.2(Vehicle State 사용 가능)와 platform-01 ✅ 기준이며, "Vehicle State" 승인에 어느 CAR_* 퍼미션이 포함되는지는 공식 문서에 없어 10/28 설명회에서 확인한다. 에뮬레이터 수신 여부는 `_workspace/05_build/spike_w1.md`(2026-10-26)와 F2-data·F2-ui QA에서 확인했다. 해제 API는 SDK 소스 구현까지 확인한다(독서등·실내등의 `unregister*`가 재등록을 하는 결함을 F1에서 발견).

| 기능 | SDK | API(정확한 이름) | 퍼미션 | 승인 상태 | 대체 경로 |
|---|---|---|---|---|---|
| F1 게이트 | AAOS Car API | `CarUxRestrictionsManager.registerListener(OnUxRestrictionsChangedListener)`, `CarUxRestrictions.isRequiresDistractionOptimization()`, `getActiveRestrictions()` | 없음(`useLibrary("android.car")`) | 플랫폼 표준, 에뮬레이터 실측 완료 | 값을 못 받으면 `FullyRestricted` |
| F2 C1 | Vehicle 2.3.2 HVAC | `checkHvacTemperatureCapabilities`, `registerHvacTemperature(area: SeatArea, listener: HvacTemperatureListener)`, `unregisterHvacTemperature`, `getHvacTemperature(area, unit: TemperatureUnit, …)` | `pleos.car.permission.CAR_CLIMATE` [normal] | 사용 가능(Vehicle State). 에뮬레이터 수신 확인 | capability NONE·현재값 null이면 풀에서 제외 |
| F2 C2 | Vehicle HVAC | `checkHvacFanSpeedCapabilities`, `registerHvacFanSpeed(area: SeatArea, listener: HvacFanSpeedListener)`, `unregisterHvacFanSpeed`, `getHvacFanSpeed` | CAR_CLIMATE | 사용 가능. 에뮬레이터 수신 확인(꺼짐 0은 null) | 제외 |
| F2 C3 | Vehicle HVAC | `checkHvacRecirculationCapabilities`, `registerHvacRecirculationState(area: SeatArea, listener: HvacRecirculationStateListener)`, `unregisterHvacRecirculationState`, `getHvacRecirculationState` | CAR_CLIMATE | 사용 가능. 에뮬레이터 수신 확인 | 제외 |
| F2 L2 | Vehicle Light | `checkAmbientLightBrightnessCapabilities`, `registerAmbientLightBrightnessListener(area: SeatArea, listener: AmbientLightBrightnessListener)`(**앱 수명 동안 1회, 색보다 먼저 등록**), `getAmbientLightBrightness`. `unregisterAmbientLightBrightnessListener`는 호출하지 않는다(해제하면 색 리스너 때문에 재구독이 안 되는 SDK 결함) | CAR_LIGHTS | 사용 가능. 에뮬레이터 수신 확인(L1과 같은 판에서도 수신, F2-ui QA 2회차) | 제외 |
| F2 L1 | Vehicle Light | `checkAmbientLightBrightnessColorCapabilities`, `registerAmbientLightBrightnessColorListener(area: SeatArea, listener: AmbientLightBrightnessColorListener)`(앱 수명 동안 1회, 밝기 다음에 등록), `getAmbientLightBrightnessColor`. `unregisterAmbientLightBrightnessColorListener`는 호출하지 않는다. `getAmbientLightSwitch`는 **호출하지 않는다**(에뮬레이터에서 VC와 무관하게 항상 OFF, 리스너 없음) | `pleos.car.permission.CAR_LIGHTS` [normal] | 사용 가능. 에뮬레이터 수신 확인(알파 0x00) | 제외 |
| F2·F3 L3 | Vehicle Light | `checkReadingLightStateCapabilities`, `registerReadingLightState(area: SeatArea, listener: LightStateListener)`(앱 수명 동안 좌석별 1회), `getCurrentReadingLightState`. `unregisterReadingLightState`는 호출하지 않는다(SDK 소스가 `removeListener` 대신 `addListener`를 호출해 해제가 되지 않음) | CAR_LIGHTS | 사용 가능. 에뮬레이터 4좌석 수신 확인 | 제외 |
| F2 D1 | Vehicle SteeringWheel | `checkSteeringWheelAngleCapabilities`, `registerSteeringWheelAngle(listener: SteeringWheelAngleListener)`, `unregisterSteeringWheelAngle`, `getCurrentSteeringWheelAngle`(중앙 복귀 확정·재개 재동기화) | `pleos.car.permission.CAR_DRIVE` [normal] | 사용 가능. 에뮬레이터 수신 확인(부호 = −VC, 3.1초 주기 반복) | 제외 |
| F2 B1 | Vehicle Brake | `checkBrakePedalStatusCapabilities`, `registerBrakePedalStatus(listener: BrakePedalStatusListener)`, `unregisterBrakePedalStatus` | CAR_DRIVE | 사용 가능. 에뮬레이터 수신 확인(1% 이상 true) | 마지막 퍼즐을 화면 버튼 "봉인 해제"로 대체 |
| F3 | Vehicle Seat | `checkSeatOccupancyCapabilities`, `getCurrentSeatOccupancy(area: SeatArea, …)`, `registerSeatOccupancy(area: SeatArea, listener: SeatOccupancyListener)`, `unregisterSeatOccupancy` | `pleos.car.permission.CAR_SEATS` [normal] | 사용 가능. 에뮬레이터 5좌석 수신 확인(값 극성 반대) | 좌석도 탭 선택이 기본. 신호는 초기값으로만 |
| F5(PT) | Vehicle EvBattery | `checkEvBatteryStateOfChargeCapabilities`, `registerEvBatteryStateOfCharge(listener: EvBatteryStateOfChargeListener)`, `unregisterEvBatteryStateOfCharge`, `getEvChargeSwitchState` | `pleos.car.permission.CAR_ENERGY` [normal] | 사용 가능(MVP에서는 선언·호출하지 않음. PT에서 git 기록으로 복원) | 수동 길이 선택 |
| F9(이후) | Gleo Interface | `assets/schema.json` function `START_ESCAPE_ROOM`, 응답 `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE` | 없음(스키마 승인) | 승인 필요, 에뮬레이터 호출 미확인 | `adb shell am start -a START_ESCAPE_ROOM -p <pkg>`로 모사 |

공통: 모든 리스너의 `onFailed(e: Exception)`을 구현하고 `e.message`로 ErrorCode 이름을 읽는다(`VehicleException` 클래스는 배포 AAR에서 난독화돼 쓸 수 없음, 프로젝트 `CLAUDE.md`). 해제는 등록 때와 같은 area·listener 객체로 한다(조명 3종은 위 결함 때문에 해제하지 않는다). `Vehicle.release()`는 부르지 않는다(앱 프로세스 수명). 콜백은 바인더 스레드로 오므로 메인 스레드로 옮긴다. `register*`는 퍼미션이 없으면 동기 예외를 던지므로 등록 예외를 실패나 null로 바꾼다. 앱은 어떤 `set*` API도 호출하지 않는다(`setAmbientLightBrightness`·`setAmbientLightBrightnessColor`가 `CAR_LIGHTS`로 표기돼 있어도 호출하지 않으며, README와 마켓 심사 노트에 같은 문장을 적는다). README "알려진 SDK 제약"에 독서등·앰비언트 공유 등록과 그 이유를 적는다.

**매니페스트에 선언한 퍼미션 전체 목록(MVP):** `pleos.car.permission.CAR_CLIMATE`, `pleos.car.permission.CAR_LIGHTS`, `pleos.car.permission.CAR_DRIVE`, `pleos.car.permission.CAR_SEATS` — 4개 도메인. 골격에 있던 `CAR_ENERGY`·`NAVI_ROUTE`와 액티비티의 `distractionOptimized=true` 메타데이터는 F1에서 **삭제했고**, release APK에서 pleos 퍼미션 4개(그 밖에는 SDK의 `VEHICLE_BINDING`, androidx의 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`)·DO 0건·v3 서명을 확인했다. `check_permissions.py`가 선언 4개 = 호출 4개 = README 표임을 확인한다. CAR_ENERGY는 PT F5에서만 다시 넣는다.

| 도메인 | 필요한 이유(한 줄) |
|---|---|
| CAR_CLIMATE | 공조 설정값(목표 온도·풍량·내외기)이 퍼즐 3종의 입력이다. 효과가 실내에 머문다 |
| CAR_LIGHTS | 앰비언트 색·밝기, 독서등이 퍼즐 3종의 입력이다. 실내 조명 API만 호출하고 외부 등화 API는 호출하지 않는다 |
| CAR_DRIVE | 정차 중 핸들 소폭 회전과 브레이크 페달이 "차가 컨트롤러"를 보여 주는 핵심 입력이다 |
| CAR_SEATS | 좌석 점유로 참가자 좌석도의 초기값을 채워 동승자 협동 퍼즐을 만든다 |

## 4. 화면·상태 설계

규제 비트는 하드코딩하지 않고 플랫폼이 내려 주는 `activeRestrictions`만 본다. 정차 = `requiresDO == false && activeRestrictions == 0x0`. 앱은 DO를 선언하지 않으므로 D단 정지·주행에서는 시스템(홈 화면 TaskViewContainer)이 앱 영역 전체를 흐린 배경 + "주행 중에는 사용할 수 없습니다." 차단 화면으로 덮고 터치를 앱에 전달하지 않는다(에뮬레이터 실측 `do_test/README.md`, `spike_w1.md`, F1 QA). 앱은 종료·백그라운드 없이 RESUMED로 남고 `onPause`/`onStop`이 오지 않으므로, 아래 표의 D단 정지·주행 열은 "시스템 차단 화면 아래에서 앱이 무엇을 그리고 있는가"다. **차단 화면의 흐린 배경은 전환 순간 화면의 정지 이미지다**(실측: 앱이 차단보다 17~34ms 먼저 S-P를 그려도 흐린 배경은 직전 퍼즐 화면이고, 차단 중 앱이 무엇을 그려도 바뀌지 않는다. 글자는 판독할 수 없고 큰 덩어리의 색·위치만 보인다). 앱이 바꿀 수 없는 시스템 동작이다. 그래도 게이트가 닫히는 순간 S2를 S-P로 바꾸는 것은 유지한다. 정차로 돌아왔을 때 보이는 화면이 S-P여야 하고, 저장·타이머 정지·리스너 해제가 같은 순간에 일어나야 하기 때문이다. 정차로 돌아오면 차단 화면만 사라지고 같은 액티비티의 현재 화면이 바로 보인다. 실차 동작은 10/28 설명회 확인 항목이다.

| 화면 | 정차(P, `0x0`) | 공회전(D, 0km/h · D단 정지, `0x50`) | 주행(D, 속도>0: 에뮬레이터 `0x50` / 최대 제한 `0x1ff`) | 1단(portrait) | 2단(landscape) |
|---|---|---|---|---|---|
| S0 홈 | 게이트 문구("정차 중 · 지금 시작할 수 있어요"), 판 길이 3·5·10분 세그먼트(이 차에서 만들 수 있는 길이만 활성, 못 만드는 길이는 비활성 + 설명. 각 칸에 최고 기록 또는 "기록 없음"), "N분 판 시작"(저장된 판이 있으면 "저장된 판을 지우고 새 판을 시작할까요?" 확인창 → 확인 시 S1로, P 확인 때 삭제), "저장된 판 … 이어하기"(있을 때), 설정 버튼 → 앱 안 설정 패널(전기차 토글·공회전 안내 다시 보지 않기. 닫기 버튼 100dp). 조작 위치 그림(핸들·브레이크·공조·조명). 3분 판도 못 만들면 "이 차에서는 지원하는 조작이 부족해요" | 시스템 차단 화면이 덮음. 아래의 S0는 "시작"·"이어하기" 비활성 상태로 유지되고 설정 패널은 닫힌다(열려 있었으면 닫고, 닫힌 동안 열리지 않는다) | 같음. `0x1ff`도 같음 | 세로 스택: 제목 → 길이 선택(큰 버튼 3개, 각 버튼에 최고 기록) → 조작 위치 그림 → 시작 | 좌: 제목·설정·조작 위치 그림, 우: 길이 선택·시작 |
| S1 준비 | "기어 P·주차 브레이크를 확인했어요" 버튼(시작·이어하기·앱 전환 복귀 모두 필수. 이어하기일 때 "이어하기 · 퍼즐 i/K", 참가 자리 변경 불가), 참가자 좌석도(차 그림 위 원형 버튼 + 오른쪽 목록 행 "운전석·조수석·2열 왼쪽·2열 오른쪽". 점유 신호로 미리 선택, 원이나 행을 탭해 고침. 운전석 비활성. 2열 가운데 없음), 비EV면 공회전 안내 카드 1회 + "다시 보지 않기"(체크 상자 + 글자 한 줄 80dp). 시연 모드에서 현재값이 0단계와 다르면 경고 | 시스템 차단 화면이 덮음. 아래의 S1은 확인 버튼 비활성 | 같음 | 세로: 안내 카드 → 좌석도(원형 버튼 72dp 이상, 간격 8dp 이상, 겹침 없음) → 확인 버튼 | 좌: 안내·좌석도, 우: 좌석 목록 행(96dp)·확인 버튼 |
| S2 진행 | 단서 카드(한 줄, 큰 글자, 어절 단위 줄바꿈. 암호형은 **판 전체 암호표** 카드 동반. 설정 메뉴 조작 단서에는 "조명을 켠 채로 … P 확인" 안내. 핸들 단서에는 "목표 칸에 1초 머문 뒤 핸들을 가운데로 돌리면 다음 단서로 넘어가요"), 현재 신호 게이지(지시형만 목표 구간 표시. 온도 눈금은 capability 범위 그대로(에뮬레이터 15~30℃). 핸들 게이지는 칸·중앙 표시. 색 게이지는 현재 색 + 후보 6색), 진행 n/K, 타이머, 힌트 버튼(2장, 값·좌석을 밝히지 않음), 봉인 카드는 "풀린 퍼즐" 줄 + 누름 i/N + "남은 N초" 막대 | 규제 콜백 즉시 S-P로 전환 + 타이머 정지·저장·리스너 해제. 차단 화면의 흐린 배경은 전환 직전 S2의 정지 이미지(시스템 동작, 판독 불가) | 같음(`0x1ff`도 같음) | 세로: 단서(+암호표) → 게이지 → 하단 바(진행·타이머·힌트). 세로 1단에서는 uiautomator가 앱 노드를 못 찾으므로 자동 조작은 좌표로 한다 | 좌: 단서·암호표·진행·타이머, 우: 게이지·힌트 |
| S-P 일시정지 | 차단 화면이 걷힌 뒤 그대로 보이는 화면. "이어서 할까요? N분 판 퍼즐 i/K · 남은 시간 M:SS", "이어하기" 버튼(→ S1 P 확인), "홈" 버튼(112dp), "저절로 다시 시작하지 않아요", "퍼즐과 암호표는 이 화면에 남기지 않아요". 퍼즐 내용·암호표·게이지 없음 | 시스템 차단 화면 아래에 있는 화면. 차단 화면은 터치를 앱에 전달하지 않으며(실측), 앱도 게이트가 닫힌 동안 "이어하기"·"홈"을 비활성으로 둔다(이중 안전장치. DO 플래그 빌드에서는 필수). 타이머는 멈춘 값 그대로 | 같음 | 가운데 문구 + 버튼 2개 | 가운데 문구 + 버튼 2개 |
| S3 결과 | 탈출 성공/시간 초과, 소요 시간, 최고 기록("새 최고 기록 · 이전 M:SS"), 복구 체크리스트(원래 값·현재 값·자동 체크 i/n. 허용 오차 C1 ±0.25℃, L1 RGB 일치. 핸들이 중앙이 아니면 "가운데로 돌려 주세요"), "새 판"·"홈" | 시스템 차단 화면이 덮음. 아래의 S3는 결과 리스너를 해제하고 체크리스트를 "정차하면 다시 확인해요"로 바꾸며 **"새 판"을 비활성**으로 둔다. 정차로 돌아오면 `get*`으로 다시 읽어 체크 갱신 | 같음 | 세로: 결과 → 체크리스트 → 버튼 | 좌: 결과·버튼, 우: 체크리스트 |
| (플래그 `pleos.drivingNotice` 켠 빌드만) S-H 닫힘 | 해당 없음 | DO 선언 빌드에서는 차단 화면 대신 앱이 아이콘 1개 + "정차하면 이어하기" 한 줄만 그린다. 터치 요소 0(clickable·focusable·checkable·scrollable 모두 0, F1 QA 2회차 실측). 설정 패널은 닫히고 열리지 않는다. 첫 규제 콜백 전(UNKNOWN)에도 S-H다. 정차하면 원래 화면(S0 또는 S-P)로 돌아온다 | 같음. `0x1ff`에서도 한 줄 | 가운데 한 줄 | 가운데 한 줄 |

공통 규칙: 창 크기(`BoxWithConstraints`)로 1단/2단을 고른다. 실측 창 크기(Devbox 2560×1440, density 160)는 세로 1단 **841×1210dp**(Compose 콘텐츠는 패딩 24dp를 빼면 793×1162dp), 가로 2단 **1682×1210dp**다. 분기 기준은 폭 1200dp 이상이면 2단(에뮬레이터 두 크기에서 확인. 다른 디스플레이(Connect-W 2:1 등)는 폼팩터 테스트로 보정). **창 크기가 바뀌면 액티비티가 다시 만들어지지만**(`onPause → onStop → onDestroy → onCreate`, 실측) 이것은 일시정지가 아니며(`isChangingConfigurations`), 세션 상태를 ViewModel + SavedState + DataStore에 두어 가로·세로 전환, 창 크기 변경, 프로세스 재생성, 시스템 차단 뒤 복귀에서 상태를 유지한다. 주요 버튼의 터치 타깃은 디자인 토큰 88~112dp(시작·P 확인·S-P 버튼 112dp, 판 길이 세그먼트 96~108dp)이고 텍스트 입력은 없다(키보드 비트 무관). 자동 스크롤·움직이는 이미지 없음. 게임 중 CPU 부하를 줄이기 위해 폴링 없이 리스너 이벤트로만 갱신하고(D1의 3.1초 주기 반복 콜백은 값이 바뀔 때만 처리), Playing이 아닐 때는 조명 3종(공유 등록, 2-2)을 뺀 리스너를 모두 해제한다. 설정은 별도 화면이 아니라 S0 안의 패널이다. 서체는 Asta Sans(OFL)이며 에뮬레이터 시스템 서체와 같아 눈으로는 차이가 거의 없다.

## 5. 시연 시나리오

Virtual Console 조작은 `.claude/skills/pleos-feature-dev/scripts/vc.sh`를 쓴다(heartbeat 처리 포함, 호출당 1.6~1.7초 걸린다. area별 필드가 있는 기능은 area를 꼭 준다). **VC는 세션을 새로 열 때마다 앰비언트 색을 `#ffffff`로, 풍량 0을 2로 되돌린다.** `vc.sh` 호출 하나하나가 세션을 열므로, 값을 유지해야 하는 시연·리허설은 `vc.sh hold`(세션을 백그라운드로 계속 유지)로 시작하고 끝에 `vc.sh release`를 부른다(리허설 실측: hold 중에는 여러 명령 뒤에도 `#00e5ff`가 유지됐다). feature id와 area 이름은 실측으로 확정했다: `steering-angle`, `ambient-light-color`, `ambient-light-brightness`, `reading-lights-switch <front_left|front_right|rear_left|rear_right>`, `hvac-temperature-set <값> driver`, `hvac-fan-speed`, `hvac-recirc`, `brake-pedal-status`, `seat-occupancy <driver|passenger|rear_left|rear_right>`. 2열 오른쪽은 `rear_right`(SDK `ROW_2_RIGHT`)다. `vc.sh park`/`idle`/`drive`는 `steering=0&brake=0`을 함께 보내므로 핸들·브레이크 값이 0으로 돌아간다(기어 전환은 핸들 퍼즐 뒤에 한다). 앱 사용자는 10이라 앱 데이터는 `adb shell pm clear --user 10 <pkg>`로 지운다(`pm clear <pkg>`로는 저장된 판·기록이 남는다). 화면 버튼은 `ui.sh tap "<글자>"`로 누르되, 세로 1단에서는 uiautomator가 앱 노드를 못 찾으므로 좌표(`adb shell input tap`)로 누른다. 콜드 부팅 직후 1~2분은 SystemUI가 불안정하므로 `emu.sh settle` 뒤에 시작한다. 모든 신호는 Virtual Console 카드로 재현되고 실제 SDK 콜백이 오는 것을 확인했으며, 아래 대본 전 단계를 리허설했다(`F2ui_rehearsal.md`, F2-ui QA). 예비로 `-Ppleos.useFakeSdk=true` 빌드가 같은 판을 6초 간격 자동 풀이로 돈다(Fake 기본값은 0단계와 같다: 온도 24, 풍량 2, 앰비언트 Cyan·100, 참가자 운전석 + 2열 오른쪽).

**시연 시드.** 디버그 빌드에서 `adb shell am start -n <pkg>/.MainActivity --es seed demo5`로 고정한다(release는 시드를 무시하고 랜덤 판을 만든다). 판은 시드·참가 좌석·지원 신호 풀·시작 때 현재값·EV 설정으로 정해지므로, 시연 재현 조건은 다음 다섯이다: 시드 `demo5`, 참가 좌석 = S1 좌석도 확정값(2단계에서 켜는 2열 오른쪽이 초기 선택에 들어온다), 지원 신호 풀 = 시연 모드에서 8종 전부로 고정(에뮬레이터 capability 결과와 무관), 시작 때 현재값 = 0단계 초기값(온도 24, 풍량 2, 외기, 앰비언트 Cyan·100, 독서등 OFF, 핸들 0, 브레이크 0), EV 설정 = 꺼짐(비EV). 디버그 빌드는 시드 시작 때 현재값이 0단계와 다르면 "시연 0단계와 다른 값이 있어요"로 항목별 지금 값·대본 값을 보여 주고 취소/그대로 시작을 고르게 한다. 이 조건에서 `demo5`가 만드는 판(5분, K=5, 참가자 운전석 + 2열 오른쪽): ① D1 지시형 "조타 키를 왼쪽 2번 구간에 맞추고 중앙으로" → ② L1 암호형 "비상 조명을 산소 계통 색(◇)으로"(암호표 ◇ 보라·○ 파랑·△ 초록) → ③ L3 지시형 "2열 오른쪽 승무원: 독서등으로 신호" → ④ C1 연쇄형 "냉각 코일 온도 = 20 + 조타 구간 번호"(= 22℃, 쾌적 범위 안) → ⑤ B1 "브레이크를 두 번 밟아 해치 봉인 해제". 2열 오른쪽이 참가 좌석이 아니면 ③은 C2 연쇄형 "환기 팬 단수 = 조타 구간 번호 × 2"(= 4단, 비EV 상한 안)로 바뀐다(장면 1과 같은 판). ④가 연쇄형으로 성립하려면 시작 시 운전석 목표 온도가 22℃와 2℃ 이상 달라야 하므로 0단계의 24℃ 설정이 꼭 필요하다(에뮬레이터 기본값 22.0℃). F2-engine QA가 골든 보드로, F2-ui 리허설이 화면으로 `demo5`의 ①~⑤가 이 기술과 같음을 확인했다.

### 5-1. Virtual Console 조작 순서

| 단계 | 조작(`scripts/vc.sh` 명령 또는 엔드포인트·값) | 기대 화면 | 재현 방법(VC/Fake) |
|---|---|---|---|
| 0 | `adb shell pm clear --user 10 <pkg>`(저장된 판·기록 제거) → **`vc.sh hold`** → `vc.sh park`; `vc.sh set hvac-temperature-set 24 driver`(기본 22.0℃라 반드시); `vc.sh set hvac-fan-speed 2`(0은 그 판에서 C2가 빠지고 VC가 어차피 2로 되돌린다); `vc.sh set hvac-recirc false`; `vc.sh set ambient-light-color '#00e5ff'`(hold 없이는 다음 호출에서 흰색으로 돌아간다), 밝기 100(기본); `vc.sh set reading-lights-switch false <4좌석 각각>`; 좌석 점유 `vc.sh set seat-occupancy true driver`(**필수**. 극성 보정 기준이라 운전석이 VC "점유"여야 보정 후 운전석만 점유로 잡힌다), 나머지 3좌석 false; 조향각·브레이크는 `park`가 0으로 맞춘다. `/vehicle/state`로 값 확인 | 앱 미실행. 이 값이 "시작 때 현재값" 입력이자 판 시작 스냅샷(복구 목표: 온도 24, 풍량 2, 앰비언트 Cyan, 독서등 OFF)이 된다 | VC |
| 1 | `adb shell am start -n <pkg>/.MainActivity --es seed demo5` | S0 홈 "정차 중 · 지금 시작할 수 있어요", 3·5·10분 모두 활성, 각 칸 "기록 없음", "5분 판 시작" 활성 | — |
| 2 | `vc.sh set seat-occupancy true rear_right` | (3단계 S1에서) 좌석도 초기 선택에 2열 오른쪽이 들어와 참가자 2명. 원이나 목록 행을 탭해 고칠 수 있음을 보여 준다 | VC |
| 3 | 화면: `ui.sh tap "5분 판 시작"` → 좌석도 확인 → `ui.sh tap "기어 P·주차 브레이크를 확인했어요"` | 0단계 경고 없음(값 일치) → S1 공회전 안내 카드(비EV 기본) → S2 ① 단서. 로그 `start seed=demo5 puzzles=[D1:DIRECT, L1:CIPHER, L3:DIRECT, C1:CHAIN, B1:SEAL]` | — |
| 4 | ① D1: `vc.sh set steering-angle 45` → `vc.sh set steering-angle 0`(호출이 1.6초 이상 걸리므로 1초 유지를 따로 기다릴 필요가 없다). SDK 값 −45.0 = 왼쪽 2번 칸(범위 8.3~11.7%, SDK −39~−55. **SDK 부호가 VC와 반대**) | 게이지 "왼쪽 2번" 목표 구간, "칸 안에 있어요 · 0.7초…" → 중앙으로 돌아오고 `getCurrentSteeringWheelAngle` 재조회로 확정되면 체크, ② 단서 | VC |
| 5 | ② L1: 암호표(◇ 보라·○ 파랑·△ 초록)에서 ◇=보라 확인 → `vc.sh set ambient-light-color '#aa00ff'`(VC 프리셋 Purple, h 280°) | 색 게이지는 현재 색(청록)과 후보 6색만 보이고 목표 표시 없음("목표는 단서를 풀어서 찾아요"), 조명 설정 P 확인 안내. 보라로 바꾸면 체크(스위치 상태 무관), ③ 단서 | VC |
| 6 | ③ L3: `vc.sh set reading-lights-switch true rear_right` | 2열 오른쪽 줄에 목표 구간 테두리. 켜면 체크(자리 단서가 확정 좌석 중 OFF였던 좌석에만 나온 것을 자막으로), ④ 단서 | VC |
| 7 | ④ C1: "20 + 2 = 22" → `vc.sh set hvac-temperature-set 22 driver` | 게이지는 현재 24.0℃만 보이고 목표 없음(눈금 15~30℃). 힌트 2장도 값을 밝히지 않는다. 22로 바꾸면 1초 안에 체크, ⑤ 단서 | VC |
| 8 | ⑤ B1: `vc.sh set brake-pedal-status 0`(뗌 기준 확보) → `100` → `0` → `100`(약 6~8초. 누름 간격 약 4.2초로 8초 이내, 전체 30초 창 안) | 첫 `100`에서 봉인 1/2와 30초 창 막대·"남은 7~8초", 둘째 `100`에서 탈출 성공(리허설 1:05~1:31), "새 최고 기록" | VC |
| 9 | S3 복구 체크리스트: `vc.sh set hvac-temperature-set 24 driver` → `vc.sh set reading-lights-switch false rear_right` → `vc.sh set ambient-light-color '#00e5ff'` | 핸들은 처음부터 완료(1/4) → 온도 24.0 완료(24.5는 ±0.25 밖이라 미완료) → 독서등 OFF 완료 → 앰비언트 Cyan RGB 일치 완료 → 4/4 | VC |
| 9-1 | (선택) S3에서 `vc.sh drive 40` → `vc.sh set hvac-temperature-set 23 driver` → `vc.sh park` → `… 24 driver` | 주행 중 체크리스트는 "정차하면 다시 확인해요"로 갱신이 멈추고 "새 판"이 비활성. 정차하면 다시 읽어 온도 줄이 "돌려 주세요"(3/4), 24로 바꾸면 4/4 | VC |
| 10 | `ui.sh tap "새 판"` → P 확인(시드 없이) | 같은 테마지만 다른 조합·순서·목표값·암호표(반복 장치. 리허설 예: `[C2:DIRECT, L3:DIRECT, C1:CHAIN, L1:CIPHER, B1:SEAL]`, 시드는 매번 다름). 앞 판의 힌트는 남지 않는다 | — |
| 11 | 판 도중 `vc.sh drive 40` | 시스템이 앱 영역을 흐린 배경 + "주행 중에는 사용할 수 없습니다." 차단 화면으로 덮는다. 흐린 배경은 전환 직전 퍼즐 화면의 정지 이미지(판독 불가, 시스템 동작)다. logcat `F1GATE`: 규제 콜백 → 리스너 해제 +0~1ms(`removeListener` 4~5건, 조명 3종 제외) → 저장 +8~11ms → S-P 그리기 +26~42ms(100ms 이내) | VC |
| 12 | `vc.sh idle` (D, 0km/h) | 같은 차단 화면(driving state 1) | VC |
| 13 | `vc.sh park` | 차단 화면이 사라지고 S-P "이어서 할까요? 5분 판 퍼즐 1/5 · 남은 시간 M:SS"가 그대로 보인다. 자동으로 S2가 뜨지 않는다 | VC |
| 13-1 | 화면: `ui.sh tap "이어하기"` → `ui.sh tap "기어 P·주차 브레이크를 확인했어요"` | 같은 퍼즐 번호·남은 시간에서 S2 재개(로그 `resume index=… elapsedMs=… brake=…`), 리스너 재등록 후 `get*`로 게이지 갱신. 그 퍼즐의 판정 진행(브레이크 횟수·핸들 유지)은 처음부터 | — |
| 13-2 | (선택) 봉인 1/2에서 `vc.sh drive 40` → `vc.sh park` → `vc.sh set brake-pedal-status 100`(밟은 채) → 이어하기 → P 확인 | 재개 직후 **0/2**(`brake=true`는 기준으로만). `0 → 100` 두 번에 탈출 | VC |
| 14 | 창 크기 변경(`spike_w1.sh window split` 또는 윈도 바 드래그 → `window join`으로 복구) | 세로 1단 841×1210dp ↔ 가로 2단 1682×1210dp로 레이아웃이 바뀌고(액티비티 재생성) 일시정지 없이 같은 퍼즐을 이어 간다. 세로에서 판 끝까지 진행 가능 | — |
| 15 | (선택) 차량 설정 앱(`ai.umos.vehiclecontrol`) 실행 → 앰비언트 변경 → 앱으로 복귀 | `onStop`에서 PAUSED_BY_APP·저장. 돌아오면 S-P 없이 S1 P 확인이 바로 뜨고, 확인 뒤 `get*` 재동기화로 바뀐 값이 판정된다 | — |
| 16 | (선택) 저장된 판이 있는 채 `ui.sh tap "5분 판 시작"` | "저장된 판을 지우고 새 판을 시작할까요?" 확인창. 취소하면 홈과 저장 판 유지. 새 판 시작 → S1 | — |
| 끝 | VC를 park, 22.0/21.5℃, 풍량 2, 외기, `#00e5ff`·100, 독서등·좌석 점유 off로 되돌리고 **`vc.sh release`**, `pm clear --user 10 <pkg>` | `driving state 0 (parked)`, `0x0` | VC |
| Fake 자동 풀이 | `./gradlew installDebug -Ppleos.useFakeSdk=true` → `--es seed demo5` → 5분 판 시작 → P 확인 | 6초마다 `auto solve #0 D1 … #4 B1`, 33초에 탈출. 결과 화면 6초 뒤 원래 값으로 되돌아가 4/4 | Fake(에뮬레이터 장애 시 영상 예비) |
| Fake 주행 전환 | Fake 빌드 판 도중 `adb shell am start -n <pkg>/.MainActivity --es fakeGate drive` → `--es fakeGate park` → 이어하기 → P 확인 | S-P(버튼 비활성) → 차단 걷힘 → 같은 퍼즐에서 재개(자동 순환은 넣지 않는다, 리더 결정) | Fake |

### 5-2. 시연 영상 콘티 (2~3분)

| 시간 | 장면 | 자막·내레이션 |
|---|---|---|
| 0:00~0:15 | 학원 앞 주차장 사진 → 에뮬레이터 홈 | "기다리는 5분, 차가 게임 패드가 됩니다. 앱은 차를 제어하지 않고 읽기만 합니다." |
| 0:15~0:30 | 기어 P, 5분 판 선택, 좌석도 확인(2열 오른쪽이 미리 잡힘), P 확인, 공회전 안내 | "정차에서만 열립니다. 내연·하이브리드 차에는 엔진 공회전 안내를 먼저 보여 줍니다." |
| 0:30~0:50 | ① 핸들: 구간 진입("칸 안에 있어요") → 중앙 복귀 | "핸들을 살짝 돌렸다 되돌리면 첫 잠금이 풀립니다." |
| 0:50~1:15 | ② 암호표(◇·○·△) → 앰비언트 보라 | "단서는 답을 바로 주지 않습니다. 암호표를 풀어야 색이 나옵니다." |
| 1:15~1:35 | ③ 자리 단서 독서등 | "좌석 점유 신호가 동승자 자리를 미리 잡아 주고, 그 자리에만 자리 단서가 나옵니다." |
| 1:35~1:55 | ④ 연쇄 "20 + 구간 번호" → 22℃ → ⑤ 브레이크 두 번(1/2 "남은 7초" → 탈출) → 탈출 연출, 최고 기록 | "앞 퍼즐의 답이 다음 열쇠입니다. 브레이크 두 번으로 해치 봉인 해제." |
| 1:55~2:15 | 복구 체크리스트 자동 체크(1/4 → 4/4) | "판이 끝나면 바꾼 설정을 원래대로. 신호로 확인합니다." |
| 2:15~2:35 | 기어 D → 차단 화면("주행 중에는 사용할 수 없습니다.") → P → 차단 화면 걷힘, S-P → 이어하기 → P 확인 | "출발하면 시스템이 앱을 덮고, 앱은 그 순간 저장합니다. 다시 정차하면 일시정지 화면에서 이어합니다." |
| 2:35~2:45 | 새 판: 다른 조합·암호표 | "같은 테마, 매번 다른 판." |
| 2:45~2:55 | 퍼미션 4개·조회 전용·DO 미선언 자막, 로고 | "조회 퍼미션 4개, 제어 없음, 정차 전용." |

영상은 실제 SDK debug 빌드 + `demo5`로 찍고, 에뮬레이터 장애 시에만 Fake 빌드(자동 풀이 + `fakeGate`)로 예비본을 찍는다. 화면 녹화에는 실명·계정·연락처가 보이지 않게 한다(블라인드 규정).

## 6. 일정

기준일 2026-10-27(화)부터 1차 접수 마감 2026-11-15(일) 23:59까지 역산. 2026-11-12(목)까지 개발하고 11/13(금)~11/15(일)은 버퍼다. 10/27 기준 F2-data·F2-engine·F1·F2-ui가 구현과 QA를 마쳤고(1주차 범위와 2주차 F2-ui가 예정보다 일찍 끝남) F3는 마무리 중이라, 남은 일정을 리허설·성능·제출 준비 중심으로 당겼다.

| 주차(기간) | 목표 | 산출물 |
|---|---|---|
| W0 (10/25~10/27, 일~화) **완료** | 기획 확정(비평 2회차), W1 스파이크(10/26), F2-data(QA PASS)·F2-engine(QA 수정 반영)·F1(QA 2회차 PASS: 게이트·S-P·플래그 S-H·DO 삭제·NaviHelper 삭제)·F2-ui(QA 2회차 PASS: 화면 S0~S3·S-P, 암호표, 연쇄형 항등식 제거, 앰비언트 공유 등록, 덮어쓰기 확인, 시연 대본 리허설) | 기획 v2~v4, 스파이크·QA 결과, 리허설 기록, release APK(DO 0건·퍼미션 4개) |
| W1 (10/28~11/02, 수~월) | 10/28(수) 설명회: Vehicle State 승인 범위·게임 앱 DO 허용·실차 차단 동작·SEAT_OCCUPANCY 해석·SDK 결함 2건·디자인 자원 확인 → 결과에 따라 퍼미션·플래그 조정. F3 마무리·QA. 연쇄형 빼기 우연 일치(1.2%) 수정. 성능 측정(주행 중 CPU < 5%, RAM ≤ 400MB, 미검증 항목) 및 기기 재부팅 뒤 최고 기록 유지 확인. 시연 대본 전체 리허설 2회(실제 SDK + Fake 예비). 남은 스파이크(낮은 우선순위): 파워 상태(ACC) 카드에서 SDK 동작, 규제 불명 `0x1ff` 재현 | 설명회 메모, F3 QA PASS, 성능 측정 기록, 리허설 기록 2차 |
| W2 (11/03~11/09, 화~월) | 플레이테스트로 퍼즐당 소요 시간·K 보정(사람 조작 기준), 세로 1단 좌석 원 배치 다듬기, 핸들 게이지 칸 폭(가로 약 33dp) 조정, 시연 영상 촬영(실제 SDK) + 예비본(Fake), 접수양식(DOCX) 초안, README 정리(알려진 SDK 제약·플래그·시연 절차), 버그 수정 | 영상 1차 편집본, 접수양식 초안, README |
| W3 (11/10~11/12, 화~목) | 통합 리허설, release 최종 빌드·`copyReleaseApk`·서명 검증·`check_permissions.py`, 영상 최종 편집, GitHub 정리 시작(블라인드 점검: 커밋 작성자·스크린샷·README). **11/12(목) 기능 동결** | `apk/팀명_앱이름_release.apk`, 영상 최종본 |
| 버퍼 (11/13~11/15, 금~일) | 기획서를 접수양식(DOCX)으로 옮기기 마무리, GitHub 저장소 공개 범위·심사용 계정 초대, 동의서, 제출 점검, 11/15 낮에 제출. 기능 변경 없음 | 접수양식, 영상 링크, 저장소 링크 |
| 11/16~11/23 | 1차 결과 대기. PT 기능 설계(F4·F5·F6) | PT 명세 초안 |
| 11/24~11/30 | F4 오늘의 판·협동 기록, F5 EV 판 길이(CAR_ENERGY 추가, `VehicleRepository` git 복원), F6 협동 확장·효과음 | PT용 APK |
| 12/01~12/02 | PT 리허설(에뮬레이터 라이브 시연 + 영상 예비), 발표 자료. 기능 동결 | 발표 자료, 시연 체크리스트 |
| 12/03~12/04 | 2차 오프라인 PT | — |
| 12/05~12/18 | 최종 선정 대기, 피드백 반영 | — |
| 2027-01~02 | F7 기능 익히기 모드·F8 테마 팩 중 택1, 전시용 최종 APK | 전시 APK |

## 7. 리스크와 대응

| 리스크 | 영향 | 확인 시점 | 대응 |
|---|---|---|---|
| 주행 전환 시 `onPause`/`onStop`이 오지 않아(에뮬레이터 실측: 시스템이 차단 화면으로 덮을 뿐 액티비티는 RESUMED) 생명주기 콜백만으로는 저장·일시정지가 안 됨. 규제 콜백을 놓치면 타이머가 계속 돌며 리스너가 주행 중에도 신호를 받음. 콜백 지연 자체는 문제가 아니고(홈 차단과 같은 순간, 구현 실측 S-P 26~42ms), 차단 화면의 흐린 배경에 직전 퍼즐 화면이 판독 불가 상태로 남는 것은 앱이 바꿀 수 없는 시스템 동작 | 진행 유실, 주행 중 CPU·신호 수신 | 에뮬레이터 실측·F1 QA 완료, 실차는 10/28 설명회 | 규제 Flow(`DrivingStateRepository.restrictions`)를 S0~S3가 보이는 동안 항상 구독해 `requiresDO=true` 또는 `activeRestrictions != 0` 첫 이벤트에서 S-P 전환·저장·리스너 해제·설정 패널 닫기를 한 번에 처리. `onPause`/`onStop`은 보조 경로. 규제 값 불명(`0x1ff`)도 닫힘으로 취급(에뮬레이터에서 재현 불가, 단위 테스트로만 확인). 실차가 에뮬레이터와 다르게(종료·백그라운드) 처리하면 보조 경로가 그대로 커버. 흐린 배경은 심사 설명 시 "시스템 스냅샷"이라고 밝힌다 |
| **Vehicle 2.3.2 SDK 결함 2건**(독서등·실내등 `unregister*`가 재등록을 함, 앰비언트 색 리스너가 있으면 밝기 속성을 구독하지 않음)으로 조명 3종을 Paused에서 해제할 수 없음. 실차 SDK 버전이 다르거나 패치되면 동작이 달라질 수 있음 | "Paused에서 리스너 전부 해제" 원칙의 예외, 심사 질문 가능성, SDK 패치 뒤 회귀 | F1·F2-ui QA 완료, SDK 패치 여부는 설명회·릴리스 노트 | 조명 3종은 앱 수명 동안 한 번만 공유 등록(밝기 → 색 순서), 해제하지 않고 멈춘 동안 값을 쓰지 않는다(2-2). 바뀔 때만 오는 이벤트라 주행 중 부담은 무시할 수준. README "알려진 SDK 제약"과 마켓 심사 노트에 적고, 부록 B로 설명회에 질문한다. SDK가 고쳐지면 등록 순서 제약만 남기고 해제를 되살린다 |
| 게임 앱의 DO 선언 허용 여부(주행 중 안내 한 줄 화면) | 플래그 결정 | 10/28 설명회 | 기본값은 DO 미선언. 플래그 `pleos.drivingNotice`는 추가 매니페스트 병합 + S-H(터치 요소 0)로 구현돼 있고 QA를 통과했다. 공식 허용 답변이 있을 때만 켠다. 켠 빌드로 `drive 40`에서 눌리는 요소가 0개임을 확인하는 절차를 남긴다 |
| 좌석 점유 값의 극성: 에뮬레이터는 VC "점유"가 SDK `false`로 오고 2열 가운데는 항상 `true`(AAOS VACANT=1을 `true`로 바꾸는 것으로 보임[추론]). 실차 해석 미확인. UNKNOWN(0)·운전석 null을 주는 차에서는 극성을 알 수 없음 | 참가자 수가 5명으로 잡히거나 동승자를 놓침 | 에뮬레이터 실측·F2-data QA 완료, 실차는 10/28 설명회(부록 B) | 좌석도 탭 선택(원형 버튼·목록 행 모두)을 기본 동작으로 두고 점유 신호는 초기값으로만 쓴다. 운전석 기준 극성 보정, 운전석 값을 모르면 운전석만 초기 선택. 2열 가운데는 선택지에서 제외 |
| 앰비언트 색·밝기, 독서등, 조향각의 실차 지원 범위·프리셋 팔레트 미확인(에뮬레이터는 8종 모두 수신 확인). 차종에 따라 지원 신호가 줄면 만들 수 있는 판 길이가 줄어듦 | 판 길이 선택지 축소, 색 판정 불일치 | 실차는 확인 불가(설명회 질문) | 판 길이별 실현 가능성으로 S0에 가능한 길이만 연다(2-1 시작 가능 규칙). 비EV에서 C1·C2·C3·D1·B1만 남아도 3분 판은 성립한다. 색 판정은 색상차 ≤ 30°로 프리셋 차이를 흡수. 기획서에는 "차종별 지원 범위에 따라 퍼즐 종류와 판 길이가 달라진다"고 명시 |
| 앰비언트 스위치가 Off여도 색·밝기 값이 유지되어 조명이 꺼진 채 "보라" 판정이 날 수 있음. `getAmbientLightSwitch`는 에뮬레이터에서 VC와 무관하게 항상 OFF를 주고 리스너도 없어 판정 조건으로 쓸 수 없음(실측) | 판정 체감 불일치 | 결정 완료(F2-data) | 스위치를 판정에 쓰지 않는다. 단서 문구 "조명을 켠 채로"로만 안내한다. 실차에서 스위치 값이 정상이라면 이후 단계에서 "On일 때만 판정"을 설정 옵션으로 검토 |
| "Vehicle State" 승인 범위에 CAR_LIGHTS·CAR_SEATS·CAR_DRIVE가 포함되지 않을 수 있음(공식 목록 없음) | 미승인 퍼미션 선언은 마켓 심사 반려. `register*`는 퍼미션 없으면 동기 예외 | 10/28 설명회 | 설명회 답변대로 도메인을 줄인다. 최소 구성은 CAR_CLIMATE + CAR_DRIVE(C1·C2·C3·D1·B1). 이 구성은 비EV에서 3분 판만 가능하고 5분 판(공조 2 + D1 + B1 = 4개)은 불가하므로, 그 경우 판 길이 선택지가 3분뿐임을 기획서에 밝힌다. 등록 예외는 실패·null로 바꾼다 |
| 엔진 공회전: 내연·하이브리드 차에서 엔진을 켠 채 플레이하면 지자체 조례 위반 소지(서울 5~25℃ 구간 2분, 과태료 5만원) | 심사위원의 안전·정책 우려, 사용자 민원 | 기획 단계(부록 A), 파워 상태(ACC) 스파이크는 W1 후순위 | 앱은 시동을 다루지 않음을 명시. 비EV 기본 설정에서 안내 1회(다시 보지 않기 가능), 공조 퍼즐 설정값만 판정·판당 2개 제한, 풍량 목표 절반 이하, 온도 목표 18~28℃, 제상 퍼즐 제외. 엔진 구동 상태 API가 없어 안내 이상은 불가능함을 기획서에 솔직히 적는다 |
| 제상 퍼즐을 살릴 경우 차종에 따라 A/C·외기·풍량이 함께 바뀔 수 있음(미확인). 실차 자동 공조 연동도 미확인 | 공회전 안내·복구 체크리스트와 모순 | 이후 단계(차종 취급설명서·실차) | MVP 풀에서 제외. 연동 동작 확인 뒤에만 재검토 |
| 조향각 단위·부호: 에뮬레이터는 VC 핸들 회전각 크기 그대로에 부호 반대(SDK = −VC), 값이 capability 범위를 넘을 수 있음(VC 500 → SDK −500). 등록 직후 첫 콜백이 낡은 값일 수 있음. 실차는 앞바퀴 각도로 추정[추론] | 칸 경계가 어긋나거나 중앙 복귀를 잘못 확정 | 에뮬레이터 실측·QA 완료, 실차 미확인 | 목표를 `AreaBounds` 비율(5~15%, 3칸)로 정의하고 SDK 부호 기준으로 판정, 범위로 자른 뒤 판정, 3.1초 주기 반복 콜백 무시, 범위 폭 10° 미만이면 제외. 중앙 복귀는 `getCurrentSteeringWheelAngle` 재조회로 확정. 시연 명령은 양수(`steering-angle 45`). 핸들 게이지 칸 폭(가로 약 33dp)은 표시 전용이라 W2에서 다듬는다 |
| 브레이크 봉인: 밟은 채 시작·재개하거나 첫 값이 null이면 누름 1회로 셀 위험, 1% 이상이면 true라 발 떨림이 누름으로 잡힐 위험 | 봉인이 의도 없이 풀림 | F2-ui QA 완료(밟은 채 재개 → 0/2 실측) | "150ms 이상 뗀 뒤의 밟음만 센다"로 흡수. 첫 값(null·true)과 낡은 0~1ms 뗌은 기준으로만 쓴다. 재개 시 횟수 0부터 |
| 연쇄형 단서가 사실상 지시형이 되는 경우(항등식, 빼기 결과가 문장 속 수와 우연히 같음) | 추리형 단서의 가치 저하 | F2-ui QA 2회차: 항등식 0건, 빼기 우연 일치 1.2% 잔존 | 2-1 연쇄형 규칙으로 걸러 지시형으로 대체. 남은 빼기 우연 일치는 부호를 뗀 상수와 비교해 W1에 제거한다. 연쇄형은 숫자 신호(C1·C2·L2)에만 낸다 |
| 판 생성과 시작 가능 판단이 어긋남(가능하다고 한 길이를 못 만들거나 같은 신호가 연달아 나옴, F2-engine QA 실패 2·3) | S0가 연 길이에서 시작 실패, "같은 조작 두 번 + 봉인" 판 | F2-engine 수정 반영, F2-ui QA 2회차 회귀 없음 | 실현 가능성을 생성기와 같은 판정 함수로 세고, "B1을 뺀 서로 다른 신호 2종 이상"·"같은 신호 연속 금지"·"두 번째 목표가 실제로 존재할 때만 2개로 셈"을 제약에 넣는다. S0 가능 길이는 운전석만으로 보수적으로 계산하고, 시작 시 실제 참가자로 생성이 실패하면 `BOARD_UNAVAILABLE`로 홈 복귀(안전망) |
| L1이 한 판에 두 번 나올 때 두 목표의 색상차가 31~60°이면 그 사이 색이 둘 다 통과 | 판정 틈 | F2-engine QA 관찰 | 생성기가 두 L1 목표를 60° 넘게 떼어 놓는다(W1 확인) |
| 정차 판정이 P만이 아닐 수 있음(북미 외 실차는 주차 브레이크·Auto Hold 정차도 `0x0`). 신호 대기 중 이어하기·핸들 퍼즐이 열릴 위험 | 바퀴가 꺾인 채 출발, 주의 분산 | 실차 확인 불가 | 시작·이어하기·앱 전환 복귀 모두 "기어 P·주차 브레이크 확인" 명시적 버튼을 거치고, 게이트가 열려도 자동으로 재개하지 않는다. 핸들 퍼즐은 범위 5~15% 소폭·판당 1개이고 중앙 복귀까지가 한 퍼즐이다. 기어 직접 조회(`GEAR_SELECTION`)는 승인 확인 뒤 이후 단계 스파이크 |
| 42dot이 게임 등을 순차 추가하겠다고 밝혀(market-03 ✅) 다른 게임사가 먼저 들어올 수 있음 | 카테고리 선점 효과 약화 | 상시 | 일반 게임(화면 터치·폰 컨트롤러)과 달리 차량 신호가 입력이라는 구조적 차이로 위치를 잡는다. 생성형 판·협동·기능 익히기 모드로 콘텐츠 속도에서 앞선다 |
| 완성차가 같은 기능을 순정으로 넣을 수 있음(Tesla·Ford 선례) | 차별점 약화 | 상시 | 개방형 앱마켓·읽기 전용·협동·생성형 판으로 위치를 잡는다 |
| 국내 게임 출시에 게임물 등급분류(GRAC)·IARC 연령 등급이 필요(`distribute/app-submission-guide.md`) | 마켓 입점 지연, 테마 팩 유료화 절차 | 입점 준비 단계 | 전체이용가 기준으로 설계(폭력·사행성 없음). 등급 절차 순서는 설명회·마켓 문의 |
| 성능 기준(주행 중 비내비 앱 CPU < 5%, RAM ≤ 400MB) 미측정. release APK가 서체 때문에 약 27.3MB | 심사 기준 미달 가능성 | W1 측정 | 주행 중에는 조명 3종 공유 리스너와 규제 Flow만 남고 폴링이 없으므로 낮을 것으로 예상[가정]. 측정 뒤 초과하면 조명 공유 Flow를 Lazily 시작으로 유지하고 그리기 갱신을 줄인다 |
| 1인 개발, 남은 기간 2주 반: F3 마무리·성능·플레이테스트·영상·양식 | 1차 마감 미완성 | 매주 월요일 | 기능은 F3 마무리와 QA 지적(빼기 우연 일치, 좌석 원 배치, 게이지 칸 폭)만 남기고 11/12 동결. 영상·양식은 W2부터 병행 |
| 에뮬레이터 시연 장애(콜드 부팅 SystemUI 크래시, VC heartbeat, `park`/`drive`가 핸들·브레이크를 0으로 되돌림, VC 새 세션이 앰비언트를 흰색으로·풍량 0을 2로 되돌림, `pm clear`가 사용자 10 데이터를 안 지움, 세로 1단에서 uiautomator 미동작) | PT 라이브 시연 실패, 리허설 오류 | 리허설 완료(10/27), W1·W3 재리허설, 12/01 | `emu.sh settle` 뒤 시작, `vc.sh hold`/`release`로 세션 유지, 시연 시드 고정과 0단계 경고, `pm clear --user 10`, 세로는 좌표 탭, Fake 빌드(자동 풀이 + `fakeGate`)와 영상을 예비로 |
| 퍼즐이 운전자에게 불필요한 조작을 반복시킨다는 비판(핸들 회전, 12V 배터리) | 정책·사용자 평가 | 비평 | 핸들은 범위 5~15%·1회·중앙 복귀 포함, 풍량 상한, 온도 쾌적 범위, 판 길이 10분 이하, 복구 체크리스트로 원상 회복 |

## 부록 A. 엔진 공회전 제한 확인 메모 (2026-10-24 웹 확인)

- 근거 법령: 대기환경보전법 제59조(자동차의 공회전 제한)는 시·도지사가 조례로 터미널·차고지·주차장 등에서 원동기를 가동한 채 주·정차하는 행위를 제한할 수 있게 한다. 이번 조사에서 법령 원문 페이지는 열지 못했으므로 조항 번호는 국가법령정보센터에서 재확인한다.
- 서울시 공식 안내(https://news.seoul.go.kr/env/archives/509990, 수정일 2023-11-02): 제한 장소는 학교 주변 환경위생정화구역·주차장·차고지·터미널·고궁 등 생활권 주변 2,772개소('20.12 기준). 허용 시간은 기온별로 0℃ 이하·30℃ 초과 제한 없음, 0~5℃ 5분, **5~25℃ 2분**, 25~30℃ 5분. 과태료 5만원. 전기차·하이브리드에 대한 언급은 없다.
- 인천시(https://www.inews24.com/view/1798423, 시행 연도는 기사에서 확인 못 함): 전 지역, 2분, 5℃ 미만·25℃ 이상이면 냉난방 목적 5분, 0℃ 미만·30℃ 이상 제한 없음, 과태료 5만원.
- 지자체마다 시간·온도·장소가 다르다(세종 2분 등). 긴급차·냉동냉장차 등만 예외다. 그래서 앱 안내 문구는 "2분"을 고정하지 않고 "지역과 기온에 따라 과태료 대상이 될 수 있다"로 쓴다.
- 전기차·하이브리드 적용 여부: 조례는 "원동기 가동" 기준이므로 전기차는 해당하지 않는다[추론]. 하이브리드는 정차 중 엔진이 자동 정지하지만 난방·배터리 충전 요구로 엔진이 돌 수 있어 내연과 같이 취급한다[추론].
- 설계 반영: (1) 앱은 시동·엔진을 제어하지 않고 엔진 공회전 여부를 알 수 있는 API도 없다. (2) 비EV 기본 설정에서 판 시작 전 안내 1회("엔진을 끄고 전원만 켠 상태를 권장", 다시 보지 않기 가능). (3) 공조 퍼즐은 설정값(목표 온도·풍량·내외기)만 판정하고 실내 온도 변화·A/C 가동을 요구하지 않으며 판당 최대 2개, 풍량 목표는 범위 상한의 절반 이하, 온도 목표는 18~28℃ 안. 제상은 차종에 따라 A/C·풍량을 함께 바꿀 수 있어 풀에서 뺐다. (4) 판 길이 기본 5분. (5) 엔진을 끄고 전원만 켠 상태(ACC/IGN ON)에서 Pleos Connect와 Vehicle 서비스가 동작하는지는 실차에서 확인하지 못했다(에뮬레이터 '파워 상태' 카드 실험은 W1 후순위).

## 부록 B. 10/28 설명회에서 확인할 것
- "Vehicle State" 승인에 포함되는 CAR_* 퍼미션 목록(특히 LIGHTS·SEATS·DRIVE).
- 게임(미디어 앱)이 `distractionOptimized`를 선언하고 주행 중 안내 한 줄 화면을 두는 것이 허용되는지. 허용 답변이 있을 때만 기능 플래그 `pleos.drivingNotice`를 켠다. 그 전까지 기본값은 DO 미선언이다.
- 실차에서 DO 미선언 앱의 주행 중 처리(차단 화면 + 액티비티 유지)가 에뮬레이터와 같은지.
- **Vehicle SDK 2.3.2 결함 2건의 확인·수정 계획.** (1) `unregisterReadingLightState`·`unregisterCabinLightState`가 `removeListener` 대신 `addListener`를 호출해 해제가 되지 않는다(`LightImpl.kt`). (2) `PropertyCallback.isEmptyPleosPropertyId`가 속성 이름을 `startsWith`로 비교해 앰비언트 색 리스너가 있으면 밝기 속성을 구독하지 않는다. 앱은 조명 3종을 앱 수명 동안 공유 등록(밝기 먼저)으로 우회하는데, 이 방식이 심사에서 문제가 되는지와 수정 버전 일정.
- `registerSeatOccupancy`/`getCurrentSeatOccupancy`의 Boolean 값 해석(실차에서 `true`가 "점유"인지 "비어 있음"인지. 에뮬레이터는 VC "점유"가 `false`로 오고 2열 가운데는 항상 `true`).
- 실차의 핸들 각도 단위·범위(앞바퀴 각도인지 핸들 회전각인지)와 앰비언트 프리셋 팔레트. 실차에서 `getAmbientLightSwitch`가 실제 스위치 상태를 돌려주는지(에뮬레이터는 항상 OFF).
- 접수양식 "SDK 추가 권한" 항목을 신청 불필요 SDK만 쓰는 앱이 어떻게 채우는지.
- 2차 PT 심사 기준과 심사용 GitHub 계정.
- 게임 출시 시 GRAC·IARC 등급 절차 순서.
- Pleos 시스템 아이콘 세트를 서드파티 앱이 쓸 수 있는지.
- Connect 디자인 키트(Figma, Internal)와 차량 3D 모델·탑뷰 이미지(좌석도·핸들 게이지에 쓸 자원)를 받을 수 있는지.

## 변경 기록
| 회차 | 고친 절 | 이유(비평 항목 번호) |
|---|---|---|
| 1 | 초안 작성 | 선택안 W1과 `02_selected.json`의 수정 방향 5가지(선례 위치 잡기, 반복 장치, 퍼미션 4개 이하, 위험 조작 제외, 페르소나·공회전 재확인) 반영 |
| 2 | 0, 1-3(표 4열·안전 설계), 1-5(주행 규제 대응 항목 신설·매니페스트), 1-6 카테고리, 2(F1·F2 주행/정차 동작·완료 조건), 2-2(리스너 수명·매니페스트 변경), 3(매니페스트 목록), 4(머리말·모든 행의 D단 정지·주행 열, S-H를 플래그 빌드 전용으로), 5-1(11~13단계), 5-2(2:15), 6(W0·W1), 7(첫 두 행), 부록 B | **B1**: 기본안을 DO 미선언으로 바꿨다. 주행 전환 시 시스템이 앱을 막고 앱은 규제 콜백·`onStop`/`onPause`에서 저장·리스너 해제, P에서 홈 "이어하기"로 복귀. "DO 선언 + 한 줄 안내"는 설명회 공식 허용 뒤에만 켜는 플래그 `pleos.drivingNotice`로 내렸다. 현재 골격의 `distractionOptimized=true` 삭제를 2-2·3절에 적었다. 시스템이 막는 방식은 리더의 에뮬레이터 확인 결과 전까지 "에뮬레이터 확인 중"으로 두었다 |
| 2 | 1-2 "확인된 근거"·"정성 근거" | **M1**: 1번 근거를 regulation-safety-01 ✅ + 심사 가이드 원문 + platform-07 ✅로 바꾸고 tech-trends-08은 ○ 정성 문단으로 내렸다. BMW 50만 대는 "BMW 발표 기준, 미검증(○)"으로 정성 문단에 옮겼다. "유일한"을 "Pleos 정책상 게임이 쓸 수 있는 형태"로 줄였다 |
| 2 | 1-2 근거 2번, 7절 | **M2**: "게임은 보이지 않는다(전체 목록 미확인)"로 고치고 42dot의 게임 순차 추가(market-03 ✅)를 기회 근거와 경쟁 리스크 행으로 함께 넣었다 |
| 2 | 1-6 예상 MAU | **M3**: 투싼을 기본 시나리오에서 빼고 상향 시나리오의 [가정]으로 분리해 값의 이유를 적었다. 차종별 개월 수 산정 근거, 아반떼 재고 단서를 추가했다. 가정 B는 사실(market-03 ✅)과 가정을 나눠 썼다. 100만 대 문장을 "시점과 무관한 규모 예시"로 밝혔다. 기본 MAU 약 2,100, 상향 약 9,500 |
| 2 | 2-1 판 생성 규칙, 2(F2), 5절 머리말·5-1 전체, 1-4 장면 1 | **M4**: 판 길이별 K를 2-1에서 정의(3분 3·5분 5·10분 8)하고 장면 1·시연 모두 5분 판 = 퍼즐 5개로 맞췄다. 디버그 빌드 전용 시연 시드(`--es seed demo5`)와 그 판의 순서·목표값을 5절에 그대로 적었다. 0단계 초기값 세팅을 넣었다. B1 시간 창을 "누름 간격 8초 이내, 전체 30초"로 늘리고 시연 시드는 2회로 고정했다 |
| 2 | 2(F1), 2-1 D1, 2-2 전이, 4절 S1, 7절 Auto Hold 행 | **M5**: 이어하기도 S1 P 확인을 거치고, 게이트가 열려도 자동 재개하지 않는다. D1은 "구간 1초 유지 후 중앙(±5%) 복귀"까지가 한 퍼즐이다. `GEAR_SELECTION`은 이후 스파이크 후보로만 적었다 |
| 2 | 2-1(C4 삭제·제외 목록), 1-3 제외 항목, 1-5, 3절 도메인 이유, 부록 A (3), 7절 | **M6**: 제상 퍼즐을 MVP 풀에서 뺐다(신호 8종). 차종별 A/C·외기·풍량 연동 가능성을 리스크로 적고 공회전 안내 문구와 맞췄다 |
| 2 | 2-1(단서 유형 3종·예시), 2(F2에 최고 기록 포함, F4를 오늘의 판·협동 기록으로), 1-3, 1-6 Retention, 1-4 장면 1·2, 0절 | **M7**: 암호형(암호표)·연쇄형(앞 답으로 계산) 단서를 MVP에 넣고 판 길이별 개인 최고 기록을 F2로 당겼다(MVP 기능 수 3개 유지). 퍼즐당 예상 소요[가정]로 K를 산정했다. Retention 서술을 두 장치에 다시 이었다 |
| 2 | 1-2 | m1: 작성 메모 문장 삭제. m2: Tesla(스크롤 휠, 제조사가 고른 게임만), Ford(작동시키기도 하는 콘셉트)로 정확히 고침 |
| 2 | 머리말, 1-3, 1-4, 4절, 5-2, 7절, 부록 A | m3: "D단 정지"(차량 상태)와 "엔진 공회전"(조례)으로 나눠 씀. 4절 표 머리글은 템플릿 문구를 유지하고 "D단 정지"를 병기 |
| 2 | 1-4 장면 1, 부록 A | m4: 안내 문구에서 "2분" 고정을 빼고 "지역과 기온에 따라"로 바꿈 |
| 2 | 2-1 C1·C2·L3 | m5: area를 capability map 키 중 해당 `SeatValue`를 포함하는 것으로 고르도록 고침 |
| 2 | 1-3, 2-1 D1, 5-1 4단계, 7절 | m6: 핸들 구간을 범위의 5~15%로 줄이고 "소폭"의 뜻을 명시(VC 기준 약 -45) |
| 2 | 2(F2), 2-2, 4절 공통 규칙 | m7: Paused·`onStop`에서 리스너 전부 해제, 재개 시 재등록 + `get*` 재동기화 |
| 2 | 2(F3), 3절 대체 경로, 7절 | m8: 뒷좌석 점유 미확인 리스크 행 추가, 좌석도 수동 선택 폴백 |
| 2 | 1-3, 2(F2), 2-1 L1 | m9: "실제 다이얼·버튼"을 "실내 조작(다이얼·버튼·설정 메뉴)"으로 넓히고, 설정 앱을 다녀와도 S2가 복원되고 재동기화로 판정됨을 적음 |
| 2 | 1-6 확대 계획 (4), 7절, 부록 B | m10: GRAC·IARC 등급을 입점 준비 항목과 리스크로 추가 |
| 2 | 머리말, 6절 | m11: 기준일 2026-10-25(일), W0 10/25~10/26, "11/12(목)까지 개발, 11/13~11/15 버퍼"로 요일·동결 시점 통일 |
| 2 | 1-6 타깃 규모 | m12: user-needs-01을 EV 2차 타깃 모수 설명에 인용(출처 목록 유지) |
| 2 | 3절 공통 | 비평 참고 사항: "앱은 어떤 `set*`도 호출하지 않는다" 문장 추가(README·심사 노트에도 적기로) |
| 2-1 | 0, 1-3(표·안전 설계), 1-4 장면 1, 1-5 주행 규제 대응, 2(F1 화면·동작·주행/정차 동작·완료 조건, F2), 2-2(전이·저장 시점), 4(머리말·전 행, S-P 행 신설), 5-1(11~13-1), 5-2(2:15), 6(W1), 7(첫 행) | **B1 후속**: 리더의 에뮬레이터 실측(`_workspace/03_plan/do_test/README.md`, 2026-10-25)으로 "에뮬레이터 확인 중"을 사실로 채웠다. DO 미선언 앱은 D단 정지·주행에서 종료·백그라운드 없이 시스템 차단 화면("주행 중에는 사용할 수 없습니다.")에 덮이고 액티비티는 RESUMED라 `onPause`/`onStop`이 오지 않는다. 따라서 저장·일시정지·리스너 해제는 규제 콜백이 1차 경로이고 생명주기 콜백은 보조로 내렸다. 차단 화면 뒤로 앱이 흐리게 비치므로 게이트가 닫히는 순간 S2를 S-P(퍼즐 내용 없는 일시정지 화면)로 바꾸는 설계를 넣었다. 정차 시 같은 액티비티가 그대로 보이므로 이어하기는 S-P → S1 P 확인으로 처리. 실차 동작은 10/28 설명회 확인 항목으로 남겼다 |
| 3 | 2-2 전이·리스너 수명, 2(F2 주행/정차 동작·완료 조건) | n1: 일시정지를 `PausedByGate`(규제 콜백, S-P → P 확인 필수)와 `PausedByApp`(게이트 열린 채 `onStop`, 돌아올 때 게이트가 닫힌 적 없으면 P 확인 없이 S2 복원 + `get*` 재동기화)으로 나눴다. F2의 "설정 앱을 다녀와도 복원" 서술과 맞췄다 |
| 3 | 2(F1 주행/정차 동작, F6 주행/정차 동작) | n2: 게이트 닫힘 처리 목록에 효과음 정지·오디오 포커스 반환을 넣고, PT F6은 규제 콜백에서 즉시 정지하도록 적었다(주행 전환 시 `onPause`가 오지 않으므로 포커스 LOSS에만 기대지 않음) |
| 3 | 2-1 생성 규칙(연쇄형 규칙 신설), 2-1 D1 판정, 5-1 4단계 | n3: D1 구간을 5~15%의 3칸(1~3번)으로 정의하고 칸 번호를 연쇄 입력값으로 삼았다. 연쇄식의 상수·배수는 결과가 `AreaBounds`·현재값 차이·비EV 풍량 상한을 모두 지키도록 생성기가 고르고, 없으면 지시형으로 대체. 앞 퍼즐을 건너뛰면 힌트 카드에 입력값을 보여 준다 |
| 3 | 2-1 생성 규칙(시드 항목), 2(F2 완료 조건, F4 완료 조건), 5절 시연 시드 | n4: 판을 정하는 입력을 "시드·참가 좌석·지원 신호 풀"로 고쳐 쓰고, 시연 모드는 지원 신호 풀 8종 고정 + 참가 좌석은 실제 점유를 따른다고 밝혔다. F4는 판이 차종 지원 범위에 따라 다를 수 있음을 명시 |
| 3 | 2(F1 화면·동작·주행/정차 동작), 4절 S-P 행, 6절 W1 | n5: S-P의 "이어하기"·"홈"을 게이트가 닫힌 동안 앱에서도 비활성으로 적고, 차단 화면의 터치 차단 여부는 W1 스파이크에 추가 |
| v2-1 | 0(차량 특화 핵심), 1-3(F1 행·안전 설계 주행 중), 1-5 주행 규제 대응, 2(F1 화면·동작·완료 조건), 4(머리말·S2 행·S-P 행), 5-1(11단계), 5-2(2:15), 7(첫 행) | **차단 화면 뒤 배경(W1 실측)**: "흐린 배경에 S-P가 비친다"를 "차단 화면은 전환 순간 화면(퍼즐 화면)을 판독할 수 없게 흐린 시스템 정지 이미지라 앱이 바꿀 수 없다"로 고쳤다. S-P 전환은 정차 복귀 시 보이는 화면이 S-P여야 하고 저장·타이머 정지·리스너 해제가 같은 순간이어야 하므로 유지. F1 완료 조건을 "규제 콜백 뒤 100ms 안에 S-P를 그리고 저장(logcat)"으로 바꿨다(실측: 앱 콜백은 홈 차단과 같은 순간 −2~+21ms, S-P는 그 뒤 9~21ms). 7절 첫 행에서 콜백 지연은 문제가 아님을 반영. 차단 화면이 터치를 전달하지 않는 실측도 F1·4절에 반영 |
| v2-2 | 2-2(전이·리스너 수명·상태 enum), 2(F2 화면·동작·주행/정차 동작·완료 조건), 2-1 L1·L2 조건, 1-3 F1 행, 1-4 장면 1, 4절 S1 행, 7절 Auto Hold 행 | **앱 전환 뒤 이어하기(리더 결정)**: 재개 조건을 `PausedByGate`·`PausedByApp` 모두 "게이트 열림 + S1 P 확인"으로 통일했다. 실측상 화면이 보이는 동안만 구독하면 떠나 있는 사이의 주행을 알 수 없고, 프로세스 범위 리스너는 동결로 이벤트가 3~6초 늦게 몰려와 순서가 보장되지 않기 때문이다. 두 원인의 구분은 저장·표시 목적으로만 남겼다. `PausedByApp` 복귀 시에는 S-P 없이 S1 P 확인을 바로 띄운다. F2의 "P 확인 없이 복원"(3회차 n1)을 철회하고, 앰비언트 색·밝기 단서 카드에 "조명 설정으로 다녀오면 P 확인을 한 번 눌러요"를 넣었다 |
| v2-3 | 0(MVP·리스크), 1-2 차별점 (3), 1-3(F3 행·허용 원칙), 1-4 장면 1·2, 1-5 개발 범위, 1-6 Retention, 2(F3 전 열), 2-1 생성 규칙(참가 좌석), 3(F3 승인 상태·대체 경로, CAR_SEATS 이유), 4절 S1 행, 5절 시연 시드·5-1 0·2단계, 5-2(1:15), 7절(좌석 점유 극성 행 신설, 뒷좌석 행 교체), 부록 B | **F3 좌석 점유(W1 실측)**: 에뮬레이터에서 값이 반대로 오고(VC 점유 → SDK `false`) 2열 가운데는 항상 `true`다. S1 좌석도 탭 선택을 폴백이 아니라 기본 동작으로 올리고, 감지값은 "운전석에는 항상 사람이 있다"로 극성을 보정한 뒤 초기 선택값으로만 쓴다. 2열 가운데는 독서등 capability가 없어 선택지에서 뺐다. 부록 B에 SEAT_OCCUPANCY 값 해석 질문을 추가했다 |
| v2-4 | 5절 머리말(feature id·area 확정, `park`/`drive`의 핸들·브레이크 초기화 주의), 시연 시드 문단(0단계 24℃ 필요 이유), 5-1(0·2·4·5·6·7·8·9·14단계), 2-1 B1 조건 | **시연 시나리오(W1 실측)**: 0단계를 실측 기본값에 맞췄다(운전석 온도 기본 22.0℃ → 24℃ 필수, 앰비언트 기본 Cyan `#00e5ff`, 좌석 점유 `driver` true). 4단계 핸들 명령을 `vc.sh set steering-angle 45`(SDK −45 = 왼쪽 2번 칸, 부호 반대)로, 5단계 보라를 VC 프리셋 `#aa00ff`로, 7·9단계 온도를 `hvac-temperature-set … driver`로 고쳤다. 브레이크 두 번은 약 6.3초로 30초 창 안임을 적었다. 14단계에 `spike_w1.sh window split/join`과 실측 창 크기를 넣었다 |
| v2-5 | 2-1 생성 규칙(신호별 생성 제약 신설), 2-1 표 C1·C2·C3·L1·L2·L3·D1·B1 판정 규칙, 1-3 안전 설계(온도 쾌적 범위), 1-5 SDK 계획(`getAmbientLightSwitch` 검토), 3절 L1 행, 부록 A (3), 7절(앰비언트 스위치 행 신설, 조향각 행 교체) | **생성기 제약(W1 실측)**: C1은 `AreaBounds` 양 끝 1℃ 안쪽·18~28℃만(capability 15℃부터, VC 16℃부터). C2는 현재값이 범위 밖(0)일 때 범위로 잘라 처리. L1은 알파 0x00이라 RGB만 비교하고 시연 시드는 VC 프리셋으로 재현되는 보라·파랑만, 스위치 Off 문제는 F2-data에서 확인. D1은 SDK 부호(왼쪽 음수, VC와 반대) 기준, 양쪽 범위 각각, 범위 초과값 자르기, 3.1초 주기 반복 콜백은 값이 바뀔 때만 판정, 범위 폭 10° 미만이면 제외. B1은 1% 이상 true라 150ms 디바운스 검토 |
| v2-6 | 4절 공통 규칙, 2(F1 완료 조건), 2-2 저장 시점, 5-1 14단계 | **창 크기(W1 실측)**: 세로 1단 841×1210dp(콘텐츠 793×1162dp), 가로 2단 1682×1210dp를 적고 분기 기준 폭 1200dp[가정]를 명시했다. 창 크기가 바뀌면 액티비티가 다시 만들어진다(`onDestroy → onCreate`)는 점을 ViewModel·SavedState·DataStore 상태 유지의 근거로 적었다 |
| v2-7 | 부록 B, 6절 W1 | 설명회 질문에 Pleos 시스템 아이콘 세트의 서드파티 사용 가능 여부, Connect 디자인 키트(Figma, Internal)·차량 3D 모델·탑뷰 이미지 제공 여부, 실차 핸들 단위·앰비언트 팔레트, 실차 차단 동작을 추가했다. 6절 W0에 스파이크 조기 완료를 적고 W1의 스파이크 항목을 남은 것(파워 상태 ACC, `0x1ff`, 묶음 area 오류 형태, `getAmbientLightSwitch`)으로 바꿨다 |
| v2-8 | 머리말, 1-5 구현 방식·검증 환경, 1-7 출처, 2-2 기존 골격, 3절 머리말·승인 상태 열·공통, 7절 에뮬레이터 장애 행 | 부수 반영: 기준일·v2 출처 표기, 콜백이 바인더 스레드로 온다는 실측, 에뮬레이터 수신 확인 표기, 디버그 전용 `SpikeActivity`가 release에 없음, `park`/`drive`의 핸들·브레이크 초기화를 시연 리스크에 추가 |
| v3-1 | 1-5(쓰지 않는 것·SDK 계획 L1), 2-1 신호별 제약 L1·표 L1 행·제외 목록, 3절 L1 행, 5-1 5단계, 7절 앰비언트 스위치 행, 부록 B, 6절 W1 | **앰비언트 스위치**: L1·L2 판정에 쓰지 않는다. 에뮬레이터는 VC 스위치와 무관하게 `getAmbientLightSwitch`가 항상 OFF를 돌려주고 리스너 API도 없어(`spike_w1.md` 추가 절) 조건으로 걸면 L1·L2가 영원히 풀리지 않는다. 3절 L1 행의 "(검토)"를 "호출하지 않는다"로 바꾸고, 필요한 안내는 단서 문구 "조명을 켠 채로"로만 한다. 6절 W1의 남은 스파이크에서 이 항목을 뺐다 |
| v3-2 | 2-1 생성 규칙("현재값을 모르는 신호" 항목 신설, 지원 신호 풀 정의, C2 제약), 2-1 표 C1·C2·C3·L2·L3 판정 규칙, 2(F2 실패·미지원 시), 3절 C1·C2 대체 경로·승인 상태 | **현재값을 모르는 신호**: 판 생성 때 `get*` 현재값이 null인 신호(풍량 꺼짐 0처럼 범위 밖이라 데이터 계층이 null로 내보내는 값 포함)는 그 판에서 뺀다. 판정 중 null은 불일치. v2의 "C2는 범위로 자른 뒤 처리"를 구현(범위 밖 = null)에 맞춰 고쳤다. L3는 현재 OFF인 좌석에만 낸다는 구현 규칙도 같은 원칙으로 적었다 |
| v3-3 | 2-1 신호별 제약 D1·표 D1 행, 1-5 SDK 계획, 3절 D1 행, 5-1 4단계, 7절 조향각 행 | **핸들 중앙 복귀**: 콜백 값이 아니라 `getCurrentSteeringWheelAngle`로 다시 읽은 값으로 확정한다(등록 직후 첫 콜백이 낡은 값일 수 있다는 F2-data QA 관찰) |
| v3-4 | 2-1 신호별 제약 B1·표 B1 행, 1-4 장면 1, 5-1 8단계, 7절(브레이크 봉인 행 신설) | **브레이크 봉인**: 상태를 모르는 첫 값(null)은 "뗀 상태"로 치지 않고 기준으로도 삼지 않는다. 실제 뗌(`false`)을 관측한 뒤의 `false→true`만 센다. 밟은 채 시작·재개해도 떼었다가 밟아야 1회(재개 직후 `DriveInputRepository`에 브레이크 읽기가 없어 기준이 null인 경로, F2-engine QA 실패 1). 150ms 떨림 무시. 시연 8단계에 뗌 기준 확보용 `brake-pedal-status 0`을 먼저 보낸다 |
| v3-5 | 2-1 생성 규칙(판의 제약·시작 가능 규칙 신설, K 항목 "짧은 판 없음"), 2(F1 화면·동작 S0, F2 실패·미지원 시·완료 조건), 1-3 생성형 퍼즐 판 행, 4절 S0 행, 7절(실차 지원 범위 행·Vehicle State 행 대응 교체, 판 생성 불일치 행 신설) | **시작 가능 규칙**: "지원 신호 3개 이상"을 "판 길이별 실현 가능성"으로 바꿨다. 제약은 비EV 공조 2개 상한, D1 1개, B1 마지막 1개, L3 참가 좌석만, B1을 뺀 서로 다른 신호 2종 이상, 같은 신호 연속 금지, 같은 신호 판당 2개 상한(두 번째 목표가 실제로 존재할 때만 2로 셈). 실현 가능성은 생성기와 같은 판정으로 세어 S0가 연 길이는 생성이 반드시 성공한다. 홈은 가능한 길이만 활성이고, 3분 판도 못 만들 때만 "이 차에서는 지원하는 조작이 부족해요". 짧은 판 폴백 없음. 최소 구성 예시(비EV C1·C2·C3·D1·B1 → 3분만, 5분은 공조 2 + D1 + B1 = 4개라 불가)를 적었다. F2-engine QA 실패 2·3을 리스크 행으로 넣었다 |
| v3-6 | 2-1 생성 규칙(판을 정하는 입력), 2(F2 완료 조건, F4 완료 조건), 5절 머리말·시연 시드 문단(재현 조건 다섯), 5-1 0단계 기대 화면 | **판을 정하는 입력**: "시드·참가 좌석·지원 신호 풀"에 "시작 때 현재값·EV 설정"을 더해 다섯으로 썼다(온도 2℃ 차이·내외기 반대 전환·독서등 OFF 좌석 규칙이 현재값에 따라 판을 바꾼다). 시연 재현 조건을 다섯 입력으로 적었다 |
| v3-7 | 2-2(저장 범위 항목 신설, 프로세스 재생성 항목, 저장 시점·리스너 수명·타이머 시계), 2(F1 완료 조건, F2 주행/정차 동작), 5-1 13-1단계, 2-1 표 D1·B1 | **판정 진행 저장**: B1 횟수·D1 칸 유지 같은 판정 진행은 저장하지 않아 재개하면 그 퍼즐은 처음부터다. PLAYING 중 프로세스가 죽었다 복원되면 `PausedByApp`으로 두고 P 확인을 받는다. 저장 세션의 버전 불일치·JSON 손상은 버리고 지운다. 타이머는 `SystemClock.elapsedRealtime` |
| v3-8 | 2(F2 화면·동작 S3), 1-3 원상 복구 행, 2-1 표 C1·L1 원복, 4절 S3 행, 5-1 9단계 | **복구 체크리스트 허용 오차**: C1 ±0.25℃, L1 RGB 일치(알파 무시). 나머지는 정수·상태 일치, D1 중앙 ±5%. 원래 값을 모르면 줄을 만들지 않는다 |
| v3-9 | 5절 머리말(VC 새 세션이 풍량 0을 2로 되돌림, Fake 기본값), 5-1 0단계(`seat-occupancy true driver` 필수 이유, 풍량 2), 7절 에뮬레이터 장애 행 | **시연 5-1**: VC는 새 세션을 열면 풍량 0을 2로 되돌리므로(F2-data QA 관찰 2) 풍량 0 유지를 가정하지 않는다. 0단계의 `seat-occupancy true driver`가 좌석 극성 보정 기준이라 필수임을 적었다. Fake 기본값이 0단계와 같음(F2-data QA 재검증)을 적었다 |
| v3-10 | 머리말, 0(리스크), 1-5 구현 방식·매니페스트, 1-7 출처, 2(F1 실패·미지원 시, F3 극성 불명 처리, F4 날짜 시드 형식), 2-1 단서 유형(암호표 구성), 2-2 기존 골격, 3절 머리말·공통(register 동기 예외), 6절 W0·W1 | 부수 반영: v3 출처 표기, DataStore 손상 교체 처리기, 운전석 원시값 null이면 극성 불명 → 운전석만(F2-data 재검증 1), 날짜 시드 `day-YYYY-MM-DD`, 암호표 진짜 1 + 미끼 2·기호 12개 이하, CAR_ENERGY·NAVI_ROUTE 삭제 시점 F2-ui·DO 삭제 F1(QA 이월 항목), `register*` 퍼미션 없을 때 동기 예외, 6절에 F2-data·F2-engine 조기 착수와 QA 실패 3건 수정 일정 |
| v4-1 | 0(리스크), 1-3 F1 행, 1-5(쓰지 않는 것·SDK 계획 실내 조명), 2(F1 주행/정차 동작·완료 조건, F3 API 열), 2-1 표 L1·L2·L3 API 열·제외 목록(실내등), 2-2 리스너 수명(예외 항목), 3절(머리말·L2·L1·L3 행·공통), 4절 공통 규칙, 7절(SDK 결함 행 신설), 부록 B | **리스너 수명 예외(F1·F2-ui QA)**: Vehicle 2.3.2 SDK 결함 2건 — `unregisterReadingLightState`(와 `unregisterCabinLightState`)가 `removeListener` 대신 `addListener`를 호출해 해제가 되지 않음(`LightImpl.kt`), `PropertyCallback.isEmptyPleosPropertyId`가 속성 이름을 `startsWith`로 비교해 색 리스너가 있으면 밝기를 구독하지 않음 — 때문에 독서등·앰비언트 색·밝기는 앱 수명 동안 한 번만 공유 등록하고(밝기 → 색 순서) 해제하지 않으며, 멈춘 동안 값을 쓰지 않는다. 나머지 신호는 일시정지 때 해제한다. 3절 표 L2를 L1 앞에 두어 등록 순서를 드러냈다. 설명회 질문에 두 결함을 넣었다 |
| v4-2 | 2-1 신호별 제약 B1·표 B1 행, 1-4 장면 1, 7절 브레이크 행 | **브레이크 규칙(F2-ui 개발자 결정 1)**: "누름 뒤 150ms 떨림 무시"를 "150ms 이상 뗀 뒤의 밟음만 센다(상태를 모르는 첫 값·낡은 0~1ms 뗌은 세지 않는다)"로 고쳤다. 에뮬레이터 실측(밟은 채 재개 → 0/2)을 적었다 |
| v4-3 | 2-1 단서 유형·연쇄형 규칙, 2-1 표 C1·C2·C3·L1·L2·L3·D1 단서 예, 1-3 생성형 퍼즐 판 행, 1-6 Retention, 2(F2 완료 조건), 7절(연쇄형 행 신설) | **연쇄형(F2-ui QA 실패 2·개발자 결정 3)**: 연쇄형은 숫자 목표(C1·C2·L2)에만 나오고 C3·L1·L3·D1은 지시형·암호형만 쓴다. 식에 보이는 수가 곧 답이 되는 식(×1+0, ×0, 입력 답 0, 입력 답 1, 결과 = 입력, 빼기 결과가 문장 속 수와 같음)은 쓰지 않고 그런 식만 가능하면 지시형으로 바꾼다. 남은 빼기 우연 일치(1.2%)는 부호를 뗀 상수 비교로 W1에 제거 |
| v4-4 | 2-2(전이: 창 크기 변경·덮어쓰기·끝난 판 세션 삭제, 저장 범위에 힌트·설정 파일), 2(F1 화면·동작·주행/정차 동작·완료 조건, F2 화면·동작(암호표·게이지·힌트·봉인 표시·S3 "새 판"·0단계 경고), F3 화면·동작(좌석 이름·원형 버튼과 목록 행·이어하기 시 변경 불가)), 1-3(F1·원상 복구 행, 안전 설계 주행 중), 4절(S0·S1·S2·S-P·S3 행·공통 규칙), 2-1 C1 제약(온도 게이지) | **화면 결정(F1·F2-ui QA)**: 창 크기 변경으로 오는 `onStop`은 일시정지가 아니다(`isChangingConfigurations`. 진짜 앱 전환만 `PausedByApp`). 저장된 판이 있을 때 시작하면 덮어쓰기 확인창 → 확인 → S1 → P 확인 때 기존 판 삭제. 암호형에는 판 전체 암호표를 보인다. 게이트가 닫히면 S3 "새 판"은 비활성이다. 설정은 S0 안 패널이고 게이트가 닫히면 닫힌다. 좌석 이름은 "운전석·조수석·2열 왼쪽·2열 오른쪽". 온도 게이지는 capability 범위(15~30℃)를 그대로 보인다. 그 밖에 QA로 확인된 표시 규칙(목표 미노출, 힌트 2장, 봉인 "남은 N초", 터치 타깃 88~112dp, 이어하기 시 참가 자리 변경 불가, S0 가능 길이는 운전석 기준 계산, `BOARD_UNAVAILABLE` 안전망)을 적었다 |
| v4-5 | 2(F1 실패·미지원 시), 2-2 매니페스트 변경, 1-5 주행 규제 대응, 4절 S-H 행, 7절 DO 플래그 행 | **주행 중 안내 플래그(F1 QA 2회차)**: `pleos.drivingNotice`는 추가 매니페스트 소스 세트 병합 방식이다(플레이스홀더가 아님). 켠 빌드는 게이트가 닫힌 동안(UNKNOWN 포함) 모든 화면 대신 S-H(아이콘 1개 + "정차하면 이어하기", 터치 요소 0)를 그리고 설정 패널을 닫는다(구현 완료, 주행 중 clickable 요소 0개 실측). 설명회 허용 뒤에만 켠다 |
| v4-6 | 5절 머리말, 시연 시드 문단(0단계 경고·release 시드 무시), 5-1(0단계 `pm clear --user 10`·`vc.sh hold`, 1·3·4·5·6·7·8·9단계 실측 화면, 9-1·13-2·15·16단계 신설, "끝" 행 `vc.sh release`, Fake 두 행), 5-2(0:15·1:35·1:55 장면, 촬영 주의), 7절 에뮬레이터 장애 행 | **시연 5-1·5-2(F2-ui 리허설·QA)**: VC가 세션을 새로 열 때마다 앰비언트를 흰색으로·풍량 0을 2로 되돌리므로 0단계 앞뒤에 `vc.sh hold`/`release`를 넣었다. 앱 데이터는 `pm clear --user 10 <pkg>`로 지운다. Fake 행을 "자동 풀이 6초 간격(33초 탈출)"과 "주행 전환은 `--es fakeGate drive/park`(자동 순환 없음)" 둘로 나눴다. 리허설 실측(핸들 1초 유지 별도 대기 불필요, 온도 눈금 15~30, 봉인 "남은 7~8초", 탈출 1:05~1:31, S-P 26~42ms, 세로는 좌표 탭, 새 판 시드 매번 다름)에 맞춰 기대 화면을 고쳤다 |
| v4-7 | 1-5(쓰지 않는 것·개발 범위 F5·매니페스트), 2(F5 Repository·Fake 열), 2-2 기존 골격, 3절(F5 승인 상태·매니페스트 문단) | **기존 코드 정리(F1)**: 차량 배터리·내비 Repository와 NaviHelper 의존성을 삭제했다(release dex에 NaviHelper 0건, `libs.versions.toml` 항목 제거). v3의 "F2-ui에서 삭제"를 "F1에서 삭제 완료"로 고치고, PT F5에서 git 기록으로 되살린다고 적었다 |
| v4-8 | 6절 전체 | **일정**: 1주차 범위(F2-data·F2-engine·F1)와 2주차 F2-ui가 10/27까지 끝났고 F3는 마무리 중이므로 W0를 완료로 표시하고, W1(설명회·F3 QA·성능 측정·빼기 우연 일치 수정·리허설 2회), W2(플레이테스트 K 보정·UI 다듬기·영상 촬영·양식 초안), W3(통합 리허설·release 최종·영상 편집)로 당겼다. 11/12 기능 동결과 11/13~15 버퍼는 유지 |
| v4-9 | 머리말, 0(차량 특화 핵심·MVP 상태), 1-5 구현 방식(`singleTop`·서체·설정 파일), 1-7 출처(QA·리허설·SDK 결함 근거·서체 라이선스), 2-1 K 항목(리허설 소요 시간 주석), 2-1 L1 제약(명령으로 재현한 초록), 7절(성능 미측정 행·L1 색상차 틈 행 신설, 1인 개발 행 갱신) | 부수 반영: 구현 실측 수치(S-P 26~42ms), `MainActivity` `singleTop`과 디버그 인텐트 처리, Asta Sans 서체·OFL 라이선스, 설정 별도 DataStore, release APK 27.3MB, 성능(CPU·RAM) 미측정과 L1 두 목표 색상차 틈을 리스크로 추가 |
