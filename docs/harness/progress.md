# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-08 13:23 +08:00
- 当前分支：`docs/harness-foundation`
- 已完成任务：`HARNESS-001` 建设 Agent Harness，状态为 `verified`
- 当前状态：Harness 已建设并通过结构、提交、范围和初始化验证
- 下一任务：`ARCH-001` 盘点现有实现并隔离旧代码，状态为唯一 `ready`

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
- 本轮必须在 Harness 完成后停止，不自动执行 `ARCH-001`。

## 当前边界

- 本任务不修改 `backend/src`、`ai/src` 或当前前端业务文件。
- 本任务不创建数据库 Schema、不迁移 MySQL、不连接 Supabase。
- 本任务不移动、删除或重构旧业务代码。
- 本任务结束后立即停止，不自动执行 `ARCH-001`。

## 风险和注意事项

- 当前代码仍是课程版原型，不能误写成已完成目标架构。
- 当前有效文档与旧实现存在差异，后续必须通过架构迁移记录明确处理。
- 文档已移动到 `docs/产品相关/`，所有入口链接需要使用新位置。
- 旧文档在本次基线登记后冻结，后续修改必须由检查脚本阻止。

## 下一步

1. 下一位 Agent 运行 `./scripts/harness-init.ps1`。
2. 阅读 Agent 入口、任务状态、交接和目标架构文档。
3. 从包含最新 Harness 的基线创建 `refactor/architecture-baseline`。
4. 只执行 `ARCH-001`，完成现状盘点、旧代码归档和行为基线。

## 会话变更

- `docs(architecture): 固化现有文档重组基线`
- `docs(harness): 建立代理入口和生命周期规范`
- `docs(harness): 建立任务状态与交接协议`
- `docs(decision): 记录架构优先和旧代码策略`
- `test(harness): 增加只读初始化和结构校验`
- `build(ci): 接入 Harness 和提交规范检查`
- `docs(harness): 同步有效文档和当前阶段`
- `fix(harness): 修正初始化退出状态判断`

## 关键决策

- Harness 状态和正式架构记录分离。
- 架构重构完成前禁止开始产品任务。
- 旧代码通过独立分支和仓库外 worktree 保留。
- Commit 使用 Conventional Commit 类型、稳定 scope 和中文动作摘要。

具体理由和影响记录在 `docs/关键记录/决策记录/`。

## 验证证据

- `./scripts/harness-check.ps1`：通过。
- `./scripts/harness-check.ps1 -BaseRef origin/main -CheckCommits -EnforceHarnessScope`：通过。
- `./scripts/harness-init.ps1`：通过，能够输出当前分支、环境和唯一推荐任务。
- JSON Schema：12 个任务全部通过结构校验。
- 业务范围：本轮未修改 `backend/src`、`ai/src` 或前端运行文件。
