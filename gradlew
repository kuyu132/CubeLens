#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [ ! -x "$JAVA_BIN" ]; then
  JAVA_BIN="java"
fi

exec "$JAVA_BIN" -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"

