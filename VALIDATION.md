# Version 2.0 validation — 28 October 2026

## Passed

- Native 3D Android APK build: JDK 21, Gradle 8.13, AGP 8.9.2, SDK 35.
- `./gradlew --no-daemon --max-workers=2 assembleDebug lintDebug`.
- `./scripts/test-physics.sh`: mesh deformation and recovery, no-gravity rest,
  3D grab/lift/depth, throw momentum, depth translation, gravity/floor settling,
  thickness retention, repeated impulses across firm/medium/soft settings,
  squishiness compliance, ball grabbing, ball/floor contacts and reset.
- `./scripts/test-scene.sh`: Java renderer geometry capture, perspective picking,
  lifting, immediate pinch depth change, ball picking/tapping, orbit and reset.
- The captured renderer geometry and its **original GLES shaders** were compiled,
  linked, and rasterized using Mesa EGL/GLES (llvmpipe), with no GL error.
  The frame was inspected visually. This validates rendering on a desktop GLES
  implementation; it does not validate Android lifecycle, touch delivery or a
  phone's GPU driver.
- APK signature verification and package/version/minimum SDK inspection.

## Still needs phone testing

The earlier Android 30 software emulator failed to boot system services on this
host. This update has not been installed or launched on a working Android device.
The user plans to test the APK on their phone. Device smoke/interaction scripts
are included but are not claimed as passed. Real-device performance, gestures,
Android lifecycle, motion sensors, haptics and GPU compatibility remain unverified.

## Phone checklist

1. Install `dist/jelly-dolphin-2.0.apk` as an update to version 1.0.
2. Confirm a shaded 3D dolphin, room, ball and raised blocks are visible.
3. Tap the dolphin, then grab a fin or body point. Check visible deformation,
   lifting, release momentum, jiggle and recovery.
4. Pinch while grabbing to move in depth; also try the Depth slider.
5. Orbit by dragging empty space. Pinch empty space to zoom.
6. Throw the dolphin at the floor, walls, blocks and ball. Tap and drag the ball.
7. Compare firm versus extra jelly. Try all six colours, gravity, Shake and Reset.
8. Background/resume and close/reopen. Colour, squishiness and gravity persist.
9. Try How to play, large font settings, and sustained play to assess frame rate.

See README for solver approximations. The APK is debug-signed for direct testing,
not signed with a production/store key.
