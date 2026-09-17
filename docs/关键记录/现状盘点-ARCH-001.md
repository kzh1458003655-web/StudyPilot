# ARCH-001 现状盘点与旧版基线

## 归档范围

- 归档分支：`archive/legacy-course-prototype`
- 基线提交：`8fce55f`（课程版资料问答、计划和演示题库评测闭环）
- 归档 worktree：位于主仓库目录之外的 `StudyPilot-legacy`；仅用于回看、运行旧行为基线和提取候选，不在其中继续开发。
- 当前重构分支：`refactor/architecture-baseline`；本任务不移动、删除或改写旧业务实现。

## 现有实现清单

| 技术层 | 旧版实现 | 迁移结论 |
| --- | --- | --- |
| 前端 | 单页 `index.html`，全局 Vue 构建，状态和请求集中在 `app.js` | 只保留交互行为与浏览器验收；按目标架构拆为 `app / modules / shared / types` |
| Java | `cn.studypilot` 根包下的 Controller、课程、计划、评测和 AI 客户端类 | 不迁移包结构；分别重建 document、qa、exam、assessment 与 Gateway 边界 |
| 数据库 | 本地 MySQL，初始化 SQL 与运行时 `CREATE/ALTER` 混用，业务边界为课程 | 不迁移 DDL；按 PostgreSQL 的 `studypilot` Schema 和项目边界重新设计 |
| C++ | 一个服务同时承担页级索引、检索、模型代理、队列、取消与指标 | 保留协议与压力行为，后续固定 RetrievalGateway 和 ModelGateway 契约 |
| 测试与脚本 | JUnit、Python 集成、Playwright 浏览器验收，以及本地一键启动脚本 | 复用可验证场景；新架构用独立的无外部服务单元测试和分层集成测试 |

## 可复现行为基线

在归档 worktree 的基线提交上执行：

```powershell
& 'D:\StudyPilot-runtime\maven\apache-maven-3.9.9\bin\mvn.cmd' `
  '-Dmaven.repo.local=D:\StudyPilot-runtime\.m2\repository' `
  -f backend\pom.xml test
```

结果：16 个 Java 测试通过，覆盖课程隔离、计划校验和评测服务端评分。已有 `tests/browser-course-test.cjs` 的最近一次证据覆盖资料隔离、模拟测验、评测结果和窄屏页面。

归档版本的 `scripts/build.ps1` 在 Windows PowerShell 5.1 下因 UTF-8 无 BOM 的中文字符串解析失败，未将该脚本当作通过的基线证据；该兼容性风险应在目标构建脚本任务中单独处理。Harness 自身的初始化和检查脚本已在当前分支修复并验证。

## 迁移边界

- 不把旧 MySQL 方言、`course_id` 边界、运行时建表或本机绝对路径迁入目标工程。
- 不复制旧 Controller 或单文件 Vue 页面；先在目标模块中建立 DTO、Gateway 和测试边界。
- 考频与评测的现有演示题库仅作为行为样例，不等同于目标版本的真题分析和资料驱动组卷。
