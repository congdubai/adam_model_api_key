@echo off
title Adam TTS Studio - Frontend (Vite)

echo ========================================================
echo        Khoi dong Adam TTS Studio Frontend (Port 5173)
echo ========================================================
echo.

cd /d "%~dp0frontend"

if not exist "node_modules" (
    echo [THONG BAO] Dang cai dat thu vien npm...
    call npm.cmd install
)

echo [THONG BAO] Dang khoi chay Vite React dev server...
call npm.cmd run dev
pause
