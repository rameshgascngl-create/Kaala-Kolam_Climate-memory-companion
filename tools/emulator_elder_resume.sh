#!/usr/bin/env bash
set -euxo pipefail

PKG="edu.gascnagercoil.kaalakolam.debug"
COMPONENT="$PKG/edu.gascnagercoil.kaalakolam.MainActivity"
STATE_PATH="files/datastore/kaala_kolam_state.json"
EVIDENCE_ROOT="app/build/emulator-evidence"
mkdir -p "$EVIDENCE_ROOT"

QUESTIONS=(
  "Compared with when you were young, how is the summer heat now?"
  "Compared with your youth, how often are nights too hot and sticky to sleep well?"
  "How far can people now trust the rains to arrive when expected?"
  "Compared with your youth, how often do sudden, very heavy downpours happen?"
  "How much water do wells and ponds hold in the dry months, compared with before?"
)

dump_window() {
  local target="${1:-/tmp/kaala-kolam-window.xml}"
  local ok=0
  for _ in $(seq 1 10); do
    if adb shell uiautomator dump /sdcard/kaala-kolam-window.xml >/dev/null 2>&1 &&
       adb exec-out cat /sdcard/kaala-kolam-window.xml >"$target" 2>/dev/null &&
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
    current="$(sha256sum /tmp/kaala-kolam-window.xml | cut -d' ' -f1)"
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
root = ET.parse("/tmp/kaala-kolam-window.xml").getroot()
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
  BOUNDS="$bounds" python3 - <<'PY' >/tmp/kaala-kolam-tap.txt
import os
import re

m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", os.environ["BOUNDS"])
if not m:
    raise SystemExit("Bad bounds: " + os.environ["BOUNDS"])
x1, y1, x2, y2 = map(int, m.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
PY
  read -r x y </tmp/kaala-kolam-tap.txt
  adb shell input tap "$x" "$y"
  wait_for_idle
}

wait_for_question() {
  local expected="$1"
  for _ in $(seq 1 40); do
    dump_window
    EXPECTED="$expected" python3 - <<'PY' && return 0 || true
import os
import sys
import xml.etree.ElementTree as ET

expected = os.environ["EXPECTED"]
root = ET.parse("/tmp/kaala-kolam-window.xml").getroot()
for node in root.iter("node"):
    rid = node.attrib.get("resource-id", "")
    tagged = (
        rid == "elder-question"
        or rid.endswith("/elder-question")
        or rid.endswith("/id/elder-question")
        or rid.endswith(":elder-question")
    )
    if tagged and node.attrib.get("text", "") == expected:
        sys.exit(0)
sys.exit(1)
PY
    sleep 0.5
  done
  echo "Timed out waiting for Elders question: $expected" >&2
  return 1
}

read_state() {
  adb shell "run-as $PKG cat $STATE_PATH" >/tmp/kaala-kolam-state.json
}

wait_for_answer_count() {
  local expected="$1"
  for _ in $(seq 1 40); do
    if read_state 2>/dev/null && EXPECTED_COUNT="$expected" python3 - <<'PY'
import json
import os
import sys

expected = int(os.environ["EXPECTED_COUNT"])
with open("/tmp/kaala-kolam-state.json", encoding="utf-8") as fh:
    state = json.load(fh)
interviews = state.get("interviews") or []
if len(interviews) != 1:
    sys.exit(1)
answers = interviews[0].get("answers") or {}
count = sum(1 for value in answers.values() if value.get("answered") is True)
sys.exit(0 if count == expected else 1)
PY
    then
      wait_for_idle
      return 0
    fi
    sleep 0.25
  done
  echo "Timed out waiting for $expected committed Elders answers." >&2
  return 1
}

assert_q5_persisted_state() {
  local phase="$1"
  read_state
  PHASE="$phase" python3 - <<'PY'
import json
import os

with open("/tmp/kaala-kolam-state.json", encoding="utf-8") as fh:
    state = json.load(fh)

assert state.get("currentTab") == "elders", state.get("currentTab")
session = state.get("elderSession") or {}
assert session.get("mode") == "ASK", session
assert session.get("questionIndex") == 4, session
assert session.get("activeId"), session

interviews = state.get("interviews") or []
assert len(interviews) == 1, len(interviews)
answers = interviews[0].get("answers") or {}
expected_ids = ["heat", "nights", "monsoon", "downpour", "water"]
for question_id in expected_ids:
    answer = answers.get(question_id)
    assert answer is not None, question_id
    assert answer.get("answered") is True, (question_id, answer)
    assert answer.get("rating") == 0, (question_id, answer)
print(
    "ELDERS_Q5_PERSISTED_PASS "
    f"phase={os.environ['PHASE']} currentTab=elders questionIndex=4 answers=5"
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
  local snapshot="/tmp/kaala-kolam-activity-processes.txt"
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

  adb shell "run-as $PKG sh -c 'echo === files ===; ls -la files; echo === files/datastore ===; ls -la files/datastore 2>&1; echo === datastore contents ===; for f in files/datastore/*; do echo --- \"\$f\"; ls -l \"\$f\"; cat \"\$f\"; echo; done'"     >"$dir/storage.txt" 2>&1

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

adb shell am start -W -n "$COMPONENT" >/tmp/kaala-kolam-start.txt
wait_for_idle
capture_evidence "fresh-launch"

# Navigation is discovered from stable Compose semantics; no fixed tap coordinates.
click_tag "touch.tab.elders"
click_tag "touch.elder-start"
click_tag "touch.elder-setup-start"
wait_for_question "${QUESTIONS[0]}"

# Answer Q1-Q5 through the UI. A committed DataStore answer is required before advancing.
for index in 0 1 2 3 4; do
  wait_for_question "${QUESTIONS[$index]}"
  click_tag "touch.elder-answer.0"
  wait_for_answer_count "$((index + 1))"
  if [ "$index" -lt 4 ]; then
    click_tag "touch.elder-forward"
    wait_for_question "${QUESTIONS[$((index + 1))]}"
  fi
done

capture_evidence "q5-before-rotation"

# Rotation must retain Q5 and all committed answers.
adb shell settings put system user_rotation 1
wait_for_idle
wait_for_question "${QUESTIONS[4]}"
adb shell settings put system user_rotation 0
wait_for_idle
wait_for_question "${QUESTIONS[4]}"
capture_evidence "q5-before-process-death"

# Prove Q5 and Q1-Q5 are durably committed before touching process state.
assert_q5_persisted_state "before-home"
PID_BEFORE="$(adb shell pidof "$PKG" | tr -d '\r')"
test -n "$PID_BEFORE"

# HOME must move the app out of TOP. ActivityManager is the authority.
adb shell input keyevent KEYCODE_HOME
wait_for_activity_manager_state "background" "$PID_BEFORE"
capture_evidence "after-home-backgrounded"

# HOME normally leaves the previous app in LAST/PREV (oom_adj about 700), which
# 'am kill' intentionally will not kill. Put another system activity on top so
# the target process becomes genuinely cached, then prove that through
# ActivityManager before executing the intended kill mechanism.
adb shell am start -W -a android.settings.SETTINGS >/tmp/kaala-kolam-cache-settle.txt
wait_for_activity_manager_state "cached" "$PID_BEFORE"
activity_manager_snapshot "$EVIDENCE_ROOT/activity-manager-before-kill.txt"
capture_evidence "before-process-kill-cached"

adb shell am kill "$PKG"

# Independently prove the original Linux process is gone before any relaunch.
wait_for_old_pid_gone "$PID_BEFORE"
activity_manager_snapshot "$EVIDENCE_ROOT/activity-manager-after-kill.txt"
test -z "$(adb shell pidof "$PKG" | tr -d '\r' || true)"

# Relaunch the exact component, then require a different live PID.
adb shell am start -W -n "$COMPONENT" >/tmp/kaala-kolam-relaunch.txt
PID_NEW="$(wait_for_new_pid "$PID_BEFORE")"
test -n "$PID_NEW"
test "$PID_NEW" != "$PID_BEFORE"
echo "NEW_PID_PASS old=$PID_BEFORE new=$PID_NEW"

wait_for_idle
wait_for_question "${QUESTIONS[4]}"
assert_q5_persisted_state "after-process-death-relaunch"
capture_evidence "after-process-death-relaunch"

echo "ELDERS_PROCESS_DEATH_RESUME_PASS api=34 oldPid=$PID_BEFORE newPid=$PID_NEW currentTab=elders questionIndex=4 answers=5"
