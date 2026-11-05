@echo off
title Adam TTS Studio - Backend (Spring Boot)

echo ========================================================
echo        Khoi dong Adam TTS Studio Backend (Port 8080)
echo ========================================================
echo.

cd /d "%~dp0backend"

REM Kiem tra Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [CANH BAO] Khong tim thay lenh 'java' trong PATH.
    echo Dang kiem tra thu muc cai dat...
    for /d %%i in ("C:\Program Files\Microsoft\jdk-21*") do if exist "%%i\bin\java.exe" set "JAVA_HOME=%%i"
    if not defined JAVA_HOME for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do if exist "%%i\bin\java.exe" set "JAVA_HOME=%%i"
    if not defined JAVA_HOME for /d %%i in ("C:\Program Files\Java\jdk-21*") do if exist "%%i\bin\java.exe" set "JAVA_HOME=%%i"

    if defined JAVA_HOME (
        echo Da tim thay JDK tai: %JAVA_HOME%
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    ) else (
        echo [LOI] Khong tim thay Java 21!
        echo Vui long cai dat JDK 21: winget install EclipseAdoptium.Temurin.21.JDK
        pause
        exit /b 1
    )
)

REM Kiem tra Maven
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "D:\maven\apache-maven-3.9.9\bin\mvn.cmd" (
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

echo [THONG BAO] Dang khoi chay Spring Boot backend...
call mvn.cmd spring-boot:run
pause
