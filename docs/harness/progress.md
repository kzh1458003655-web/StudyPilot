# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-08 15:42 +08:00
- 当前分支：`refactor/architecture-baseline`
- 已完成任务：`HARNESS-001`、`ARCH-001`，状态均为 `verified`
- 当前状态：旧课程版已归档，目标架构尚未开始实现
- 下一任务：`ARCH-002` 建立后端目标骨架，状态为唯一 `ready`

## 已完成

- [x] 盘点当前仓库、有效文档、冻结旧文档和现有 CI。
- [x] 将此前未提交的文档重组单独固化为基线 commit。
- [x] 新建 `docs/harness-foundation` 分支。
- [x] 建立根级 `AGENTS.md`。
- [x] 建立 Harness README 和生命周期规范。
- [x] 建立任务清单及 JSON Schema。
- [x] 建立正式关键记录和 ADR。
- [x] 建立只读初始化与检查脚本。
- [x] 接入 CI、Commit 和 PR 规则。
- [x] 更新有效文档中的 Harness 入口和阶段状态。
- [x] 运行 Harness 结构、提交范围和初始化验收。

## 当前进行

- 当前没有正在执行的实现任务。
- `ARCH-001` 已按架构优先门禁结束，不提前进入后端、数据库或产品开发。

## 当前边界

- 本任务未修改 `backend/src`、`ai/src` 或当前前端业务文件。
- 本任务未创建目标数据库 Schema、未迁移 MySQL、未连接 Supabase。
- 旧业务代码未被移动、删除或重构；仅建立了归档分支与仓库外 worktree。

## 风险和注意事项

- 当前代码仍是课程版原型，不能误写成已完成目标架构。
- 归档版本的 `scripts/build.ps1` 在 Windows PowerShell 5.1 下存在 UTF-8 无 BOM 的解析风险；已记录，不能将其当作通过证据。
- 目标 PostgreSQL/Supabase 测试环境尚未接入，后续不得伪造数据库集成结果。

## 下一步

1. 下一位 Agent 运行 `./scripts/harness-init.ps1`。
2. 阅读 Agent 入口、任务状态、交接和后端架构文档。
3. 从 `refactor/backend-skeleton` 分支执行 `ARCH-002`，先建立无外部服务可测试的 Java 骨架。
4. 不迁移旧 Controller、旧 MySQL SQL 或旧运行时建表逻辑。

## 会话变更

- `docs(architecture): 固化现有文档重组基线`
- `docs(harness): 建立代理入口和生命周期规范`
- `docs(harness): 建立任务状态与交接协议`
- `docs(decision): 记录架构优先和旧代码策略`
- `test(harness): 增加只读初始化和结构校验`
- `build(ci): 接入 Harness 和提交规范检查`
- `docs(harness): 同步有效文档和当前阶段`
- `fix(harness): 修正初始化退出状态判断`
- `fix(harness): 修复 Windows PowerShell 兼容`
- `refactor(architecture): 归档课程版并建立行为基线`

## 关键决策

- Harness 状态和正式架构记录分离。
- 架构重构完成前禁止开始产品任务。
- 旧代码通过独立分支和仓库外 worktree 保留。
- Commit 使用 Conventional Commit 类型、稳定 scope 和中文动作摘要。
- 当前没有旧实现被直接复用到目标架构。

具体理由和影响记录在 `docs/关键记录/决策记录/`。

## 验证证据

- `./scripts/harness-check.ps1`：通过。
- `./scripts/harness-check.ps1 -BaseRef origin/main -CheckCommits -EnforceHarnessScope`：通过。
- `./scripts/harness-init.ps1`：通过，能够输出当前分支、环境和唯一推荐任务。
- JSON Schema：12 个任务全部通过结构校验。
- 归档 worktree Maven 测试：16 个 Java 测试通过。
- 业务范围：本轮未修改 `backend/src`、`ai/src` 或前端运行文件。
