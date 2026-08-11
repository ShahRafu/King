#!/usr/bin/env sh
set -e

# Simple Gradle wrapper launcher: prefers project wrapper JAR, falls back to system gradle.
# Note: gradle-wrapper.jar must be present at gradle/wrapper/gradle-wrapper.jar

PRGDIR="$(cd "$(dirname "$0")" && pwd)"

# If wrapper JAR exists, run it with java
if [ -f "$PRGDIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -cp "$PRGDIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi

# If system gradle is available, use it
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "ERROR: Could not find Gradle wrapper (gradle/wrapper/gradle-wrapper.jar) or system 'gradle' command."
echo "Generate wrapper locally (gradle wrapper --gradle-version <version>) and commit gradle/wrapper/* or install Gradle on the runner."
exit 1
