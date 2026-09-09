# 本地演示模式：使用临时嵌入式 PostgreSQL，不需要先安装数据库。
# 正式运行请使用 .\scripts\start-target.ps1，并配置 STUDYPILOT_DB_* 环境变量。
[CmdletBinding()]
param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [string]$OutputRoot = 'D:\大四课程设计\StudyPilot-output'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null

foreach ($port in @($BackendPort, $FrontendPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "端口 $port 已被占用。请先关闭已有服务或换一个端口。"
    }
}

# Spring Boot receives both values through one Maven property. Without the inner quotes,
# Maven parses the database flag as a Maven CLI option and the demonstration server never starts.
$backendCommand = ".\mvnw.cmd spring-boot:run `"-Dspring-boot.run.arguments=--server.port=$BackendPort --studypilot.local-embedded-db.enabled=true`""
$backend = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', $backendCommand) -WorkingDirectory (Join-Path $projectRoot 'backend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $OutputRoot 'local-demo-backend.stdout.log') -RedirectStandardError (Join-Path $OutputRoot 'local-demo-backend.stderr.log') -PassThru
try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        try {
            if ((Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$BackendPort/api/v1/health" -TimeoutSec 2).StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 1 }
    }
    if (-not $ready) { throw '后端未能在 90 秒内就绪，请查看 D 盘输出目录中的 local-demo-backend.stderr.log。' }

    $frontend = Start-Process -FilePath 'pnpm.cmd' -ArgumentList @('exec', 'vite', '--host', '127.0.0.1', '--port', $FrontendPort) -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $OutputRoot 'local-demo-frontend.stdout.log') -RedirectStandardError (Join-Path $OutputRoot 'local-demo-frontend.stderr.log') -PassThru
    Start-Sleep -Seconds 2
    [pscustomobject]@{
        backendPid = $backend.Id
        frontendPid = $frontend.Id
        backendPort = $BackendPort
        frontendPort = $FrontendPort
        mode = 'local-demo-embedded-postgresql'
    } | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $OutputRoot 'local-demo-processes.json')
    Write-Host "本地演示已启动：http://127.0.0.1:$FrontendPort"
    Write-Host '说明：本模式的数据仅用于演示，服务停止后不会作为正式数据保留。'
} catch {
    Stop-Process -Id $backend.Id -Force -ErrorAction SilentlyContinue
    throw
}
