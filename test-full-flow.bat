@echo off
cd /d "%~dp0"
echo ========================================================
echo   KHOI DONG HE THONG TEST (SERVER + 2 CLIENTS)
echo ========================================================

echo [1/3] Khoi dong Server tren Port 8888...
start "SERVER - Port 8888" cmd /k "cd /d "%~dp0" && java -cp "build\classes;C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar" server.Server"

ping 127.0.0.1 -n 3 >nul

echo [2/3] Khoi dong Client 1 (admin / 123)...
start "CLIENT 1" java -cp "build\classes;C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar" client.MainClient

ping 127.0.0.1 -n 2 >nul

echo [3/3] Khoi dong Client 2 (duc / 123)...
start "CLIENT 2" java -cp "build\classes;C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar" client.MainClient

echo ========================================================
echo Da khoi dong xong! Hay dang nhap de kiem thu.
echo ========================================================
