# DriveWire 4
DriveWire 4 Java by Aaron Wolfe

Latest version is 4.3.6p

## Download compiled versions 
- [DriveWire4 4.3.6p for Windows x64 with Java included](https://github.com/qbancoffee/drivewire4/releases/tag/4.3.6p_Windows_x64)
- [DriveWire4 4.3.6p for linux x86_64 with Java included](https://github.com/qbancoffee/drivewire4/releases/tag/4.3.5p_linux_x86_64)
- [DriveWire4 4.3.6p for linux arm_64 with Java included](https://github.com/qbancoffee/drivewire4/releases/tag/4.3.5p_linux_aarch64)
- [DriveWire4 4.3.6p for MacOS aarch64(apple silicone) with Java included](https://github.com/qbancoffee/drivewire4/releases/tag/4.3.6p_macOS_aarch64)
- [DriveWire4 4.3.6p for MacOS x86_64 with Java included](https://github.com/qbancoffee/drivewire4/releases/tag/4.3.6p_macOS_x86_64)

# DriveWire 4 Version 4.3.6p - Release Notes

## Overview

This release focuses on **cross-platform compatibility improvements**, particularly for **macOS Apple Silicon** and **Windows** systems. Major changes include a complete serial library migration and enhanced file path handling.



## Serial Library Migration: nrjavaserial to jSerialComm

### Background

The previous serial communication library, **nrjavaserial 5.2.1**, does not support **macOS Apple Silicon (aarch64)**. This is a [known issue](https://github.com/NeuronRobotics/nrjavaserial/issues/219) where the native library loader looks for the wrong library name on ARM Macs. Support for Apple Silicon was targeted for version 5.3.0, which was never released.

### Solution

DriveWire 4 now uses **[jSerialComm 2.10.4](https://fazecast.github.io/jSerialComm/)**, a modern serial communication library with native support for:

- **macOS x86_64** (Intel)
- **macOS aarch64** (Apple Silicon M1/M2/M3/M4)
- **Windows x86/x64**
- **Linux x86/x64/ARM**

### Migration Details

The following files were updated to use the jSerialComm API:

| File | Changes |
|------|---------|
| `DWSerialDevice.java` | Port opening, configuration, and data transfer |
| `DWSerialReader.java` | Serial data reading thread |
| `DriveWireServer.java` | Serial port testing and enumeration |
| `DWCmdServerShowSerial.java` | Serial port status display |
| `DWUtils.java` | Port enumeration utilities |
| `DWAPISerial.java` | Serial API endpoint |
| `DWAPISerialPortDef.java` | Port definition handling |
| `DWProtocolHandler.java` | Protocol communication layer |
| `VModemProtocolHandler.java` | Virtual modem support |
| `MCXProtocolHandler.java` | MCX protocol support |
| `DWCmdServerTurbo.java` | Turbo mode serial handling |




---

## Windows Path Compatibility Fix

### Problem

When loading disk images or ZIP files through the **right-click context menu** on the drive table, Windows file paths (e.g., `C:\Users\name\disk.dsk`) were not being handled correctly. The Apache Commons VFS library expects properly formatted `file://` URIs.

### Solution

`DWImageMounter.java` was updated with comprehensive cross-platform path handling:

#### 1. Cross-Platform Filename Extraction

The `getFilename()` method now handles both Unix (`/`) and Windows (`\`) path separators:

```java
int lastSlash = location.lastIndexOf('/');
int lastBackslash = location.lastIndexOf('\\');
int lastSeparator = Math.max(lastSlash, lastBackslash);
```

#### 2. Path-to-URI Normalization

A new `normalizeToFileUri()` method converts file paths to proper `file://` URIs:

| Input | Output |
|-------|--------|
| `C:\path\to\file.dsk` | `file:///C:/path/to/file.dsk` |
| `/path/to/file.dsk` | `file:///path/to/file.dsk` |
| `file:///already/uri` | `file:///already/uri` (unchanged) |

#### 3. Consistent URI Handling

All file loading paths now use normalized URIs:
- ZIP file contents via `findAllFileUris()`
- Non-ZIP disk images via `mountDisks()`
- Browser-initiated loads via `DWBrowser.java`

---

## Windows Installer Improvements

### Features

- **Silent Launch**: Application starts without a visible command prompt window using `javaw.exe`
- **Bundled JRE Priority**: Automatically uses the included JRE if present, falls back to system Java
- **Start Menu Integration**: Creates shortcuts for both launching and uninstalling
- **Self-Contained**: Does not modify system-wide Java settings

### Installation

1. Extract the distribution archive
2. Run `install_windows.bat`
3. Launch from **Start Menu > DriveWire4**

### Uninstallation

- **Start Menu > DriveWire4 > Uninstall**, or
- Run `uninstall.bat` from the installation directory

---

## Platform Compatibility

### Tested Platforms

| Platform | Architecture | Status |
|----------|--------------|--------|
| Windows 10 | x64 | Verified |
| Windows 11 | x64 | Verified |
| Ubuntu Linux | x86_64 | Verified |
| Raspberry Pi OS | aarch64 | Verified |
| macOS | x86_64 (Intel) | Not Verified |
| macOS | aarch64 (Apple Silicon) | Verified |

### macOS Apple Silicon Notes

This is the **first version of DriveWire 4 with native Apple Silicon support**. Previous versions required Rosetta 2 emulation for serial port functionality, which often resulted in enumeration failures or crashes.

---

## Build System

### Maven Configuration

The project uses Maven with platform-specific profiles for SWT libraries:

```bash
# Build for current platform (auto-detected)
mvn clean package

# Build for specific platform
mvn clean package -P linux-x86_64
mvn clean package -P linux-aarch64
mvn clean package -P macosx-x86_64
mvn clean package -P macosx-aarch64
mvn clean package -P windows-x86_64
```
Additionaly you can run the buildall script to create versions for all platforms. the binaries will be created in ./dist-buildall in their respective directories.
```bash
chmod +x buildall.sh
./buildall.sh
```

### Dependencies

All dependencies are resolved from Maven Central and Nuiton repositories. No GitHub-hosted JARs are required.

| Dependency | Version | Purpose |
|------------|---------|---------|
| jSerialComm | 2.10.4 | Serial port communication |
| SWT | 4.37 | GUI framework |
| Apache Commons VFS | 1.0 | Virtual file system |
| Log4j | 1.2.17 | Logging |

---

## Migration from Previous Versions

### For Users

Simply download and install the new version. Configuration files are compatible with previous 4.3.x releases.

### For Developers

If you have custom code that interfaces with DriveWire's serial port handling:

1. Replace `gnu.io.*` imports with `com.fazecast.jSerialComm.*`
2. Update method calls per the API mapping table above
3. Replace `PortInUseException` with `DWPortInUseException`
4. Replace `UnsupportedCommOperationException` with `DWUnsupportedCommOperationException`

---

## Known Issues

- **Linux WebKit**: Ensure `libswt-webkit-gtk-4-jni` is installed before first run
- **Windows Antivirus**: Some antivirus software may flag `commons-vfs-1.0.jar`; add an exclusion if needed

---

# Quality of Life Improvements

## SWT Library Update

The SWT library has been updated from version **4.2.4 (2012)** to **4.37 (September 2025)**.  
This update brings a wide range of improvements to SWT, most notably within the embedded browser component.

As a result, DriveWire 4 can now load more modern web pages and support current web technologies.  
Outdated or broken URLs have been removed from the bookmark menu, and new, relevant links have been added.

## Updated Bookmark Menu

The bookmark menu now includes:

- **NitrOS9**  
  - [NitrOS 9 Documentation Wiki](https://sourceforge.net/projects/nitros9/)  
  - [NitrOS 9 Ease Of Use Project](http://www.lcurtisboyle.com/nitros9/nitros9.html)  
- **Software Websites**  
  - [Curtis Boyle’s Games Site](http://www.lcurtisboyle.com/nitros9/coco_game_list.html)  
  - [The Color Computer Archive](https://colorcomputerarchive.com/)  
  - [Mike Snyder’s COCO Quest](https://cocoquest.com)  
- **DriveWire Websites**  
  - [DriveWire 4 Wiki *(in development)*](https://github.com/qbancoffee/drivewire4/wiki/DriveWire-4)  
  - [DriveWire 4 GitHub Repository](https://github.com/qbancoffee/drivewire4)  
  - [DriveWire4 SourceForge Repository *(discontinued)*](https://sourceforge.net/projects/drivewireserver/)  
  - [Official DriveWire Repository *(Boisy Pitre)*](https://github.com/boisy/DriveWire)  
  - [pyDriveWire](https://github.com/n6il/pyDriveWire)  
  - [DriveWire Codebase](https://github.com/qbancoffee/drivewire4)  

## Enhanced Browser Capabilities

Leveraging the new SWT library, DriveWire 4 can now render HTML `<audio>` controls within its built-in browser.  
The **Load HDB-DOS** page is a local web page that includes an HTML audio player for three HDB-DOS WAV files:

- CoCo 1  
- CoCo 2  
- CoCo 3  

These WAV files can be played directly within the DriveWire 4 browser, allowing for convenient and integrated HDB-DOS loading across CoCo systems.

Although the browser component itself no longer handles disk or archive mounting, it now seamlessly integrates with the new **DWImageMounter** system to provide consistent behavior when interacting with DSK or archive files.  
The toolbar spinner is now visible and fully functional, providing clear feedback during browser-based operations.

Users can now **insert images directly from the Color Computer Archive**, a feature that was not previously possible.  
This improvement is due to two key factors:  
1. The previous SWT version did not render the website correctly, preventing interaction with download links.  
2. The archive distributes disk images within ZIP files, with DSK files one level deeper than the archive root, a structure older versions could not traverse.  

With SWT 4.37 and the `DWImageMounter.java` ZIP traversal logic, DriveWire 4 can now properly access and mount these images directly from the site.

`DWBrowser.java` has also been enhanced to improve drive management and user interaction within the browser interface.  
Users can **insert** or **append** a drive by clicking on its associated row in the drive table.  
Clicking a row updates the drive number displayed in the browser spinner, keeping the interface synchronized.  
Conversely, changing the spinner’s value whether by typing a number or using the **+** and **–** buttons, highlights the corresponding drive row.

The **floppy disk icon** located to the left of the spinner can be clicked to **toggle Append Mode** directly from the browser toolbar, providing a quick way to switch between standard and append behaviors.

## Disk and File Management Enhancements

All functionality related to loading or mounting **CoCo image and binary files** has been moved from `DWBrowser.java` to a new, dedicated class: `DWImageMounter.java`.  
While DriveWire 4 has long supported these file types, this change delegates management of them to `DWImageMounter.java` rather than `DWBrowser.java`.  

`DWImageMounter.java` handles all CoCo-related disk and binary extensions, including:  
**BIN, DSK, VHD, BAS, ARC, IMG, DMK, OS9, and ZIP.**  

It is responsible for traversing ZIP files, locating valid images (including those nested within subdirectories), and prompting the user to load them.  
Because this logic has been centralized, the following features now share the same core implementation:

- **Insert disk for drive X**  
- **Insert disk from URL for drive X**  
- **Load disks directly from the web browser**
- **Append to inserted DSK image**

As a result, improvements or fixes to mounting behavior apply consistently across all use cases.

Appending to files has also been implemented.  
A new **Append** menu item has been added to the disk table context menu, and a **Toggle Append Mode** option is available under the **Tools** menu for quick activation.

## Stability and UI Fixes

In several areas of the user interface, shell handling and event management have been improved for better reliability.

A bug that prevented loading multiple files due to use of an inactive shell has been resolved by replacing references to `getActiveShell()` with `Display.getMainShell()`.  
This method is part of the SWT **Display** class and ensures the correct shell is always returned, active or not.

Other stability improvements include:

- Fixing a bug in the **Tools** menu event handler logic that would cause DriveWire 4 to crash.  
- Resolving a crash when selecting **Display Timers** from the **Tools** menu.  
  This issue was caused by a null shell reference for the timers dialog; using the main shell reference from the Display now prevents the crash entirely.

## Prepackaged Releases

Prepackaged releases are available for:

- **Windows**  
- **Linux x86_64**  
- **Linux aarch_64**

Each release is bundled with its own **Java Runtime Environment (JRE)** in a self-contained directory.  
Installation scripts are provided that will:

- Copy DriveWire 4 and its bundled JRE to your system  
- Create a desktop launcher  
- Create an uninstall script  

These installers do **not** install Java system-wide and will **not interfere with existing Java installations**, allowing DriveWire 4 to run as a fully standalone application.

## Linux Installation Note

Before running DriveWire 4 for the first time on Linux, ensure that the package  
`libswt-webkit-gtk-4-jni` is installed.  

If this library is missing, DriveWire 4 may behave abnormally and alter its installation files.  
In such cases, reinstalling DriveWire 4 **after** installing `libswt-webkit-gtk-4-jni` will be necessary.

## Platform Compatibility

This version of DriveWire 4 has been tested and confirmed to work on:

- **Ubuntu (x86_64)**  
- **Raspbian aarch64 (Raspberry Pi)**  
- **Windows 10**  
- **Windows 11**

Although startup scripts are included for **Intel** and **ARM Macs**, functionality has not been verified due to lack of hardware for testing.











This repo holds two netbeans projects for two versions of DriveWire 4 by Aaron Wolfe that have been slightly modified so that they can be compiled and run with newer versions of java.

- drivewire4_from_source is DriveWire4 version 4.3.3p assembled from the source files available on sourceforge.<br>
Get the original source files from sourceforge.
https://sourceforge.net/projects/drivewireserver/<br>



### Compiling
To compile and run, make sure you have:
- Java JDK 17 or greater installed.
- ant
- NetBeans 16 or higher.(you don't stricly need this if you just want to compile)
  
The following was tested under linux but it should work the same under Windows.
With ant installed, navigate to the project directory "drivewire4/drivewire4_from_source" and type the following to compile:

```bash
ant
```
Depending on the version of ant you have this step might produce an error. you'll see something like the following.

```bash
BUILD FAILED
/home/rockyhill/Downloads/src/drivewire4/drivewire4_from_source/nbproject/build-impl.xml:1560: /home/rockyhill/Downloads/src/drivewire4/drivewire4_from_source/build/test/classes does not exist.
```
to solve this all you need to do is create test/classes directory inside of the newly created build directory. on linux you can issue the following command.
```bash
mkdir -p build/test/classes
```

Run the ant command again.
```bash
ant
```
For use on ARM64 you'll need to compile a small shared library so you'll need.
- g++
- X11 development headers
<br>
If you are going to run this on a raspberry pi that uses the X11 windowing system, then you'll need a small library that calls XInitThreads(). In order to use SWT, XInitThreads() needs to be called beforehand. For whatver reason, SWT does not take care of this so we must. 
I found and modified a small library "xdll.cpp" that someone made to solve this issue and it can be found in drivewire4/drivewire4_from_source

Make sure you are compiling this on a raspberry pi and and that you have the X11 develeopment headers and g++ installed.
to compile issue the following command. You can also cross compile this for arm64 from your dev computer but I haven't tried that yet.



```bash
g++ -o libx.so -shared -fPIC -Wl,-soname,libx.so -L/usr/lib/X11 -I/usr/include/X11 xdll.cpp -lX11
```

Copy the newly created shared library "libx.so" to "drivewire4/drivewire4_from_source/lib"

You can now start DriveWire 4 using the included script for arm64.
```bash
./drivewire4_linux_arm_64
```


For the drivewire4_from_source project use the script for your Operating system after compiling.

- drivewire4_linux_x86_64
- drivewire4_linux_arm_64
- drivewire4_windows_x86_64.bat
- drivewire4_macosx.x86_64
- drivewire4_macosx.aarch64

  
Ant uses "nbproject/private/private.properties" to get the path to the JDK from NetBeans. In my case "user.properties.file=/home/rockyhill/snap/netbeans/74/build.properties".
<br>
This is quickly solved by either updating that path manually or opening the project in NetBeans and building once. This is a minor annoyance but it's solvable.
<br>
After running it, you'll notice that the UI is very differnt from the UI that one sees when running the latest version of DriveWire 4.
I suspect this might be a version isssue so hopefully someone will have the source for that and one day this repo can be updated.
See screenshots of the UI below.



## Screenshots
<br>

### Main UI on Windows 10 64 bit de-compiled version.
<img src="https://github.com/qbancoffee/drivewire4/blob/main/images/dw4_decompiled_win64.png" width="300">
<br>

### Main UI on Linux 64 bit de-compiled version.
<img src="https://github.com/qbancoffee/drivewire4/blob/main/images/dw4_decompiled_linux64.png" width="300">
<br>

### Main UI on 64 bit linux sourceforge version.
<img src="https://github.com/qbancoffee/drivewire4/blob/main/images/dw4_mainwin.png" width="300">
<br>

### Lite UI on 64 bit linux sourceforge version.
<br>
<img src="https://github.com/qbancoffee/drivewire4/blob/main/images/dw4_liteui.png" width="300">


<br>



