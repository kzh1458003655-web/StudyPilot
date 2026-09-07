$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
Set-Location -LiteralPath $Script:ProjectRoot
$config = Get-StudyConfig

$maven = Join-Path $config.RUNTIME 'maven/apache-maven-3.9.9/bin/mvn.cmd'
if (-not (Test-Path -LiteralPath $maven)) { throw 'Maven 未准备好，请先运行 setup.ps1。' }
if ($config.JAVA) {
    $javaPath = $config.JAVA
    $env:JAVA_HOME = if ((Test-Path -LiteralPath $javaPath -PathType Container)) {
        $javaPath
    } else {
        Split-Path -Parent (Split-Path -Parent $javaPath)
    }
}
$mavenRepo = Join-Path $config.RUNTIME '.m2/repository'
New-Item -ItemType Directory -Force -Path $mavenRepo | Out-Null
& $maven "-Dmaven.repo.local=$mavenRepo" -f backend/pom.xml -B -ntp package
if ($LASTEXITCODE -ne 0) { throw 'Java 构建或测试失败。' }

# 用 vswhere 兼容 Visual Studio Community 和 Build Tools，不依赖固定安装路径。
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
if (-not (Test-Path -LiteralPath $vswhere)) { throw '请安装 VS 2022 的“使用 C++ 的桌面开发”工作负载。' }
$vsPath = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $vsPath) { throw 'Visual Studio 未安装 MSVC x64 编译工具。' }
$vcvars = Join-Path $vsPath 'VC/Auxiliary/Build/vcvars64.bat'
$compile = 'call "' + $vcvars + '" >nul && cd /d "' + (Join-Path $Script:ProjectRoot 'ai') + '" && if not exist bin mkdir bin && cl /nologo /std:c++17 /EHsc /utf-8 /O2 /MD /I ..\runtime src\main.cpp /Fo:bin\main.obj /Fe:bin\study-ai.exe ws2_32.lib crypt32.lib'
& cmd.exe /d /c $compile
if ($LASTEXITCODE -ne 0) { throw 'C++ 构建失败。' }
Write-Host 'Java 与 C++ 均构建完成。'
