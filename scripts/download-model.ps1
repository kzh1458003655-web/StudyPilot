param([string]$Runtime, [switch]$Force)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

# 固定仓库、文件名和哈希，保证三名开发者使用完全相同的量化模型。
$modelName = 'Qwen3.5-4B-Q4_K_M.gguf'
$modelUrl = 'https://huggingface.co/unsloth/Qwen3.5-4B-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf?download=true'
$expectedSha256 = '00FE7986FF5F6B463E62455821146049DB6F9313603938A70800D1FB69EF11A4'

if (-not $Runtime) {
    $Runtime = if (Test-Path -LiteralPath $Script:ConfigPath) { (Get-StudyConfig).RUNTIME } else { Join-Path $Script:ProjectRoot '.local/runtime' }
}
$Runtime = [IO.Path]::GetFullPath($Runtime)
$target = Join-Path $Runtime $modelName
New-Item -ItemType Directory -Force -Path $Runtime | Out-Null
if ((Test-Path -LiteralPath $target) -and -not $Force) {
    $actual = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
    if ($actual -eq $expectedSha256) { Write-Host "模型已存在且校验通过：$target"; exit 0 }
    throw "现有模型哈希不匹配：$actual。确认文件无用后使用 -Force 重新下载。"
}
Write-Host '开始下载约 2.6 GiB 的模型，网络中断后可重新运行本脚本续传。'
& curl.exe -L --fail --retry 3 --retry-delay 3 -C - -o $target $modelUrl
if ($LASTEXITCODE -ne 0) { throw '模型下载失败。可稍后重试，已下载部分会被保留。' }
$actual = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
if ($actual -ne $expectedSha256) { throw "模型哈希不匹配。期望 $expectedSha256，实际 $actual。" }
Write-Host "模型下载并校验完成：$target"
