# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-17 +08:00
- 已验证任务：`HARNESS-001`、`ARCH-001` 至 `ARCH-004`、`DB-001`、`INT-001`、`PROD-001` 至 `PROD-003`。
- 交付状态：`RELEASE-001` 已完成，所有 Harness 任务均为 `verified`。

## 已完成产品能力

- 以课程项目作为资料、问答、考频、练习和作答记录的隔离边界，支持课程归档和恢复。
- 文字型 PDF 导入、页级解析、C++ 索引、RAG 引用；没有资料或未命中时使用本地模型通用知识回答。
- 独立考频分析页面自动列出历年试卷，完成题目切分、知识点词典归一与题目频次统计。
- 智能组卷支持无指定参考内容直接生成，保存历史练习，并在页面切换后保持生成任务。
- 在线作答支持单选规则评分、受校验的简答题 JSON 评分、删除练习和多次独立作答。
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

部署入口已切换为 `start-target.ps1`、`stop-target.ps1` 和 `verify-target.ps1`，不再将旧 MySQL 课程脚本作为目标架构的启动方式。真实本地模型实连、浏览器级 Playwright 场景和最终文档一致性审计均已通过；交付入口见 [最终交付清单](../交付清单.md)。

2026-09-17 已按当前页面和代码重新整理产品架构、需求、模块实现说明和团队分工。三个成员模块确定为课程资料与知识问答、智能组卷与在线测评、历年试卷考频分析；模型测试采用一人复核一款模型的方式。
