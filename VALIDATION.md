# Validation — 28 October 2026

## Passed

- Android debug APK compilation with JDK 21, Gradle 8.13, AGP 8.9.2, SDK 35.
- `./gradlew --no-daemon --max-workers=2 assembleDebug lintDebug` using the
  checksum-pinned wrapper: successful. Lint: zero errors, eight warnings, covering
  English UI strings, portrait orientation, and colour-change gradient allocation.
- `./scripts/test-physics.sh`: 396,009 assertions across deformation, damping,
  wall reflection, collision impulses, gravity, drag following, release momentum,
  softness, reset, and repeated impulses at every tested softness setting.
- `bash scripts/setup-cloud.sh`: repeated successfully with installed dependencies.
- `apksigner verify --verbose`: APK verifies with v1 and v2 signatures.
- Manifest inspection: Android 6.0 minimum, target Android 15, launchable activity,
  no network or dangerous permissions, no native ABI restriction.

## Not yet verified on a device

The Android 30 software emulator was installed and started, but its system server
failed to initialise storage/settings services. Installation and launch on that
emulator failed before application execution. This is an emulator boot failure,
not evidence that the application runs. UI automation scripts are supplied but
have not passed a device run. Physical-device installation, rendering, touch
interaction, device shaking, and haptics still need phone validation.

## Phone checklist

1. Install `dist/jelly-dolphin-1.0.apk` on Android 6.0 or newer.
2. Tap the dolphin: it should deform, wobble, and settle.
3. Drag and release: it should follow, fly, and bounce off the walls.
4. Select each colour. Compare firm and extra jelly settings.
5. Turn gravity on: it should fall and bounce at the floor.
6. Try Shake, Reset, and physically shaking the phone.
7. Close and reopen: colour, squishiness, and gravity should be remembered.
8. Background and resume the app; repeat on a small screen or large font setting.

On a working emulator or connected test phone, run `scripts/smoke-device.sh` and
`python3 tests/device-interactions.py` with `adb` on PATH. The second script changes
colour, squishiness, and gravity preferences to exercise persistence.

This APK is debug-signed for direct installation and testing. Store distribution
requires a release signing key and further device coverage.
