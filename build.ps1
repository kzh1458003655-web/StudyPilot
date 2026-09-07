$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$config=Get-Content 'config.local.json' -Raw|ConvertFrom-Json
$repo=Join-Path $PSScriptRoot '.build-cache/m2'
& "$($config.RUNTIME)/maven/apache-maven-3.9.9/bin/mvn.cmd" "-Dmaven.repo.local=$repo" -f backend/pom.xml -B -ntp package
if($LASTEXITCODE -ne 0){throw 'Java构建或测试失败'}
& 'ai/build.cmd'
if($LASTEXITCODE -ne 0){throw 'C++构建失败'}
Write-Output '构建完成；停止并重新启动应用后使用新版本。'
