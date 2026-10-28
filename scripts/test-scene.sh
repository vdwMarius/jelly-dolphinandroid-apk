#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/scene build/host-tests
javac -d build/host-tests $(find tests/host -name '*.java') app/src/main/java/com/jellydolphin/app/{SoftBody3D,RoomRenderer}.java
java -cp build/host-tests SceneTest
python3 tests/render-scene.py
