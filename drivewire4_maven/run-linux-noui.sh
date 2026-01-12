#!/bin/bash
# DriveWire 4 - Linux Launch Script (No UI - Server Only)

cd "$(dirname "$0")"

# If we're in a packaged distribution ZIP, the jar is in the current directory.
JAR=$(ls -1 drivewire4.jar 2>/dev/null | head -n 1)
if [ -z "$JAR" ]; then
  JAR=$(ls -1 drivewire4-*.jar 2>/dev/null | head -n 1)
fi
LIBDIR="./lib"

# If we're running from the source tree, build (or rebuild) into target/.
if [ -z "$JAR" ]; then
  JAR="target/drivewire4-4.3.6p.jar"
  LIBDIR="target/lib"

  if [ ! -f "$JAR" ]; then
      echo "Building project for this host platform..."
      mvn clean package -DskipTests
  fi
fi

# Run server without UI
java -Djava.library.path="$LIBDIR" \
     -jar "$JAR" --noui "$@"
