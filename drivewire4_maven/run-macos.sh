#!/usr/bin/env bash
# DriveWire 4 - macOS Run Script
#
# This script is intended for running a packaged distribution folder.
# It does NOT build/compile anything.

set -euo pipefail

cd "$(dirname "$0")"

# Prefer bundled JRE if present (visible ./jre folder)
JAVA="java"
if [[ -x "./jre/bin/java" ]]; then
  JAVA="./jre/bin/java"
fi

JAR="./drivewire4.jar"
if [[ ! -f "$JAR" ]]; then
  echo "ERROR: $JAR not found in $(pwd)" >&2
  echo "This run script expects a packaged distribution folder." >&2
  exit 1
fi

LIBDIR="./lib"
if [[ -d "$LIBDIR" ]]; then
  exec "$JAVA" -XstartOnFirstThread -Dswt.autoScale=false -Djava.library.path="$LIBDIR" -jar "$JAR" "$@"
else
  exec "$JAVA" -XstartOnFirstThread -Dswt.autoScale=false -jar "$JAR" "$@"
fi
