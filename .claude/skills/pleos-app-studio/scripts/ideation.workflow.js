export const meta = {
  name: 'pleos-ideation',
  description: '리서치 요약에서 관점별로 Pleos 차량 앱 아이디어를 내고, 중복을 합친 뒤 세 렌즈로 심사해 순위를 매긴다',
  whenToUse: 'pleos-app-studio 아이디어 단계. args: {digestPath, today, angles?, ideasPerAngle?, lenses?, weights?, topN?, previousTitles?, direction?}',
  phases: [
    { title: '발상', detail: '관점마다 아이디어 카드를 만든다' },
    { title: '정리', detail: '사실상 같은 아이디어를 합치고 id를 다시 매긴다', model: 'sonnet' },
    { title: '심사', detail: '렌즈마다 모든 아이디어를 채점한다' },
    { title: '종합', detail: '가중 합산과 통과 조건으로 순위를 매긴다' },
  ],
}

const CATEGORIES = ['Maps/Navigation', 'Mobility Services', 'Lifestyle', 'Entertainment', 'Business', 'Music/Audio', 'Kids', 'Social/Communication']
const STR = { type: 'string' }
const STRS = { type: 'array', items: STR }
const IDEA = { type: 'object',
  required: ['id', 'title', 'oneLiner', 'persona', 'painPoint', 'coreLoop', 'vehicleSignals', 'parkedExperience', 'drivingExperience', 'demoScenario', 'category'],
  properties: {
    id: STR, title: STR, oneLiner: STR, angle: STR, persona: STR, painPoint: STR, evidenceIds: STRS,
    coreLoop: STRS,
    vehicleSignals: { type: 'array', items: { type: 'object', required: ['api', 'purpose'], properties: { api: STR, purpose: STR } } },
    naviUsage: STR, parkedExperience: STR, drivingExperience: STR, demoScenario: STR, mvpScope: STR,
    retentionHook: STR, businessModel: STR, category: { type: 'string', enum: CATEGORIES }, risks: STRS,
  } }
const IDEAS = { type: 'object', required: ['ideas'], properties: { ideas: { type: 'array', items: IDEA } } }
const MERGED = { type: 'object', required: ['ideas', 'mergeLog'], properties: { ideas: { type: 'array', items: IDEA }, mergeLog: STRS } }
const SCORES = { type: 'object', required: ['lens', 'scores'], properties: {
  lens: STR,
  scores: { type: 'array', items: { type: 'object', required: ['ideaId', 'criteria', 'gate', 'rationale'], properties: {
    ideaId: STR,
    criteria: { type: 'object', additionalProperties: { type: 'number' } },
    gate: { type: 'string', enum: ['pass', 'fail'] },
    gateReason: STR, rationale: STR } } } } }

// 기본 관점: 공모전이 제시한 니즈 4분야 + 플랫폼 역량에서 거꾸로 출발하는 관점 1개.
const angles = Array.isArray(args?.angles) && args.angles.length ? args.angles : [
  { key: 'mobility-life', desc: '모빌리티 & 라이프 편의: 출퇴근·주차·충전·일상 이동의 불편' },
  { key: 'entertainment', desc: '인카 엔터테인먼트: 정차·충전 대기·동승자의 시간(주행 중 영상·게임 금지 준수)' },
  { key: 'smart-safety', desc: '스마트 드라이빙 & 안전: 운전 습관, 차량 상태, 위험 예방' },
  { key: 'family-care', desc: '패밀리 & 동승자 케어: 아이·반려동물·고령 동승자' },
  { key: 'platform-first', desc: '플랫폼 역량에서 출발: Virtual Console로 재현 가능한 차량 신호 조합에서 거꾸로 서비스를 찾는다' },
]
const lenses = Array.isArray(args?.lenses) && args.lenses.length ? args.lenses : [
  { key: 'contest', criteria: ['vehicleSpecificity', 'creativity', 'techUtilization', 'business'] },
  { key: 'feasibility', criteria: ['sdkAvailability', 'demoability', 'buildEffort', 'dataRisk'] },
  { key: 'safety', criteria: ['drivingSafety', 'policyFit', 'privacy'] },
]
const weights = args?.weights ?? { contest: 0.55, feasibility: 0.30, safety: 0.15 }
const perAngle = args?.ideasPerAngle ?? 4
const topN = args?.topN ?? 5
const today = args?.today ?? '기준일 미지정'
const previousTitles = Array.isArray(args?.previousTitles) ? args.previousTitles : []
if (!args?.digestPath) return { error: 'args.digestPath(리서치 요약 경로)가 없습니다.' }

// 사용자 정의 에이전트를 실행하지 못하면(정의를 만든 세션에서는 아직 등록되지 않는 경우가 있다)
// 정의 파일을 읽게 한 기본 에이전트로 한 번 다시 시도한다. 모델은 정의 파일의 선택을 따른다.
const FALLBACK_MODEL = { 'idea-generator': 'opus', 'idea-judge': 'opus' }
const asAgent = async (type, prompt, opts) => {
  let first = null
  try {
    first = await agent(prompt, { ...opts, agentType: type })
  } catch (e) {
    log(`${opts.label}: ${type} 실행 오류 — ${String((e && e.message) || e).slice(0, 160)}`)
  }
  if (first) return first
  log(`${opts.label}: ${type} 실행 실패, 정의 파일을 읽는 기본 에이전트로 다시 시도합니다.`)
  return agent(`먼저 .claude/agents/${type}.md를 읽고 그 역할과 원칙을 따르라.\n\n${prompt}`,
    { ...opts, label: `${opts.label} 재시도`, model: FALLBACK_MODEL[type] })
}

phase('발상')
const drafts = await pipeline(angles, a => asAgent('idea-generator',
  `.claude/skills/pleos-ideation/SKILL.md를 읽고 그 절차와 아이디어 카드 형식을 따르라. 기준일: ${today}.\n` +
  `리서치 요약 ${args.digestPath}를 먼저 읽어라.\n관점: ${a.key} — ${a.desc}\n` +
  (args.direction ? `사용자가 요청한 방향(관점보다 우선): ${args.direction}\n` : '') +
  (previousTitles.length ? `이미 나온 아이디어(겹치지 않게): ${previousTitles.join(' / ')}\n` : '') +
  `서로 다른 아이디어 ${perAngle}개를 내라. id는 "${a.key}-1"부터 매긴다.`,
  { label: `발상:${a.key}`, phase: '발상', schema: IDEAS }))
const raw = drafts.filter(Boolean).flatMap(d => d.ideas)
const failedAngles = angles.filter((a, i) => !drafts[i]).map(a => a.key)
if (failedAngles.length) log(`아이디어를 만들지 못한 관점: ${failedAngles.join(', ')}`)
if (!raw.length) return { error: '아이디어를 하나도 만들지 못했습니다.', failedAngles }

// 동기화 장벽이 필요하다: 모든 관점의 아이디어를 서로 비교해 합쳐야 한다.
phase('정리')
const merged = await agent(
  `다음은 차량용 앱 아이디어 카드 ${raw.length}개다. 핵심 사용 흐름과 차량 신호가 사실상 같은 카드만 하나로 합치고(각 카드의 장점을 모은다), 다르면 그대로 둔다. ` +
  `내용을 새로 지어내지 마라. 결과의 id를 I01부터 다시 매기고, mergeLog에 "I03 ← family-care-2 + smart-safety-1"처럼 모든 카드의 대응을 남겨라.\n${JSON.stringify(raw)}`,
  { label: '정리', phase: '정리', schema: MERGED, model: 'sonnet' })
let ideas = merged && merged.ideas.length ? merged.ideas : null
if (!ideas) {
  log('정리 단계가 실패해 합치지 않은 원본으로 심사합니다.')
  ideas = raw.map((x, i) => ({ ...x, id: 'I' + String(i + 1).padStart(2, '0') }))
}
log(`아이디어 ${raw.length}개를 ${ideas.length}개로 정리했습니다.`)

phase('심사')
const judged = (await parallel(lenses.map(l => () => asAgent('idea-judge',
  `.claude/skills/pleos-idea-evaluation/SKILL.md를 읽고 「${l.key}」 렌즈의 기준(${l.criteria.join(', ')})과 그 렌즈의 통과 조건만 적용해 아래 아이디어를 모두 채점하라. ` +
  `모든 아이디어를 먼저 읽은 뒤 채점하라. criteria의 키는 위 기준 키를 그대로 쓴다.\n${JSON.stringify(ideas)}`,
  { label: `심사:${l.key}`, phase: '심사', schema: SCORES })
  .then(r => (r ? { ...r, lens: l.key } : null))))).filter(Boolean)

phase('종합')
const missingLenses = lenses.map(l => l.key).filter(k => !judged.some(j => j.lens === k))
if (missingLenses.length) log(`채점에 실패한 렌즈: ${missingLenses.join(', ')} — 남은 렌즈로 가중치를 다시 나눕니다.`)
if (!judged.length) return { error: '모든 렌즈의 채점이 실패했습니다.', ideas, mergeLog: merged ? merged.mergeLog : [] }

const round2 = x => Math.round(x * 100) / 100
const rows = ideas.map(idea => {
  const perLens = {}, notes = {}, criteria = {}, gates = []
  for (const j of judged) {
    const s = j.scores.find(x => x.ideaId === idea.id)
    if (!s) continue
    const vals = Object.values(s.criteria).filter(Number.isFinite)
    if (vals.length) perLens[j.lens] = round2(vals.reduce((a, b) => a + b, 0) / vals.length)
    criteria[j.lens] = s.criteria
    notes[j.lens] = s.rationale
    if (s.gate === 'fail') gates.push(`${j.lens}: ${s.gateReason || '사유 없음'}`)
  }
  const scored = Object.keys(perLens)
  const w = scored.reduce((sum, k) => sum + (weights[k] ?? 0), 0)
  const total = w ? round2(scored.reduce((sum, k) => sum + (weights[k] ?? 0) * perLens[k], 0) / w) : null
  return { id: idea.id, title: idea.title, total, perLens, criteria, gates, notes, lensesScored: scored.length }
})
const ranked = rows.filter(r => !r.gates.length && r.total !== null).sort((a, b) => b.total - a.total)
const rejected = rows.filter(r => r.gates.length)
const unscored = rows.filter(r => !r.gates.length && r.total === null)
log(`순위 ${ranked.length}개, 통과 조건 탈락 ${rejected.length}개, 채점 누락 ${unscored.length}개. 상위 ${Math.min(topN, ranked.length)}개를 추천합니다.`)

return {
  today, weights, ideas, ranked,
  top: ranked.slice(0, topN).map(r => r.id),
  rejected, unscored, missingLenses, failedAngles,
  mergeLog: merged ? merged.mergeLog : [],
}
