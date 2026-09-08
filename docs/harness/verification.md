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

## ARCH-001 验证记录

## PROD-001 验证记录

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| 后端完整测试 | `backend\mvnw.cmd test` | 27 项通过，覆盖资料解析、状态、项目隔离、引用快照、资料删除和数据库持久化。 |
| 前端质量入口 | `frontend\pnpm run check` | 通过：ESLint、Prettier、vue-tsc、Vitest 和生产构建。 |
| 无数据库启动 | `spring-boot:run --server.port=18083` | 服务启动；`/health` 返回 UP，依赖检查返回真实 C++ 与模型状态。 |
| 真实产品路径 | `-Dstudypilot.e2e=true -Dtest=GroundedQaEndToEndTest test` | 通过：临时 PostgreSQL、Spring Boot、真实 C++ 索引和本地模型完成项目、PDF、问答引用和清理。 |

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| Harness 脚本兼容性 | Windows PowerShell 5.1 执行 `harness-check.ps1`、`harness-init.ps1` | 通过；修复 UTF-8 BOM 和 `ConvertFrom-Json -Depth` 兼容问题 |
| 归档分支 | `git branch --show-current` 与 `git worktree list` | `archive/legacy-course-prototype` 指向 `8fce55f`，worktree 位于主仓库之外 |
| Java 行为基线 | 归档 worktree 中执行 Maven 测试 | 16 项通过 |
| 文档与复用记录 | `现状盘点-ARCH-001.md`、重构复用清单、进度与交接 | 已记录边界、候选、风险和下一任务 |

## ARCH-002 验证记录

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| Maven Wrapper | Java 21 下在 `backend` 执行 `mvnw.cmd clean test` | 2 项测试通过，无需 PostgreSQL、C++ 服务或本地模型 |
| Windows 构建脚本 | `powershell -ExecutionPolicy Bypass -File .\\backend\\scripts\\build.ps1` | 通过；脚本确认 Java 21 并原样返回 Maven 退出码 |
| HTTP 公共契约 | `HealthControllerTest`、`ErrorContractTest` | 验证 `/api/v1/health`、请求 ID 和 404 错误响应 |
| 依赖与配置 | 审核 `backend/pom.xml`、`application.yml` | 仅 PostgreSQL 驱动和环境变量占位；无 MySQL、H2、JPA、MyBatis、Flyway 或密钥 |

## ARCH-003 验证记录

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| 前端质量入口 | `pnpm run check` | 通过：ESLint、Prettier、vue-tsc、Vitest 和 Vite 生产构建均完成 |
| 数据边界样例 | `architecture-sample` 模块 | 外部 DTO 经 Zod Schema 与 mapper 转为内部领域模型 |
| 模块边界 | `app`、`modules`、`shared`、`types` 目录与模块 `index.ts` | 已建立；未迁入旧全局 Vue 页面和脚本 |

## ARCH-004 验证记录

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| Java Gateway | `backend/mvnw.cmd test` | 3 项通过，含本地 HTTP Server 的 C++ 检索协议映射测试 |
| C++ 服务健康 | 实际运行 `study-ai.exe` 后调用 `GET /health` | 返回 `model_ready=true`、队列与索引指标 |
| 检索服务 | 实际调用 `POST /retrieve` | 按协议返回 `hits` 数组 |
| 本地模型转发 | 实际调用 `POST /completion` | C++ 成功转发到本地 Qwen3.5-4B，最小提示返回 `OK` |

## DB-001 验证记录

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| DDL 的真实 PostgreSQL 解释 | `backend\\mvnw.cmd -Dtest=PostgreSqlSchemaIntegrationTest test` | 通过；临时启动并关闭 Embedded PostgreSQL 14.22 |
| 可重复初始化 | 上述测试连续执行 `00`、`10`、`20`、`30`、`40`、`50`、`90` 七份 SQL | 通过；第二遍 DDL 不报错 |
| 结构与外键行为 | 上述测试查询 Schema、写入项目/资料/页/分块，并删除项目 | 通过；24 张表、JSONB 字段、级联删除均符合设计 |
| JDBC 边界 | `PostgreSqlJdbcConfigTest` | 通过；无变量时无数据源，完整变量时创建 Hikari 与 `NamedParameterJdbcTemplate` |
| 静态约束 | `SchemaConventionTest` | 通过；文件使用目标 Schema，未含旧 MySQL 关键字 |
| 外部测试库脚本 | `backend/scripts/verify-schema.ps1` | 已检查其缺失 URL 时的保护性失败；本机没有 `psql` 和受控测试库，未将其冒充为实连验证 |

## INT-001 验证记录

| 检查 | 命令或证据 | 结果 |
| --- | --- | --- |
| 后端与数据库 | `backend\\mvnw.cmd test` | 12 项通过，包含真实隔离 PostgreSQL、Java-C++ 协议与 503 不可用契约 |
| 前端质量入口 | `frontend\\pnpm run check` | 通过：ESLint、Prettier、vue-tsc、Vitest 与生产构建 |
| Java 到 C++ | 实际启动 Java 8080 后调用 `/api/v1/health/dependencies` | 200；响应包含 C++ 模型就绪、47 个分块和队列指标 |
| 前端代理链路 | 实际启动 Vite 后调用 `http://localhost:5173/api/v1/health/dependencies` | 200；请求经 `/api` 代理进入 Java，没有浏览器直连 C++ 端口 |
| 外部服务不可用 | `DependencyHealthControllerTest` | 通过；AI Gateway 异常转换为 `EXTERNAL_SERVICE_UNAVAILABLE` 与 HTTP 503 |
