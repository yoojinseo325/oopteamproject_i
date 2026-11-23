$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$jdkCandidates = @()
if ($env:JAVA_HOME) { $jdkCandidates += $env:JAVA_HOME }
$jdkCandidates += @(Get-ChildItem -Path 'C:\Program Files\Eclipse Adoptium\jdk-*','C:\Program Files\Java\jdk-*' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object FullName)
$jdkPath = $jdkCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if ($jdkPath) {
    $compiler = Join-Path $jdkPath 'bin\javac.exe'
    $jarTool = Join-Path $jdkPath 'bin\jar.exe'
} else {
    $compiler = (Get-Command javac -ErrorAction Stop).Source
    $jarTool = (Get-Command jar -ErrorAction Stop).Source
}
$classes = Join-Path $projectRoot 'build\classes'
# 패키지 이동/삭제 후 이전 .class가 JAR에 남지 않도록 생성 폴더만 비운다.
$resolvedClasses = [IO.Path]::GetFullPath($classes)
$buildRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'build')) + [IO.Path]::DirectorySeparatorChar
if (-not $resolvedClasses.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Class output must stay inside the project build folder.'
}
if (Test-Path -LiteralPath $resolvedClasses) {
    Remove-Item -LiteralPath $resolvedClasses -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classes,(Join-Path $projectRoot 'dist') | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
& $compiler --release 17 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
& $jarTool "-J-Djava.io.tmpdir=$classes" --create --file (Join-Path $projectRoot 'dist\BaseUp.jar') --main-class baseup.App -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed.' }
Write-Output 'Build complete: dist\BaseUp.jar'
