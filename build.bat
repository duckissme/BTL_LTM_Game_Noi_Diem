@echo off
cd /d "%~dp0"
echo ========================================================
echo   BIEN DICH DU AN BTL_LTM
echo ========================================================

if not exist "build\classes" mkdir "build\classes"

javac -cp "C:\Users\minhd\Downloads\mysql-connector-j-8.3.0.jar;src" -d "build\classes" -encoding UTF-8 src\common\*.java src\database\*.java src\entity\*.java src\server\*.java src\client\network\*.java src\client\view\*.java src\client\*.java src\test\*.java

if %ERRORLEVEL% equ 0 (
    copy /Y "db.properties" "build\classes\db.properties" >nul
    echo [OK] Bien dich thanh cong vao build\classes!
) else (
    echo [ERROR] Co loi xay ra!
)
pause
