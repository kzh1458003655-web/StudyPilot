# StudyPilot Harness

本目录保存 Agent 的执行状态和接力机制。它回答“现在做到哪里、下一步做什么、怎样验证”，不替代产品、架构、数据库和接口的正式文档。

Harness 采用指令、状态、验证、范围和生命周期五个部分，参考 [Learn Harness Engineering](https://github.com/walkinglabs/learn-harness-engineering) 的仓库即事实来源和跨会话接力方法。

## 快速开始

从仓库根目录运行：

```powershell
.\scripts\harness-init.ps1
```

然后依次阅读：

1. [根级 Agent 入口](../../AGENTS.md)
2. [任务清单](feature_list.json)
3. [当前进度](progress.md)
4. [会话交接](session-handoff.md)
5. 当前任务对应的正式文档

## 文件职责

| 文件 | 用途 |
| --- | --- |
| `feature_list.json` | 机器可读的任务、依赖、状态和证据来源 |
| `feature_list.schema.json` | 任务清单的数据结构约束 |
| `progress.md` | 人类可读的当前进度、风险和下一步 |
| `session-handoff.md` | 下一位 Agent 的直接接手说明 |
| `verification.md` | 验证命令、通过标准和实际证据 |
| `lifecycle.md` | 从启动到提交、交接的标准流程 |

## 文档权威关系

| 内容 | 事实来源 |
| --- | --- |
| 产品定位和需求 | [产品相关文档](../产品相关/) |
| 后端目标架构 | [后端框架设计](../../backend/后端框架设计.md) |
| 前端目标架构 | [前端框架设计](../../frontend/前端框架设计.md) |
| 跨模块架构、数据库、接口和 ADR | [关键记录](../关键记录/README.md) |
| 当前执行状态 | 本目录 |
| 历史资料 | 名称含 `-旧` 的冻结文档 |

发现冲突时，不在进度日志里直接做隐式决定。先在关键记录中新增 ADR，再同步受影响的正式文档和任务状态。

## 状态规则

- `blocked`：前置条件未满足；
- `ready`：依赖已完成，是候选下一任务；
- `in_progress`：本次会话正在执行；
- `done`：实现结束但证据尚未完整；
- `verified`：验收标准和证据全部满足。

同一时间最多一个任务为 `in_progress`。每次开始工作以 `feature_list.json` 的唯一 `ready` 任务为准，不能跳过依赖。

## 只读检查

```powershell
.\scripts\harness-check.ps1
```

检查脚本只读取仓库，不安装依赖、不修改文件、不切换分支、不创建 commit。详细检查项见 [验证说明](verification.md)。
