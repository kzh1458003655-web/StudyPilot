[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$wrapper = Join-Path $root 'mvnw.cmd'

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java 21 was not found. Install Java 21 and retry.'
}

$version = (& cmd.exe /d /c 'java -version 2>&1' | Select-Object -First 1)
if ($version -notmatch 'version "21\.') {
    throw "Java 21 is required. Detected: $version"
}

if (-not (Test-Path -LiteralPath $wrapper)) {
    throw 'mvnw.cmd is missing; reproducible build cannot start.'
}

Push-Location $root
try {
    & $wrapper clean test
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
