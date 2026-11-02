$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$candidates += @(Get-ChildItem -Path 'C:\Program Files\Eclipse Adoptium\jdk-*','C:\Program Files\Java\jdk-*' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object FullName)
$jdk = $candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
$javac = if ($jdk) { Join-Path $jdk 'bin\javac.exe' } else { (Get-Command javac).Source }
$java = if ($jdk) { Join-Path $jdk 'bin\java.exe' } else { (Get-Command java).Source }
$testClasses = Join-Path $projectRoot 'build\test-classes'
New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
$tests = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\test\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
& $javac --release 17 -encoding UTF-8 -cp 'build/classes' -d $testClasses @tests
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& $java -cp "build/classes;$testClasses" baseup.ServiceTest
if ($LASTEXITCODE -ne 0) { throw 'Service tests failed.' }
& $java -cp "build/classes;$testClasses" baseup.StepFlowTest
if ($LASTEXITCODE -ne 0) { throw 'Step flow tests failed.' }
& $java -cp "build/classes;$testClasses" baseup.KboContentTest
if ($LASTEXITCODE -ne 0) { throw 'KBO content tests failed.' }
& $java -cp "build/classes;$testClasses" baseup.UiSmokeTest
if ($LASTEXITCODE -ne 0) { throw 'UI tests failed.' }
