@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
if exist .sources.txt del .sources.txt
for /R "src" %%F in (*.java) do echo "%%~fF">>.sources.txt
javac --release 17 -encoding UTF-8 -d out @.sources.txt
set RESULT=%ERRORLEVEL%
del .sources.txt
if not "%RESULT%"=="0" pause & exit /b %RESULT%
java -cp out baseup.SmokeTest
pause
