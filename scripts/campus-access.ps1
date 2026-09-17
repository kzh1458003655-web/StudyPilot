[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Start', 'Stop')]
    [string]$Action
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = 'D:\StudyPilot-runtime'
$outputRoot = 'D:\StudyPilot-output\campus-access'
$modelPort = 18082
$aiPort = 18081
$backendPort = 8080
$frontendPort = 5173
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

function Test-Http([string]$Url) {
    try {
        return (Invoke-WebRequest -UseBasicParsing -NoProxy -Uri $Url -TimeoutSec 2).StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-Http([string]$Url, [int]$Seconds, [string]$Name) {
    for ($attempt = 0; $attempt -lt $Seconds; $attempt++) {
        if (Test-Http $Url) { return }
        Start-Sleep -Seconds 1
    }
    throw "$Name 未能在 $Seconds 秒内启动，请查看 $outputRoot 中的日志。"
}

function Stop-CampusFrontend {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $frontendPort -ErrorAction SilentlyContinue)
    foreach ($listener in $listeners) {
        $processId = $listener.OwningProcess
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId"
        if ($process.Name -notmatch 'node' -or $process.CommandLine -notmatch 'vite') {
            throw "端口 $frontendPort 被非 Vite 进程占用，已拒绝停止：$($process.Name)"
        }
        Stop-Process -Id $processId -Force
    }
    Start-Sleep -Milliseconds 500
    if (Get-NetTCPConnection -State Listen -LocalPort $frontendPort -ErrorAction SilentlyContinue) {
        throw "端口 $frontendPort 仍在监听。"
    }
}

function Get-CampusAddress {
    $configuration = Get-NetIPConfiguration |
        Where-Object { $_.NetAdapter.Status -eq 'Up' -and $_.IPv4DefaultGateway -and $_.IPv4Address } |
        Sort-Object @{ Expression = { if ($_.InterfaceAlias -match 'WLAN|Wi-Fi') { 0 } else { 1 } } } |
        Select-Object -First 1
    if (-not $configuration) { throw '没有找到已联网的 IPv4 网卡。' }
    return $configuration.IPv4Address.IPAddress
}

if ($Action -eq 'Stop') {
    Stop-CampusFrontend
    Write-Host '校园网访问已关闭。后台、数据库和模型服务仍在本机运行。' -ForegroundColor Green
    exit 0
}

if (-not (Test-Http "http://127.0.0.1:$modelPort/health")) {
    $modelServer = Join-Path $runtimeRoot 'llama\llama-server.exe'
    $modelFile = Join-Path $runtimeRoot 'Qwen3.5-4B-Q4_K_M.gguf'
    if (-not (Test-Path -LiteralPath $modelServer) -or -not (Test-Path -LiteralPath $modelFile)) {
        throw '未找到本地模型运行文件，请先执行项目安装步骤。'
    }
    Start-Process -FilePath $modelServer `
        -ArgumentList @('-m', $modelFile, '--host', '127.0.0.1', '--port', $modelPort, '-ngl', '99', '-c', '16384', '-np', '4') `
        -WorkingDirectory (Split-Path -Parent $modelServer) -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $outputRoot 'model.stdout.log') `
        -RedirectStandardError (Join-Path $outputRoot 'model.stderr.log') | Out-Null
    Wait-Http "http://127.0.0.1:$modelPort/health" 60 '本地模型'
}

if (-not (Test-Http "http://127.0.0.1:$aiPort/health")) {
    $aiExecutable = Join-Path $projectRoot 'ai\bin\study-ai.exe'
    if (-not (Test-Path -LiteralPath $aiExecutable)) { throw '未找到已编译的 C++ AI 服务。' }
    $aiIndex = Join-Path $projectRoot '..\StudyPilot-runtime\ai-index'
    New-Item -ItemType Directory -Force -Path $aiIndex | Out-Null
    Start-Process -FilePath $aiExecutable -ArgumentList @($aiIndex) `
        -WorkingDirectory $projectRoot -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $outputRoot 'ai.stdout.log') `
        -RedirectStandardError (Join-Path $outputRoot 'ai.stderr.log') | Out-Null
    Wait-Http "http://127.0.0.1:$aiPort/health" 30 'C++ AI 服务'
}

if (-not (Test-Http "http://127.0.0.1:$backendPort/api/v1/health")) {
    $javaTemp = 'D:\StudyPilot-tmp'
    New-Item -ItemType Directory -Force -Path $javaTemp | Out-Null
    $backendCommand = "set `"JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=$javaTemp`"&& set `"MAVEN_USER_HOME=$javaTemp\m2`"&& .\mvnw.cmd spring-boot:run `"-Dspring-boot.run.arguments=--server.port=$backendPort --server.address=127.0.0.1 --studypilot.local-embedded-db.enabled=true`""
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', $backendCommand) `
        -WorkingDirectory (Join-Path $projectRoot 'backend') -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $outputRoot 'backend.stdout.log') `
        -RedirectStandardError (Join-Path $outputRoot 'backend.stderr.log') | Out-Null
    Wait-Http "http://127.0.0.1:$backendPort/api/v1/health" 120 'Java 后端'
}

$frontendListeners = @(Get-NetTCPConnection -State Listen -LocalPort $frontendPort -ErrorAction SilentlyContinue)
if ($frontendListeners -and $frontendListeners.LocalAddress -notcontains '0.0.0.0' -and $frontendListeners.LocalAddress -notcontains '::') {
    Stop-CampusFrontend
    $frontendListeners = @()
}
if (-not $frontendListeners) {
    $frontendCommand = "set CI=true&& pnpm exec vite --host 0.0.0.0 --port $frontendPort"
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', $frontendCommand) `
        -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $outputRoot 'frontend.stdout.log') `
        -RedirectStandardError (Join-Path $outputRoot 'frontend.stderr.log') | Out-Null
    Wait-Http "http://127.0.0.1:$frontendPort" 60 '前端'
}

$campusIp = Get-CampusAddress
$campusUrl = "http://${campusIp}:$frontendPort"
Wait-Http $campusUrl 10 '校园网入口'
Write-Host "校园网访问已开启：$campusUrl" -ForegroundColor Green
Write-Host '关闭时双击“关闭校园网访问.cmd”。'

