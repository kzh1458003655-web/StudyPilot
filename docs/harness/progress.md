# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-17 18:43 +08:00
- 已验证任务：`HARNESS-001`、`ARCH-001` 至 `ARCH-004`、`DB-001`、`INT-001`、`PROD-001` 至 `PROD-003`、`RELEASE-001`、`UX-001`。
- 交付状态：前端集中式模拟服务已完成，可在不启动后端、数据库和模型时用于样式调试。

## 已完成产品能力

- 以备考项目作为资料、问答、真题、试卷、作答和诊断的隔离边界。
- 文字型 PDF 导入、页级解析、C++ 索引、溯源问答、引用快照和资料不足拒答。
- 文本真题分析、知识点归一与统计，基于项目知识资料生成 2 道单选题和 2 道简答题的模拟卷。
- 多次独立作答、单选规则评分、受校验的简答题 JSON 评分、错题、掌握度和复习建议。
- 基于 MSW 的前端模拟模式保持真实 `/api/v1` 请求结构，按项目提供资料、问答、考频、试卷、测评及异常边界数据，并通过浏览器本地存储保留调试操作。
- 用户资料默认写入 `D:\大四课程设计\StudyPilot-runtime\uploads`，运行日志、测试证据和进程记录默认写入 `D:\大四课程设计\StudyPilot-output`。

## 本轮验证结论

2026-09-09 已执行 `scripts\verify-target.ps1`：

- 后端 Maven 测试 41 项零失败，2 项明确依赖外部 C++／模型服务的 E2E 测试未在本轮启用；
- 前端 `pnpm run check` 通过，包含 ESLint、Prettier、TypeScript、Vitest 与生产构建；
- 模拟卷 HTTP 工作流通过，使用临时 PostgreSQL 和受控 Gateway 完成项目、PDF、组卷、作答和评分；
- 真实 `GroundedQaEndToEndTest` 通过：Qwen3-4B Q4、C++ 检索服务、Spring Boot 与临时 PostgreSQL 完成 PDF 导入、检索和带引用回答；
- Playwright Chromium 浏览器测试通过：创建项目后进入该项目的问答空间，HTML/JSON 报告写入 D 盘；
- Harness 检查通过；详细日志和验证摘要保留在 D 盘输出目录。

## 当前边界与下一步

样式调试可进入 `frontend` 后运行 `corepack pnpm run dev:mock`，无需启动 Java、PostgreSQL、C++ 检索或本地模型；普通 `dev` 模式仍使用真实后端。模拟场景与重置方法见 `frontend/src/mocks/README.md`。

本轮 `pnpm run check` 的测试、类型检查和构建步骤通过，但仓库已有 63 个未按当前 Prettier 配置格式化的基线文件使 `format:check` 失败；本任务没有批量改写这些无关文件，新增和修改的源文件已单独格式化。
