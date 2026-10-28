#!/usr/bin/env bash
# Reproducible toolchain installation; no source or dependency files are rewritten.
set -euo pipefail
TOOLS=/workspace/toolchains
mkdir -p "$TOOLS/downloads"
fetch() { curl --fail --location --retry 2 "$1" -o "$2"; }
if [[ ! -x "$TOOLS/jdk-21.0.8+9/bin/javac" ]]; then
  base='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz'
  fetch "$base" "$TOOLS/downloads/jdk.tar.gz"
  fetch "$base.sha256.txt" "$TOOLS/downloads/jdk.sha256"
  python3 - "$TOOLS/downloads" <<'PY'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1])
assert hashlib.sha256((p/'jdk.tar.gz').read_bytes()).hexdigest()==(p/'jdk.sha256').read_text().split()[0], 'JDK checksum mismatch'
PY
  tar -xzf "$TOOLS/downloads/jdk.tar.gz" -C "$TOOLS"
fi
if [[ ! -x "$TOOLS/gradle-8.13/bin/gradle" ]]; then
  fetch https://downloads.gradle.org/distributions/gradle-8.13-bin.zip "$TOOLS/downloads/gradle.zip"
  fetch https://downloads.gradle.org/distributions/gradle-8.13-bin.zip.sha256 "$TOOLS/downloads/gradle.sha256"
  python3 - "$TOOLS/downloads" <<'PY'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1])
assert hashlib.sha256((p/'gradle.zip').read_bytes()).hexdigest()==(p/'gradle.sha256').read_text().strip(), 'Gradle checksum mismatch'
PY
  unzip -q "$TOOLS/downloads/gradle.zip" -d "$TOOLS"
fi
if [[ ! -x "$TOOLS/android-sdk/cmdline-tools/latest/bin/sdkmanager" ]]; then
  fetch https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip "$TOOLS/downloads/android-cli.zip"
  mkdir -p "$TOOLS/android-sdk/cmdline-tools"
  unzip -q "$TOOLS/downloads/android-cli.zip" -d "$TOOLS/android-sdk/cmdline-tools"
  mv "$TOOLS/android-sdk/cmdline-tools/cmdline-tools" "$TOOLS/android-sdk/cmdline-tools/latest"
fi
cat > "$TOOLS/activate.sh" <<'ACTIVATE'
export JAVA_HOME=/workspace/toolchains/jdk-21.0.8+9
export ANDROID_HOME=/workspace/toolchains/android-sdk
export PATH="$JAVA_HOME/bin:/workspace/toolchains/gradle-8.13/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts=localhost|127.* -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
ACTIVATE
source "$TOOLS/activate.sh"
# sdkmanager exits once it has all acceptance responses; tolerate yes receiving SIGPIPE.
set +o pipefail
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses >/dev/null
set -o pipefail
sdkmanager --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
cd /workspace/jelly-dolphinandroid-apk
./scripts/test-physics.sh
gradle --no-daemon --max-workers=2 assembleDebug lintDebug
