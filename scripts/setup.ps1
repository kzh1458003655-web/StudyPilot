param([switch]$SkipModel, [string]$Runtime)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
Set-Location -LiteralPath $Script:ProjectRoot

if (-not [Environment]::Is64BitOperatingSystem) { throw '本项目只支持 64 位 Windows。' }
if (-not $Runtime) {
    if (Test-Path -LiteralPath $Script:ConfigPath) {
        $rawConfig = Get-Content -LiteralPath $Script:ConfigPath -Raw | ConvertFrom-Json
        $Runtime = $rawConfig.RUNTIME
        if (-not [IO.Path]::IsPathRooted($Runtime)) { $Runtime = [IO.Path]::GetFullPath((Join-Path $Script:ProjectRoot $Runtime)) }
    } else {
        # 运行环境和模型约需数 GB，自动选取剩余空间最大的本地磁盘。
        $drive = Get-PSDrive -PSProvider FileSystem | Where-Object { $_.Free -gt 8GB } | Sort-Object Free -Descending | Select-Object -First 1
        if (-not $drive) { throw '没有找到至少 8GB 可用空间的磁盘。可用 -Runtime 指定安装目录。' }
        $Runtime = Join-Path $drive.Root 'StudyPilot-runtime'
    }
}
$runtime = [IO.Path]::GetFullPath($Runtime)
$downloads = Join-Path $runtime '.downloads'
New-Item -ItemType Directory -Force -Path $runtime, $downloads, 'data', 'logs', 'app' | Out-Null

function Get-Archive([string]$Url, [string]$FileName) {
    $target = Join-Path $downloads $FileName
    if (-not (Test-Path -LiteralPath $target)) {
        Write-Host "下载 $FileName"
        & curl.exe -L --fail --retry 3 -o $target $Url
        if ($LASTEXITCODE -ne 0) { throw "下载失败：$Url" }
    }
    return $target
}

# 使用便携版依赖，不修改系统 PATH，也不要求管理员权限。
$javaExe = Join-Path $runtime 'java/bin/java.exe'
if (-not (Test-Path -LiteralPath $javaExe)) {
    $archive = Get-Archive 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' 'temurin-jdk21.zip'
    $temp = Join-Path $runtime 'extract-java'
    Expand-Archive -LiteralPath $archive -DestinationPath $temp -Force
    $jdk = Get-ChildItem $temp -Directory | Select-Object -First 1
    Move-Item -LiteralPath $jdk.FullName -Destination (Join-Path $runtime 'java')
    Remove-Item -LiteralPath $temp -Recurse -Force
}

$mavenHome = Join-Path $runtime 'maven/apache-maven-3.9.9'
if (-not (Test-Path -LiteralPath (Join-Path $mavenHome 'bin/mvn.cmd'))) {
    $archive = Get-Archive 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' 'apache-maven-3.9.9.zip'
    New-Item -ItemType Directory -Force -Path (Join-Path $runtime 'maven') | Out-Null
    Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $runtime 'maven') -Force
}

$mysqlHome = Join-Path $runtime 'mysql/mysql-8.4.0-winx64'
if (-not (Test-Path -LiteralPath (Join-Path $mysqlHome 'bin/mysqld.exe'))) {
    $archive = Get-Archive 'https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-8.4.0-winx64.zip' 'mysql-8.4.0-winx64.zip'
    New-Item -ItemType Directory -Force -Path (Join-Path $runtime 'mysql') | Out-Null
    Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $runtime 'mysql') -Force
}

# 项目以 NVIDIA 显卡为主要运行环境；CUDA 运行库随 llama.cpp 一起放入本地目录。
$llamaHome = Join-Path $runtime 'llama'
if (-not (Test-Path -LiteralPath (Join-Path $llamaHome 'llama-server.exe'))) {
    New-Item -ItemType Directory -Force -Path $llamaHome | Out-Null
    $archive = Get-Archive 'https://github.com/ggml-org/llama.cpp/releases/download/b10819/llama-b10819-bin-win-cuda-12.4-x64.zip' 'llama-b10819-cuda.zip'
    $cudaArchive = Get-Archive 'https://github.com/ggml-org/llama.cpp/releases/download/b10819/cudart-llama-bin-win-cuda-12.4-x64.zip' 'llama-b10819-cudart.zip'
    Expand-Archive -LiteralPath $archive -DestinationPath $llamaHome -Force
    Expand-Archive -LiteralPath $cudaArchive -DestinationPath $llamaHome -Force
}

# 每台电脑生成独立密码和模型接口密钥；config.local.json 已被 Git 忽略。
if (-not (Test-Path -LiteralPath $Script:ConfigPath)) {
    [ordered]@{
        RUNTIME = $runtime
        STUDY_DB_PASSWORD = 'S' + [guid]::NewGuid().ToString('N')
        ROOT_PASSWORD = 'R' + [guid]::NewGuid().ToString('N')
        MODEL_KEY = [guid]::NewGuid().ToString('N')
        JAVA = $javaExe
    } | ConvertTo-Json | Set-Content -LiteralPath $Script:ConfigPath -Encoding utf8
} else {
    # 旧开发机可能把 JAVA 留空并意外使用系统中的其他版本，统一切到项目自带的 Java 21。
    $existingConfig = Get-Content -LiteralPath $Script:ConfigPath -Raw | ConvertFrom-Json
    if (-not $existingConfig.JAVA) {
        $existingConfig | Add-Member -NotePropertyName JAVA -NotePropertyValue $javaExe -Force
        $existingConfig | ConvertTo-Json | Set-Content -LiteralPath $Script:ConfigPath -Encoding utf8
    }
}
$config = Get-StudyConfig
if (-not $SkipModel) { & (Join-Path $PSScriptRoot 'download-model.ps1') -Runtime $config.RUNTIME }

# 初始化项目私有的 MySQL 数据目录，不注册 Windows 服务。
$dataDir = Join-Path $Script:ProjectRoot 'data/mysql'
$mysqld = Join-Path $mysqlHome 'bin/mysqld.exe'
$mysql = Join-Path $mysqlHome 'bin/mysql.exe'
if (-not (Test-Path -LiteralPath (Join-Path $dataDir 'mysql'))) {
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
    & $mysqld --no-defaults "--basedir=$mysqlHome" "--datadir=$dataDir" --initialize-insecure
    if ($LASTEXITCODE -ne 0) { throw 'MySQL 数据目录初始化失败。' }
    $dbProcess = Start-Process -FilePath $mysqld -ArgumentList @('--no-defaults', "--basedir=$mysqlHome", "--datadir=$dataDir", '--port=13306', '--bind-address=127.0.0.1', '--mysqlx=0', '--console') -WindowStyle Hidden -PassThru
    try {
        $deadline = [DateTime]::UtcNow.AddSeconds(45)
        do {
            Start-Sleep -Milliseconds 500
            & $mysql --host=127.0.0.1 --port=13306 -u root --execute 'SELECT 1' 2>$null
            $ready = $LASTEXITCODE -eq 0
        } until ($ready -or [DateTime]::UtcNow -ge $deadline)
        if (-not $ready) { throw 'MySQL 初始化实例未能启动。' }
        $sql = "ALTER USER 'root'@'localhost' IDENTIFIED BY '$($config.ROOT_PASSWORD)'; CREATE DATABASE IF NOT EXISTS studypilot CHARACTER SET utf8mb4; CREATE USER IF NOT EXISTS 'study'@'localhost' IDENTIFIED BY '$($config.STUDY_DB_PASSWORD)'; ALTER USER 'study'@'localhost' IDENTIFIED BY '$($config.STUDY_DB_PASSWORD)'; GRANT ALL ON studypilot.* TO 'study'@'localhost'; FLUSH PRIVILEGES;"
        & $mysql --host=127.0.0.1 --port=13306 -u root --execute $sql
        if ($LASTEXITCODE -ne 0) { throw '数据库和开发账号创建失败。' }
        $env:MYSQL_PWD = $config.ROOT_PASSWORD
        & (Join-Path $mysqlHome 'bin/mysqladmin.exe') --host=127.0.0.1 --port=13306 -u root shutdown
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    } finally {
        if (-not $dbProcess.HasExited) { Stop-Process -Id $dbProcess.Id -Force }
    }
}

& (Join-Path $PSScriptRoot 'build.ps1')
Write-Host '环境准备完成。运行 .\scripts\start.ps1 启动，运行 .\scripts\test.ps1 验证。'
