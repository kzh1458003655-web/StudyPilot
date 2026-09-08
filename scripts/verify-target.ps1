[CmdletBinding()]
param(
    [string]$OutputRoot,
    [switch]$SkipWorkflow
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'target-common.ps1')
$output = Initialize-StudyPilotOutput $OutputRoot
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

function Invoke-LoggedCommand([string]$Name, [string]$WorkingDirectory, [string]$FilePath, [string[]]$Arguments) {
    $stdout = Join-Path $output "$stamp-$Name.stdout.log"
    $stderr = Join-Path $output "$stamp-$Name.stderr.log"
    Write-Host "执行 $Name ..."
    # 不把原生程序 stderr 合并回 PowerShell 管道，避免 Java 的普通告警被显示为 NativeCommandError。
    $command = Get-Command $FilePath -ErrorAction SilentlyContinue
    $executable = if ($null -ne $command) { $command.Source } else { Join-Path $WorkingDirectory $FilePath }
    $process = Start-Process -FilePath $executable -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    Get-Content -LiteralPath $stdout
    if ((Get-Item -LiteralPath $stderr).Length -gt 0) {
        Write-Host "$Name 的标准错误输出："
        Get-Content -LiteralPath $stderr
    }
    if ($process.ExitCode -ne 0) { throw "$Name 失败，请查看 $stdout 和 $stderr" }
}

Invoke-LoggedCommand 'backend-tests' (Join-Path $Script:StudyPilotRoot 'backend') '.\mvnw.cmd' @('test')
Invoke-LoggedCommand 'frontend-check' (Join-Path $Script:StudyPilotRoot 'frontend') 'pnpm.cmd' @('run', 'check')
Invoke-LoggedCommand 'harness-check' $Script:StudyPilotRoot 'powershell.exe' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', '.\scripts\harness-check.ps1')
if (-not $SkipWorkflow) {
    Invoke-LoggedCommand 'mock-exam-workflow' (Join-Path $Script:StudyPilotRoot 'backend') '.\mvnw.cmd' @('-Dstudypilot.e2e=true', '-Dtest=MockExamWorkflowEndToEndTest', 'test')
}

$workflow = if ($SkipWorkflow) { 'HTTP 工作流：本次按 -SkipWorkflow 跳过。' } else { 'HTTP 工作流：MockExamWorkflowEndToEndTest，使用临时 PostgreSQL 和受控网关。' }
@(
    '# StudyPilot 目标架构验证记录', '',
    "- 执行时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
    '- 后端：Maven 单元、集成与数据库测试。',
    '- 前端：ESLint、Prettier、类型检查、Vitest 与生产构建。',
    '- Harness：任务状态和文档链接检查。',
    "- $workflow",
    "- 详细命令日志目录：$output"
) | Set-Content -LiteralPath (Join-Path $output "$stamp-验证摘要.md") -Encoding utf8
Write-Host "验证通过。摘要与日志已写入 $output"
