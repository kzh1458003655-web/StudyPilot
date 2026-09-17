# Session Handoff

## 当前目标

- 当前分支：`feat/frequency-analysis-visuals`。
- `UX-003` 已完成考频分析演示实现和生产构建，状态为 `done`；等待用户在 Mock 模式确认视觉效果后再决定是否标记 `verified`。
- `UX-002` 与依赖的 `UX-001` 保持验证状态。

## 已验证事实

- Java 后端按模块化单体组织；数据库仅在三个 `STUDYPILOT_DB_*` 环境变量均存在时连接 PostgreSQL。
- 资料、问答、考频、练习和作答均以 `projectId` 为边界；模拟卷固定生成 2 道单选和 2 道简答。
- 当前三个用户主入口是知识问答、智能组卷和考频分析；在线作答与测评从智能组卷进入。
- 没有课程资料或没有检索命中时，知识问答仍调用本地模型回答，但不生成资料引用。
- 历年试卷在考频页面直接选择，不需要填写资料编号；当前使用确定性词典归一知识点。
- 当前启动脚本加载 Qwen3.5-4B Q4_K_M；三模型原始测试数据保存在 `tests/model-benchmark/`。
- `scripts\verify-target.ps1` 已在 Windows PowerShell 验证通过，输出摘要和日志到 `D:\大四课程设计\StudyPilot-output`。它覆盖后端 41 项测试、前端检查、Harness 和模拟卷 HTTP 工作流。
- `start-target.ps1 -StartAiService` 需要操作者先启动 llama.cpp 18082；脚本启动 C++ 18081、Java 8080 和 Vite 5173。完整步骤见 `docs/部署与运行指南.md`。
- 2026-09-09 已用 Qwen3-4B Q4 复跑 `GroundedQaEndToEndTest`；真实 C++／本地模型资料问答链路通过。本机当前未保持模型进程运行。
- `frontend` 运行 `corepack pnpm run dev:mock` 可只启动前端；MSW 接管现有 `/api/v1` 请求，8 个模拟项目及调试操作保存在浏览器 `localStorage`，不需要数据库或模型。
- 普通 `corepack pnpm run dev` 未启用模拟服务，仍通过 Vite 代理连接真实 Java 后端。
- 2026-09-17 前端全量 13 项 Vitest、类型检查、ESLint、生产构建和 Playwright Chrome 通道测试通过；真实浏览器以 Mock 模式验证 1440×900、1024×768、390×844 无横向溢出。
- 课程模块已移至顶部 Tabs；归档课程仅在右侧抽屉中恢复，归档直链会重定向到 `/projects?archive=1`，恢复后不自动进入课程。
- 全量 `format:check` 仍受仓库原有 44 个格式化基线文件影响，本轮文件均已定向格式化。
- 考频页使用三套数据库系统原理试卷的固定展示数据：300 分、8 类考点、前三类合计 71.4%；点击重新分析后显示约八秒的单一节点图，再恢复四项结果图表。
- `frontend` 的定向 ESLint 与 `corepack pnpm run build` 已通过；按用户要求没有启动浏览器，也没有运行 E2E 或单元测试。

## 下一会话启动顺序

1. 运行 `./scripts/harness-init.ps1`，再阅读 `feature_list.json`、本文件和 `docs/产品相关/系统验证记录.md`。
2. 检查 Git 状态，不覆盖尚未提交的脚本、部署文档和测评页面测试。
3. 阅读 [最终交付清单](../交付清单.md) 与验证记录，按其命令复现目标功能。
4. 用户确认 `UX-003` 视觉效果后，将任务状态更新为 `verified`；若需调整，仅修改考频模块和对应 Mock 数据。
