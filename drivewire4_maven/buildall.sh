#!/usr/bin/env bash
set -euo pipefail

PLATFORMS=(linux-x86_64 linux-aarch64 macos-x86_64 macos-aarch64 windows-x86_64)
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/dist-build"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

for P in "${PLATFORMS[@]}"; do
  echo "=== Building for $P ==="
  (cd "$ROOT_DIR" && mvn -DskipTests -Dswt.platform="$P" clean package)

  ZIP_PATH=$(ls -1t "$ROOT_DIR"/target/*"$P"*.zip 2>/dev/null | head -n 1 || true)
  if [ -z "$ZIP_PATH" ]; then
    echo "ERROR: Could not find target/*$P*.zip after building for $P" >&2
    exit 1
  fi

  DEST="$OUT_DIR/$P"
  mkdir -p "$DEST"
  unzip -q "$ZIP_PATH" -d "$DEST"

  # Flatten single top-level directory
  TOP=$(find "$DEST" -mindepth 1 -maxdepth 1 -type d | head -n 1 || true)
  if [ -n "$TOP" ] && [ -f "$TOP/drivewire4.jar" ]; then
    shopt -s dotglob
    mv "$TOP"/* "$DEST"/
    rmdir "$TOP"
    shopt -u dotglob
  fi

  # Ensure required working-dir files
  for f in config.xml drivewireUI.xml drivewire4.jar dw4_icon.png help.xml master.xml; do
    if [ ! -f "$DEST/$f" ]; then
      echo "ERROR: Missing $f in $DEST" >&2
      exit 1
    fi
  done

  # Ensure soundbank-deluxe.gm from source is present in working directory
  if [ -f "$ROOT_DIR/soundbank-deluxe.gm" ]; then
    cp "$ROOT_DIR/soundbank-deluxe.gm" "$DEST/soundbank-deluxe.gm"
  else
    echo "WARNING: soundbank-deluxe.gm not found in source; no soundbank copied to $DEST" >&2
  fi

  # Remove buildall scripts from binary distribution folder
  rm -f "$DEST/buildall.sh" "$DEST/buildall.bat" 2>/dev/null || true


  # Ensure HDB-DOS wavs are present in hdb-dos directory
  for f in hdbdw3cc1.wav hdbdw3cc2.wav hdbdw3cc3.wav; do
    if [ ! -f "$DEST/hdb-dos/$f" ]; then
      echo "ERROR: Missing hdb-dos/$f in $DEST" >&2
      exit 1
    fi
  done

  # Ensure visible jre placeholder
  mkdir -p "$DEST/jre"
  if [ ! -f "$DEST/jre/README-JRE.txt" ]; then
    echo "Place a platform JRE here (jre/bin/java)." > "$DEST/jre/README-JRE.txt"
  fi

  # Ensure HDB-DOS payload exists
  mkdir -p "$DEST/hdb-dos"
  if [ ! -f "$DEST/hdb-dos/index.html" ]; then
    echo "ERROR: Missing hdb-dos/index.html in $DEST" >&2
    exit 1
  fi

  # Platform-specific installers only
  if [ -d "$DEST/installers" ]; then
    case "$P" in
      linux-*)
        rm -rf "$DEST/installers/windows" "$DEST/installers/macos" || true
        ;;
      macos-*)
        rm -rf "$DEST/installers/windows" "$DEST/installers/linux" || true
        ;;
      windows-*)
        rm -rf "$DEST/installers/linux" "$DEST/installers/macos" || true
        ;;
    esac
  fi

  # Move installer/uninstaller scripts into the working directory (more intuitive for users)
  if [ -d "$DEST/installers" ]; then
    case "$P" in
      linux-*)
        if [ -f "$DEST/installers/linux/install_linux.sh" ]; then
          mv "$DEST/installers/linux/install_linux.sh" "$DEST/install_linux.sh"
          chmod +x "$DEST/install_linux.sh" || true
        fi
        if [ -f "$DEST/installers/linux/uninstall_linux.sh" ]; then
          mv "$DEST/installers/linux/uninstall_linux.sh" "$DEST/uninstall_linux.sh"
          chmod +x "$DEST/uninstall_linux.sh" || true
        fi
        ;;
      macos-*)
        if [ -f "$DEST/installers/macos/install_macos.sh" ]; then
          mv "$DEST/installers/macos/install_macos.sh" "$DEST/install_macos.sh"
          chmod +x "$DEST/install_macos.sh" || true
        fi
        if [ -f "$DEST/installers/macos/uninstall_macos.sh" ]; then
          mv "$DEST/installers/macos/uninstall_macos.sh" "$DEST/uninstall_macos.sh"
          chmod +x "$DEST/uninstall_macos.sh" || true
        fi
        ;;
      windows-*)
        if [ -f "$DEST/installers/windows/install_windows.bat" ]; then
          mv "$DEST/installers/windows/install_windows.bat" "$DEST/install_windows.bat"
        fi
        if [ -f "$DEST/installers/windows/uninstall_windows.bat" ]; then
          mv "$DEST/installers/windows/uninstall_windows.bat" "$DEST/uninstall_windows.bat"
        fi
        if [ -f "$DEST/installers/windows/dw4_icon.ico" ]; then
          mv "$DEST/installers/windows/dw4_icon.ico" "$DEST/dw4_icon.ico"
        fi
        ;;
    esac
    rm -rf "$DEST/installers" || true
  fi

  # Remove irrelevant run scripts; keep only those for the target platform
  case "$P" in
    linux-*)
      rm -f "$DEST"/run-macos*.sh "$DEST"/run-windows.bat 2>/dev/null || true
      ;;
    macos-*)
      rm -f "$DEST"/run-linux*.sh "$DEST"/run-windows.bat 2>/dev/null || true
      # RoSetta helper isn't needed in per-arch builds
      rm -f "$DEST"/run-macos-rosetta.sh 2>/dev/null || true
      ;;
    windows-*)
      rm -f "$DEST"/run-linux*.sh "$DEST"/run-macos*.sh 2>/dev/null || true
      ;;
  esac

  # Include libx.so only for linux-aarch64
  if [ "$P" = "linux-aarch64" ]; then
    mkdir -p "$DEST/lib"
    if [ -f "$ROOT_DIR/libx.so" ]; then
      cp "$ROOT_DIR/libx.so" "$DEST/lib/libx.so"
    fi
  else
    rm -f "$DEST/lib/libx.so" 2>/dev/null || true
  fi

  echo "Built: $DEST"
done

echo "All builds complete in: $OUT_DIR"
