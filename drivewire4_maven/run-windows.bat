@echo off
REM DriveWire 4 - Windows Launch Script (no build)

cd /d "%~dp0"

set "JAVA=java"
if exist "jre\bin\java.exe" (
  set "JAVA=jre\bin\java.exe"
)

set "JAR=drivewire4.jar"
if not exist "%JAR%" (
  echo ERROR: %JAR% not found in "%cd%"
  echo This launcher expects a packaged distribution folder containing drivewire4.jar.
  pause
  exit /b 1
)

set "LIBDIR=lib"
if exist "%LIBDIR%" (
  "%JAVA%" -Djava.library.path="%LIBDIR%" -jar "%JAR%" %*
) else (
  "%JAVA%" -jar "%JAR%" %*
)
