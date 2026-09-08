# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-09 00:46 +08:00
- 已验证任务：`HARNESS-001`、`ARCH-001` 至 `ARCH-004`、`DB-001`、`INT-001`、`PROD-001` 至 `PROD-003`。
- 当前任务：`VERIFY-001` 系统级验证闭环。

## 已完成产品能力

- 以备考项目作为资料、问答、真题、试卷、作答和诊断的隔离边界。
- 文字型 PDF 导入、页级解析、C++ 索引、溯源问答、引用快照和资料不足拒答。
- 文本真题分析、知识点归一与统计，基于项目知识资料生成 2 道单选题和 2 道简答题的模拟卷。
- 多次独立作答、单选规则评分、受校验的简答题 JSON 评分、错题、掌握度和复习建议。
- 用户资料默认写入 `D:\大四课程设计\StudyPilot-runtime\uploads`，运行日志、测试证据和进程记录默认写入 `D:\大四课程设计\StudyPilot-output`。

## 本轮验证结论

2026-09-09 已执行 `scripts\verify-target.ps1`：

- 后端 Maven 测试 41 项零失败，2 项明确依赖外部 C++／模型服务的 E2E 测试未在本轮启用；
- 前端 `pnpm run check` 通过，包含 ESLint、Prettier、TypeScript、Vitest 与生产构建；
- 模拟卷 HTTP 工作流通过，使用临时 PostgreSQL 和受控 Gateway 完成项目、PDF、组卷、作答和评分；
- Harness 检查通过；详细日志和验证摘要保留在 D 盘输出目录。

## 当前边界与下一步

部署入口已切换为 `start-target.ps1`、`stop-target.ps1` 和 `verify-target.ps1`，不再将旧 MySQL 课程脚本作为目标架构的启动方式。完整本地模型实连测试仍需先启动 llama.cpp 和 C++ 服务，再运行 `GroundedQaEndToEndTest`；浏览器级 Playwright 场景尚未建立，因此 `VERIFY-001` 保持 `ready`，不能提前标为已验证。
