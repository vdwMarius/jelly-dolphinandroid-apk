#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/physics-tests
javac -d build/physics-tests app/src/main/java/com/jellydolphin/app/SoftBody3D.java tests/PhysicsTest.java
java -cp build/physics-tests PhysicsTest
