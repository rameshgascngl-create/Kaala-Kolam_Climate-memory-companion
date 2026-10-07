#!/usr/bin/env bash
set -euxo pipefail

PKG="edu.gascnagercoil.kaalakolam.debug"
COMPONENT="$PKG/edu.gascnagercoil.kaalakolam.MainActivity"
STATE_PATH="files/datastore/kaala_kolam_state.json"
EVIDENCE_ROOT="app/build/class-emulator-evidence"
mkdir -p "$EVIDENCE_ROOT"

dump_window() {
  local target="${1:-/tmp/kaala-kolam-class-window.xml}"
  local ok=0
  for _ in $(seq 1 10); do
    if adb shell uiautomator dump /sdcard/kaala-kolam-class-window.xml >/dev/null 2>&1 &&
       adb exec-out cat /sdcard/kaala-kolam-class-window.xml >"$target" 2>/dev/null &&
       test -s "$target"; then
      ok=1
      break
    fi
    sleep 0.5
  done
  test "$ok" -eq 1
}

wait_for_idle() {
  local previous=""
  local stable=0
  local current=""
  for _ in $(seq 1 24); do
    dump_window
    current="$(sha256sum /tmp/kaala-kolam-class-window.xml | cut -d' ' -f1)"
    if [ "$current" = "$previous" ]; then
      stable=$((stable + 1))
      if [ "$stable" -ge 2 ]; then
        return 0
      fi
    else
      stable=0
    fi
    previous="$current"
    sleep 0.25
  done
  echo "UI did not become idle within the bounded wait." >&2
  return 1
}

bounds_for_tag() {
  local tag="$1"
  TAG="$tag" python3 - <<'PY'
import os
import sys
import xml.etree.ElementTree as ET

tag = os.environ["TAG"]
root = ET.parse("/tmp/kaala-kolam-class-window.xml").getroot()
for node in root.iter("node"):
    rid = node.attrib.get("resource-id", "")
    if rid == tag or rid.endswith("/" + tag) or rid.endswith("/id/" + tag) or rid.endswith(":" + tag):
        bounds = node.attrib.get("bounds", "")
        if bounds:
            print(bounds)
            sys.exit(0)
sys.exit(1)
PY
}

wait_for_tag() {
  local tag="$1"
  local bounds=""
  for _ in $(seq 1 40); do
    dump_window
    bounds="$(bounds_for_tag "$tag" 2>/dev/null || true)"
    if [ -n "$bounds" ]; then
      printf '%s\n' "$bounds"
      return 0
    fi
    sleep 0.5
  done
  echo "Timed out waiting for semantic tag: $tag" >&2
  return 1
}

click_tag() {
  local tag="$1"
  local bounds
  bounds="$(wait_for_tag "$tag")"
  BOUNDS="$bounds" python3 - <<'PY' >/tmp/kaala-kolam-class-tap.txt
import os
import re

m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", os.environ["BOUNDS"])
if not m:
    raise SystemExit("Bad bounds: " + os.environ["BOUNDS"])
x1, y1, x2, y2 = map(int, m.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  read -r x y </tmp/kaala-kolam-class-tap.txt
  adb shell input tap "$x" "$y"
  wait_for_idle
}

wait_for_text() {
  local expected="$1"
  for _ in $(seq 1 40); do
    dump_window
    EXPECTED="$expected" python3 - <<'PY' && return 0 || true
import os
import sys
import xml.etree.ElementTree as ET

expected = os.environ["EXPECTED"]
root = ET.parse("/tmp/kaala-kolam-class-window.xml").getroot()
for node in root.iter("node"):
    if node.attrib.get("text", "") == expected:
        sys.exit(0)
sys.exit(1)
PY
    sleep 0.5
  done
  echo "Timed out waiting for text: $expected" >&2
  return 1
}

wait_for_tag_text() {
  local tag="$1"
  local expected="$2"
  for _ in $(seq 1 40); do
    dump_window
    TAG="$tag" EXPECTED="$expected" python3 - <<'PY' && return 0 || true
import os
import sys
import xml.etree.ElementTree as ET

tag = os.environ["TAG"]
expected = os.environ["EXPECTED"]
root = ET.parse("/tmp/kaala-kolam-class-window.xml").getroot()
for node in root.iter("node"):
    rid = node.attrib.get("resource-id", "")
    tagged = (
        rid == tag
        or rid.endswith("/" + tag)
        or rid.endswith("/id/" + tag)
        or rid.endswith(":" + tag)
    )
    if tagged and node.attrib.get("text", "") == expected:
        sys.exit(0)
sys.exit(1)
PY
    sleep 0.5
  done
  echo "Timed out waiting for tag text: tag=$tag text=$expected" >&2
  return 1
}

read_state() {
  adb shell "run-as $PKG cat $STATE_PATH" >/tmp/kaala-kolam-class-state.json
}

class_state_ok() {
  read_state
  python3 - <<'PY'
import json
import sys

with open("/tmp/kaala-kolam-class-state.json", encoding="utf-8") as fh:
    state = json.load(fh)

pool = state.get("classPool") or {}
flags = state.get("memoryFlags") or {}
ok = (
    state.get("currentTab") == "class"
    and state.get("language") == "ta"
    and pool.get("sample") is True
    and pool.get("groupBy") == "DECADE"
    and flags.get("pool") is True
)
sys.exit(0 if ok else 1)
PY
}

wait_for_class_state() {
  for _ in $(seq 1 40); do
    if class_state_ok 2>/dev/null; then
      return 0
    fi
    sleep 0.25
  done
  read_state || true
  cat /tmp/kaala-kolam-class-state.json >&2 || true
  echo "Timed out waiting for persisted Class state." >&2
  return 1
}

assert_class_state() {
  local phase="$1"
  read_state
  PHASE="$phase" python3 - <<'PY'
import json
import os

with open("/tmp/kaala-kolam-class-state.json", encoding="utf-8") as fh:
    state = json.load(fh)

assert state.get("currentTab") == "class", state.get("currentTab")
assert state.get("language") == "ta", state.get("language")
pool = state.get("classPool") or {}
assert pool.get("sample") is True, pool
assert pool.get("groupBy") == "DECADE", pool
flags = state.get("memoryFlags") or {}
assert flags.get("pool") is True, flags
print(
    "CLASS_PERSISTED_PASS "
    f"phase={os.environ['PHASE']} currentTab=class language=ta "
    "sample=true groupBy=DECADE poolFlag=true"
)
PY
}

activity_manager_snapshot() {
  local target="$1"
  adb shell dumpsys activity processes "$PKG" | tr -d '\r' >"$target"
}

activity_manager_state_ok() {
  local mode="$1"
  local pid="$2"
  local snapshot="$3"
  MODE="$mode" PID="$pid" PKG="$PKG" SNAPSHOT="$snapshot" python3 - <<'PY'
import os
import re
import sys

mode = os.environ["MODE"]
pid = os.environ["PID"]
pkg = os.environ["PKG"]
text = open(os.environ["SNAPSHOT"], encoding="utf-8", errors="replace").read()

if not re.search(rf"\b{re.escape(pid)}:{re.escape(pkg)}(?:/|\b)", text):
    sys.exit(1)

adj_match = re.search(r"\bcurRaw=(-?\d+)", text)
if not adj_match:
    adj_match = re.search(r"\boom adj:.*?\bcur=(-?\d+)", text)
if not adj_match:
    sys.exit(1)
adj = int(adj_match.group(1))

state_name_match = re.search(r"state:\s*cur=([A-Z_]+)", text)
state_name = state_name_match.group(1) if state_name_match else ""
proc_state_match = re.search(r"\bcurProcState=(\d+)", text)
proc_state = int(proc_state_match.group(1)) if proc_state_match else None

if mode == "background":
    ok = adj >= 700
elif mode == "cached":
    named_cached = "CACHED" in state_name
    numeric_cached = proc_state is not None and proc_state >= 16
    ok = adj >= 900 and (named_cached or numeric_cached)
else:
    raise SystemExit(f"Unknown ActivityManager mode: {mode}")

if not ok:
    sys.exit(1)

print(
    "ACTIVITY_MANAGER_STATE_PASS "
    f"mode={mode} pid={pid} oomAdj={adj} "
    f"stateName={state_name or 'unknown'} "
    f"curProcState={proc_state if proc_state is not None else 'unknown'}"
)
PY
}

wait_for_activity_manager_state() {
  local mode="$1"
  local pid="$2"
  local snapshot="/tmp/kaala-kolam-class-activity-processes.txt"
  for _ in $(seq 1 40); do
    activity_manager_snapshot "$snapshot"
    if activity_manager_state_ok "$mode" "$pid" "$snapshot"; then
      return 0
    fi
    sleep 0.5
  done
  echo "Timed out waiting for ActivityManager mode=$mode pid=$pid." >&2
  cat "$snapshot" >&2 || true
  return 1
}

wait_for_old_pid_gone() {
  local old_pid="$1"
  local current=""
  local proc_exists=""
  for _ in $(seq 1 20); do
    current="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
    proc_exists="$(adb shell "if [ -d /proc/$old_pid ]; then echo yes; else echo no; fi" | tr -d '\r')"
    if [ -z "$current" ] && [ "$proc_exists" = "no" ]; then
      echo "OLD_PID_GONE_PASS pid=$old_pid"
      return 0
    fi
    sleep 0.5
  done
  echo "Old PID did not disappear: old=$old_pid current=$current procExists=$proc_exists" >&2
  return 1
}

wait_for_new_pid() {
  local old_pid="$1"
  local new_pid=""
  for _ in $(seq 1 40); do
    new_pid="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
    if [ -n "$new_pid" ] && [ "$new_pid" != "$old_pid" ]; then
      if adb shell "test -d /proc/$new_pid"; then
        printf '%s\n' "$new_pid"
        return 0
      fi
    fi
    sleep 0.25
  done
  echo "Timed out waiting for a new app PID after relaunch; old=$old_pid current=$new_pid" >&2
  return 1
}

capture_evidence() {
  local stage="$1"
  local dir="$EVIDENCE_ROOT/$stage"
  mkdir -p "$dir"
  set +e

  adb exec-out screencap -p >"$dir/screenshot.png" 2>"$dir/screenshot.err"
  dump_window "$dir/uiautomator.xml" >"$dir/uiautomator-command.txt" 2>&1

  adb shell "run-as $PKG sh -c 'echo === files ===; ls -la files; echo === files/datastore ===; ls -la files/datastore 2>&1; echo === datastore contents ===; for f in files/datastore/*; do echo --- \"\$f\"; ls -l \"\$f\"; cat \"\$f\"; echo; done'" \
    >"$dir/storage.txt" 2>&1

  local pid
  pid="$(adb shell pidof "$PKG" | tr -d '\r')"
  {
    echo "=== app process logcat pid=$pid ==="
    if [ -n "$pid" ]; then
      adb logcat -d --pid="$pid"
    fi
    echo "=== AndroidRuntime ==="
    adb logcat -d AndroidRuntime:E '*:S'
  } >"$dir/logcat.txt" 2>&1

  adb shell dumpsys activity activities >"$dir/activities.txt" 2>&1
  adb shell dumpsys activity processes "$PKG" >"$dir/activity-processes.txt" 2>&1
  if [ -n "$pid" ]; then
    adb shell "cat /proc/$pid/oom_score_adj" >"$dir/oom-score-adj.txt" 2>&1
  fi
  adb shell dumpsys window windows >"$dir/windows.txt" 2>&1
  set -e
}

on_exit() {
  local status=$?
  trap - EXIT
  if [ "$status" -ne 0 ]; then
    capture_evidence "failure"
  fi
  exit "$status"
}
trap on_exit EXIT

APK="$(find app/build -type f -name 'app-debug.apk' -print -quit)"
test -n "$APK"
adb install -r "$APK"
adb shell pm clear "$PKG"

adb shell wm size 390x844
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb logcat -c

adb shell am start -W -n "$COMPONENT" >/tmp/kaala-kolam-class-start.txt
wait_for_idle

click_tag "touch.tab.class"
wait_for_tag "class-import-input"

click_tag "touch.class-sample-toggle"
click_tag "touch.class-group.2"

click_tag "class-import-input"
adb shell input text "draftcode"
wait_for_tag_text "class-import-input" "draftcode"
adb shell input keyevent KEYCODE_BACK
wait_for_idle

click_tag "touch.tool.language"
wait_for_text "வகுப்புத் தொகுப்பு"
wait_for_tag_text "class-import-input" "draftcode"
wait_for_class_state
capture_evidence "class-ta-decade-before-rotation"

# Configuration rotation must retain the local import draft and persisted Class state.
adb shell settings put system user_rotation 1
wait_for_idle
wait_for_text "வகுப்புத் தொகுப்பு"
wait_for_tag_text "class-import-input" "draftcode"
assert_class_state "landscape"
adb shell settings put system user_rotation 0
wait_for_idle
wait_for_text "வகுப்புத் தொகுப்பு"
wait_for_tag_text "class-import-input" "draftcode"
assert_class_state "portrait-restored"
capture_evidence "class-ta-decade-after-rotation"

# Prove durable Class state before manipulating process state.
assert_class_state "before-home"
PID_BEFORE="$(adb shell pidof "$PKG" | tr -d '\r')"
test -n "$PID_BEFORE"

adb shell input keyevent KEYCODE_HOME
wait_for_activity_manager_state "background" "$PID_BEFORE"
capture_evidence "after-home-backgrounded"

adb shell am start -W -a android.settings.SETTINGS >/tmp/kaala-kolam-class-cache-settings.txt
adb shell am start -W \
  -a android.intent.action.OPEN_DOCUMENT \
  -c android.intent.category.OPENABLE \
  -t text/plain >/tmp/kaala-kolam-class-cache-documents.txt
wait_for_activity_manager_state "cached" "$PID_BEFORE"
activity_manager_snapshot "$EVIDENCE_ROOT/activity-manager-before-kill.txt"
capture_evidence "before-process-kill-cached"

adb shell am kill "$PKG"
wait_for_old_pid_gone "$PID_BEFORE"
activity_manager_snapshot "$EVIDENCE_ROOT/activity-manager-after-kill.txt"
test -z "$(adb shell pidof "$PKG" | tr -d '\r' || true)"

adb shell am start -W -n "$COMPONENT" >/tmp/kaala-kolam-class-relaunch.txt
PID_NEW="$(wait_for_new_pid "$PID_BEFORE")"
test -n "$PID_NEW"
test "$PID_NEW" != "$PID_BEFORE"
echo "NEW_PID_PASS old=$PID_BEFORE new=$PID_NEW"

wait_for_idle
wait_for_text "வகுப்புத் தொகுப்பு"
wait_for_class_state
assert_class_state "after-process-death-relaunch"
capture_evidence "after-process-death-relaunch"

echo "CLASS_PROCESS_DEATH_RESUME_PASS api=34 oldPid=$PID_BEFORE newPid=$PID_NEW currentTab=class language=ta sample=true groupBy=DECADE rotation=pass"
