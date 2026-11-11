$ErrorActionPreference = 'Stop'
try {
    $projectRoot = Split-Path -Parent $PSScriptRoot
    Set-Location -LiteralPath $projectRoot
    $jarPath = Join-Path $projectRoot 'dist\BaseUp.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) { & (Join-Path $PSScriptRoot 'build.ps1') }
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\javaw.exe') }
    $candidates += @(Get-ChildItem -Path 'C:\Program Files\Eclipse Adoptium\jdk-*','C:\Program Files\Java\jdk-*' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\javaw.exe' })
    $javaExe = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $javaExe) { $javaExe = (Get-Command javaw -ErrorAction Stop).Source }
    $launchArguments = @(('"-Dbaseup.home=' + $projectRoot + '"'), '-jar', ('"' + $jarPath + '"'))
    Start-Process -FilePath $javaExe -ArgumentList $launchArguments -WorkingDirectory $projectRoot -WindowStyle Hidden
} catch {
    Write-Output 'BASEUP could not start. Install JDK 17 or later, then try again.'
    Write-Output $_.Exception.Message
    Read-Host 'Press Enter to close'
    exit 1
}
