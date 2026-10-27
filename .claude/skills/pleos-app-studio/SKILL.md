---
name: pleos-app-studio
description: "Pleos Connect 차량용 앱(HMG 차량용 앱 공모전 출품작)을 아이디어 도출부터 제출까지 만드는 하네스 오케스트레이터. 리서치(시장·운전자 니즈·경쟁·기술 트렌드·규제·플랫폼 역량)와 사실 검증, 아이디어 발상과 심사·순위, 아이디어 선택, 기획서(접수양식)·기능 명세·시연 시나리오 작성과 비평, 기능 구현과 QA, 제출 점검을 단계별로 조율한다. 사용자가 차량 앱·Pleos 앱 아이디어를 찾자, 시장/니즈를 조사하자, 아이디어를 평가·비교하자, 기획서를 쓰자, 기획한 기능을 구현하자, 제출 전에 점검하자고 하면 사용한다. 리서치만 다시, 아이디어 다시 뽑기, 다른 방향으로 아이디어 추가, 기획서 수정·보완, 기능 추가, 이전 결과를 바탕으로 개선, 다시 실행 같은 후속 요청에도 사용한다. 쓰지 않는 경우: 이미 정해진 작은 코드 수정 하나(pleos-feature-dev), 퍼미션·빌드만 빠르게 확인(pleos-qa-check), 차량 앱 기획과 무관한 일반 시장 조사·보고서, SDK API 이름 같은 단순 질문."
---

# Pleos 앱 스튜디오 오케스트레이터

## 실행 모드: 혼합

| 단계 | 실행 모드 | 이유 |
|---|---|---|
| 0. 기존 작업 확인 | 메인 직접 | `_workspace/`와 `docs/` 상태로 시작 지점을 정한다 |
| 1. 리서치 | 워크플로(`scripts/research.workflow.js`) | 조사 축을 미리 나열할 수 있고, 축마다 사실 검증 절차를 코드로 정할 수 있다 |
| 2. 아이디어 | 워크플로(`scripts/ideation.workflow.js`) | 관점별 발상 → 병합 → 렌즈별 심사 → 합산이 코드로 정해진다 |
| 3. 선택 | 메인 직접(사용자 결정) | 무엇을 만들지는 사용자가 정한다 |
| 4. 기획 | 지속형 에이전트 협업 | 기획자가 비평을 같은 맥락에서 반영해야 한다 |
| 5. 구현 | 지속형 에이전트 협업 | 개발자가 QA 지적을 같은 맥락에서 고쳐야 한다 |
| 6. 제출 점검 | 서브에이전트 1회 | 독립 검증 한 번이면 된다 |

사용자가 이 스킬을 불러오면 1·2단계의 Workflow 사용에 동의한 것으로 본다. 기본 규모는 리서치 약 11개, 아이디어 약 9개 에이전트 호출이다. 사용자가 "철저히", "꼼꼼히", "전수"를 요청할 때만 `thorough: true`로 키운다.

**비용 기준(2026-10-24 실측):** 리서치 축 1개(발견 4건, 핵심 주장 2건)에 에이전트 3개, 약 45만 토큰, 약 15분이 들었다. 기본 리서치(축 5개)는 병렬이라 시간은 비슷하지만 토큰은 대략 200만 안팎으로 예상된다. 1단계를 시작하기 전에 이 예상치를 사용자에게 한 줄로 알리고, 축을 줄이자고 하면 `args.axes`를 줄인다.

## 에이전트 구성

| 역할 | `subagent_type` / `agentType` | 모델 | 스킬 | 단계 |
|---|---|---|---|---|
| 조사원 | `mobility-researcher` | sonnet | pleos-research | 1 |
| 사실 검증가 | `fact-checker` | opus | pleos-research | 1 |
| 발상가 | `idea-generator` | opus | pleos-ideation | 2 |
| 심사위원 | `idea-judge` | opus | pleos-idea-evaluation | 2 |
| 기획 책임자 | `product-planner` | fable | pleos-app-planning | 4 |
| 기획 비평가 | `plan-critic` | opus | pleos-app-planning, pleos-idea-evaluation | 4 |
| 개발자 | `pleos-android-dev` | opus | pleos-feature-dev | 5 |
| QA | `pleos-qa` | opus | pleos-qa-check, pleos-feature-dev | 5, 6 |

워크플로 안의 병합·누락 검토 에이전트는 기본 유형에 `model: 'sonnet'`을 쓴다(기계적인 정리 작업).

## 산출물 경로

| 경로 | 내용 | git |
|---|---|---|
| `_workspace/run_meta.json` | 단계별 실행 기록: 기준일, 워크플로 runId, 선택한 아이디어, 지속형 에이전트 ID | 제외 |
| `_workspace/01_research/01_workflow_result.json` | 리서치 워크플로 반환값 원본 | 제외 |
| `_workspace/02_ideation/02_workflow_result.json`, `02_selected.json` | 아이디어 워크플로 반환값, 선택한 아이디어 카드 | 제외 |
| `_workspace/03_plan/03_planner_plan.md`, `03_critic_review_r{N}.md`, `freeze_03.sha` | 기획 초안·비평·동결 해시 | 제외 |
| `_workspace/05_build/tasks.json`, `05_qa_{기능 id}.md`, `shots/` | 구현 작업 목록, 기능별 QA 보고서, 스크린샷 | 제외 |
| `_workspace/06_submission/06_qa_submission.md` | 제출 점검 보고서 | 제외 |
| `docs/research-digest.md` | 리서치 요약(최종) | 포함 |
| `docs/idea-shortlist.md` | 아이디어 후보와 순위(최종) | 포함 |
| `docs/app-plan.md` | 확정 기획 문서(최종). 구현의 기준 | 포함 |

`_workspace/`는 사후 검증을 위해 지우지 않는다. 이 환경에는 공유 작업 목록 도구(TaskCreate)가 없으므로 작업 목록은 `_workspace/05_build/tasks.json` 파일로 관리한다.

## 0단계: 기존 작업 확인

1. 기준일을 정한다: `date +%F`. 워크플로 스크립트는 날짜 함수를 쓸 수 없으므로 `args.today`로 넘긴다. 저장소 루트의 절대 경로도 확인한다: `git rev-parse --show-toplevel`. `Workflow`의 `scriptPath`는 작업 디렉터리 기준으로 풀리므로, 아래 단계의 스크립트 경로는 항상 `{루트}/.claude/skills/pleos-app-studio/scripts/…` 절대 경로로 넘긴다.
2. `_workspace/run_meta.json`과 `docs/`를 읽고 요청에 맞는 시작 단계를 고른다.

| 요청 | 조건 | 시작 |
|---|---|---|
| 아이디어부터 / 처음부터 | 없음 | 1단계 |
| 리서치만 다시, 특정 축 보강 | – | 1단계(해당 축만 `args.axes`로) |
| 아이디어 다시·더·다른 방향 | `docs/research-digest.md` 있음 | 2단계(`previousTitles`, `direction` 전달) |
| 아이디어 고르기 | `docs/idea-shortlist.md` 있음 | 3단계 |
| 기획서 작성·수정 | 선택안 있음 | 4단계(수정이면 기존 planner에 SendMessage, 없으면 새로 실행하고 기존 문서 경로 전달) |
| 기능 구현·추가 | `docs/app-plan.md` 있음 | 5단계 |
| 제출 점검 | – | 6단계 |

3. 앞 단계 산출물이 없으면 그 단계부터 실행할지 사용자에게 묻는다. 새 입력으로 처음부터 다시 하면 기존 `_workspace/`를 `_workspace_{YYYYMMDD-HHMM}/`로 옮긴 뒤 시작한다.
4. 직전 워크플로를 일부만 고쳐 다시 돌릴 때는 `run_meta.json`의 runId로 `resumeFromRunId`를 쓴다. 바뀌지 않은 `agent()` 호출은 캐시 결과가 그대로 쓰인다.

## 1단계: 리서치

**실행 모드:** 워크플로 조율

1. `.claude/skills/pleos-research/references/axes.json`을 읽어 배열 그대로 `args.axes`로 쓴다. 사용자가 특정 축이나 질문을 요청하면 해당 축만 넘기거나 질문을 더한다.
2. 실행한다.
   ```
   Workflow({ scriptPath: "{루트}/.claude/skills/pleos-app-studio/scripts/research.workflow.js",
              args: { axes: [...], today: "YYYY-MM-DD", thorough: false } })
   ```
   완료 알림을 기다린다. 결과를 받기 전에 내용을 짐작해 쓰지 않는다.
3. 반환값을 `_workspace/01_research/01_workflow_result.json`에 저장하고 runId를 `run_meta.json`에 기록한다. 완료 알림이 가리키는 output 파일은 `{summary, agentCount, logs, result, …}`로 감싼 JSON이므로 `result` 키를 꺼내 저장한다(알림 본문의 결과는 잘려 있다).
4. `docs/research-digest.md`를 쓴다. 형식:
   - 맨 위: 기준일, 축별 완료 여부, 확인/반박/불확실/미검증 개수, 실패한 축
   - 축마다: **확인된 사실**(`confirmed`, 발견 사항 id·주장·출처 링크), **불확실**(`uncertain`, "⚠ 미확인" 표시), **반박되어 고친 내용**(`refuted`의 `correction`, 원 주장은 지운다), **미검증 참고**(`unverified`, 정성 근거로만), **시사점**
   - 끝: 누락 검토가 찾은 항목, 조사 축별 `gaps`
5. 사용자에게 핵심 발견 5개와 누락 항목을 짧게 보고하고, 이어서 2단계로 간다. 누락 항목이 기획에 꼭 필요해 보이면 그 축만 다시 조사할지 묻는다.

## 2단계: 아이디어

**실행 모드:** 워크플로 조율

1. 실행한다.
   ```
   Workflow({ scriptPath: "{루트}/.claude/skills/pleos-app-studio/scripts/ideation.workflow.js",
              args: { digestPath: "docs/research-digest.md", today: "YYYY-MM-DD",
                      ideasPerAngle: 4, topN: 5, direction: <사용자 방향 또는 생략>,
                      previousTitles: <다시 뽑기면 기존 아이디어 제목 목록> } })
   ```
   관점(`angles`), 렌즈(`lenses`), 가중치(`weights`)는 스크립트 기본값을 쓴다. 가중치의 근거는 `pleos-idea-evaluation` 스킬에 있다.
2. 반환값을 `_workspace/02_ideation/02_workflow_result.json`에 저장하고 runId를 기록한다.
3. `docs/idea-shortlist.md`를 쓴다.
   - 상위 N개 비교표: 순위, id, 제목, 총점, contest·feasibility·safety 점수, 한 줄 소개, 핵심 차량 신호, 가장 큰 리스크
   - 상위안마다 아이디어 카드 전체와 렌즈별 심사평
   - 통과 조건 탈락안: 제목과 탈락 사유(아이디어를 버리는 이유를 남겨야 같은 안이 다시 나오지 않는다)
   - 정리 기록(`mergeLog`), 실패한 관점·렌즈

## 3단계: 선택

**실행 모드:** 메인 직접

`AskUserQuestion`으로 상위 3개 중 하나를 고르게 한다. 선택지마다 한 줄 소개, 총점, 가장 큰 리스크를 보여 준다. 사용자가 다른 안, 두 안의 조합, 수정 방향을 말하면 그대로 받아 카드를 고쳐 쓴다(조합이면 두 카드를 합친 카드를 메인이 직접 작성하고, 출처 카드 id를 남긴다). 선택한 카드를 `_workspace/02_ideation/02_selected.json`에 저장하고 `run_meta.json`에 기록한다.

## 4단계: 기획

**실행 모드:** 지속형 에이전트 협업

1. 기획자를 실행한다. `Agent`를 `subagent_type: "product-planner"`로 호출하고(환경이 `name` 매개변수를 지원하면 `name: "planner"`), 프롬프트에 다음을 넣는다: 선택 카드 경로, `docs/research-digest.md`, 기준일, 산출 경로 `_workspace/03_plan/03_planner_plan.md`, 사용자 요청 사항. 결과에 나온 에이전트 ID를 `run_meta.json`에 기록한다. `SendMessage`가 지연 로딩 도구면 `ToolSearch`의 `select:SendMessage`로 불러온다.
2. 기획자 완료 알림을 받으면 비평가를 실행한다: `subagent_type: "plan-critic"`, 기획 문서 경로, 회차 1, 산출 경로 `_workspace/03_plan/03_critic_review_r1.md`. 에이전트 ID를 기록한다.
3. 판정이 `FIX`면 기획자에게 `SendMessage`로 리뷰 경로와 BLOCKER·MAJOR 목록을 보내 해당 절만 고치게 한다. 고친 뒤 비평가에게 `SendMessage`로 2회차 검토를 요청한다. 검토는 최대 2회다. 2회차에도 BLOCKER가 남으면 남은 BLOCKER와 선택지(그대로 진행 / 기능 축소 / 다른 아이디어)를 사용자에게 묻는다.
4. **산출물 동결.** `PASS`가 나오면 기획자에게 동결을 알리고("이후 수정은 `_v2.md` 새 파일로"), `shasum _workspace/03_plan/03_planner_plan.md > _workspace/03_plan/freeze_03.sha`를 기록한 뒤 문서를 `docs/app-plan.md`로 복사한다.
5. 사용자에게 핵심 결정 요약, MVP 기능 목록, 일정, 남은 MAJOR를 보여 주고 구현으로 넘어갈지 확인받는다.

## 5단계: 구현

**실행 모드:** 지속형 에이전트 협업

1. 시작 전에 `shasum -c _workspace/03_plan/freeze_03.sha`로 기획 문서가 동결 뒤 바뀌지 않았는지 확인한다(바뀌었으면 `docs/app-plan.md`를 다시 복사하고 사용자에게 알린다).
2. `docs/app-plan.md`의 기능 명세에서 MVP 기능을 순서대로 `_workspace/05_build/tasks.json`에 적는다: `[{"id":"F1","name":"…","status":"todo","fixRounds":0}]`. 사용자가 특정 기능만 요청하면 그것만 적는다.
3. 개발자(`subagent_type: "pleos-android-dev"`)와 QA(`subagent_type: "pleos-qa"`)를 처음 한 번 실행하고, 이후 기능은 같은 에이전트에 `SendMessage`로 이어서 지시한다. 에이전트 ID를 `run_meta.json`에 기록한다.
4. 기능마다 반복한다.
   1. 개발자에게 기능 id와 명세 절을 보내 구현하게 한다. `tasks.json` 상태를 `doing`으로 바꾼다.
   2. 개발자 완료 보고(바뀐 파일·빌드·스크린샷)를 받으면 QA에게 검증을 요청한다. QA는 실패 항목을 개발자에게 직접 보내고 판정을 리더에게 보고한다.
   3. `FAIL`이면 개발자가 고친 뒤 QA가 재검증한다. 기능당 수정은 최대 2회다. 그래도 실패하면 `tasks.json`에 `blocked`와 사유를 쓰고 사용자에게 진행 여부를 묻는다.
   4. `PASS`면 `done`으로 바꾸고 다음 기능으로 간다.
5. MVP가 모두 끝나면 `./gradlew copyReleaseApk`로 제출용 APK를 만들고, 기능별 결과와 스크린샷 경로를 사용자에게 보고한다.
6. 커밋은 하지 않는다. 커밋할지 사용자에게 묻고, 요청하면 저장소 로컬 작성자 설정(`TEAM`)을 그대로 쓴다.

## 6단계: 제출 점검

**실행 모드:** 서브에이전트 위임(단발 호출)

`Agent`를 `subagent_type: "pleos-qa"`로 한 번 호출해 `pleos-qa-check`의 「제출 점검 순서」를 실행하게 하고, 보고서를 `_workspace/06_submission/06_qa_submission.md`에 쓰게 한다. `REVIEW` 항목(커밋 작성자, 문서 속 실명)과 `WARN` 항목(임시 식별자)은 사용자에게 그대로 보여 주고 확인받는다. 기획서 초안은 `docs/app-plan.md` 1절에서 접수양식 DOCX로 옮겨 적는 일이 남았다고 알린다(개인정보 동의서가 같은 파일에 있으므로 사용자가 직접 작성한다).

## 데이터 전달

| 단계 경계 | 방법 | 내용 |
|---|---|---|
| 메인 → 1단계 | `args` | `axes.json`, 기준일, 규모 |
| 1단계 → 2단계 | 파일 | `docs/research-digest.md`(워크플로 반환값을 메인이 요약) |
| 2단계 → 3단계 | 파일 + 반환값 | `02_workflow_result.json`의 `ranked`, `docs/idea-shortlist.md` |
| 3단계 → 4단계 | 파일 | `02_selected.json` |
| 4단계 내부 | 파일 + SendMessage | 기획 문서·리뷰 파일 경로를 메시지로 전달 |
| 4단계 → 5단계 | 동결 파일 | `docs/app-plan.md` + `freeze_03.sha` |
| 5단계 내부 | 파일 + SendMessage | `tasks.json`, QA 보고서, 개발자↔QA 직접 메시지 |
| 5단계 → 6단계 | 저장소 상태 | 작업 트리, `apk/` |

## 오류 처리

| 상황 | 대응 |
|---|---|
| 워크플로의 일부 `agent()`가 `null` | 스크립트가 걸러 내고 `log()`로 알린다. 반환값의 `skippedAxes`·`failedAngles`·`missingLenses`를 요약 문서와 사용자 보고에 밝힌다 |
| 워크플로 전체 실패 | 실행 기록 디렉터리의 `journal.jsonl`에서 실제 반환값을 확인하고, 원인을 고친 뒤 `resumeFromRunId`로 이어서 실행한다 |
| 결과가 비어 있음 | 성공으로 보지 않는다. `journal.jsonl`을 열어 각 에이전트의 실제 반환값을 확인한다 |
| 조사원이 웹 도구를 못 씀(`gaps`에 "웹 조사 불가") | 키트 근거만으로 진행하되 요약 문서 맨 위에 "웹 조사 미수행 축"을 밝히고, 사용자에게 다시 돌릴지 묻는다 |
| 사용량 한도 소진·인증 만료·권한 거부 | 다시 시도하지 않는다. 부분 산출물을 열어 어디까지 됐는지 확인하고 누락을 `_workspace/`에 기록한 뒤 사용자에게 보고한다(한도라면 풀리는 시각도). 메인은 직접 확인한 사실만 채우고 에이전트의 판단은 추측해 채우지 않는다 |
| 사용자 정의 에이전트 유형을 찾지 못함(정의를 만든 세션에서는 아직 등록되지 않는 경우가 있다) | 워크플로 스크립트는 정의 파일을 읽게 한 기본 에이전트로 한 번 자동 재시도한다. 4~6단계의 `Agent` 호출이 실패하면 `subagent_type: "general-purpose"`에 "먼저 `.claude/agents/{유형}.md`를 읽고 그 역할과 원칙을 따르라"를 붙이고, 정의 파일의 `model`을 지정해 실행한다 |
| 지속형 에이전트가 응답하지 않음 | `SendMessage`로 상태를 묻고 다시 지시한다. 그래도 안 되면 같은 `subagent_type`을 새로 실행하고 이전 산출물 경로와 진행 상황을 프롬프트로 넘긴다 |
| 비평·QA가 2회 뒤에도 실패 | 남은 문제와 선택지를 사용자에게 묻는다. 무한 반복하지 않는다 |
| 서로 충돌하는 자료 | 지우지 않고 출처와 함께 나란히 적는다 |
| 에뮬레이터가 꺼져 있음 | `pleos-feature-dev/scripts/emu.sh boot`. 띄울 수 없으면 에뮬레이터 항목을 미검증으로 남긴다 |

## 테스트 시나리오

### 정상 흐름
1. 사용자: "Pleos로 공모전에 낼 차량 앱 아이디어부터 같이 찾아줘."
2. 0단계: `_workspace/`가 없어 1단계부터. 기준일 기록.
3. 1단계: 축 5개 조사·검증·누락 검토 → `docs/research-digest.md`.
4. 2단계: 관점 5개 × 4개 → 정리 → 렌즈 3개 심사 → `docs/idea-shortlist.md`(상위 5개).
5. 3단계: 사용자가 2위 안을 고르고 "주행 중 화면을 더 단순하게"라고 덧붙임 → 카드 수정·저장.
6. 4단계: 기획 → 비평 1회차 `FIX`(C2: API 이름 불일치) → 수정 → 2회차 `PASS` → 동결 → `docs/app-plan.md`. 사용자가 구현 진행을 승인.
7. 5단계: F1~F3 구현·QA → `copyReleaseApk`. 6단계 점검 보고.

### 오류 흐름
1. 1단계에서 `market` 축 조사원이 실패해 `null`을 반환한다.
2. 스크립트가 `skippedAxes: ["market"]`를 반환하고 `log()`로 알린다.
3. 메인은 요약 문서 맨 위에 "시장·경쟁 축 미조사"를 적고 사용자에게 그 축만 다시 돌릴지 묻는다.
4. 사용자가 승인하면 `args.axes`에 `market`만 넣어 1단계를 다시 실행하고, 결과를 기존 요약에 합친다.
