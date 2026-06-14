#!/bin/sh
# Gradle wrapper script for Unix
GRADLE_OPTS="${GRADLE_OPTS} \"-Xdock:name=FinanzManager\""
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
