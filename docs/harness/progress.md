# StudyPilot 进度记录

## 当前状态

- 最后更新：2026-09-08 23:49 +08:00
- 最新完成任务：`PROD-002` 真题分析与模拟题生成
- 已验证任务：`HARNESS-001`、`ARCH-001` 至 `ARCH-004`、`DB-001`、`INT-001`、`PROD-001`、`PROD-002`
- 当前可执行任务：`PROD-003` 在线作答与智能测评

## 已完成产品能力

- 项目、文字型 PDF 上传、页级解析、分块、索引、删除和项目隔离。
- 溯源问答：检索白名单、引用快照、资料不足拒答与真实 C++／本地模型端到端验证。
- 真题分析：文本题目划分、知识点归一和按题目计数的考频统计。
- 模拟组卷：当前项目知识资料检索、模型 JSON 解析、固定 2 道单选和 2 道简答校验、PostgreSQL 保存与预览页面。
- 用户上传资料默认写入 `D:\大四课程设计\StudyPilot-runtime\uploads`；可查看的测试证据和导出物默认写入 `D:\大四课程设计\StudyPilot-output`。

## PROD-002 验证结论

- 算法与服务测试验证真题编号划分、知识点归一、考频统计、模型 JSON 解析、题型/答案/来源/重复度和固定题数校验。
- `PostgreSqlSchemaIntegrationTest` 在真实临时 PostgreSQL 中验证模拟卷、JSONB 选项、校验记录及跨项目读取拒绝。
- `ExamControllerTest` 覆盖分析、统计、生成、读取四项 REST 契约。
- `MockExamWorkflowEndToEndTest` 使用临时 PostgreSQL、Spring MVC 和受控 Gateway 完成完整 HTTP 工作流；外部 C++／模型实际连通性继续由 `GroundedQaEndToEndTest` 验证。
- 前端已新增 `/projects/:projectId/exams` 页面；静态检查、单测和 Vite 生产构建通过。

## 当前边界与下一步

`PROD-003` 需要实现一次独立作答记录、单选规则判分、简答题辅助评分、考试报告、错题与掌握度计算。不得复用课程版的全局课程状态；作答、答案和报告必须绑定项目、试卷和独立 attempt。

## PROD-003 完成

- 在线作答、客观题规则判分、简答题模型辅助评分、评分依据、错题、掌握度和复习建议已实现。
- 同一模拟卷每次点击开始都会创建独立 attempt；提交必须覆盖全部题目，已提交 attempt 不可重复提交。
- 当前唯一任务为 `VERIFY-001`：全系统构建、隔离、异常、浏览器与模型质量验证。
