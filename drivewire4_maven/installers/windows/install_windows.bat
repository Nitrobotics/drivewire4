@echo off
setlocal EnableDelayedExpansion

:: DriveWire 4 Windows installer (no admin required)
set "APP_NAME=DriveWire4"
set "TARGET_DIR=%USERPROFILE%\%APP_NAME%"
set "START_MENU_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%APP_NAME%"

:: Source is the same directory as this script
set "SOURCE_DIR=%~dp0"
:: Remove trailing backslash
if "%SOURCE_DIR:~-1%"=="\" set "SOURCE_DIR=%SOURCE_DIR:~0,-1%"

echo ============================================
echo  %APP_NAME% Windows Installer
echo ============================================
echo.
echo   Source: %SOURCE_DIR%
echo   Target: %TARGET_DIR%
echo.

:: Verify source has expected distribution files
if not exist "%SOURCE_DIR%\config.xml" (
    echo ERROR: config.xml not found in %SOURCE_DIR%
    pause
    exit /b 1
)

:: Remove existing installation
if exist "%TARGET_DIR%" (
    echo Removing old installation...
    rd /s /q "%TARGET_DIR%" 2>nul
    ping -n 2 127.0.0.1 >nul
)

:: Create and copy
echo Copying files to %TARGET_DIR%...
mkdir "%TARGET_DIR%" 2>nul
robocopy "%SOURCE_DIR%" "%TARGET_DIR%" /E /NFL /NDL /NJH /NJS /NC /NS /NP
echo Copy complete.

:: Create uninstaller in the install directory
echo Creating uninstaller...
(
echo @echo off
echo set /p "YN=Uninstall %APP_NAME%? [Y/N]: "
echo if /I "%%YN%%" NEQ "Y" exit /b
echo rd /s /q "%TARGET_DIR%"
echo rd /s /q "%START_MENU_DIR%"
echo echo Uninstalled.
echo pause
) > "%TARGET_DIR%\uninstall.bat"

:: Modify drivewireUI.xml
set "XML_FILE=%TARGET_DIR%\drivewireUI.xml"
if exist "%XML_FILE%" (
    echo Configuring XML...
    set "HDB_PATH=%TARGET_DIR:\=/%"
    powershell -NoProfile -Command "$f='%XML_FILE%';$c=Get-Content $f -Raw;if($c -notmatch 'HDB-DOS'){$c=$c-replace'</Local>','<Folder title=\"HDB-DOS\"><URL title=\"Load HDB-DOS\">file:///!HDB_PATH!/hdb-dos/index.html</URL></Folder></Local>';Set-Content $f $c}"
)

:: Start Menu
echo Creating Start Menu shortcuts...
if not exist "%START_MENU_DIR%" mkdir "%START_MENU_DIR%"

:: Determine Java path - prefer bundled JRE
set "JAVA_PATH=%TARGET_DIR%\jre\bin\javaw.exe"
if not exist "%JAVA_PATH%" set "JAVA_PATH=javaw.exe"

:: Main app shortcut - launches javaw.exe directly (no console window)
powershell -NoProfile -Command "$w=New-Object -COM WScript.Shell;$s=$w.CreateShortcut('%START_MENU_DIR%\%APP_NAME%.lnk');$s.TargetPath='%JAVA_PATH%';$s.Arguments='-Djava.library.path=\"%TARGET_DIR%\lib\" -jar \"%TARGET_DIR%\drivewire4.jar\"';$s.WorkingDirectory='%TARGET_DIR%';$s.IconLocation='%TARGET_DIR%\dw4_icon.ico';$s.Save()"

:: Uninstaller shortcut
powershell -NoProfile -Command "$w=New-Object -COM WScript.Shell;$s=$w.CreateShortcut('%START_MENU_DIR%\Uninstall.lnk');$s.TargetPath='%TARGET_DIR%\uninstall.bat';$s.WorkingDirectory='%TARGET_DIR%';$s.Save()"

echo.
echo ============================================
echo  Installation Complete!
echo ============================================
echo  Launch: Start Menu - %APP_NAME%
echo.
pause
