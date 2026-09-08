param([switch]$SkipBrowser)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
Set-Location -LiteralPath $Script:ProjectRoot

# 先验证源代码可构建，再验证数据库、C++、模型和 Java 的完整请求链。
if (Test-Path -LiteralPath (Join-Path $Script:ProjectRoot 'data/processes.json')) {
    & (Join-Path $PSScriptRoot 'stop.ps1')
}
& (Join-Path $PSScriptRoot 'build.ps1')
& (Join-Path $PSScriptRoot 'start.ps1') -NoBrowser
$health = Wait-StudyUrl 'http://127.0.0.1:18080/api/health' 120
if (-not $health.database -or -not $health.ai.model_ready) {
    throw "健康检查未通过：$($health | ConvertTo-Json -Compress)"
}
$python = Get-Command python.exe -ErrorAction SilentlyContinue
if ($python) {
    & $python.Source tests/integration.py
    if ($LASTEXITCODE -ne 0) { throw '真实模型接口测试失败。' }
    & $python.Source tests/assessment_integration.py
    if ($LASTEXITCODE -ne 0) { throw '考频与评测接口测试失败。' }
} else {
    Write-Warning '未找到 Python，跳过 integration.py；四个服务的健康检查已通过。'
}
if (-not $SkipBrowser -and (Get-Command node.exe -ErrorAction SilentlyContinue)) {
    & node.exe tests/browser-course-test.cjs
    if ($LASTEXITCODE -ne 0) { throw '浏览器端到端测试失败。' }
}
Write-Host '开发环境测试通过。'
