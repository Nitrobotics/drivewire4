#!/bin/bash
set -e
INSTALLDIR="$HOME/drive_wire_4_java"
if [ -n "$1" ]; then
  INSTALLDIR="$1"
fi
if [ -d "$INSTALLDIR" ]; then
  rm -rf "$INSTALLDIR"
fi
rm -f "$HOME/.local/share/applications/drivewire4.desktop"
echo "Uninstalled DriveWire4."
