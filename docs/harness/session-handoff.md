# Session Handoff

## 当前目标

- 已验证至 `PROD-002`。
- 当前分支：`feat/grounded-qa`。
- 下一唯一任务：`PROD-003`，状态为 `ready`。
- 目标：完成在线作答、客观题规则判分、简答题辅助评分、测评报告、错题和掌握度闭环。

## 已验证事实

- Java 后端为模块化单体；数据源仅在 `STUDYPILOT_DB_*` 明确配置时创建。所有业务查询需要 `projectId`。
- 资料问答与真题组卷接口已可用。资料存储默认路径是 `D:\大四课程设计\StudyPilot-runtime\uploads`，可通过 `STUDYPILOT_STORAGE_ROOT` 覆盖。
- 真题分析读取 `PAST_EXAM`，组卷读取 `TEXTBOOK`、`LECTURE`、`KNOWLEDGE`。模型题目固定为 2 道单选和 2 道简答，保存前经过程序校验。
- `MockExamWorkflowEndToEndTest` 使用受控模型、检索和索引 Gateway，验证真实 HTTP、临时 PostgreSQL 和 PDF 导入的可重复闭环；`GroundedQaEndToEndTest` 仍是实际 C++ 与本地模型链路证据。
- 前端模块路径：`document`、`qa`、`exam`。模拟考页面为 `/projects/:projectId/exams`。

## 下一会话启动顺序

1. 运行 `./scripts/harness-init.ps1`，阅读本文件、`feature_list.json` 与 `verification.md`。
2. 检查 Git 状态；不要覆盖当前未提交的 `PROD-002` 变更。
3. 从 `PROD-003` 开始，为 attempt、answer、grading、mastery/report 建立项目隔离的 Repository、Service、REST 与 Vue 模块。
4. 先写客观题确定性判分和 attempt 多次独立记录，再接入简答题模型辅助评分。
5. 完成后同步更新 Harness、接口和数据库记录，并运行后端、前端和端到端验证。

## PROD-003 已完成

测评模块实现与证据见 `docs/产品相关/在线作答与智能测评模块实现说明.md`。下一任务为 `VERIFY-001`：执行系统级构建、端到端、异常、项目隔离、浏览器和模型验证，生成最终交付资料。
