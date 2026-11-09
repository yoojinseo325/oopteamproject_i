@echo off
chcp 65001 >nul
cd /d "%~dp0"
java -jar BASEUP.jar
if errorlevel 1 (
  echo 실행 오류: Java 17 이상이 설치되어 있는지 확인하세요.
  pause
)
