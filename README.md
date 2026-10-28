# Jelly Dolphin

An offline native Android pocket toy. Tap a glossy dolphin to squish it, drag and
release to throw it, and watch its spring-driven body and tail wobble as it bounces
against the playground walls. Six colours, adjustable squishiness, optional gravity,
a Shake button, device-shake support, and saved appearance/physics preferences.
No accounts, network permissions, external services, or runtime dependencies.

## Install

The directly installable testing APK is `dist/jelly-dolphin-1.0.apk`.
Copy it to an Android 6.0+ device, open it, and allow installation from the app you
used to open the file when Android prompts. This is a debug-signed testing build,
not a Play Store release. Keep a dedicated private release signing key for publishing;
never commit signing credentials. With an attached device:

```sh
adb install -r dist/jelly-dolphin-1.0.apk
```

## Build and test

Use JDK 21, Android SDK platform 35 and build-tools 35.0.0. The Gradle wrapper pins
Gradle 8.13 and verifies its SHA-256. No third-party application dependencies.

```sh
# In this cloud environment:
bash scripts/setup-cloud.sh
source /workspace/toolchains/activate.sh
# On a normal development machine, set JAVA_HOME and ANDROID_HOME instead.
./gradlew --no-daemon --max-workers=2 assembleDebug lintDebug
./scripts/test-physics.sh
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.
Lint report: `app/build/reports/lint-results-debug.html`.
The standalone physics suite checks taps, damping, collision impulses, gravity,
drag following, release momentum, softness, reset, and long-run numerical stability.

## Implementation

`JellyPhysics` integrates position and damped deformation springs in fixed substeps.
`DolphinView` renders vector curves with the same deformation field applied to the
outline, flippers, belly, eyes, and smile. It uses a single-pointer spring anchor
and bounded release velocity. Animation and sensors pause with the activity.
`MainActivity` supplies accessible native controls and persists preferences.

The initial UI is English and designed for portrait phones. Device-shake behaviour
and haptics require suitable hardware. See `VALIDATION.md` for checks actually run.
