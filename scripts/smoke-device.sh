#!/usr/bin/env bash
# Requires a booted emulator or connected Android device. Does not erase app data.
set -euo pipefail
cd "$(dirname "$0")/.."
adb get-state
[[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == 1 ]]
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.jellydolphin.app
adb logcat -c
adb shell am start -W -n com.jellydolphin.app/.MainActivity
sleep 3
adb shell pidof com.jellydolphin.app
mkdir -p build/device-smoke
adb exec-out screencap -p > build/device-smoke/launch.png
adb shell uiautomator dump /sdcard/jelly-ui.xml
adb pull /sdcard/jelly-ui.xml build/device-smoke/ui.xml
python3 - <<'PY'
import xml.etree.ElementTree as E
nodes=list(E.parse('build/device-smoke/ui.xml').iter('node'))
for label in ['Jelly Dolphin','Shake','Reset']:
 assert any(n.get('text')==label for n in nodes),label+' missing'
for label in ['Gravity','Squishiness, firm to extra jelly']:
 assert any(n.get('content-desc')==label for n in nodes),label+' missing'
print('PASS: native UI and controls present')
PY
if adb logcat -d -s AndroidRuntime:E | rg 'FATAL EXCEPTION'; then exit 1; fi
printf 'PASS: APK installed and launched without an AndroidRuntime crash\n'
