# Harness 验证说明

## Harness 结构检查

从仓库根目录运行：

```powershell
.\scripts\harness-check.ps1
```

检查范围：

- 必需文件存在；
- `feature_list.json` 可解析并符合 Schema；
- 任务 ID 唯一；
- 依赖项存在且不存在依赖环；
- 状态与依赖一致；
- 同时最多一个 `in_progress`；
- 只有一个可执行任务；
- `verified` 任务具有证据；
- Harness 和关键记录中的本地链接有效；
- 冻结旧文档存在。

## Agent 初始化检查

```powershell
.\scripts\harness-init.ps1
```

初始化脚本还会输出：

- 仓库根目录；
- 当前分支和工作区状态；
- Git、Java、Node.js、pnpm 和 Python 的可用情况；
- 当前唯一推荐任务；
- Agent 必须阅读的入口。

初始化和结构检查都是只读操作，不安装依赖、不修改文件、不切换分支、不创建 commit。

## Commit 与变更范围检查

CI 在 Pull Request 中使用基线 commit 运行：

```powershell
.\scripts\harness-check.ps1 -BaseRef <base-sha> -CheckCommits -EnforceHarnessScope
```

它还会验证：

- Commit 主题符合 `type(scope): 中文动词 + 对象`；
- 冻结旧文档没有被修改、删除或移出归档名称；
- Harness 建设分支没有修改业务源码、数据库实现或产品运行文件。

`EnforceHarnessScope` 只用于 Harness 建设任务；后续代码任务仍执行结构、状态、Commit 和冻结文档检查，但不启用 Harness 源码范围限制。

## 后续任务验证原则

- 每个任务在 `acceptanceCriteria` 中声明最低完成条件。
- 任务的真实命令和结果写入 `evidence` 或本文件对应阶段。
- 外部环境缺失时记录阻塞，不能用未执行、Mock 或静态检查冒充真实集成验证。
- 修改范围较小时先执行针对性检查；PR 前执行该任务要求的完整检查。
- 阶段结束时执行有效文档一致性检查。

## 当前 Harness 验证记录

验证日期：2026-09-08

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| JSON 和 Harness 结构 | `.\scripts\harness-check.ps1` | 通过 |
| Commit 风格 | `.\scripts\harness-check.ps1 -BaseRef origin/main -CheckCommits` | 通过 |
| 冻结文档与 Harness 范围 | `.\scripts\harness-check.ps1 -BaseRef origin/main -CheckCommits -EnforceHarnessScope` | 通过 |
| Agent 启动演练 | `.\scripts\harness-init.ps1` | 通过 |

初始化演练曾发现脚本误用外部命令退出码，已通过 `fix(harness): 修正初始化退出状态判断` 修复并重新验证。

本次未运行产品构建和端到端测试，因为 Harness 任务没有修改业务源码，也不得借验证开始后续重构。现有 CI 仍会在 Harness 检查通过后执行原有 Java 和 C++ 构建。
