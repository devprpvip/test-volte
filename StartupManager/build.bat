@echo off
echo === Build StartupManager Portable (Windows) ===
echo Yeu cau: MinGW-w64 hoac Visual Studio

where gcc >nul 2>nul
if %errorlevel%==0 (
    echo Dung MinGW GCC...
    gcc -Os -s -municode -mwindows -DUNICODE -D_UNICODE -o StartupManager.exe main.c startup.c resource.rc -lcomctl32 -lshlwapi -lshell32
    if exist StartupManager.exe (
        echo Build thanh cong!
        dir StartupManager.exe
        for %%A in (StartupManager.exe) do echo Kich thuoc: %%~zA bytes
        pause
        exit /b 0
    )
)

where clang >nul 2>nul
if %errorlevel%==0 (
    clang -Os -s -municode -mwindows -o StartupManager.exe main.c startup.c resource.rc -lcomctl32 -lshlwapi -lshell32
    if exist StartupManager.exe (
        echo Build thanh cong voi clang!
        dir StartupManager.exe
        pause
        exit /b 0
    )
)

echo Khong tim thay gcc/clang. Thu dung windres + gcc...
windres resource.rc -o resource.o
gcc -Os -s -municode -mwindows -c main.c -o main.o
gcc -Os -s -municode -mwindows -c startup.c -o startup.o
gcc -Os -s -municode -mwindows -o StartupManager.exe main.o startup.o resource.o -lcomctl32 -lshlwapi -lshell32

if exist StartupManager.exe (
    echo Build thanh cong!
    dir StartupManager.exe
) else (
    echo Build that bai! Cai MinGW-w64: https://www.mingw-w64.org/
)
pause
