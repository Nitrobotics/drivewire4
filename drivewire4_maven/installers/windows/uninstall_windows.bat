\
@echo off
setlocal
set "DEST=%LOCALAPPDATA%\DriveWire4"
echo Removing "%DEST%"
rmdir /S /Q "%DEST%"
echo Uninstalled.
endlocal
