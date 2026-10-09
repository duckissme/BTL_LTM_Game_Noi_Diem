@echo off
cd /d "%~dp0"
title SERVER - Port 8888
echo ========================================================
echo   KHOI DONG SERVER (PORT 8888)
echo ========================================================
java -cp "build\classes;C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar" server.Server
pause
