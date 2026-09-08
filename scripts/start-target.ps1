[CmdletBinding()]
param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [string]$OutputRoot,
    [switch]$ApplySchema,
    [switch]$StartAiService
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'target-common.ps1')
$output = Initialize-StudyPilotOutput $OutputRoot

# 业务模块依赖真实 PostgreSQL；尽早检查可避免无数据源模式下的误启动。
Require-StudyPilotEnvironment 'STUDYPILOT_DB_URL' | Out-Null
Require-StudyPilotEnvironment 'STUDYPILOT_DB_USERNAME' | Out-Null
Require-StudyPilotEnvironment 'STUDYPILOT_DB_PASSWORD' | Out-Null

if ($ApplySchema) {
    & (Join-Path $Script:StudyPilotRoot 'backend\scripts\verify-schema.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL Schema 初始化失败。' }
}

$statePath = Join-Path $output 'target-processes.json'
if (Test-Path -LiteralPath $statePath) {
    throw "发现已有目标架构进程记录：$statePath。请先执行 .\scripts\stop-target.ps1。"
}

$records = [System.Collections.Generic.List[object]]::new()
try {
    if ($StartAiService) {
        if (-not (Test-StudyPilotHttp 'http://127.0.0.1:18082/health')) {
            throw '本地模型服务 18082 未就绪。请先按部署指南启动 llama-server，再启动 C++ 服务。'
        }
        $aiExe = Join-Path $Script:StudyPilotRoot 'ai\bin\study-ai.exe'
        if (-not (Test-Path -LiteralPath $aiExe)) {
            throw "找不到 $aiExe。请先在 ai 目录执行 build.cmd 编译 C++ 服务。"
        }
        $aiIndex = 'D:\大四课程设计\StudyPilot-runtime\ai-index'
        New-Item -ItemType Directory -Force -Path $aiIndex | Out-Null
        $ai = Start-Process -FilePath $aiExe -ArgumentList @($aiIndex) -WorkingDirectory (Join-Path $Script:StudyPilotRoot 'ai') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $output 'ai.stdout.log') -RedirectStandardError (Join-Path $output 'ai.stderr.log') -PassThru
        $records.Add([pscustomobject]@{ name = 'ai'; pid = $ai.Id; startedAt = $ai.StartTime.ToUniversalTime().ToString('o') })
        Wait-StudyPilotHttp 'http://127.0.0.1:18081/health' 30 | Out-Null
    } elseif (-not (Test-StudyPilotHttp 'http://127.0.0.1:18081/health')) {
        Write-Warning 'C++ 服务 18081 未启动。后端健康接口可用，但资料导入、问答、组卷和简答题评分会返回依赖不可用。'
    }

    $backend = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', ".\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=$BackendPort") -WorkingDirectory (Join-Path $Script:StudyPilotRoot 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $output 'backend.stdout.log') -RedirectStandardError (Join-Path $output 'backend.stderr.log') -PassThru
    $records.Add([pscustomobject]@{ name = 'backend'; pid = $backend.Id; startedAt = $backend.StartTime.ToUniversalTime().ToString('o') })
    Wait-StudyPilotHttp "http://127.0.0.1:$BackendPort/api/v1/health" 90 | Out-Null

    $frontend = Start-Process -FilePath 'pnpm.cmd' -ArgumentList @('dev', '--', '--host', '127.0.0.1', '--port', $FrontendPort) -WorkingDirectory (Join-Path $Script:StudyPilotRoot 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $output 'frontend.stdout.log') -RedirectStandardError (Join-Path $output 'frontend.stderr.log') -PassThru
    $records.Add([pscustomobject]@{ name = 'frontend'; pid = $frontend.Id; startedAt = $frontend.StartTime.ToUniversalTime().ToString('o') })
    Wait-StudyPilotHttp "http://127.0.0.1:$FrontendPort" 60 | Out-Null

    Write-StudyPilotProcessState $output $records.ToArray()
    Write-Host "StudyPilot 已启动：http://127.0.0.1:$FrontendPort"
    Write-Host "运行日志和进程记录：$output"
} catch {
    foreach ($record in $records) { Stop-Process -Id $record.pid -Force -ErrorAction SilentlyContinue }
    throw
}
