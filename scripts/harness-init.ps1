[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$checkScript = Join-Path $PSScriptRoot 'harness-check.ps1'

Set-Location -LiteralPath $projectRoot

Write-Host '=== StudyPilot Harness 初始化 ===' -ForegroundColor Cyan
Write-Host "仓库根目录：$projectRoot"

& $checkScript
$checkSucceeded = $?
if (-not $checkSucceeded) {
    throw 'Harness 结构检查失败，请先修复并记录失败原因。'
}

$branch = (& git branch --show-current).Trim()
Write-Host "当前分支：$branch"

$statusLines = @(& git -c core.quotepath=false status --short)
if ($statusLines.Count -eq 0) {
    Write-Host '工作区状态：干净'
} else {
    Write-Host '工作区状态：存在未提交改动' -ForegroundColor Yellow
    $statusLines | ForEach-Object { Write-Host "  $_" }
}

Write-Host ''
Write-Host '工具可用性：'
foreach ($toolName in @('git', 'java', 'node', 'pnpm', 'python')) {
    $tool = Get-Command $toolName -ErrorAction SilentlyContinue
    if ($null -eq $tool) {
        Write-Host "  [缺失] $toolName" -ForegroundColor Yellow
    } else {
        Write-Host "  [可用] $toolName -> $($tool.Source)"
    }
}

$featurePath = Join-Path $projectRoot 'docs/harness/feature_list.json'
$featureList = Get-Content -LiteralPath $featurePath -Raw -Encoding UTF8 | ConvertFrom-Json -Depth 100
$actionable = @($featureList.features | Where-Object status -in @('ready', 'in_progress', 'done'))
$current = $actionable[0]

Write-Host ''
Write-Host '当前唯一推荐任务：' -ForegroundColor Cyan
Write-Host "  $($current.id) [$($current.status)] $($current.name)"
Write-Host "  分支：$($current.branch)"
Write-Host "  说明：$($current.description)"

Write-Host ''
Write-Host '开始修改前请依次阅读：'
Write-Host '  1. AGENTS.md'
Write-Host '  2. docs/harness/feature_list.json'
Write-Host '  3. docs/harness/progress.md'
Write-Host '  4. docs/harness/session-handoff.md'
Write-Host '  5. 当前任务对应的正式文档'

Write-Host ''
Write-Host '初始化完成；本脚本没有修改仓库。' -ForegroundColor Green
