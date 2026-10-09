Set-Location $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  KHOI DONG LUONG TEST (SERVER + 2 CLIENTS)" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

$cp = "build\classes;C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar"

Write-Host "[1/3] Khoi dong Server tren Port 8888..." -ForegroundColor Green
Start-Process cmd.exe -ArgumentList "/k", "title SERVER - Port 8888 && java -cp `"$cp`" server.Server"

Start-Sleep -Seconds 2

Write-Host "[2/3] Khoi dong Client 1 (admin / 123)..." -ForegroundColor Green
Start-Process java.exe -ArgumentList "-cp", "`"$cp`"", "client.MainClient"

Start-Sleep -Seconds 1

Write-Host "[3/3] Khoi dong Client 2 (duc / 123)..." -ForegroundColor Green
Start-Process java.exe -ArgumentList "-cp", "`"$cp`"", "client.MainClient"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Da khoi dong xong! Hay dang nhap de kiem thu." -ForegroundColor Yellow
