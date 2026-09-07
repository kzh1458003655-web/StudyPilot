# 公共路径和检查函数。其他脚本通过点调用复用本文件，避免各处维护不同端口或目录。
$Script:ProjectRoot = Split-Path -Parent $PSScriptRoot
$Script:ConfigPath = Join-Path $Script:ProjectRoot 'config.local.json'

function Get-StudyConfig {
    if (-not (Test-Path -LiteralPath $Script:ConfigPath)) {
        throw '缺少 config.local.json。请先运行 .\scripts\setup.ps1。'
    }
    $config = Get-Content -LiteralPath $Script:ConfigPath -Raw | ConvertFrom-Json
    if (-not [IO.Path]::IsPathRooted($config.RUNTIME)) {
        $config.RUNTIME = [IO.Path]::GetFullPath((Join-Path $Script:ProjectRoot $config.RUNTIME))
    }
    if ($config.JAVA -and -not [IO.Path]::IsPathRooted($config.JAVA)) {
        $config.JAVA = [IO.Path]::GetFullPath((Join-Path $Script:ProjectRoot $config.JAVA))
    }
    return $config
}

function Wait-StudyUrl([string]$Url, [int]$Seconds) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        try { return Invoke-RestMethod -Uri $Url -TimeoutSec 3 }
        catch { Start-Sleep -Milliseconds 600 }
    }
    throw "服务未在 ${Seconds} 秒内就绪：$Url。请检查 logs 目录。"
}

function Test-StudyPort([int]$Port) {
    try {
        $client = [Net.Sockets.TcpClient]::new()
        $connected = $client.ConnectAsync('127.0.0.1', $Port).Wait(400)
        $client.Dispose()
        return $connected
    } catch { return $false }
}
