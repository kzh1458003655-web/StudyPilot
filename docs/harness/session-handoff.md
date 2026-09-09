# Session Handoff

## 当前目标

- 当前分支：`feat/assessment-diagnosis`。
- 所有任务已验证完成，当前没有待执行的 Harness 任务。
- 后续变更应以新的任务项开始，并更新相应验证证据。

## 已验证事实

- Java 后端按模块化单体组织；数据库仅在三个 `STUDYPILOT_DB_*` 环境变量均存在时连接 PostgreSQL。
- 资料、问答、真题、试卷、作答和诊断均以 `projectId` 为边界；模拟卷固定生成 2 道单选和 2 道简答。
- `scripts\verify-target.ps1` 已在 Windows PowerShell 验证通过，输出摘要和日志到 `D:\大四课程设计\StudyPilot-output`。它覆盖后端 41 项测试、前端检查、Harness 和模拟卷 HTTP 工作流。
- `start-target.ps1 -StartAiService` 需要操作者先启动 llama.cpp 18082；脚本启动 C++ 18081、Java 8080 和 Vite 5173。完整步骤见 `docs/部署与运行指南.md`。
- 2026-09-09 已用 Qwen3-4B Q4 复跑 `GroundedQaEndToEndTest`；真实 C++／本地模型资料问答链路通过。本机当前未保持模型进程运行。

## 下一会话启动顺序

1. 运行 `./scripts/harness-init.ps1`，再阅读 `feature_list.json`、本文件和 `docs/产品相关/系统验证记录.md`。
2. 检查 Git 状态，不覆盖尚未提交的脚本、部署文档和测评页面测试。
3. 阅读 [最终交付清单](../交付清单.md) 与验证记录，按其命令复现目标功能。
4. 新增功能前先建立新的 Harness 任务、验收标准和对应测试，不修改已验证的历史证据来掩盖回归。
