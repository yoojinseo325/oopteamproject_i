$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$staging = Join-Path $projectRoot ('build\package-' + [guid]::NewGuid().ToString('N'))
$bundle = Join-Path $staging 'BaseUp-v1'
New-Item -ItemType Directory -Force -Path $bundle,(Join-Path $bundle 'dist') | Out-Null
foreach ($name in @('src','data','docs','scripts','README.md','VERSION','.gitignore','BaseUp.iml','run.cmd','build.cmd','test.cmd')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $name) -Destination $bundle -Recurse
}
New-Item -ItemType Directory -Force -Path (Join-Path $bundle '.idea'),(Join-Path $bundle '.run') | Out-Null
foreach ($name in @('modules.xml','misc.xml','compiler.xml','encodings.xml')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot ('.idea\' + $name)) -Destination (Join-Path $bundle '.idea')
}
Copy-Item -LiteralPath (Join-Path $projectRoot '.run\BASEUP.run.xml') -Destination (Join-Path $bundle '.run')
Copy-Item -LiteralPath (Join-Path $projectRoot 'dist\BaseUp-v1.jar') -Destination (Join-Path $bundle 'dist')
Compress-Archive -LiteralPath $bundle -DestinationPath (Join-Path $projectRoot 'dist\BaseUp-v1-share.zip') -Force
Write-Output 'Share package: dist\BaseUp-v1-share.zip (no personal learning records)'
