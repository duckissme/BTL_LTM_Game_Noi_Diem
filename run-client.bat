@echo off
cd /d "%~dp0"
title CLIENT - Game Noi Diem
echo ========================================================
echo   KHOI DONG CLIENT
echo ========================================================
java -cp "build\classes;C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar" client.MainClient
