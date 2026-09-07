# 知序 StudyPilot

本地运行的课程资料问答与复习规划系统。当前实现面向单用户、多课程学习场景，每门课程拥有独立的资料、对话和计划，包含：

- 文字型 PDF 上传、页级检索、带资料名称和页码的问答；
- 基于复习目标、薄弱点、日期和每日时长的计划生成；
- 任务打卡、计划调整、已完成任务保留与版本记录；
- Qwen3-4B Q4_K_M 本地模型、C++ 检索服务、Java 业务服务、MySQL 数据库和 Vue 前端；
- 不上传资料时仍可进行通用课程问答；启用“参考课程资料”后，答案附带本课程的资料来源和页码。

## 启动

首次使用时复制 `config.example.json` 为 `config.local.json`，再填写本机运行目录、数据库密码和随机模型密钥。`config.local.json` 已被 Git 忽略，不要提交到仓库。

配置完成后双击 `启动知序.cmd`，浏览器访问 `http://127.0.0.1:18080`。停止时双击 `停止知序.cmd`。

模型、llama.cpp、MySQL 和 Maven 运行文件在本机 `D:\StudyPilot-runtime`；源代码、数据、日志、构建产物和文档在本项目目录。首次运行前需要确认 `D:\StudyPilot-runtime` 存在，且 `config.local.json` 中的 `RUNTIME` 指向该目录。

## 构建与测试

执行 `build.ps1` 会编译 Java 与 C++，并运行 Java 时间约束测试。需要先停止正在运行的程序，再执行构建并启动。

真实模型集成测试：

```powershell
& 'C:\Users\14580\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tests\integration.py
& 'C:\Users\14580\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tests\edge_cases.py
& 'C:\Users\14580\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tests\course_integration.py
```

课程隔离验收覆盖同一文件跨课程上传、跨课程读取/删除/会话/提问拦截、无资料通用问答和来源归属；记录位于 `tests/course-integration-results.json`。浏览器验收和页面截图位于 `tests/ui-evidence/`。

## 模型选型

在 RTX 4060 Laptop 8GB、llama.cpp、Q4_K_M 量化、4096 上下文、单并发、关闭思考模式的统一条件下，对 Qwen3-4B 和 Qwen3.5-4B 各执行 15 个中文测试用例。Qwen3-4B 得分为 27/30，高于 Qwen3.5-4B 的 25/30；平均首字时间分别为 0.080 秒和 0.204 秒，生成速度分别为 58.47 和 52.66 token/s。项目选择 Qwen3-4B，是因为其资料页码遵循和结构化计划输出更稳定，并不表示其在所有问题上都更强。完整原始记录、统计表和截图保存在 `tests/model-evaluation/`。

## 分工

| 模块 | 负责内容 |
|---|---|
| 课程资料智能问答 | 聊天页面、SSE 交互、资料检索、会话与引用记录 |
| 考频统计与模拟出题 | 试题提取、知识点统计、模拟题生成与展示 |
| 在线答题与智能评测 | 答题页面、客观题判分、简答题评分与反馈 |

## 参与开发

组员先通过 GitHub Issues 提交问题或功能方案，再从 `main` 创建功能分支并提交 Pull Request。仓库已经提供缺陷报告和功能建议模板，具体约定见 `CONTRIBUTING.md`。

## 学习资料与注释

- `学习路线与源码导读.md`：适合在编辑器中配合源码阅读，说明请求链、关键函数和修改顺序；
- `deliverables/知序系统开发学习手册.docx`：适合打印或答辩前复习，涵盖架构、RAG、BM25、Agent、3NF、事务、模型部署和测试；
- 源码中已在 `frontend/app.js`、`ApiController.java`、`PlanService.java`、`AiClient.java`、`LocalOriginFilter.java` 与 `ai/src/main.cpp` 补充关键流程的中文注释。

## 已知范围

仅支持可提取文字的 PDF，不处理扫描件；模型服务只面向本机单用户演示，默认一个生成请求执行、其余排队；系统不包含登录、测验或自动提醒。
