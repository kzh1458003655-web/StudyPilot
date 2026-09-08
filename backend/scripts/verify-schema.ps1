[CmdletBinding()]
param([string]$DatabaseUrl = $env:STUDYPILOT_DB_URL)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($DatabaseUrl)) {
    throw 'Set STUDYPILOT_DB_URL to a non-production PostgreSQL connection URL before schema verification.'
}

$psql = Get-Command psql.exe -ErrorAction SilentlyContinue
if (-not $psql) { throw 'psql.exe was not found. Install PostgreSQL client tools first.' }

$schema = Join-Path (Split-Path -Parent $PSScriptRoot) 'src/main/resources/db/schema'
@('00-schema.sql','10-common.sql','20-document.sql','30-qa.sql','40-exam.sql','50-assessment.sql','90-indexes.sql') | ForEach-Object {
    & $psql.Source $DatabaseUrl --set ON_ERROR_STOP=1 --file (Join-Path $schema $_)
    if ($LASTEXITCODE -ne 0) { throw "Schema step failed: $_" }
}

Write-Host 'PostgreSQL schema verification passed.'
