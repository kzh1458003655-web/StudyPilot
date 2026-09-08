# Session Handoff

## 当前目标

- 已验证任务：`HARNESS-001`、`ARCH-001` 至 `ARCH-004`、`DB-001`、`INT-001`
- 当前分支：`test/architecture-integration`
- 下一唯一任务：`PROD-001`，状态为 `ready`
- 下一任务分支：`feat/grounded-qa`

## 现在已经可靠的事实

- 旧课程版只在 `archive/legacy-course-prototype` 分支和仓库外 `StudyPilot-legacy` worktree 保留。
- 后端为 Java 21 目标代码，目标数据库为 PostgreSQL；不要迁回 MySQL、JPA、H2、MyBatis 或运行时建表。
- C++ 的 `/health`、`/retrieve`、`/completion` 协议已实际验证。Java 只能经过 `ModelGateway` 和 `RetrievalGateway` 调用它。
- 前端目标骨架已通过 `pnpm run check`，未开始完整产品页面。
- `DB-001` 完整 DDL 在 `backend/src/main/resources/db/schema`，包含 24 张表与集中索引；`PostgreSqlSchemaIntegrationTest` 使用临时真实 PostgreSQL 连续执行两遍 DDL 并通过。
- 数据源仅在三个 `STUDYPILOT_DB_*` 环境变量存在时创建；`verify-schema.ps1` 适用于操作者已明确指定的独立测试库。
- Java 的 `/api/v1/health/dependencies` 通过 Gateway 调用 C++ 健康接口；Vite 的 `/api` 代理使浏览器只与 Java 通信。真实成功链路与 503 失败契约均已验证。

## 下一会话启动顺序

1. 在仓库根目录运行 `./scripts/harness-init.ps1`，阅读 `AGENTS.md`、任务清单、本文件和 `verification.md`。
2. 检查 Git 状态与本地 Java 版本。当前系统默认 Java 是 25；后端脚本需要 JDK 21，不能修改脚本来规避该要求。
3. 从当前分支创建 `feat/grounded-qa`，把 `PROD-001` 改为 `in_progress`。
4. 从项目、文字型 PDF 导入、分块索引、检索、回答与引用快照开始实现，不得跨项目检索。
5. 验证资料不足的拒答、项目隔离和浏览器端到端路径后，才将 `PROD-001` 标记为 `verified`。

## 已知限制

- 本机没有 JDK 21 与 `psql`；直接 Maven 测试在 Java 25 上使用 `--release 21` 已通过，但正式 Java 21 脚本验证仍待具有 JDK 21 的环境。
- 本机无外部 PostgreSQL 测试库；嵌入式 PostgreSQL 已完成 Schema 的真实验证，Supabase 联调尚未执行。
- 产品功能 `PROD-001`、`PROD-002`、`PROD-003` 均尚未开始。
