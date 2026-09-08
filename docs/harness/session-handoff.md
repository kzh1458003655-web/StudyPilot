# Session Handoff

## 当前目标

- 已完成任务：`HARNESS-001`、`ARCH-001`
- 完成状态：均为 `verified`
- 当前分支：`refactor/architecture-baseline`
- 下一任务：`ARCH-002` 建立后端目标骨架，状态为唯一 `ready`

## 本次已完成

- 建立 Agent 根入口和 Harness 生命周期。
- 建立完整任务图和机器可读 Schema。
- 明确 Harness、产品文档、架构文档和关键记录的职责边界。
- 明确旧代码只通过归档分支和仓库外 worktree 参考。
- 建立关键记录、数据库演进、接口索引、复用清单和 ADR。
- 建立只读初始化与结构检查脚本。
- 接入 CI、Commit、冻结文档、Harness 范围和 PR 模板检查。
- 同步有效文档并修复 README 的旧路径。
- 建立 `archive/legacy-course-prototype` 分支和仓库外 `StudyPilot-legacy` worktree。
- 在归档 worktree 上建立 16 项 Java 测试通过的行为基线。
- 盘点旧前端、Java、MySQL、C++ 和测试脚本，记录可复用候选与不兼容边界。
- 修复 Harness 初始化和检查脚本在 Windows PowerShell 5.1 下的兼容性。

## 验证证据

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| Harness 结构 | `.\scripts\harness-check.ps1` | 通过（Windows PowerShell 5.1） |
| Commit、冻结文档和任务范围 | `.\scripts\harness-check.ps1 -BaseRef origin/main -CheckCommits -EnforceHarnessScope` | 通过 |
| Agent 初始化 | `.\scripts\harness-init.ps1` | 通过，能输出当前分支、环境和 ARCH-002 推荐任务 |
| 任务 Schema | `Test-Json` 与 Harness 检查 | 12 个任务通过 |
| 归档行为基线 | 归档 worktree 中执行 Maven 测试 | 16 项通过 |
| 业务源码范围 | 相对 `origin/main` 的 Git 变更检查 | 未修改业务源码或数据库实现 |

## 下一会话启动顺序

下一位 Agent 必须：

1. 在仓库根目录运行 `./scripts/harness-init.ps1`。
2. 阅读 `AGENTS.md`、任务清单、进度和本交接文档。
3. 阅读后端、前端和产品架构文档。
4. 确认 `HARNESS-001`、`ARCH-001` 均为 `verified`，`ARCH-002` 是唯一 `ready` 任务。
5. 从 `refactor/backend-skeleton` 创建任务分支。
6. 建立 Java 模块化单体、公共异常与 Gateway 契约，并保证无外部服务时可执行骨架测试。
7. 不得迁入旧 Controller、MySQL DDL 或完整产品功能。

## 阻塞和风险

- 归档旧代码只能从 `archive/legacy-course-prototype` 的仓库外 worktree 读取；不要在目标结构中复制旧目录。
- 当前可运行版本使用课程边界和本地 MySQL，与目标项目边界和 PostgreSQL 架构不同。
- 未获得数据库验证环境前，只能记录该外部条件，不能伪造 Supabase 验证结果。

## 推荐下一步

运行 Harness 初始化检查，阅读后端架构与现状盘点，然后开始 `ARCH-002`。
