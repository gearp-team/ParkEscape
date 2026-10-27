#!/usr/bin/env python3
"""매니페스트·README의 Pleos 퍼미션을 앱 코드가 실제로 호출하는 SDK API와 대조한다.

API_INDEX.md의 @RequiresPermission 선언을 SDK별로 읽고, app/src/main 아래 Kotlin 파일마다
import한 SDK의 메서드 호출만 골라 요구 퍼미션을 모은 뒤 다음을 확인한다.
  - 누락: 코드가 요구하지만 매니페스트에 없는 퍼미션(실행 시 SecurityException)
  - 불필요: 매니페스트에 있지만 코드가 쓰지 않는 퍼미션(미승인 퍼미션 선언은 마켓 심사 반려)
  - README 불일치: 매니페스트와 README "사용 SDK와 퍼미션" 표가 다른 퍼미션
같은 이름의 메서드가 import한 여러 SDK에 있고 그중 하나는 퍼미션이 필요 없으면(예: NaviHelper.addListener와
SpeechToText.addListener) 판정하지 않고 WARN으로 사람이 확인하게 한다.

사용법: python3 check_permissions.py [--root <repo>] [--json]
FAIL이 하나라도 있으면 종료 코드 1을 돌려준다.
"""
import argparse
import json
import re
import sys
from pathlib import Path

SDK_HEADING = re.compile(r"^## (\w+) / ")
FUN_DECL = re.compile(r"\bfun ([A-Za-z0-9_]+)\(")
PERM = re.compile(r'RequiresPermission\(value = "(pleos\.car\.permission\.[A-Z_]+)"\)')
CALL = re.compile(r"\.([A-Za-z_][A-Za-z0-9_]*)\s*\(")
MANIFEST_PERM = re.compile(r'<uses-permission[^>]*android:name="(pleos\.car\.permission\.[A-Z_]+)"')
README_PERM = re.compile(r"pleos\.car\.permission\.[A-Z_]+")
IMPORT_SDK = [
    ("ai.pleos.playground.vehicle", "Vehicle"),
    ("ai.pleos.playground.navi", "NaviHelper"),
    ("ai.pleos.playground.stt", "SpeechToText"),
    ("ai.pleos.playground.tts", "TextToSpeech"),
    ("ai.pleos.playground.location.fused", "FusedLocation"),
]


def load_api(api_index: Path) -> dict:
    """{SDK: {메서드: 퍼미션 집합}}. 퍼미션이 없는 메서드는 빈 집합이다."""
    api, sdk = {}, None
    for line in api_index.read_text(encoding="utf-8").splitlines():
        heading = SDK_HEADING.match(line)
        if heading:
            sdk = heading.group(1)
            continue
        if sdk is None or not line.startswith("- `"):
            continue
        decl = line.split("`")[1] if line.count("`") >= 2 else line
        perms = set(PERM.findall(decl))
        for name in FUN_DECL.findall(decl):
            api.setdefault(sdk, {}).setdefault(name, set()).update(perms)
    return api


def imported_sdks(text: str) -> set:
    found = set()
    for line in text.splitlines():
        if line.startswith("import "):
            for prefix, sdk in IMPORT_SDK:
                if line[len("import "):].startswith(prefix):
                    found.add(sdk)
    return found


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    root = Path(args.root).resolve()

    api_index = root / "pleos-claude-kit" / "API_INDEX.md"
    manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
    readme = root / "README.md"
    for path in (api_index, manifest, readme):
        if not path.exists():
            print(f"error: {path} not found", file=sys.stderr)
            return 2

    api = load_api(api_index)
    declared = set(MANIFEST_PERM.findall(manifest.read_text(encoding="utf-8")))
    in_readme = set(README_PERM.findall(readme.read_text(encoding="utf-8")))

    required = {}   # 메서드 → {"perms": 집합, "sites": [...]}
    ambiguous = {}  # 메서드 → {"perms": 집합, "sites": [...]}
    for kt in sorted((root / "app" / "src" / "main").rglob("*.kt")):
        text = kt.read_text(encoding="utf-8")
        sdks = imported_sdks(text)
        if not sdks:
            continue
        rel = str(kt.relative_to(root))
        for lineno, line in enumerate(text.splitlines(), 1):
            stripped = line.strip()
            if stripped.startswith(("//", "*", "/*", "import ", "package ")):
                continue
            for name in CALL.findall(line):
                candidates = [api[s][name] for s in sdks if name in api.get(s, {})]
                if not candidates or not any(candidates):
                    continue
                bucket = ambiguous if any(not c for c in candidates) else required
                entry = bucket.setdefault(name, {"perms": set(), "sites": []})
                entry["perms"].update(*candidates)
                entry["sites"].append(f"{rel}:{lineno}")

    missing = [
        {"method": n, "needs_one_of": sorted(e["perms"]), "sites": e["sites"]}
        for n, e in sorted(required.items()) if not e["perms"] & declared
    ]
    used = set().union(*(e["perms"] for e in required.values())) if required else set()
    maybe_used = set().union(*(e["perms"] for e in ambiguous.values())) if ambiguous else set()
    unused = sorted(declared - used - maybe_used)
    readme_only = sorted(in_readme - declared)
    manifest_only = sorted(declared - in_readme)
    ok = not (missing or unused or readme_only or manifest_only)

    if args.json:
        print(json.dumps({
            "ok": ok,
            "required": {n: sorted(e["perms"]) for n, e in sorted(required.items())},
            "ambiguous": {n: {"perms": sorted(e["perms"]), "sites": e["sites"]} for n, e in sorted(ambiguous.items())},
            "declared": sorted(declared), "missing": missing, "unused": unused,
            "readme_only": readme_only, "manifest_only_not_in_readme": manifest_only,
        }, ensure_ascii=False, indent=2))
    else:
        print("SDK calls requiring permissions:")
        for n, e in sorted(required.items()):
            print(f"  {n}: {', '.join(sorted(e['perms']))}")
        print(f"declared in manifest: {', '.join(sorted(declared)) or '-'}")
        for n, e in sorted(ambiguous.items()):
            print(f"WARN {n} exists in several imported SDKs; one needs {', '.join(sorted(e['perms']))} — check which SDK is called at {', '.join(e['sites'])}")
        for m in missing:
            print(f"FAIL missing permission for {m['method']} (needs one of {', '.join(m['needs_one_of'])}) at {', '.join(m['sites'])}")
        for p in unused:
            print(f"FAIL declared but unused: {p}")
        for p in readme_only:
            print(f"FAIL README lists {p} but manifest does not declare it")
        for p in manifest_only:
            print(f"FAIL manifest declares {p} but README table does not list it")
        print("PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
