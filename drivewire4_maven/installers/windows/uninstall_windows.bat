@echo off
setlocal EnableDelayedExpansion
:: DriveWire 4 Windows uninstaller.
::   uninstall_windows.bat [install folder]     (default: the folder this script is in)
:: ALWAYS asks first. NEVER deletes a .xml file (config.xml, drivewireUI.xml, master.xml, help.xml, ...):
:: every other file is removed, then the folders left empty. The Start Menu entries are removed.
:: Exit code 0 = uninstalled, 1 = cancelled by the user, 2 = nothing to uninstall.
set "APP_NAME=DriveWire4"
set "START_MENU_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%APP_NAME%"
set "TARGET_DIR=%~1"
if "%TARGET_DIR%"=="" set "TARGET_DIR=%~dp0"
if "%TARGET_DIR:~-1%"=="\" set "TARGET_DIR=%TARGET_DIR:~0,-1%"

if not exist "%TARGET_DIR%\" (
    echo Nothing to uninstall: "%TARGET_DIR%" does not exist.
    exit /b 2
)
echo ============================================
echo  %APP_NAME% Uninstaller
echo ============================================
echo   Folder: %TARGET_DIR%
echo   Every file in it is removed EXCEPT the .xml files, which are always kept:
for /r "%TARGET_DIR%" %%f in (*.xml) do echo      keep %%f
echo.
set "YN="
set /p "YN=Uninstall %APP_NAME% from this folder? [Y/N]: "
if /I not "%YN%"=="Y" (
    echo Cancelled - nothing was changed.
    exit /b 1
)

:: files: everything but *.xml (and this script, if it lives in the folder - it removes itself last)
for /r "%TARGET_DIR%" %%f in (*) do (
    if /I not "%%~xf"==".xml" if /I not "%%~ff"=="%~f0" del /f /q "%%f" 2>nul
)
:: folders: deepest first, only the ones now empty (rd without /s never removes a folder holding a kept .xml)
for /f "delims=" %%d in ('dir /ad /s /b "%TARGET_DIR%" 2^>nul ^| sort /r') do rd "%%d" 2>nul
if exist "%START_MENU_DIR%\" rd /s /q "%START_MENU_DIR%"
echo Uninstalled. The .xml files remain in "%TARGET_DIR%".
:: a script inside the folder deletes itself after it has finished running
if /I "%~dp0"=="%TARGET_DIR%\" (goto) 2>nul & del /f /q "%~f0"
exit /b 0
