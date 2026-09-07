param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
& (Join-Path $Script:ProjectRoot 'start.ps1') -NoBrowser:$NoBrowser
