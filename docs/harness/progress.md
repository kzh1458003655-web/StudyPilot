# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-08 18:35 +08:00
- 最新完成分支：`test/architecture-integration`
- 已验证任务：`HARNESS-001`、`ARCH-001`、`ARCH-002`、`ARCH-003`、`ARCH-004`、`DB-001`、`INT-001`
- 当前可执行任务：`PROD-001`，实现资料知识库与溯源问答

## 已完成的架构基础

- 旧课程版固定在 `archive/legacy-course-prototype` 分支和仓库外 worktree；归档版本 16 项 Java 测试通过。
- Java 后端已建立 Java 21、Spring Boot 3.5、模块化单体、统一错误响应、版本化健康检查和 C++ Gateway 适配器。
- Vue 3、TypeScript、Vite 的前端目标骨架已经建立，DTO、运行时 Schema、mapper 和内部模型具有单向边界。
- 实际 C++ 服务完成过健康、检索和本地 Qwen3.5-4B 最小调用验证。
- PostgreSQL Schema 已建立：24 张表覆盖资料、溯源问答、真题组卷和测评诊断；JDBC 使用环境变量和 `NamedParameterJdbcTemplate`。

## DB-001 验证结论

- `PostgreSqlSchemaIntegrationTest` 在 Windows 上临时启动真实 PostgreSQL 14.22。
- 测试连续执行两遍七份 DDL，确认初始化可重复，验证 24 张表、`JSONB`、资料分块写入和项目级级联删除。
- 无数据库变量时后端不会创建数据源；本机默认 Java 当前为 25，Maven 仍以 `--release 21` 通过编译和测试，但 `backend/scripts/build.ps1` 正确拒绝非 21 版本。
- 本机没有 `psql` 客户端，因此 `verify-schema.ps1` 的外部测试库路径尚未执行；该路径不影响已完成的隔离 PostgreSQL 验证。

## INT-001 验证结论

- Java 8080 通过 C++ 18081 获取模型与索引状态；Vite 5173 经 `/api` 代理得到相同的统一响应。
- 后端完整测试 12 项、前端完整检查均通过；C++ 不可用时 API 的 503 和稳定错误码已有自动化证据。

## 当前边界与风险

- 三个产品模块尚未实现，当前不能称为可交付的完整学习系统。
- 前端和 C++ 的一部分运行代码仍来自课程版原型；基础契约已验证，后续产品开发必须继续遵守既定的模块边界。
- 真实 Supabase 测试库尚未接入。不可把任何个人或生产连接信息写入仓库、终端记录或测试输出。
- 正式运行仍要求 JDK 21；本机系统 Java 被切到 25，需要在成员机器或 CI 中安装/选择 JDK 21 后执行 `backend/scripts/build.ps1`。

## 下一步

1. 从 `test/architecture-integration` 创建 `feat/grounded-qa` 分支，并将 `PROD-001` 置为 `in_progress`。
2. 实现项目、文字型 PDF、分块、检索、带引用问答与无依据拒答闭环。
3. 将端到端结果、项目隔离与错误场景写入 `verification.md`。

## 本阶段提交

- `refactor(architecture): 归档课程版并建立行为基线`
- `refactor(backend): 建立 Java 目标骨架和公共 Gateway 契约`
- `refactor(frontend): 建立 Vue 目标骨架与数据边界`
- `feat(ai): 接入 C++ 网关适配器`
- `refactor(database): 建立 PostgreSQL Schema 和访问边界`
