# DriveWire 4 - Maven Build

A modern implementation of the DriveWire protocol for connecting vintage computers to modern systems.

## Requirements

- **Java 21 LTS** (strongly recommended)
- Maven 3.6+

**⚠️ IMPORTANT:** Java 25 is NOT compatible with SWT on macOS and will cause native crashes in CoreGraphics. Please use **Java 21 LTS** for best compatibility.

To check your Java version:
```bash
java -version
```

To install Java 21 on macOS (using Homebrew):
```bash
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## Building

```bash
mvn clean package
```

This will:
1. Compile all source files
2. Download platform-specific SWT libraries automatically
3. Create `target/drivewire4-4.3.6p.jar`
4. Copy all dependencies to `target/lib/`

## Running

### macOS (IMPORTANT!)
SWT on macOS requires the `-XstartOnFirstThread` JVM flag. Use the provided script:

```bash
./run-macos.sh
```

Or run manually:
```bash
java -XstartOnFirstThread -Djava.library.path=target/lib -jar target/drivewire4-4.3.6p.jar
```

**Do NOT use `mvn exec:java` on macOS** - it will crash because Maven spawns a new thread.

### Linux
```bash
./run-linux.sh
```

Or:
```bash
java -Djava.library.path=target/lib -jar target/drivewire4-4.3.6p.jar
```

Or with Maven (works on Linux):
```bash
mvn exec:java
```

### Windows
```batch
run-windows.bat
```

Or:
```batch
java -Djava.library.path=target\lib -jar target\drivewire4-4.3.6p.jar
```

## Platform Support

The build automatically detects your OS and architecture:

| Platform | Architecture | SWT Library |
|----------|-------------|-------------|
| Linux | x86_64 (amd64) | org.eclipse.swt.gtk.linux.x86_64 |
| Linux | aarch64 (ARM64) | org.eclipse.swt.gtk.linux.aarch64 |
| macOS | x86_64 (Intel) | org.eclipse.swt.cocoa.macosx.x86_64 |
| macOS | aarch64 (Apple Silicon) | org.eclipse.swt.cocoa.macosx.aarch64 |
| Windows | x86_64 | org.eclipse.swt.win32.win32.x86_64 |

## Troubleshooting

### macOS: SIGSEGV crash in CoreGraphics (rgba32_image_mark)
This crash occurs with **Java 25** due to incompatibility with SWT's native Cocoa bindings.
- **Solution:** Use **Java 21 LTS** instead of Java 25
- The crash happens even with `-XstartOnFirstThread`
- This is a known issue with very new Java versions and SWT

### macOS: Generic SIGSEGV crash
- Make sure you're using `-XstartOnFirstThread` (use `run-macos.sh`)
- Make sure you're using Java 21 LTS, not Java 25
- Use the `run-macos.sh` script

### macOS: "no swt-cocoa" or "no swt-carbon" error
- Make sure you're running the packaged JAR, not `mvn exec:java`
- Verify the correct SWT JAR is in `target/lib/`

### Linux: "no swt-gtk" error
- Install GTK3 development libraries: `sudo apt install libgtk-3-0`

### Serial port issues
- The RXTX native library loading is disabled by default
- Serial communication uses nrjavaserial which includes native libraries

## Configuration

- `config.xml` - Server configuration
- `drivewireUI.xml` - UI preferences
- `master.xml` - Master configuration (created on first run)

## Dependencies

All dependencies are managed by Maven and downloaded automatically from:
- Maven Central
- Nuiton Repository (for gnu.cajo)

The `swing2swt-1.6.0.jar` library is included in the `lib/` directory.

### macOS: Additional options if GUI crashes

If the full SWT GUI crashes on macOS, try these alternatives:

**Option 1: Lite UI (Swing-based)**
```bash
./run-macos-lite.sh
```
This uses a simpler Swing-based UI that doesn't depend on SWT/Cocoa.

**Option 2: Rosetta 2 (x86_64 emulation)**
```bash
./run-macos-rosetta.sh
```
This runs the x86_64 version under Rosetta 2 emulation, which may be more stable.

**Option 3: Server only (no GUI)**
```bash
./run-macos-noui.sh
```
This runs only the server without any graphical interface.
