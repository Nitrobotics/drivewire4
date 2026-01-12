#!/usr/bin/env bash
set -euo pipefail
APP_DEST="${1:-/Applications}"
APP_DIR="$APP_DEST/DriveWire 4.app"
echo "Removing: $APP_DIR"
rm -rf "$APP_DIR"
echo "Uninstalled."
