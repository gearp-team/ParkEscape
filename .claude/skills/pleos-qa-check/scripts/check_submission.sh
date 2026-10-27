#!/usr/bin/env bash
# 공모전 제출물 점검: release APK 파일명·서명·패키지, README 필수 항목, 임시 식별자, 비밀값·실명 노출.
# 사용법: check_submission.sh   (저장소 루트에서 실행. 먼저 ./gradlew copyReleaseApk)
# FAIL이 하나라도 있으면 종료 코드 1. WARN과 REVIEW는 사람이 확인할 항목이다.
set -uo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT" || exit 2
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/35.0.0"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export JAVA_HOME
fails=0

pass() { echo "PASS   $*"; }
fail() { echo "FAIL   $*"; fails=$((fails + 1)); }
warn() { echo "WARN   $*"; }
review() { echo "REVIEW $*"; }

prop() { sed -n "s/^$1=//p" gradle.properties | head -1; }
team=$(prop pleos.teamName)
app=$(prop pleos.appName)
app_id=$(sed -n 's/.*applicationId *= *"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)
apk="apk/${team}_${app}_release.apk"

# 1. APK
if [ -f "$apk" ]; then
  pass "APK exists: $apk"
  size=$(stat -f %z "$apk" 2>/dev/null || stat -c %s "$apk")
  if [ "$size" -lt 100000000 ]; then pass "APK size $((size / 1048576))MB < 100MB (GitHub file limit)"; else fail "APK is ${size} bytes; upload to GitHub Releases instead"; fi
  if "$BT/apksigner" verify "$apk" >/dev/null 2>&1; then pass "APK signature verifies"; else fail "APK signature does not verify"; fi
  badging=$("$BT/aapt2" dump badging "$apk" 2>/dev/null)
  pkg=$(printf '%s\n' "$badging" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  if [ "$pkg" = "$app_id" ]; then pass "APK package $pkg matches applicationId"; else fail "APK package '$pkg' != applicationId '$app_id' (stale APK? run ./gradlew copyReleaseApk)"; fi
  if printf '%s\n' "$badging" | grep -q "application-debuggable"; then fail "APK is debuggable (must be a release build)"; else pass "APK is not debuggable"; fi
else
  fail "APK missing: $apk (run ./gradlew copyReleaseApk)"
fi
other=$(ls apk/*.apk 2>/dev/null | grep -v "^$apk$" || true)
[ -z "$other" ] || warn "other APKs in apk/: $other"

# 2. README 필수 항목(공모전 GitHub 제출 안내)
if [ -f README.md ]; then
  head -1 README.md | grep -q '^# ' && pass "README has app name heading" || fail "README first line is not '# <앱 이름>'"
  grep -q '한 줄 소개' README.md && pass "README has 한 줄 소개" || fail "README missing 한 줄 소개"
  grep -q '한 줄 소개: (작성 예정)' README.md && fail "README 한 줄 소개 is still a placeholder"
  grep -q '실행 방법' README.md && pass "README has 실행 방법" || fail "README missing 실행 방법"
  grep -q '테스트 환경' README.md && pass "README has 테스트 환경" || fail "README missing 테스트 환경 및 필요한 설정"
  grep -q 'apk/' README.md && pass "README points to apk/" || fail "README missing APK 위치 또는 Release 링크"
else
  fail "README.md missing"
fi

# 3. 임시 식별자
[ "$team" = "TEAM" ] && warn "pleos.teamName is still the placeholder 'TEAM'"
case "$app_id" in com.team.*|com.example.*) warn "applicationId '$app_id' looks temporary; change it before Pleos Playground registration";; esac
[ "$app" = "PleosApp" ] && warn "app name 'PleosApp' is temporary and contains the Pleos trademark"

# 4. 비밀값·서명 키가 git에 들어가지 않았는지
tracked_secrets=$(git ls-files | grep -E '(^|/)(keystore\.properties|local\.properties)$|\.jks$|\.keystore$|^keystore/' || true)
[ -z "$tracked_secrets" ] && pass "no keystore/local.properties tracked" || fail "secret files tracked by git: $tracked_secrets"
secret_hits=$(git grep -n -I -E 'storePassword *= *[^k ]|keyPassword *= *[^k ]|client_?[sS]ecret *[:=] *"[^"]+|BEGIN (RSA |EC )?PRIVATE KEY' -- . ':!*.md' 2>/dev/null || true)
[ -z "$secret_hits" ] && pass "no hard-coded secrets in tracked files" || fail "possible secrets: $secret_hits"
for p in '/pleos-claude-kit/' '/pleos-claude-kit.zip' '/keystore/' '/keystore.properties'; do
  grep -qxF "$p" .gitignore && pass ".gitignore has $p" || fail ".gitignore missing $p"
done

# 5. 블라인드 규정: 사람이 확인
review "commit authors (must not reveal real names):"
git log --format='         %an <%ae>' | sort -u
review "README/docs must not contain real names, contacts, affiliations, or personal account names"

echo
if [ "$fails" -eq 0 ]; then echo "RESULT PASS"; else echo "RESULT FAIL ($fails)"; fi
[ "$fails" -eq 0 ]
