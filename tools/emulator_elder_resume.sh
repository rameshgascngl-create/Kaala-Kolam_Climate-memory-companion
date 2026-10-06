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

assert_restored_state() {
  read_state
  python3 - <<'PY'
import json

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
print("ELDERS_STORAGE_RESTORE_PASS currentTab=elders questionIndex=4 answers=5")
PY
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

# Real process death: HOME, am kill, then launcher relaunch with no navigation tap.
adb shell input keyevent KEYCODE_HOME
sleep 1
PID_BEFORE="$(adb shell pidof "$PKG" | tr -d '\r')"
test -n "$PID_BEFORE"
adb shell am kill "$PKG"

for _ in $(seq 1 10); do
  PID_AFTER="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
  test -z "$PID_AFTER" && break
  sleep 0.5
done
test -z "$(adb shell pidof "$PKG" | tr -d '\r' || true)"

adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/tmp/kaala-kolam-relaunch.txt
wait_for_idle
wait_for_question "${QUESTIONS[4]}"
assert_restored_state
capture_evidence "after-process-death-relaunch"

echo "ELDERS_PROCESS_DEATH_RESUME_PASS api=34 currentTab=elders questionIndex=4 answers=5"
