export const meta = {
  name: 'pleos-research',
  description: 'Pleos 차량 앱 아이디어를 위한 축별 리서치, 반박 중심 사실 검증, 누락 검토',
  whenToUse: 'pleos-app-studio 리서치 단계. args: {axes, today, thorough?, maxFindingsPerAxis?, maxClaimsPerAxis?}',
  phases: [
    { title: '조사', detail: '축마다 키트와 웹 자료를 조사한다' },
    { title: '검증', detail: '축마다 결정에 중요한 주장을 반박 관점에서 검증한다' },
    { title: '누락 검토', detail: '빠진 관점과 근거 없는 항목을 찾는다', model: 'sonnet' },
  ],
}

const EVIDENCE = { type: 'object', required: ['source', 'grade'], properties: {
  source: { type: 'string' }, title: { type: 'string' }, date: { type: 'string' },
  grade: { type: 'string', enum: ['A', 'B', 'C'] } } }
const FINDINGS = { type: 'object', required: ['axis', 'findings', 'gaps'], properties: {
  axis: { type: 'string' },
  findings: { type: 'array', items: { type: 'object',
    required: ['id', 'claim', 'evidence', 'implication', 'decisionCritical'], properties: {
      id: { type: 'string' }, claim: { type: 'string' }, detail: { type: 'string' },
      evidence: { type: 'array', minItems: 1, items: EVIDENCE },
      implication: { type: 'string' }, decisionCritical: { type: 'boolean' } } } },
  gaps: { type: 'array', items: { type: 'string' } } } }
const VERDICTS = { type: 'object', required: ['verdicts'], properties: {
  verdicts: { type: 'array', items: { type: 'object', required: ['id', 'status', 'reason'], properties: {
    id: { type: 'string' },
    status: { type: 'string', enum: ['confirmed', 'refuted', 'uncertain'] },
    reason: { type: 'string' }, correction: { type: 'string' } } } } } }
const MISSING = { type: 'object', required: ['missing'], properties: {
  missing: { type: 'array', items: { type: 'object', required: ['topic', 'why'], properties: {
    topic: { type: 'string' }, why: { type: 'string' }, suggestedAxis: { type: 'string' } } } } } }

const SKILL = '.claude/skills/pleos-research/SKILL.md'

// 사용자 정의 에이전트를 실행하지 못하면(정의를 만든 세션에서는 아직 등록되지 않는 경우가 있다)
// 정의 파일을 읽게 한 기본 에이전트로 한 번 다시 시도한다. 모델은 정의 파일의 선택을 따른다.
const FALLBACK_MODEL = { 'mobility-researcher': 'sonnet', 'fact-checker': 'opus' }
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
const axes = Array.isArray(args?.axes) ? args.axes : []
if (!axes.length) return { error: '조사 축이 없습니다. args.axes에 pleos-research/references/axes.json의 내용을 넘긴다.' }
const today = args.today ?? '기준일 미지정'
const thorough = args.thorough === true
const maxFindings = args.maxFindingsPerAxis ?? 8
const maxClaims = args.maxClaimsPerAxis ?? (thorough ? 6 : 3)
// 기본은 축마다 검증자 1명이 모든 핵심 주장을 검증한다. "철저히"면 관점이 다른 검증자 3명의 과반 판정을 쓴다.
const verifierLenses = thorough
  ? ['출처를 직접 열어 수치·단위·기준 연도가 맞는지', '자료의 최신성과 맥락 왜곡(글로벌↔국내, 예측↔실측)', 'Pleos 키트와 프로젝트 CLAUDE.md의 플랫폼 사실과 맞는지']
  : ['출처·수치·최신성·플랫폼 사실 전반']
const majority = Math.floor(verifierLenses.length / 2) + 1

const perAxis = await pipeline(
  axes,
  axis => asAgent('mobility-researcher',
    `${SKILL}를 읽고 그 절차와 출처 기준을 따르라. 기준일: ${today}.\n` +
    `조사 축(JSON): ${JSON.stringify(axis)}\n` +
    `발견 사항은 최대 ${maxFindings}건, 그중 decisionCritical은 최대 ${maxClaims}건만 표시하라. ` +
    `발견 사항 id는 "${axis.key}-01"부터 매긴다.`,
    { label: `조사:${axis.key}`, phase: '조사', schema: FINDINGS }),
  (found, axis) => {
    if (!found) return null
    const flagged = found.findings.filter(f => f.decisionCritical)
    const critical = flagged.slice(0, maxClaims)
    if (flagged.length > critical.length) log(`${axis.key}: 핵심 주장 ${flagged.length}건 중 ${critical.length}건만 검증합니다(나머지는 미검증으로 표시).`)
    if (!critical.length) return { axis: axis.key, found, votes: [] }
    return parallel(verifierLenses.map((lens, i) => () => asAgent('fact-checker',
      `${SKILL}의 「사실 검증」 절을 따르라. 기준일: ${today}. 검증 관점: ${lens}.\n` +
      `다음 주장들을 하나씩 반박하려는 관점에서 검증하고, 주장마다 판정을 반환하라:\n${JSON.stringify(critical)}`,
      { label: `검증:${axis.key}${verifierLenses.length > 1 ? '#' + (i + 1) : ''}`, phase: '검증', schema: VERDICTS })))
      .then(sets => ({ axis: axis.key, found, votes: sets.filter(Boolean) }))
  },
)

const done = perAxis.filter(Boolean)
const skippedAxes = axes.map(a => a.key).filter(k => !done.some(d => d.axis === k))
if (skippedAxes.length) log(`조사에 실패한 축 ${skippedAxes.length}개: ${skippedAxes.join(', ')}`)

const axesOut = done.map(d => {
  const findings = d.found.findings.map(f => {
    const verdicts = d.votes.map(v => v.verdicts.find(x => x.id === f.id)).filter(Boolean)
    if (!verdicts.length) return { ...f, verification: 'unverified', verdicts: [] }
    const count = s => verdicts.filter(v => v.status === s).length
    const verification = count('confirmed') >= majority ? 'confirmed'
      : count('refuted') >= majority ? 'refuted' : 'uncertain'
    return { ...f, verification, verdicts }
  })
  return { axis: d.axis, findings, gaps: d.found.gaps, verifiersReturned: d.votes.length }
})

phase('누락 검토')
const summary = axesOut.map(a => ({
  axis: a.axis,
  claims: a.findings.map(f => `${f.id} [${f.verification}] ${f.claim}`),
  gaps: a.gaps,
}))
const missing = await agent(
  `차량용 앱(Pleos Connect, 현대자동차그룹 차량용 앱 공모전) 아이디어를 내기 위한 리서치 결과 요약이다. ` +
  `조사 축: ${axes.map(a => `${a.key}(${a.title})`).join(', ')}. 기준일: ${today}.\n` +
  `빠진 관점, 검증하지 않은 중요한 주장, 기획서의 기획 배경·사업성(타깃 규모, 예상 MAU, Retention)에 필요한데 근거가 없는 항목만 찾아라. ` +
  `이미 있는 내용을 다시 요약하지 마라.\n${JSON.stringify(summary)}`,
  { label: '누락 검토', phase: '누락 검토', schema: MISSING, model: 'sonnet' })
if (!missing) log('누락 검토 결과를 받지 못했습니다.')

const all = axesOut.flatMap(a => a.findings)
const stats = {
  axesRequested: axes.length,
  axesDone: done.length,
  skippedAxes,
  findings: all.length,
  confirmed: all.filter(f => f.verification === 'confirmed').length,
  refuted: all.filter(f => f.verification === 'refuted').length,
  uncertain: all.filter(f => f.verification === 'uncertain').length,
  unverified: all.filter(f => f.verification === 'unverified').length,
}
log(`발견 사항 ${stats.findings}건: 확인 ${stats.confirmed}, 반박 ${stats.refuted}, 불확실 ${stats.uncertain}, 미검증 ${stats.unverified}`)
return { today, thorough, axes: axesOut, missing: missing ? missing.missing : null, stats }
