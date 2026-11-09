@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
if exist .sources.txt del .sources.txt
for /R "src" %%F in (*.java) do echo "%%~fF">>.sources.txt
javac --release 17 -encoding UTF-8 -d out @.sources.txt
set RESULT=%ERRORLEVEL%
del .sources.txt
if not "%RESULT%"=="0" (
  echo.
  echo Java 컴파일 실패: JDK 17 이상 설치와 JAVA PATH 설정을 확인하세요.
  pause
  exit /b %RESULT%
)
java -cp out baseup.Main
if errorlevel 1 pause
