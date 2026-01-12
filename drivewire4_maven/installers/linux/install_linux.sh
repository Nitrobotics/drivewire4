#!/bin/bash
set -e

APP_NAME=DriveWire4
# The build process moves this installer into the distribution working directory.
# Treat the installer's own directory as the distribution root.
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
INSTALLDIR="$HOME/drive_wire_4_java"

if [ -n "$1" ]; then
  INSTALLDIR="$1"
fi

echo "Installing $APP_NAME"
echo "  Source: $SOURCE_DIR"
echo "  Target: $INSTALLDIR"

if [ -d "$INSTALLDIR" ]; then
  rm -rf "$INSTALLDIR"
fi

mkdir -p "$INSTALLDIR"
cp -R "$SOURCE_DIR"/* "$INSTALLDIR"/

# Ensure launcher + uninstaller are executable
chmod +x "$INSTALLDIR"/run-linux.sh 2>/dev/null || true

# Create a simple start launcher that prefers a bundled JRE
cat > "$INSTALLDIR/start" <<'EOS'
#!/bin/bash
cd "$(dirname "$0")"

JAVA="java"
if [ -x "./jre/bin/java" ]; then
  JAVA="./jre/bin/java"
fi

# SWT on Linux can require X11 backend on some distros/Wayland configs
export GDK_BACKEND=${GDK_BACKEND:-x11}

exec "$JAVA" -Djava.library.path="./lib" -jar "./drivewire4.jar" "$@"
EOS
chmod +x "$INSTALLDIR/start"

# Desktop launcher
mkdir -p "$HOME/.local/share/applications"
cat > "$HOME/.local/share/applications/drivewire4.desktop" <<EOF2
[Desktop Entry]
Type=Application
Name=DriveWire 4
Exec=/bin/bash "$INSTALLDIR/start"
Path=$INSTALLDIR
Icon=$INSTALLDIR/dw4_icon.png
Terminal=false
Categories=Utility;
EOF2

# Uninstaller
cat > "$INSTALLDIR/uninstall_linux.sh" <<'EOU'
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
echo "DriveWire4 uninstalled."
EOU
chmod +x "$INSTALLDIR/uninstall_linux.sh"

# Update drivewireUI.xml to point at the local copy of HDB-DOS
XML_FILE="$INSTALLDIR/drivewireUI.xml"
if [ -f "$XML_FILE" ]; then
  DW4_HDB_URL="file://$INSTALLDIR/hdb-dos/index.html"
  FOLDER="<Folder title=\"HDB-DOS\"><URL title=\"Load HDB-DOS\">$DW4_HDB_URL</URL></Folder>"
  # Only insert the folder if it is not already present
  if ! grep -q "$FOLDER" "$XML_FILE"; then
    # Insert the folder immediately before the closing </Local> tag
    sed -i.bak "s#</Local>#$FOLDER</Local>#" "$XML_FILE"
  fi
fi

echo "Installation complete."
echo "You can launch from your application menu, or run: $INSTALLDIR/start"
