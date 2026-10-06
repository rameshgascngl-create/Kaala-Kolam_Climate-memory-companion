#!/usr/bin/env bash
set -euo pipefail

PKG="edu.gascnagercoil.kaalakolam.debug"
COMPONENT="$PKG/edu.gascnagercoil.kaalakolam.MainActivity"
STATE_FILE="tests/emulator/elder-q5-state.json"
QUESTION="How much water do wells and ponds hold in the dry months, compared with before?"

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
# 390 dp viewport at 160 dpi; Elders is the third of six bottom tabs.
adb shell input tap 162 780
sleep 1
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
assert_question_five

echo "ELDERS_PROCESS_DEATH_RESUME_PASS questionIndex=4 api=34"
