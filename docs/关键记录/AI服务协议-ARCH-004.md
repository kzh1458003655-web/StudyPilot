# AI 服务协议（ARCH-004）

## 目标

Java 业务模块只能通过 `ModelGateway` 与 `RetrievalGateway` 调用 AI 服务。C++ 服务负责检索索引、推理队列、模型进程代理、取消和运行指标；它不拥有项目、资料元数据、会话、试卷或测评业务数据。

## C++ HTTP 协议

| 能力 | 方法与路径 | 关键输入 | 成功响应 | 失败语义 |
| --- | --- | --- | --- | --- |
| 服务状态 | `GET /health` | 无 | `model_ready`、`chunks`、`pending`、`completed` | 模型未就绪时 `model_ready=false`，服务本身仍可返回 200 |
| 建索引 | `POST /documents` | 文档 ID、名称、页码、文本块 | `indexed=true` | 非法 ID 或请求体为 400 |
| 删索引 | `DELETE /documents/{id}` | 文档 ID | `deleted=true` | 删除不存在索引保持幂等 |
| 检索 | `POST /retrieve` | `query`、已按项目过滤的 `document_ids`、`limit` | 原文 `hits` | 无命中返回空数组，不生成回答 |
| 非流式推理 | `POST /completion` | OpenAI Chat 请求体 | 模型 JSON 响应 | 队列满 429；模型不可用或超时 503 |
| 流式推理 | `POST /stream` | OpenAI Chat 请求体 | SSE 文本流 | 客户端断开即停止转发；模型不可用 503 |

## Java 映射规则

- 429 映射为 `EXTERNAL_SERVICE_UNAVAILABLE` 与 HTTP 503，对用户提示“服务繁忙，可重试”；
- C++ 5xx、连接失败或超时映射为 `EXTERNAL_SERVICE_UNAVAILABLE`；
- C++ 返回无法解析或不满足 Gateway DTO 的数据映射为 `EXTERNAL_SERVICE_INVALID_RESPONSE` 与 HTTP 502；
- 业务模块在调用检索前确定 `projectId` 和允许的 `documentIds`；C++ 不接受项目数据库查询职责；
- 模型调用的提示词、任务参数、结果校验和模型运行记录分别属于业务工作流与 Java `ModelRunService`，不得下沉到 C++ 路由。

## 取消与可观测性

- Java 为每个请求生成请求 ID，并传递 `X-Request-Id`；
- 客户端取消 SSE 时，Java 关闭到 C++ 的下游连接；C++ 关闭到本地模型的转发；
- C++ 健康接口中的 `pending`、`completed` 和 `model_ready` 可用于运行面板，但不作为业务数据库事实；
- GPU 显存、首 token 时间和 token/s 由 C++ 进程采集后以独立指标接口或日志导出，不能写入前端业务状态。

## 验收边界

本阶段固定协议和错误语义，不迁移旧 C++ 的课程业务接口，也不宣称已完成 Java HTTP Adapter、真实 C++ 联调或模型性能测试；这些属于 `DB-001` 与 `INT-001` 的验证范围。
