$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
& (Join-Path $Script:ProjectRoot 'stop.ps1')
