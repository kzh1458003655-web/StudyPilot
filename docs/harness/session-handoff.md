# Session Handoff

## 当前目标

- 当前分支：`feat/assessment-diagnosis`。
- 已完成至 `PROD-003`；唯一可执行任务是 `VERIFY-001`。
- 目标是补齐系统级验证证据，随后整理最终交付资料。

## 已验证事实

- Java 后端按模块化单体组织；数据库仅在三个 `STUDYPILOT_DB_*` 环境变量均存在时连接 PostgreSQL。
- 资料、问答、真题、试卷、作答和诊断均以 `projectId` 为边界；模拟卷固定生成 2 道单选和 2 道简答。
- `scripts\verify-target.ps1` 已在 Windows PowerShell 验证通过，输出摘要和日志到 `D:\大四课程设计\StudyPilot-output`。它覆盖后端 41 项测试、前端检查、Harness 和模拟卷 HTTP 工作流。
- `start-target.ps1 -StartAiService` 需要操作者先启动 llama.cpp 18082；脚本启动 C++ 18081、Java 8080 和 Vite 5173。完整步骤见 `docs/部署与运行指南.md`。
- `GroundedQaEndToEndTest` 是真实 C++／本地模型资料问答链路的已有证据；本机当前未保持模型进程运行。

## 下一会话启动顺序

1. 运行 `./scripts/harness-init.ps1`，再阅读 `feature_list.json`、本文件和 `docs/产品相关/系统验证记录.md`。
2. 检查 Git 状态，不覆盖尚未提交的脚本、部署文档和测评页面测试。
3. 若需要完成 `VERIFY-001`，先建立可重复的浏览器端 Playwright 场景，并在实际 llama.cpp/C++ 服务运行时复跑 `GroundedQaEndToEndTest`。
4. 将真实命令、通过结果和未通过项写入 Harness 验证记录；满足全部验收标准后再变更 `feature_list.json` 的状态。
