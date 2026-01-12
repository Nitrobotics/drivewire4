#!/usr/bin/env bash
set -euo pipefail

APP_NAME="DriveWire 4"

# Dist root (the working directory that contains drivewire4.jar, config.xml, drivewireUI.xml, jre/, etc.)
# NOTE: The buildall scripts move this installer into the distribution working directory,
# so we treat the installer's own directory as the distribution root.
SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default install location
DEST_DIR="/Applications"
if [[ $# -ge 1 && -n "${1:-}" ]]; then
  DEST_DIR="$1"
fi

APP_DIR="${DEST_DIR}/${APP_NAME}.app"

# If the destination directory isn't writable, instruct the user to re-run with sudo.
if [[ ! -w "${DEST_DIR}" ]]; then
  echo "ERROR: '${DEST_DIR}' is not writable." >&2
  echo "Please run the installer with sudo, for example:" >&2
  echo "  sudo ./install_macos.sh" >&2
  echo "Or install into a user-writable location, for example:" >&2
  echo "  ./install_macos.sh \"${HOME}/Applications\"" >&2
  exit 1
fi

echo "Installing ${APP_NAME} to: ${APP_DIR}"

rm -rf "${APP_DIR}"
mkdir -p "${APP_DIR}/Contents/MacOS" "${APP_DIR}/Contents/Resources/app" "${APP_DIR}/Contents/Resources/jre"

# Copy distribution payload into app bundle (excluding the app bundle itself if re-running)
rsync -a --exclude "${APP_NAME}.app" --exclude ".DS_Store" "${SOURCE_DIR}/" "${APP_DIR}/Contents/Resources/app/"

# Copy bundled JRE (expected at ./jre next to the installer working dir)
if [[ -d "${SOURCE_DIR}/jre" ]]; then
  echo "Copying bundled JRE from: ${SOURCE_DIR}/jre"
  rsync -a "${SOURCE_DIR}/jre/" "${APP_DIR}/Contents/Resources/jre/"
else
  echo "NOTE: No ./jre folder found next to the installer; the app will require a system Java or you can add a JRE later at:"
  echo "  ${APP_DIR}/Contents/Resources/jre/"
fi

# If installed with sudo (common for /Applications), ensure the installed payload is writable
# by the invoking user, because DriveWire edits files like config.xml and drivewireUI.xml.
if [[ -n "${SUDO_USER:-}" ]]; then
  echo "Adjusting ownership so '${SUDO_USER}' can modify configuration files inside the app bundle..."
  chown -R "${SUDO_USER}:staff" "${APP_DIR}/Contents/Resources/app" "${APP_DIR}/Contents/Resources/jre" 2>/dev/null || \
    chown -R "${SUDO_USER}:$(id -gn "${SUDO_USER}")" "${APP_DIR}/Contents/Resources/app" "${APP_DIR}/Contents/Resources/jre" || true
  chmod -R u+rwX "${APP_DIR}/Contents/Resources/app" "${APP_DIR}/Contents/Resources/jre" || true
fi

# Create launcher
cat > "${APP_DIR}/Contents/MacOS/DriveWire4" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PAYLOAD="${APP_ROOT}/Resources/app"

# Prefer bundled JRE in the app bundle (visible)
JAVA_BIN="${APP_ROOT}/Resources/jre/bin/java"
if [[ -x "${JAVA_BIN}" ]]; then
  echo "Using bundled JRE: ${JAVA_BIN}"
else
  echo "Bundled JRE not found or not executable at ${JAVA_BIN}; falling back to system Java" >&2
  if command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(command -v java)"
  else
    osascript -e 'display dialog "Java not found.\n\nInstall a JRE or place one in:\nDriveWire 4.app/Contents/Resources/jre/" buttons {"OK"} default button "OK" with icon stop'
    exit 1
  fi
fi

cd "${PAYLOAD}"

# SWT on macOS requires this.
exec "${JAVA_BIN}" -XstartOnFirstThread -Dswt.autoScale=false -Djava.library.path="${PAYLOAD}/lib" -jar "${PAYLOAD}/drivewire4.jar" "$@"
EOF
chmod +x "${APP_DIR}/Contents/MacOS/DriveWire4"

# Generate and install an .icns from the provided PNG, if available.
# (Uses macOS tools: sips + iconutil.)
ICON_PNG="${SOURCE_DIR}/dw4_icon.png"
ICON_ICNS="${APP_DIR}/Contents/Resources/dw4_icon.icns"
if [[ -f "${ICON_PNG}" && "$(uname)" == "Darwin" ]]; then
  TMPDIR="$(mktemp -d)"
  ICONSET="${TMPDIR}/dw4_icon.iconset"
  mkdir -p "${ICONSET}"

  # Create required sizes
  sips -z 16 16     "${ICON_PNG}" --out "${ICONSET}/icon_16x16.png" >/dev/null
  sips -z 32 32     "${ICON_PNG}" --out "${ICONSET}/icon_16x16@2x.png" >/dev/null
  sips -z 32 32     "${ICON_PNG}" --out "${ICONSET}/icon_32x32.png" >/dev/null
  sips -z 64 64     "${ICON_PNG}" --out "${ICONSET}/icon_32x32@2x.png" >/dev/null
  sips -z 128 128   "${ICON_PNG}" --out "${ICONSET}/icon_128x128.png" >/dev/null
  sips -z 256 256   "${ICON_PNG}" --out "${ICONSET}/icon_128x128@2x.png" >/dev/null
  sips -z 256 256   "${ICON_PNG}" --out "${ICONSET}/icon_256x256.png" >/dev/null
  sips -z 512 512   "${ICON_PNG}" --out "${ICONSET}/icon_256x256@2x.png" >/dev/null
  sips -z 512 512   "${ICON_PNG}" --out "${ICONSET}/icon_512x512.png" >/dev/null
  sips -z 1024 1024 "${ICON_PNG}" --out "${ICONSET}/icon_512x512@2x.png" >/dev/null

  iconutil -c icns "${ICONSET}" -o "${ICON_ICNS}"
  rm -rf "${TMPDIR}"
fi

# Info.plist
cat > "${APP_DIR}/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleName</key><string>${APP_NAME}</string>
  <key>CFBundleDisplayName</key><string>${APP_NAME}</string>
  <key>CFBundleIdentifier</key><string>com.groupunix.drivewire4</string>
  <key>CFBundleVersion</key><string>1.0</string>
  <key>CFBundleShortVersionString</key><string>1.0</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleExecutable</key><string>DriveWire4</string>
  <key>LSMinimumSystemVersion</key><string>10.13</string>
  <key>CFBundleIconFile</key><string>dw4_icon</string>
</dict>
</plist>
EOF

# Preserve original behavior: point UI to local HDB-DOS (avoid duplicates)
XML_FILE="${APP_DIR}/Contents/Resources/app/drivewireUI.xml"
if [[ -f "${XML_FILE}" ]]; then
  LOCALURL="file://${APP_DIR}/Contents/Resources/app/hdb-dos/index.html"
  python3 - <<PY
import pathlib

p = pathlib.Path(r'''${XML_FILE}''')
xml = p.read_text(encoding='utf-8', errors='ignore')

localurl = r'''${LOCALURL}'''
folder = f'<Folder title="HDB-DOS"><URL title="Load HDB-DOS">{localurl}</URL></Folder>'

# Insert exactly once inside <Local>...</Local>
if folder not in xml and '</Local>' in xml:
    xml = xml.replace('</Local>', folder + '</Local>', 1)

p.write_text(xml, encoding='utf-8')
PY
else
  echo "WARNING: drivewireUI.xml not found; skipping UI configuration edits."
fi

echo "Install complete. Launch from Applications: ${APP_NAME}.app"
