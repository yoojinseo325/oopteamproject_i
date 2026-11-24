@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
(for /r src\main\java %%F in (*.java) do @echo %%F) > build\sources.txt
(for /r src\test\java %%F in (*.java) do @echo %%F) >> build\sources.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (pause & exit /b 1)
java -cp build\classes baseup.shortapp.MixedBackendTest data\short_questions.tsv
java -cp "build\classes;resources" baseup.shortapp.MigrationTest data\short_questions.tsv
pause
endlocal
