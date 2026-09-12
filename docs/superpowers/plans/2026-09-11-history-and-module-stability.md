# StudyPilot 历史记录与模块稳定性 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 模块切换时不丢失已完成记录，问答历史可恢复，历史模拟卷可重新作答和删除，并将“模拟考”统一改名为“智能组卷”。

**Architecture:** 数据库继续作为历史记录的唯一来源。Java 增加课程范围内的问答历史查询与模拟卷删除接口；Vue 页面在进入或切换课程时重新加载数据。推理请求由 Java/C++ 独立完成，页面卸载不主动取消；用户返回后从数据库历史看到已完成结果。

**Tech Stack:** Vue 3、TypeScript、Axios、Java 25、Spring Boot、PostgreSQL、Playwright、JUnit 5。

---

### Task 1: 问答历史查询与恢复

**Files:**
- Modify: `backend/src/main/java/cn/studypilot/qa/repository/QaRepository.java`
- Modify: `backend/src/main/java/cn/studypilot/qa/repository/JdbcQaRepository.java`
- Modify: `backend/src/main/java/cn/studypilot/qa/controller/QaController.java`
- Create: `backend/src/main/java/cn/studypilot/qa/dto/QaHistoryResponse.java`
- Modify: `frontend/src/modules/qa/api/requests.ts`
- Modify: `frontend/src/modules/qa/schemas/apiSchema.ts`
- Modify: `frontend/src/modules/qa/types/domain.ts`
- Modify: `frontend/src/modules/qa/pages/QaPage.vue`
- Create: `frontend/src/modules/qa/stores/qaTaskStore.ts`
- Test: `backend/src/test/java/cn/studypilot/qa/QaControllerTest.java`
- Test: `frontend/tests/e2e/full-browser-workflow.real-api.spec.ts`

- [ ] 增加按课程读取最近会话、消息和引用快照的仓储查询。
- [ ] 增加 `GET /api/v1/qa/history?projectId=...`，不允许跨课程读取。
- [ ] 页面进入课程和切回问答时加载历史，恢复最后一个会话编号以支持连续追问。
- [ ] 将进行中的问答 Promise 和状态放到 Pinia 课程任务存储，组件卸载不取消请求；返回页面时等待或刷新历史。
- [ ] 用真实浏览器在回答完成前切到智能组卷，确认不会被强制导航；完成后返回问答，问题和回答仍存在。

### Task 2: 历史模拟卷管理与重做

**Files:**
- Modify: `backend/src/main/java/cn/studypilot/exam/repository/MockExamRepository.java`
- Modify: `backend/src/main/java/cn/studypilot/exam/repository/JdbcMockExamRepository.java`
- Modify: `backend/src/main/java/cn/studypilot/exam/service/MockExamQueryService.java`
- Modify: `backend/src/main/java/cn/studypilot/exam/controller/ExamController.java`
- Modify: `frontend/src/modules/exam/api/requests.ts`
- Modify: `frontend/src/modules/exam/pages/ExamPage.vue`
- Create: `frontend/src/modules/exam/stores/examTaskStore.ts`
- Modify: `frontend/src/modules/assessment/pages/AssessmentPage.vue`
- Test: `frontend/tests/e2e/full-browser-workflow.real-api.spec.ts`
- Test: `backend/src/test/java/cn/studypilot/exam/MockExamRepositoryIntegrationTest.java`

- [ ] 增加课程范围内的模拟卷删除接口，依靠外键级联清理作答记录。
- [ ] 后端验证跨课程和不存在的试卷不能删除，并验证删除后 attempt、answer 随外键级联清理。
- [ ] 历史记录提供“重新作答”和“删除”两个明确操作。
- [ ] 重新进入同一试卷时创建新的 attempt，并清空页面答案和上次结果。
- [ ] 将进行中的组卷 Promise 和状态放到 Pinia 课程任务存储；组件卸载后禁止旧组件触发路由跳转，返回页面后自动等待结果并刷新历史。
- [ ] 真实浏览器在组卷完成前切走并返回，确认不会强制跳转且最终历史可见；再验证重做和删除。

### Task 3: 模块名称统一

**Files:**
- Modify: `frontend/src/app/layouts/AppShell.vue`
- Modify: `frontend/src/modules/exam/routes.ts`
- Modify: `frontend/src/modules/exam/pages/ExamPage.vue`
- Modify: `frontend/src/modules/assessment/pages/AssessmentPage.vue`
- Modify: `frontend/src/modules/qa/pages/QaPage.vue`
- Modify: `frontend/src/modules/exam/pages/FrequencyPage.vue`

- [ ] 将面向用户的“模拟考”统一替换为“智能组卷”，保留具体动作“生成模拟卷”和“重新作答”。
- [ ] 检查桌面宽度与窄屏下导航、历史操作按钮不重叠。

### Task 4: 完整回归

- [ ] 运行后端全部测试。
- [ ] 运行前端 lint、format、typecheck、unit test 和 build。
- [ ] 真实浏览器验证课程、上传、问答历史、模块切换、考频、组卷、重做、删除、作答和测评。
- [ ] 连续重复主流程，确认本地模型输出和模块切换稳定。
- [ ] 提交并推送共享分支。
