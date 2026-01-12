@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
cd /d "%ROOT%"
set "OUT=%ROOT%dist-build"

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%" >nul

set PLATFORMS=linux-x86_64 linux-aarch64 macos-x86_64 macos-aarch64 windows-x86_64

for %%P in (%PLATFORMS%) do (
  echo === Building for %%P ===
  call mvn -DskipTests -Dswt.platform=%%P clean package
  if errorlevel 1 (
    echo ERROR: Maven build failed for %%P
    exit /b 1
  )

  set "ZIP="
  for /f "delims=" %%Z in ('powershell -NoProfile -Command "Get-ChildItem -Path '%ROOT%target' -Filter '*%%P*.zip' | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName"') do set "ZIP=%%Z"

  if "!ZIP!"=="" (
    echo ERROR: Could not find target^\*%%P^*.zip after building for %%P
    exit /b 1
  )

  set "DEST=%OUT%\%%P"
  mkdir "!DEST!" >nul

  powershell -NoProfile -Command "Expand-Archive -Path '!ZIP!' -DestinationPath '!DEST!' -Force"

  rem Flatten single top-level directory if present
  for /f "delims=" %%D in ('powershell -NoProfile -Command "Get-ChildItem -Directory '!DEST!' | Select-Object -First 1 -ExpandProperty FullName"') do set "TOP=%%D"
  if exist "!TOP!\drivewire4.jar" (
    powershell -NoProfile -Command "Get-ChildItem -Force '!TOP!' | ForEach-Object { Move-Item -Force $_.FullName '!DEST!' }"
    rmdir /s /q "!TOP!"
  )

  for %%F in (config.xml drivewireUI.xml drivewire4.jar dw4_icon.png help.xml master.xml) do (
    if not exist "!DEST!\%%F" (
      echo ERROR: Missing %%F in !DEST!
      exit /b 1
    )
  )

  rem Ensure soundbank-deluxe.gm from source is present in working directory
  if exist "%ROOT%soundbank-deluxe.gm" copy /y "%ROOT%soundbank-deluxe.gm" "!DEST!\soundbank-deluxe.gm" >nul

  rem Remove buildall scripts from binary distribution folder
  del /q "!DEST!\buildall.sh" 2>nul
  del /q "!DEST!\buildall.bat" 2>nul


  rem Ensure HDB-DOS wavs are present in hdb-dos directory
  for %%F in (hdbdw3cc1.wav hdbdw3cc2.wav hdbdw3cc3.wav) do (
    if not exist "!DEST!\hdb-dos\%%F" (
      echo ERROR: Missing hdb-dos\%%F in !DEST!
      exit /b 1
    )
  )


  if not exist "!DEST!\jre" mkdir "!DEST!\jre"
  if not exist "!DEST!\jre\README-JRE.txt" echo Place a platform JRE here (jre\bin\java.exe).>"!DEST!\jre\README-JRE.txt"

  if not exist "!DEST!\hdb-dos" mkdir "!DEST!\hdb-dos"

  if not exist "!DEST!\hdb-dos\index.html" (
    echo ERROR: Missing hdb-dos\index.html in !DEST!
    exit /b 1
  )

  rem Platform-specific installers only
  if exist "!DEST!\installers" (
    if not "%%P"=="windows-x86_64" (
      rem remove windows installers from non-windows
      rmdir /s /q "!DEST!\installers\windows" 2>nul
    )
    echo %%P | findstr /i "linux" >nul && (
      rmdir /s /q "!DEST!\installers\macos" 2>nul
    )
    echo %%P | findstr /i "macos" >nul && (
      rmdir /s /q "!DEST!\installers\linux" 2>nul
    )
    if "%%P"=="windows-x86_64" (
      rmdir /s /q "!DEST!\installers\linux" 2>nul
      rmdir /s /q "!DEST!\installers\macos" 2>nul
    )
  )

  rem Move installer/uninstaller scripts into the working directory (more intuitive for users)
  echo %%P | findstr /i "linux" >nul && (
    if exist "!DEST!\installers\linux\install_linux.sh" (
      move /y "!DEST!\installers\linux\install_linux.sh" "!DEST!\install_linux.sh" >nul
    )
    if exist "!DEST!\installers\linux\uninstall_linux.sh" (
      move /y "!DEST!\installers\linux\uninstall_linux.sh" "!DEST!\uninstall_linux.sh" >nul
    )
  )

  echo %%P | findstr /i "macos" >nul && (
    if exist "!DEST!\installers\macos\install_macos.sh" (
      move /y "!DEST!\installers\macos\install_macos.sh" "!DEST!\install_macos.sh" >nul
    )
    if exist "!DEST!\installers\macos\uninstall_macos.sh" (
      move /y "!DEST!\installers\macos\uninstall_macos.sh" "!DEST!\uninstall_macos.sh" >nul
    )
  )

  if "%%P"=="windows-x86_64" (
    if exist "!DEST!\installers\windows\install_windows.bat" (
      move /y "!DEST!\installers\windows\install_windows.bat" "!DEST!\install_windows.bat" >nul
    )
    if exist "!DEST!\installers\windows\uninstall_windows.bat" (
      move /y "!DEST!\installers\windows\uninstall_windows.bat" "!DEST!\uninstall_windows.bat" >nul
    )
    if exist "!DEST!\installers\windows\dw4_icon.ico" (
      move /y "!DEST!\installers\windows\dw4_icon.ico" "!DEST!\dw4_icon.ico" >nul
    )
  )

  if exist "!DEST!\installers" rmdir /s /q "!DEST!\installers" 2>nul

  rem Remove irrelevant run scripts; keep only those for the target platform
  echo %%P | findstr /i "linux" >nul && (
    del /q "!DEST!\run-macos*.sh" "!DEST!\run-windows.bat" 2>nul
  )
  echo %%P | findstr /i "macos" >nul && (
    del /q "!DEST!\run-linux*.sh" "!DEST!\run-windows.bat" "!DEST!\run-macos-rosetta.sh" 2>nul
  )
  if "%%P"=="windows-x86_64" (
    del /q "!DEST!\run-linux*.sh" "!DEST!\run-macos*.sh" 2>nul
  )

  rem Include libx.so only for linux-aarch64
  if "%%P"=="linux-aarch64" (
    if not exist "!DEST!\lib" mkdir "!DEST!\lib"
    if exist "%ROOT%libx.so" copy /y "%ROOT%libx.so" "!DEST!\lib\libx.so" >nul
  ) else (
    del /q "!DEST!\lib\libx.so" 2>nul
  )

  echo Built: !DEST!
)

echo All builds complete in: %OUT%
endlocal
