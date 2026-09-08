# 目标架构脚本的公共约定。运行日志、进程记录和测试产物默认写到 D 盘。
$Script:StudyPilotRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

function Get-StudyPilotOutputRoot([string]$RequestedRoot) {
    $candidate = if ([string]::IsNullOrWhiteSpace($RequestedRoot)) {
        if ([string]::IsNullOrWhiteSpace($env:STUDYPILOT_OUTPUT_ROOT)) {
            'D:\大四课程设计\StudyPilot-output'
        } else { $env:STUDYPILOT_OUTPUT_ROOT }
    } else { $RequestedRoot }
    return [IO.Path]::GetFullPath($candidate)
}

function Initialize-StudyPilotOutput([string]$RequestedRoot) {
    $root = Get-StudyPilotOutputRoot $RequestedRoot
    New-Item -ItemType Directory -Force -Path $root | Out-Null
    return $root
}

function Require-StudyPilotEnvironment([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "缺少环境变量 $Name。请先按 docs/部署与运行指南.md 配置，且不要把密码写入仓库。"
    }
    return $value
}

function Wait-StudyPilotHttp([string]$Url, [int]$TimeoutSeconds = 60) {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try { return Invoke-RestMethod -Uri $Url -TimeoutSec 3 }
        catch { Start-Sleep -Milliseconds 750 }
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "服务未在 $TimeoutSeconds 秒内就绪：$Url"
}

function Test-StudyPilotHttp([string]$Url) {
    try { Invoke-RestMethod -Uri $Url -TimeoutSec 3 | Out-Null; return $true }
    catch { return $false }
}

function Write-StudyPilotProcessState([string]$OutputRoot, [object[]]$Processes) {
    [ordered]@{
        startedAt = [DateTime]::UtcNow.ToString('o')
        processes = @($Processes)
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $OutputRoot 'target-processes.json') -Encoding utf8
}
