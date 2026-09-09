[CmdletBinding()]
param(
    [string]$BaseRef = '',
    [switch]$CheckCommits,
    [switch]$EnforceHarnessScope
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$failures = [Collections.Generic.List[string]]::new()

function Add-CheckFailure([string]$Message) {
    $failures.Add($Message)
}

function Convert-ToRepoPath([string]$Path) {
    return $Path.Replace('\', '/').Trim()
}

function Test-LocalMarkdownLinks([string]$RelativePath) {
    $absolutePath = Join-Path $projectRoot $RelativePath
    if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) {
        return
    }

    $content = Get-Content -LiteralPath $absolutePath -Raw -Encoding UTF8
    $matches = [regex]::Matches($content, '\[[^\]]*\]\((?<target>[^)]+)\)')
    foreach ($match in $matches) {
        $target = $match.Groups['target'].Value.Trim()
        if ($target.StartsWith('<') -and $target.EndsWith('>')) {
            $target = $target.Substring(1, $target.Length - 2)
        }
        if ($target -match '^(https?://|mailto:|#)') {
            continue
        }

        $pathOnly = ($target -split '#', 2)[0]
        if ([string]::IsNullOrWhiteSpace($pathOnly)) {
            continue
        }

        try {
            $decodedPath = [Uri]::UnescapeDataString($pathOnly)
            $baseDirectory = Split-Path -Parent $absolutePath
            $resolvedPath = [IO.Path]::GetFullPath((Join-Path $baseDirectory $decodedPath))
            if (-not (Test-Path -LiteralPath $resolvedPath)) {
                Add-CheckFailure "失效本地链接：$RelativePath -> $target"
            }
        } catch {
            Add-CheckFailure "无法解析本地链接：$RelativePath -> $target"
        }
    }
}

Set-Location -LiteralPath $projectRoot

$requiredPaths = @(
    'AGENTS.md',
    'README.md',
    'docs/harness/README.md',
    'docs/harness/feature_list.json',
    'docs/harness/feature_list.schema.json',
    'docs/harness/progress.md',
    'docs/harness/session-handoff.md',
    'docs/harness/verification.md',
    'docs/harness/lifecycle.md',
    'docs/关键记录/README.md',
    'docs/关键记录/架构迁移总纲.md',
    'docs/关键记录/数据库演进记录.md',
    'docs/关键记录/接口契约索引.md',
    'docs/关键记录/重构复用清单.md',
    'docs/关键记录/决策记录/ADR-001-harness-source-of-truth.md',
    'docs/关键记录/决策记录/ADR-002-architecture-first.md',
    'docs/关键记录/决策记录/ADR-003-legacy-code-strategy.md',
    'docs/关键记录/决策记录/ADR-004-commit-and-pr-policy.md',
    'docs/产品相关/仓库组织预期.md',
    'docs/产品相关/产品架构.md',
    'docs/产品相关/需求规划.md',
    'backend/docs/后端框架设计.md',
    'frontend/前端框架设计.md'
)

$frozenDocs = @(
    'README-旧.md',
    'CONTRIBUTING-旧.md',
    '软件测试记录-旧.md',
    'docs/项目架构图-旧.md'
)

foreach ($relativePath in $requiredPaths + $frozenDocs) {
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $relativePath))) {
        Add-CheckFailure "缺少必需文件：$relativePath"
    }
}

$featureList = $null
$featurePath = Join-Path $projectRoot 'docs/harness/feature_list.json'
$schemaPath = Join-Path $projectRoot 'docs/harness/feature_list.schema.json'
if ((Test-Path -LiteralPath $featurePath) -and (Test-Path -LiteralPath $schemaPath)) {
    try {
        $featureRaw = Get-Content -LiteralPath $featurePath -Raw -Encoding UTF8
        # Keep the Harness check usable from both Windows PowerShell 5.1 and PowerShell 7.
        $featureList = $featureRaw | ConvertFrom-Json
        if (Get-Command Test-Json -ErrorAction SilentlyContinue) {
            $schemaRaw = Get-Content -LiteralPath $schemaPath -Raw -Encoding UTF8
            if (-not (Test-Json -Json $featureRaw -Schema $schemaRaw -ErrorAction Stop)) {
                Add-CheckFailure 'feature_list.json 不符合 feature_list.schema.json。'
            }
        }
    } catch {
        Add-CheckFailure "任务清单无法解析或通过 Schema：$($_.Exception.Message)"
    }
}

if ($null -ne $featureList) {
    $features = @($featureList.features)
    $allowedStatuses = @('blocked', 'ready', 'in_progress', 'done', 'verified')
    $requiredFeatureFields = @(
        'id', 'phase', 'name', 'description', 'dependencies', 'status',
        'acceptanceCriteria', 'branch', 'evidence', 'docsToUpdate'
    )

    foreach ($feature in $features) {
        $propertyNames = @($feature.PSObject.Properties.Name)
        foreach ($field in $requiredFeatureFields) {
            if ($field -notin $propertyNames) {
                Add-CheckFailure "任务 $($feature.id) 缺少字段：$field"
            }
        }
        if ($feature.status -notin $allowedStatuses) {
            Add-CheckFailure "任务 $($feature.id) 使用非法状态：$($feature.status)"
        }
        if ($feature.branch -notmatch '^[a-z]+/[a-z0-9][a-z0-9-]*$') {
            Add-CheckFailure "任务 $($feature.id) 使用非法分支名：$($feature.branch)"
        }
        if (@($feature.acceptanceCriteria).Count -eq 0) {
            Add-CheckFailure "任务 $($feature.id) 没有验收标准。"
        }
        if ($feature.status -eq 'verified' -and @($feature.evidence).Count -eq 0) {
            Add-CheckFailure "已验证任务 $($feature.id) 没有证据。"
        }
    }

    $duplicateIds = @($features | Group-Object id | Where-Object Count -gt 1)
    foreach ($duplicate in $duplicateIds) {
        Add-CheckFailure "任务 ID 重复：$($duplicate.Name)"
    }

    $featureById = @{}
    foreach ($feature in $features) {
        $featureById[$feature.id] = $feature
    }

    $inDegree = @{}
    $dependents = @{}
    foreach ($feature in $features) {
        $inDegree[$feature.id] = 0
        $dependents[$feature.id] = [Collections.Generic.List[string]]::new()
    }

    foreach ($feature in $features) {
        foreach ($dependency in @($feature.dependencies)) {
            if (-not $featureById.ContainsKey($dependency)) {
                Add-CheckFailure "任务 $($feature.id) 依赖不存在的任务：$dependency"
                continue
            }
            $inDegree[$feature.id] = [int]$inDegree[$feature.id] + 1
            $dependents[$dependency].Add($feature.id)
        }

        if ($feature.status -in @('ready', 'in_progress', 'done', 'verified')) {
            $unverifiedDependencies = @(
                $feature.dependencies | Where-Object {
                    $featureById.ContainsKey($_) -and $featureById[$_].status -ne 'verified'
                }
            )
            if ($unverifiedDependencies.Count -gt 0) {
                Add-CheckFailure "任务 $($feature.id) 状态为 $($feature.status)，但依赖尚未 verified：$($unverifiedDependencies -join ', ')"
            }
        }

        foreach ($docPath in @($feature.docsToUpdate)) {
            if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $docPath))) {
                Add-CheckFailure "任务 $($feature.id) 引用了不存在的文档：$docPath"
            }
        }
    }

    $queue = [Collections.Generic.Queue[string]]::new()
    foreach ($feature in $features) {
        if ([int]$inDegree[$feature.id] -eq 0) {
            $queue.Enqueue($feature.id)
        }
    }

    $visitedCount = 0
    while ($queue.Count -gt 0) {
        $currentId = $queue.Dequeue()
        $visitedCount++
        foreach ($dependentId in $dependents[$currentId]) {
            $inDegree[$dependentId] = [int]$inDegree[$dependentId] - 1
            if ([int]$inDegree[$dependentId] -eq 0) {
                $queue.Enqueue($dependentId)
            }
        }
    }
    if ($visitedCount -ne $features.Count) {
        Add-CheckFailure '任务依赖存在循环。'
    }

    $inProgress = @($features | Where-Object status -eq 'in_progress')
    if ($inProgress.Count -gt 1) {
        Add-CheckFailure "同时存在多个 in_progress 任务：$($inProgress.id -join ', ')"
    }

    $actionable = @($features | Where-Object status -in @('ready', 'in_progress', 'done'))
    # 全部任务均已验证时没有下一任务是合法终态；开发中仍要求唯一可操作任务。
    $allVerified = $features.Count -gt 0 -and @($features | Where-Object status -ne 'verified').Count -eq 0
    if ($actionable.Count -eq 0 -and $allVerified) {
        # Final delivery state: no additional work item should be invented merely to satisfy the checker.
    } elseif ($actionable.Count -ne 1) {
        Add-CheckFailure "必须恰好存在一个可操作任务（ready/in_progress/done），当前为：$($actionable.id -join ', ')"
    }
}

$markdownFiles = @(
    'AGENTS.md',
    'README.md',
    'docs/harness/README.md',
    'docs/harness/progress.md',
    'docs/harness/session-handoff.md',
    'docs/harness/verification.md',
    'docs/harness/lifecycle.md',
    'docs/关键记录/README.md',
    'docs/关键记录/架构迁移总纲.md',
    'docs/关键记录/数据库演进记录.md',
    'docs/关键记录/接口契约索引.md',
    'docs/关键记录/重构复用清单.md'
)
foreach ($markdownFile in $markdownFiles) {
    Test-LocalMarkdownLinks $markdownFile
}

if (-not [string]::IsNullOrWhiteSpace($BaseRef)) {
    & git cat-file -e "$BaseRef`^{commit}" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Add-CheckFailure "无法解析 Git 基线：$BaseRef"
    } else {
        $diffLines = @(& git -c core.quotepath=false diff --name-status "$BaseRef...HEAD")
        $changedPaths = [Collections.Generic.List[string]]::new()
        foreach ($line in $diffLines) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            $parts = $line -split "`t"
            $status = $parts[0]
            for ($index = 1; $index -lt $parts.Count; $index++) {
                $changedPaths.Add((Convert-ToRepoPath $parts[$index]))
            }

            if ($status -match '^[MD]' -and (Convert-ToRepoPath $parts[1]) -in $frozenDocs) {
                Add-CheckFailure "冻结旧文档被修改或删除：$($parts[1])"
            }
            if ($status -match '^[RC]' -and (Convert-ToRepoPath $parts[1]) -in $frozenDocs) {
                Add-CheckFailure "冻结旧文档被移动或复制为其他路径：$($parts[1])"
            }
        }

        if ($EnforceHarnessScope) {
            $disallowedPatterns = @(
                '^backend/src/',
                '^backend/pom\.xml$',
                '^ai/src/',
                '^frontend/src/',
                '^frontend/(app\.js|index\.html|style\.css|vue\.global\.prod\.js)$',
                '^start\.ps1$',
                '^stop\.ps1$',
                '^build\.ps1$'
            )
            foreach ($changedPath in $changedPaths) {
                if ($disallowedPatterns | Where-Object { $changedPath -match $_ }) {
                    Add-CheckFailure "Harness 任务修改了业务或运行代码：$changedPath"
                }
            }
        }

        if ($CheckCommits) {
            $commitPattern = '^(feat|fix|refactor|docs|test|chore|build)\([a-z0-9][a-z0-9-]*\): \S.+$'
            $commitHashes = @(& git rev-list --no-merges "$BaseRef..HEAD")
            foreach ($commitHash in $commitHashes) {
                $subject = (& git log -1 --format=%s $commitHash).Trim()
                if ($subject -notmatch $commitPattern) {
                    Add-CheckFailure "Commit 格式不合规：$commitHash $subject"
                    continue
                }
                if ($subject -match '[。.]$') {
                    Add-CheckFailure "Commit 摘要不能以句号结尾：$commitHash $subject"
                }
            }
        }
    }
} elseif ($CheckCommits -or $EnforceHarnessScope) {
    Add-CheckFailure '检查 Commit 或 Harness 范围时必须提供 -BaseRef。'
}

if ($failures.Count -gt 0) {
    Write-Host 'Harness 检查失败：' -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host "- $failure" -ForegroundColor Red
    }
    exit 1
}

$actionableSummary = if ($null -ne $featureList) {
    $current = @($featureList.features | Where-Object status -in @('ready', 'in_progress', 'done')) | Select-Object -First 1
    if ($null -eq $current) { '全部任务已 verified' } else { "$($current.id) [$($current.status)] $($current.name)" }
} else {
    '任务清单未加载'
}

Write-Host 'Harness 检查通过。' -ForegroundColor Green
Write-Host "仓库：$projectRoot"
Write-Host "当前可操作任务：$actionableSummary"
