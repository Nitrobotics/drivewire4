@echo off
setlocal EnableDelayedExpansion

:: DriveWire 4 Windows installer (no admin required)
:: An existing installation is removed ONLY through the uninstaller, which asks first and never deletes a .xml
:: file. The installer then never overwrites an existing .xml file: config.xml, drivewireUI.xml and the rest keep
:: your settings; only .xml files the folder does not have yet are copied in.
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

:: Never install a folder onto itself (running the copy inside the installation used to delete the source)
if /I "%SOURCE_DIR%"=="%TARGET_DIR%" (
    echo ERROR: this installer is inside the installation folder itself.
    echo Run install_windows.bat from the extracted distribution instead. Nothing was changed.
    pause
    exit /b 1
)

:: Verify source has expected distribution files
if not exist "%SOURCE_DIR%\drivewire4.jar" (
    echo ERROR: drivewire4.jar not found in %SOURCE_DIR%
    pause
    exit /b 1
)
if not exist "%SOURCE_DIR%\uninstall_windows.bat" (
    echo ERROR: uninstall_windows.bat not found in %SOURCE_DIR%
    pause
    exit /b 1
)

:: Existing installation: the uninstaller asks first and keeps every .xml file. It runs from a temporary copy so
:: it can remove the installed uninstall.bat as well.
if exist "%TARGET_DIR%\" (
    echo An installation already exists in %TARGET_DIR%.
    set "UNINST=%TEMP%\dw4_uninstall_%RANDOM%.bat"
    copy /y "%SOURCE_DIR%\uninstall_windows.bat" "!UNINST!" >nul
    call "!UNINST!" "%TARGET_DIR%"
    set "RC=!ERRORLEVEL!"
    del /q "!UNINST!" 2>nul
    if "!RC!"=="1" (
        echo Installation cancelled - the existing installation was left as it is.
        pause
        exit /b 1
    )
)

:: Create and copy: everything except .xml, then only the .xml files the folder does not have yet
echo Copying files to %TARGET_DIR%...
mkdir "%TARGET_DIR%" 2>nul
robocopy "%SOURCE_DIR%" "%TARGET_DIR%" /E /XF *.xml /NFL /NDL /NJH /NJS /NC /NS /NP
robocopy "%SOURCE_DIR%" "%TARGET_DIR%" *.xml /E /XC /XN /XO /NFL /NDL /NJH /NJS /NC /NS /NP
echo Copy complete.

:: The installed uninstaller is the same one: asks first, keeps the .xml files
copy /y "%SOURCE_DIR%\uninstall_windows.bat" "%TARGET_DIR%\uninstall.bat" >nul

:: Modify drivewireUI.xml (adds the HDB-DOS link only when it is missing; nothing else in the file changes)
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
