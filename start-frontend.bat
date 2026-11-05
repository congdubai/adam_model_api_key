@echo off
chcp 65001 > nul
title Adam TTS Studio - Frontend (Vite)

echo ========================================================
echo        Khởi động Adam TTS Studio Frontend (Port 5173)
echo ========================================================
echo.

cd /d "%~dp0frontend"

if not exist "node_modules" (
    echo Chưa thấy thư mục node_modules. Đang chạy npm install...
    call npm.cmd install
)

echo Đang khởi chạy Vite React dev server...
call npm.cmd run dev
pause

