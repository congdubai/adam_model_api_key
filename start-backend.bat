@echo off
chcp 65001 > nul
title Adam TTS Studio - Backend (Spring Boot)

echo ========================================================
echo        Khởi động Adam TTS Studio Backend (Port 8080)
echo ========================================================
echo.

cd /d "%~dp0backend"

REM Kiểm tra Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [CẢNH BÁO] Chưa tìm thấy lệnh 'java' trong PATH hệ thống.
    echo Đang kiểm tra thư mục cài đặt tiêu chuẩn...
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe" (
        for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%i"
    ) else if exist "C:\Program Files\Microsoft\jdk-21*\bin\java.exe" (
        for /d %%i in ("C:\Program Files\Microsoft\jdk-21*") do set "JAVA_HOME=%%i"
    ) else if exist "C:\Program Files\Java\jdk-21*\bin\java.exe" (
        for /d %%i in ("C:\Program Files\Java\jdk-21*") do set "JAVA_HOME=%%i"
    )
    if defined JAVA_HOME (
        echo Đã tìm thấy JDK tại: %JAVA_HOME%
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    ) else (
        echo [LỖI] Không tìm thấy Java 21!
        echo Vui lòng cài đặt JDK 21 bằng lệnh: winget install EclipseAdoptium.Temurin.21.JDK
        echo Hoặc tải từ: https://adoptium.net/temurin/releases/?version=21
        pause
        exit /b 1
    )
)

REM Kiểm tra Maven
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "D:\maven\apache-maven-3.9.9\bin\mvn.cmd" (
        set "PATH=D:\maven\apache-maven-3.9.9\bin;%PATH%"
    ) else if exist "C:\Program Files\Maven\bin\mvn.cmd" (
        set "PATH=C:\Program Files\Maven\bin;%PATH%"
    ) else (
        echo [CẢNH BÁO] Không tìm thấy 'mvn' trong PATH.
        echo Vui lòng cài đặt Maven hoặc cấu hình PATH.
        pause
        exit /b 1
    )
)

echo Đang khởi chạy Spring Boot backend...
mvn spring-boot:run
pause
