[CmdletBinding()]
param([string]$OutputRoot)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'target-common.ps1')
$output = Initialize-StudyPilotOutput $OutputRoot
$statePath = Join-Path $output 'target-processes.json'
if (-not (Test-Path -LiteralPath $statePath)) {
    Write-Host '没有找到由 start-target.ps1 创建的进程记录。'
    exit 0
}

$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$records = @($state.processes)
[array]::Reverse($records)
foreach ($record in $records) {
    $process = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if ($null -eq $process) { continue }
    # PID 可能复用；仅停止启动时间与记录相符的进程。
    $started = ([DateTime]$record.startedAt).ToUniversalTime()
    if ([Math]::Abs(($process.StartTime.ToUniversalTime() - $started).TotalSeconds) -gt 3) {
        Write-Warning "跳过 PID $($record.pid)：启动时间与记录不符。"
        continue
    }
    Stop-Process -Id $record.pid -Force
    Write-Host "已停止 $($record.name)（PID $($record.pid)）。"
}
Remove-Item -LiteralPath $statePath -Force
Write-Host "已清除进程记录；日志仍保留在 $output。"
