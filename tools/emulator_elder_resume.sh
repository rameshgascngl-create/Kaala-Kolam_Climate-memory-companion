#!/usr/bin/env bash
set -euxo pipefail

PKG="edu.gascnagercoil.kaalakolam.debug"
COMPONENT="$PKG/edu.gascnagercoil.kaalakolam.MainActivity"
STATE_FILE="tests/emulator/elder-q5-state.json"
QUESTION="How much water do wells and ponds hold in the dry months, compared with before?"
EVIDENCE_ROOT="app/build/emulator-evidence"
mkdir -p "$EVIDENCE_ROOT"

capture_evidence() {
  local stage="$1"
  local dir="$EVIDENCE_ROOT/$stage"
  mkdir -p "$dir"
  set +e

  adb exec-out screencap -p >"$dir/screenshot.png" 2>"$dir/screenshot.err"

  adb shell uiautomator dump /sdcard/kaala-kolam-window.xml >"$dir/uiautomator-command.txt" 2>&1
  adb exec-out cat /sdcard/kaala-kolam-window.xml >"$dir/uiautomator.xml" 2>"$dir/uiautomator.err"

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

adb shell wm size 390x844
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0

adb shell am force-stop "$PKG" || true
adb shell "run-as $PKG mkdir -p files/datastore"
STATE_B64="$(base64 -w0 "$STATE_FILE")"
adb shell "run-as $PKG sh -c 'echo $STATE_B64 | base64 -d > files/datastore/kaala_kolam_state.json'"

dump_window() {
  adb shell uiautomator dump /sdcard/kaala-kolam-window.xml >/dev/null
  adb shell cat /sdcard/kaala-kolam-window.xml > /tmp/kaala-kolam-window.xml
}

assert_question_five() {
  dump_window
  grep -F "$QUESTION" /tmp/kaala-kolam-window.xml
  grep -F "Resume Elder" /tmp/kaala-kolam-window.xml
}

adb shell am start -W -n "$COMPONENT" >/tmp/kaala-kolam-start.txt
sleep 2
capture_evidence "after-restored-launch"

# Diagnostic-only navigation retained for this evidence run. The next revision
# replaces this coordinate with stable Compose semantics after classification.
adb shell input tap 162 780
sleep 1
capture_evidence "after-legacy-navigation"
assert_question_five

# Rotation must retain the same persisted question/session.
adb shell settings put system user_rotation 1
sleep 2
assert_question_five
adb shell settings put system user_rotation 0
sleep 2
assert_question_five

# Simulate OS process death rather than clearing app data/task state.
adb shell input keyevent KEYCODE_HOME
sleep 1
PID_BEFORE="$(adb shell pidof "$PKG" | tr -d '\r')"
test -n "$PID_BEFORE"
adb shell am kill "$PKG"

for _ in 1 2 3 4 5; do
  PID_AFTER="$(adb shell pidof "$PKG" | tr -d '\r' || true)"
  test -z "$PID_AFTER" && break
  sleep 1
done
test -z "$(adb shell pidof "$PKG" | tr -d '\r' || true)"

# Relaunch from the launcher. The restored task must return to the same Elders question;
# no navigation tap is allowed after process death.
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/tmp/kaala-kolam-relaunch.txt
sleep 3
capture_evidence "after-process-death-relaunch"
assert_question_five

echo "ELDERS_PROCESS_DEATH_RESUME_PASS questionIndex=4 api=34"
