@echo off
title Adam TTS Studio - Backend (Spring Boot)

echo ========================================================
echo        Khoi dong Adam TTS Studio Backend (Port 8080)
echo ========================================================
echo.

cd /d "%~dp0backend"

REM Kiem tra JAVA_HOME
if not defined JAVA_HOME (
    for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk*") do if exist "%%i\bin\java.exe" set "JAVA_HOME=%%i"
    if not defined JAVA_HOME for /d %%i in ("C:\Program Files\Microsoft\jdk*") do if exist "%%i\bin\java.exe" set "JAVA_HOME=%%i"
    if not defined JAVA_HOME for /d %%i in ("C:\Program Files\Java\jdk*") do if exist "%%i\bin\java.exe" set "JAVA_HOME=%%i"
)

if defined JAVA_HOME (
    echo Da xac dinh JAVA_HOME: %JAVA_HOME%
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    where java >nul 2>nul
    if %ERRORLEVEL% neq 0 (
        echo [LOI] Khong tim thay Java JDK!
        echo Vui long cai dat JDK 21+: winget install EclipseAdoptium.Temurin.21.JDK
        pause
        exit /b 1
    )
)

REM Kiem tra Maven
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "D:\apache-maven-3.9.12\bin\mvn.cmd" (
        set "PATH=D:\apache-maven-3.9.12\bin;%PATH%"
    ) else if exist "D:\maven\apache-maven-3.9.9\bin\mvn.cmd" (
        set "PATH=D:\maven\apache-maven-3.9.9\bin;%PATH%"
    ) else if exist "C:\Program Files\Maven\bin\mvn.cmd" (
        set "PATH=C:\Program Files\Maven\bin;%PATH%"
    ) else (
        echo [CANH BAO] Khong tim thay 'mvn' trong PATH.
        echo Vui long cai dat Maven hoac cau hinh PATH.
        pause
        exit /b 1
    )
)

REM Tu dong giai phong port 8080 neu bi chiem boi tien trinh cu
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo [THONG BAO] Port 8080 dang bi chiem boi PID %%a. Dang tat de giai phong...
    taskkill /F /PID %%a >nul 2>nul
)

echo [THONG BAO] Dang khoi chay Spring Boot backend...
call mvn.cmd spring-boot:run
pause
