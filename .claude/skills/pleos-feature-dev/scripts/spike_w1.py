#!/usr/bin/env python3
"""SPIKE-W1 측정 드라이버. spike_w1.sh가 부른다(직접 실행해도 된다).

debug 빌드의 SpikeActivity(com.team.pleosapp/.spike.SpikeActivity)가 남기는 logcat 태그 SPIKE와
홈 화면(HOME_SCREEN TaskViewContainer) 로그를 Virtual Console 조작 시각과 맞춰 본다.

  spike_w1.py signals   퍼즐 신호 8종 + 좌석 점유를 VC로 바꾸고 콜백 값·지연을 잰다
  spike_w1.py data      signals와 같은 조작을 F2-data Repository 4종(DataCheckActivity, 태그 DATA)으로 보고 Flow 값 변화를 본다
  spike_w1.py ambswitch VC 앰비언트 스위치를 바꾸며 getAmbientLightSwitch 값과 L1·L2 콜백을 본다
  spike_w1.py gate      정차↔공회전↔주행 전환 때 앱 규제 콜백·화면 그리기·홈 차단 로그 시각을 잰다
  spike_w1.py record    공회전 전환·정차 복귀를 화면 녹화해 프레임마다 앱 영역 색(퍼즐이 흐림 뒤에 남는지)을 본다
  spike_w1.py touch     차단 화면이 덮인 동안 앱 버튼 좌표에 input tap을 보내 클릭이 오는지 본다
  spike_w1.py appswitch 다른 앱(차량 설정)으로 갔다 돌아올 때 규제 값(A: 상시 리스너, B: STARTED 동안 Flow)을 본다
  spike_w1.py window [split|join]  윈도 바를 끌어 세로 1단(1:1)으로 나누거나 되돌린 뒤 앱 창 크기(dp)와 TaskView 경계를 출력한다

출력: _workspace/05_build/spike_out/<명령>_<시각>/ 아래에 raw logcat, steps.tsv, result.txt
시각: 기기 시계와 호스트 시계가 다르므로 `adb shell date +%s%N`으로 차이(offset)를 재서 호스트 시각을 기기 시각으로 바꾼다.
"""
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.parse
import urllib.request

SDK = os.environ.get("ANDROID_HOME", os.path.expanduser("~/Library/Android/sdk"))
ADB = os.path.join(SDK, "platform-tools", "adb")
PORT = int(os.environ.get("VC_PORT", "8088"))
BASE = f"http://127.0.0.1:{PORT}"
PKG = "com.team.pleosapp"
SPIKE = f"{PKG}/.spike.SpikeActivity"
SETTINGS = "ai.umos.vehiclecontrol/.VehicleControlActivity"
ROOT = subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip() or os.getcwd()
OUT_ROOT = os.path.join(ROOT, "_workspace", "05_build", "spike_out")


# ---- 기본 도구 ------------------------------------------------------------------------------
def now_ms() -> float:
    return time.time() * 1000.0


def adb(*args, check=True) -> str:
    r = subprocess.run([ADB, *args], capture_output=True, text=True)
    if check and r.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {r.stderr.strip()}")
    return r.stdout


def clock_offset_ms() -> float:
    """기기 시계 - 호스트 시계(ms). 왕복 시간이 가장 짧은 표본을 쓴다."""
    best = None
    for _ in range(9):
        t0 = now_ms()
        dev = int(adb("shell", "date", "+%s%N").strip()) / 1e6
        t1 = now_ms()
        rtt = t1 - t0
        off = dev - (t0 + t1) / 2
        if best is None or rtt < best[0]:
            best = (rtt, off)
    print(f"clock offset device-host = {best[1]:.0f} ms (rtt {best[0]:.0f} ms)")
    return best[1]


class Console:
    """Virtual Console. heartbeat(POST /browser/session, 1초 간격)를 켜 둔 동안에만 쓰기를 받는다."""

    def __init__(self):
        adb("forward", f"tcp:{PORT}", "tcp:8088")
        self._stop = threading.Event()
        self._hb = threading.Thread(target=self._beat, daemon=True)
        self._hb.start()
        time.sleep(1.5)
        with urllib.request.urlopen(f"{BASE}/vehicle/catalog") as r:
            self.catalog = {f["id"]: f for f in json.load(r)["features"]}

    def _beat(self):
        while not self._stop.is_set():
            try:
                urllib.request.urlopen(urllib.request.Request(f"{BASE}/browser/session?client=spike-w1", method="POST"), timeout=2).read()
            except Exception:
                pass
            self._stop.wait(1.0)

    def close(self):
        self._stop.set()
        try:
            urllib.request.urlopen(urllib.request.Request(f"{BASE}/browser/session?client=spike-w1&closing=1", method="POST"), timeout=2).read()
        except Exception:
            pass

    def put(self, path: str):
        req = urllib.request.Request(BASE + path, method="PUT")
        with urllib.request.urlopen(req, timeout=5) as r:
            body = r.read().decode()
        if '"error"' in body:
            raise RuntimeError(f"PUT {path} -> {body}")

    def path_for(self, fid: str, value, area=None) -> str:
        """vc.sh set과 같은 규칙으로 카탈로그에서 PUT 경로를 만든다."""
        f = self.catalog[fid]
        c = f["control"]
        params = {k: str(v) for k, v in (c.get("params") or {}).items()}
        field = (c.get("field_by_area") or {}).get(str(area), c.get("field"))
        params[field] = str(value).lower() if isinstance(value, bool) else str(value)
        if c.get("area_field") and area is not None:
            params[c["area_field"]] = area
        return f["write_endpoint"] + "?" + urllib.parse.urlencode(params)

    # vc.sh park/idle/drive와 같은 순서
    def park(self, record):
        record("controls0", "/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0")
        record("gearP", "/vehicle/gear?gear=Park")

    def idle(self, record):
        record("controls0", "/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0")
        record("gearD", "/vehicle/gear?gear=Drive")

    def drive(self, record, kph=40):
        record("gearD", "/vehicle/gear?gear=Drive")
        record(f"speed{kph}", f"/vehicle/controls?speed={kph}&accelerator=20&brake=0&steering=0")


class Run:
    """한 번의 측정. PUT마다 호스트 시각을 기록하고 끝나면 logcat을 저장한다."""

    def __init__(self, name: str):
        self.name = name
        self.dir = os.path.join(OUT_ROOT, f"{name}_{time.strftime('%Y%m%d-%H%M%S')}")
        os.makedirs(self.dir, exist_ok=True)
        self.vc = Console()
        self.offset = clock_offset_ms()
        self.steps = []  # (label, sig, expect, dev_t0, dev_t1, path)

    def put(self, label, path, sig="", expect=""):
        t0 = now_ms()
        self.vc.put(path)
        t1 = now_ms()
        self.steps.append((label, sig, expect, t0 + self.offset, t1 + self.offset, path))

    def set(self, sig, fid, value, area=None, wait=2.5, label=None):
        self.put(label or f"{sig}:{fid}={value}" + (f"@{area}" if area else ""), self.vc.path_for(fid, value, area), sig, str(value))
        time.sleep(wait)

    def recorder(self, sig=""):
        return lambda label, path: self.put(label, path, sig)

    def finish(self):
        self.vc.close()
        time.sleep(1.0)
        raw = adb("logcat", "-d", "-v", "epoch")
        with open(os.path.join(self.dir, "logcat.txt"), "w") as f:
            f.write(raw)
        with open(os.path.join(self.dir, "steps.tsv"), "w") as f:
            f.write(f"# offset_ms={self.offset:.0f}\nlabel\tsig\texpect\tdev_t0\tdev_t1\tpath\n")
            for s in self.steps:
                f.write("\t".join([s[0], s[1], s[2], f"{s[3]:.0f}", f"{s[4]:.0f}", s[5]]) + "\n")
        return raw


def launch_spike(fresh=True):
    if fresh:
        adb("shell", "am", "force-stop", PKG)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-W", "-n", SPIKE)
    time.sleep(5)


LINE = re.compile(r"^\s*(\d+\.\d+)\s+\d+\s+\d+\s+\w\s+(\S+)\s*:\s?(.*)$")


def parse(raw):
    """(기기 시각 ms, 태그, 메시지) 목록."""
    out = []
    for line in raw.splitlines():
        m = LINE.match(line)
        if m:
            out.append((float(m.group(1)) * 1000.0, m.group(2), m.group(3)))
    return out


def write_result(run, text):
    print(text)
    with open(os.path.join(run.dir, "result.txt"), "w") as f:
        f.write(text + "\n")
    print(f"\nsaved: {run.dir}")


# ---- signals --------------------------------------------------------------------------------
SIGNAL_STEPS = [
    # (sig, feature, value, area)
    ("C1", "hvac-temperature-set", 18, "driver"),
    ("C1", "hvac-temperature-set", 26.5, "driver"),
    ("C1", "hvac-temperature-set", 22, "driver"),
    ("C1", "hvac-temperature-set", 25, "passenger"),
    ("C1", "hvac-temperature-set", 21.5, "passenger"),
    ("C2", "hvac-fan-speed", 5, None),
    ("C2", "hvac-fan-speed", 8, None),
    ("C2", "hvac-fan-speed", 0, None),
    ("C2", "hvac-fan-speed", 2, None),
    ("C3", "hvac-recirc", True, None),
    ("C3", "hvac-recirc", False, None),
    ("L1", "ambient-light-color", "#aa00ff", None),
    ("L1", "ambient-light-color", "#2979ff", None),
    ("L1", "ambient-light-color", "#ffffff", None),
    ("L1", "ambient-light-color", "#ff0000", None),  # 카탈로그 프리셋 밖의 색이 받아들여지는지
    ("L1", "ambient-light-color", "#00e5ff", None),
    ("L2", "ambient-light-brightness", 30, None),
    ("L2", "ambient-light-brightness", 0, None),
    ("L2", "ambient-light-brightness", 100, None),
    ("L1L2", "ambient-light-switch", "Off", None),   # 스위치 Off일 때 색·밝기 콜백이 바뀌는지
    ("L1L2", "ambient-light-switch", "On", None),
    ("L3", "reading-lights-switch", True, "front_left"),
    ("L3", "reading-lights-switch", True, "front_right"),
    ("L3", "reading-lights-switch", True, "rear_left"),
    ("L3", "reading-lights-switch", True, "rear_right"),
    ("L3", "reading-lights-switch", False, "front_left"),
    ("L3", "reading-lights-switch", False, "front_right"),
    ("L3", "reading-lights-switch", False, "rear_left"),
    ("L3", "reading-lights-switch", False, "rear_right"),
    ("D1", "steering-angle", -45, None),
    ("D1", "steering-angle", 0, None),
    ("D1", "steering-angle", 100, None),
    ("D1", "steering-angle", 500, None),
    ("D1", "steering-angle", -470, None),
    ("D1", "steering-angle", 0, None),
    ("B1", "brake-pedal-status", 100, None),
    ("B1", "brake-pedal-status", 0, None),
    ("B1", "brake-pedal-status", 5, None),
    ("B1", "brake-pedal-status", 0, None),
    ("B1", "brake-pedal-status", 1, None),
    ("B1", "brake-pedal-status", 0, None),
    ("F3", "seat-occupancy", True, "driver"),
    ("F3", "seat-occupancy", True, "passenger"),
    ("F3", "seat-occupancy", True, "rear_left"),
    ("F3", "seat-occupancy", True, "rear_right"),
    ("F3", "seat-occupancy", False, "driver"),
    ("F3", "seat-occupancy", False, "passenger"),
    ("F3", "seat-occupancy", False, "rear_left"),
    ("F3", "seat-occupancy", False, "rear_right"),
]

CB = re.compile(r"^CB (\w+) area=(\S+) v=(.*?) thread=\S+ t=(\d+)$")


def cmd_signals():
    run = Run("signals")
    run.vc.park(run.recorder())
    time.sleep(2)
    launch_spike()
    for sig, fid, value, area in SIGNAL_STEPS:
        run.set(sig, fid, value, area)
    # 빠른 브레이크 두 번(호출 간격 0.3초): 누름 하나가 PUT 두 번(100, 0)일 때 전환이 빠짐없이 오는지
    for value in (100, 0, 100, 0):
        run.put(f"B1fast:brake={value}", run.vc.path_for("brake-pedal-status", value), "B1", str(value))
        time.sleep(0.3)
    time.sleep(2)
    raw = run.finish()
    write_result(run, analyze_signals(run, raw))


def analyze_signals(run, raw):
    events = []  # (t, sig, area, value)
    for t, tag, msg in parse(raw):
        if tag != "SPIKE":
            continue
        m = CB.match(msg)
        if m:
            events.append((int(m.group(4)), m.group(1), m.group(2), m.group(3)))
    text = analyze_events(run, events)
    fails = [msg for _, tag, msg in parse(raw) if tag == "SPIKE" and msg.startswith("FAIL")]
    lines = [text, f"# FAIL lines={len(fails)}"]
    lines.extend("# " + f for f in fails[:20])
    return "\n".join(lines)


def analyze_events(run, events):
    """스텝마다 해당 신호의 값이 바뀐 첫 이벤트와 PUT 기준 지연을 적는다. events: (기기 시각 ms, 신호, area, 값)."""
    lines = ["label\tcallbacks(area=value)\tfirst_cb_after_put_start_ms\tafter_put_end_ms"]
    starts = [s[3] for s in run.steps] + [float("inf")]
    last = {}
    # 스텝 전 마지막 값
    first_step_t = run.steps[0][3] if run.steps else 0
    for t, sig, area, v in events:
        if t < first_step_t:
            last[(sig, area)] = v
    for i, (label, sig, expect, d0, d1, path) in enumerate(run.steps):
        if not sig:
            continue
        sigs = {"L1L2": ("L1", "L2"), "B1fast": ("B1",)}.get(sig, (sig,))
        window = [e for e in events if starts[i] - 30 <= e[0] < starts[i + 1] - 30 and e[1] in sigs]
        changed = []
        for t, s, area, v in window:
            if last.get((s, area)) != v:
                changed.append((t, s, area, v))
            last[(s, area)] = v
        if changed:
            desc = ", ".join(f"{s}:{a}={v}" for _, s, a, v in changed)
            lines.append(f"{label}\t{desc}\t{changed[0][0] - d0:.0f}\t{changed[0][0] - d1:.0f}")
        else:
            lines.append(f"{label}\t(no change; periodic={len(window)})\t-\t-")
    # D1 주기 콜백(SPIKE는 원시 콜백, DATA는 같은 값을 거른 뒤)
    d1 = [t for t, s, _, _ in events if s == "D1"]
    if len(d1) > 2:
        gaps = sorted(b - a for a, b in zip(d1, d1[1:]))
        lines.append(f"# D1 callbacks={len(d1)} median_gap_ms={gaps[len(gaps) // 2]:.0f}")
    else:
        lines.append(f"# D1 callbacks={len(d1)}")
    return "\n".join(lines)


# ---- data (F2-data Repository 확인) ----------------------------------------------------------
DATA_ACTIVITY = f"{PKG}/.spike.DataCheckActivity"
DATA_LINE = re.compile(r"^(\w+) v=(.*) t=(\d+)$")


def cmd_data():
    """새 Repository 4종(DataCheckActivity, 태그 DATA)으로 같은 VC 조작을 하고 Flow 값 변화를 본다."""
    run = Run("data")
    run.vc.park(run.recorder())
    time.sleep(2)
    adb("shell", "am", "force-stop", PKG)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-W", "-n", DATA_ACTIVITY)
    time.sleep(5)
    for sig, fid, value, area in SIGNAL_STEPS:
        run.set(sig, fid, value, area)
    time.sleep(2)
    raw = run.finish()
    msgs = [(t, m) for t, tag, m in parse(raw) if tag == "DATA"]
    head = [m.split(" t=")[0] for _, m in msgs if m.startswith(("START", "SUPPORT", "READ"))]
    events = []
    for _, m in msgs:
        d = DATA_LINE.match(m)
        if d and d.group(1) in ("C1", "C2", "C3", "L1", "L2", "L3", "D1", "B1", "F3"):
            events.append((int(d.group(3)), d.group(1), "-", d.group(2)))
    write_result(run, "\n".join(head) + "\n" + analyze_events(run, events))


# ---- ambswitch ------------------------------------------------------------------------------
def cmd_ambswitch():
    """VC 앰비언트 스위치(Off/On/Auto)를 바꿀 때 getAmbientLightSwitch 값과 L1·L2 콜백을 본다(스위치는 리스너 API가 없다)."""
    run = Run("ambswitch")
    run.vc.park(run.recorder())
    time.sleep(2)
    launch_spike()
    marks = []
    for value in ("Off", "On", "Auto", "On"):
        marks.append((f"switch={value}", now_ms() + run.offset))
        run.set("L1SW", "ambient-light-switch", value, wait=1.5)
        adb("shell", "am", "start", "-n", SPIKE, "--es", "probe", "ambient-switch")
        time.sleep(1.5)
    raw = run.finish()
    app = [(t, m.split(" t=")[0]) for t, tag, m in parse(raw) if tag == "SPIKE" and re.match(r"(CAP|GET|CB|FAIL) (L1SW|L1|L2)\b", m)]
    lines = ["# 시작 시 조회"] + [m for t, m in app if t < marks[0][1] and "L1SW" in m]
    for i, (label, tm) in enumerate(marks):
        end = marks[i + 1][1] if i + 1 < len(marks) else float("inf")
        lines.append(f"== {label}")
        lines.extend(f"  +{t - tm:6.0f} ms  {m}" for t, m in app if tm - 30 <= t < end - 30)
    write_result(run, "\n".join(lines))


# ---- gate -----------------------------------------------------------------------------------
def cmd_gate():
    run = Run("gate")
    run.vc.park(run.recorder())
    time.sleep(2)
    launch_spike()
    rec = run.recorder("gate")
    for _ in range(2):
        run.vc.idle(rec); time.sleep(4)
        run.vc.park(rec); time.sleep(4)
        run.vc.drive(rec); time.sleep(4)
        run.vc.park(rec); time.sleep(4)
    raw = run.finish()
    write_result(run, analyze_gate(run, raw))


def gate_events(raw):
    ev = []
    for t, tag, msg in parse(raw):
        if tag == "SPIKE" and (msg.startswith("REG") or msg.startswith("DRAW") or msg.startswith("LIFE")):
            ev.append((t, "app", msg.split(" t=")[0]))
        elif tag == "HOME_SCREEN" and PKG in msg and ("onFullscreenAvailable" in msg or "updateDrivingRestrictionRegion" in msg):
            ev.append((t, "home", re.sub(r"ComponentInfo\{[^}]*\}", "spike", msg)[:160]))
        elif tag == "HOME_SCREEN" and "updateDrivingRestrictionRegion" in msg:
            ev.append((t, "home", msg[:160]))
    return ev


def analyze_gate(run, raw):
    ev = gate_events(raw)
    lines = []
    trig = [s for s in run.steps if s[0] in ("gearD", "gearP")]
    for i, (label, _, _, d0, d1, _) in enumerate(trig):
        end = trig[i + 1][3] if i + 1 < len(trig) else float("inf")
        lines.append(f"== {label} put_start={d0:.0f} put_end={d1:.0f}")
        for t, src, msg in ev:
            if d0 - 30 <= t < end - 30:
                lines.append(f"  +{t - d0:6.0f} ms  {src:4}  {msg}")
    return "\n".join(lines)


# ---- record ---------------------------------------------------------------------------------
PATCH = (2280, 1120, 40, 40)  # 앱 창 오른쪽 아래 배경(글자·아이콘 없음), 기기 픽셀


def cmd_record():
    """화면을 녹화해 정차→D 전환 프레임마다 앱 영역 배경색을 본다.
    SpikeActivity 배경: PLAY 초록(0x1B5E20), PAUSED 회색(0x424242). 차단 화면은 밝은 흐림으로 덮는다."""
    run = Run("record")
    run.vc.park(run.recorder())
    time.sleep(2)
    launch_spike()
    rec = subprocess.Popen([ADB, "shell", "screenrecord", "--time-limit", "16", "--bit-rate", "20000000", "/sdcard/spike_gate.mp4"])
    time.sleep(3)
    rec_gate = run.recorder("gate")
    run.vc.idle(rec_gate); time.sleep(3)
    # 차단 화면이 덮인 채로 앱 배경을 빨강으로 바꾼다: 흐림 색이 따라 바뀌면 실시간 흐림, 그대로면 전환 순간의 정지 이미지
    adb("shell", "am", "start", "-n", SPIKE, "--es", "bg", "red"); time.sleep(4)
    run.vc.park(rec_gate); time.sleep(3)
    adb("shell", "am", "start", "-n", SPIKE, "--es", "bg", "none")
    rec.wait()
    time.sleep(1)
    mp4 = os.path.join(run.dir, "gate.mp4")
    adb("pull", "/sdcard/spike_gate.mp4", mp4)
    adb("shell", "rm", "/sdcard/spike_gate.mp4", check=False)
    run.finish()
    x, y, w, h = PATCH
    pts = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "frame=pts_time",
                          "-of", "csv=p=0", mp4], capture_output=True, text=True).stdout.split()
    rgb = subprocess.run(["ffmpeg", "-v", "error", "-i", mp4, "-vf", f"crop={w}:{h}:{x}:{y},scale=1:1",
                          "-fps_mode", "passthrough", "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
                         capture_output=True).stdout
    lines = [f"patch={PATCH} frames={len(pts)}", "pts_s\tR\tG\tB\tclass"]
    prev = None
    for i, p in enumerate(pts):
        if 3 * i + 3 > len(rgb):
            break
        r, g, b = rgb[3 * i], rgb[3 * i + 1], rgb[3 * i + 2]
        cls = classify(r, g, b)
        if cls != prev:
            lines.append(f"{float(p):.3f}\t{r}\t{g}\t{b}\t{cls}")
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", mp4, "-vf", f"select=eq(n\\,{i})", "-frames:v", "1",
                            os.path.join(run.dir, f"frame_{i:04d}_{cls}.png")])
            prev = cls
    write_result(run, "\n".join(lines))


def classify(r, g, b):
    if r > g + 80 and r > b + 80:
        return "red_crisp"
    if r > g + 6 and r > b + 6 and r > 150:
        return "red_under_overlay"
    if g > r + 40 and g > b + 40 and r + g + b < 300:
        return "play_crisp"
    if abs(r - g) < 12 and abs(g - b) < 12 and r < 100:
        return "paused_crisp"
    if g > r + 8 and g > b + 8:
        return "play_under_overlay"
    if r > 150 and abs(r - g) < 12 and abs(g - b) < 12:
        return "overlay_neutral"
    return "other"


# ---- touch ----------------------------------------------------------------------------------
def button_center(raw):
    pts = re.findall(r"BTN center=(\d+),(\d+)", raw)
    return (int(pts[-1][0]), int(pts[-1][1])) if pts else None


def cmd_touch():
    run = Run("touch")
    run.vc.park(run.recorder())
    time.sleep(2)
    launch_spike()
    c = button_center(adb("logcat", "-d", "-s", "SPIKE:*"))
    if c is None:
        sys.exit("BTN 좌표를 찾지 못했다")
    x, y = c
    marks = []

    def tap(label):
        marks.append((label, now_ms() + run.offset))
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(1.5)

    tap("park-1")
    run.vc.drive(run.recorder("gate")); time.sleep(3)
    shot(os.path.join(run.dir, "drive_before_tap.png"))
    tap("drive-1"); tap("drive-2")
    dump = adb("exec-out", "uiautomator", "dump", "/dev/tty", check=False)
    with open(os.path.join(run.dir, "drive_uiautomator.xml"), "w") as f:
        f.write(dump)
    shot(os.path.join(run.dir, "drive_after_tap.png"))
    run.vc.idle(run.recorder("gate")); time.sleep(3)
    tap("idle-1")
    run.vc.park(run.recorder("gate")); time.sleep(3)
    tap("park-2")
    shot(os.path.join(run.dir, "park_after.png"))
    raw = run.finish()
    lines = [f"button center = {x},{y}"]
    app = [(t, msg.split(" t=")[0]) for t, tag, msg in parse(raw) if tag == "SPIKE" and (msg.startswith(("TOUCH", "CLICK", "REG src=A ")))]
    for i, (label, tm) in enumerate(marks):
        end = marks[i + 1][1] if i + 1 < len(marks) else float("inf")
        got = [m for t, m in app if tm - 30 <= t < end - 30 and m.startswith(("TOUCH", "CLICK"))]
        lines.append(f"{label}: " + ("; ".join(got) if got else "앱이 받은 터치 없음"))
    write_result(run, "\n".join(lines))


def shot(path):
    with open(path, "wb") as f:
        f.write(subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True).stdout)


# ---- appswitch ------------------------------------------------------------------------------
def cmd_appswitch():
    run = Run("appswitch")
    run.vc.park(run.recorder())
    time.sleep(2)
    launch_spike()
    marks = []

    def mark(label):
        marks.append((label, now_ms() + run.offset))

    # 1) 정차 유지: 설정으로 갔다가 돌아온다
    mark("case1:open-settings"); adb("shell", "am", "start", "-W", "-n", SETTINGS); time.sleep(3)
    mark("case1:return"); adb("shell", "am", "start", "-W", "-n", SPIKE); time.sleep(3)
    # 2) 떠나 있는 동안 주행 → 정차
    mark("case2:open-settings"); adb("shell", "am", "start", "-W", "-n", SETTINGS); time.sleep(3)
    mark("case2:drive"); run.vc.drive(run.recorder("gate")); time.sleep(3)
    mark("case2:park"); run.vc.park(run.recorder("gate")); time.sleep(3)
    mark("case2:return"); adb("shell", "am", "start", "-W", "-n", SPIKE); time.sleep(3)
    # 3) 떠나 있는 동안 주행, 주행 중에 돌아온다
    mark("case3:open-settings"); adb("shell", "am", "start", "-W", "-n", SETTINGS); time.sleep(3)
    mark("case3:drive"); run.vc.drive(run.recorder("gate")); time.sleep(3)
    mark("case3:return-while-driving"); adb("shell", "am", "start", "-W", "-n", SPIKE); time.sleep(3)
    shot(os.path.join(run.dir, "case3_return_while_driving.png"))
    mark("case3:park"); run.vc.park(run.recorder("gate")); time.sleep(3)
    # 4) 오래 떠나 있기(프로세스 동결 기준 10초를 넘김) → 주행 → 정차 → 복귀
    mark("case4:open-settings"); adb("shell", "am", "start", "-W", "-n", SETTINGS); time.sleep(25)
    frozen = adb("shell", "dumpsys", "activity", "processes", check=False)
    mark("case4:drive"); run.vc.drive(run.recorder("gate")); time.sleep(3)
    mark("case4:park"); run.vc.park(run.recorder("gate")); time.sleep(3)
    mark("case4:return"); adb("shell", "am", "start", "-W", "-n", SPIKE); time.sleep(3)
    raw = run.finish()
    frozen_lines = []
    block = None
    for l in frozen.splitlines():
        if "ProcessRecord{" in l:
            block = l.strip() if f":{PKG}/" in l or l.rstrip().endswith(f"{PKG}}}") or f" {PKG}/" in l else None
        elif block and "isFrozen=" in l:
            frozen_lines.append(block[:80] + " " + l.strip().split("isFrozen=")[-1].join(["isFrozen=", ""]))
            block = None
    app = [(t, msg.split(" t=")[0]) for t, tag, msg in parse(raw) if tag == "SPIKE" and msg.startswith(("REG", "LIFE"))]
    lines = []
    for i, (label, tm) in enumerate(marks):
        end = marks[i + 1][1] if i + 1 < len(marks) else float("inf")
        lines.append(f"== {label}")
        lines.extend(f"  +{t - tm:6.0f} ms  {m}" for t, m in app if tm - 30 <= t < end - 30)
    lines.append("# case4 동결 상태(dumpsys activity processes): " + ("; ".join(frozen_lines) if frozen_lines else "표시 없음"))
    write_result(run, "\n".join(lines))


# ---- window ---------------------------------------------------------------------------------
def cmd_window():
    """window [split|join]: split은 오른쪽 끝 윈도 바(2547,720)를 가운데로 끌어 1:1(세로 1단)로, join은 되돌린다.
    (홈 화면 WindowBarDragHandler가 직전 앱을 끌어와 옆 창에 놓는다. 2560x1440 에뮬레이터 좌표)"""
    action = sys.argv[2] if len(sys.argv) > 2 else "show"
    if action in ("split", "join"):
        adb("logcat", "-c")
        if action == "split":
            adb("shell", "input", "swipe", "2547", "720", "1690", "720", "1200")
        else:
            adb("shell", "input", "swipe", "1706", "720", "2550", "720", "1200")
        time.sleep(3)
    raw = adb("logcat", "-d", "-v", "epoch")
    wins = [m for _, tag, m in parse(raw) if tag == "SPIKE" and m.startswith(("WIN", "LIFE config"))]
    bounds = [m for _, tag, m in parse(raw) if tag == "HOME_SCREEN" and "updateBounds" in m and PKG in m]
    print("\n".join(wins[-5:]))
    print("\n".join(re.sub(r"ComponentInfo\{[^}]*\}", "spike", b)[:200] for b in bounds[-3:]))


if __name__ == "__main__":
    cmds = {"signals": cmd_signals, "data": cmd_data, "ambswitch": cmd_ambswitch, "gate": cmd_gate, "record": cmd_record, "touch": cmd_touch, "appswitch": cmd_appswitch, "window": cmd_window}
    if len(sys.argv) < 2 or sys.argv[1] not in cmds:
        print(__doc__)
        sys.exit(2)
    cmds[sys.argv[1]]()
