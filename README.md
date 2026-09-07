# 知序 StudyPilot

本地运行的课程资料问答与复习规划系统。当前实现面向单用户、多课程学习场景，每门课程拥有独立的资料、对话和计划，包含：

- 文字型 PDF 上传、页级检索、带资料名称和页码的问答；
- 基于复习目标、薄弱点、日期和每日时长的计划生成；
- 任务打卡、计划调整、已完成任务保留与版本记录；
- Qwen3.5-4B Q4_K_M 本地模型、C++ 检索服务、Java 业务服务、MySQL 数据库和 Vue 前端；
- 不上传资料时仍可进行通用课程问答；启用“参考课程资料”后，答案附带本课程的资料来源和页码。

## 组员首次部署

系统面向 64 位 Windows 和 NVIDIA 显卡。电脑需要提前安装 Git、Visual Studio 2022 的“使用 C++ 的桌面开发”工作负载；Python 和 Node.js 只用于扩展测试，不影响基本启动。

克隆仓库后，在项目根目录运行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\setup.ps1
.\scripts\start.ps1
```

`setup.ps1` 会自动选择剩余空间最大的磁盘，在 `StudyPilot-runtime` 中准备便携版 Java 21、Maven、MySQL、llama.cpp，生成本机配置和数据库，并下载、校验 Qwen3.5-4B Q4_K_M。也可以用 `-Runtime E:\StudyPilot-runtime` 指定位置。首次部署需要下载约 4GB 文件，之后更新代码不会重复下载。模型 SHA-256 固定为 `00FE7986FF5F6B463E62455821146049DB6F9313603938A70800D1FB69EF11A4`。

浏览器访问 `http://127.0.0.1:18080`。停止服务运行 `.\scripts\stop.ps1`。本机配置、数据库、模型、日志和构建缓存均已从 Git 排除。

## 构建与测试

修改源代码后的推荐检查：

```powershell
.\scripts\build.ps1                 # 编译 Java、运行 Java 测试并编译 C++
.\scripts\test.ps1 -SkipBrowser     # 构建、启动、健康检查和真实模型接口测试
.\scripts\test.ps1                  # 本机有 Node.js 时再执行浏览器测试
```

根目录原有的 `build.ps1`、`start.ps1`、`stop.ps1` 和两个中文 `.cmd` 入口仍然保留。开发时优先使用 `scripts` 中的统一入口。

真实模型集成测试：

```powershell
& 'C:\Users\14580\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tests\integration.py
& 'C:\Users\14580\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tests\edge_cases.py
& 'C:\Users\14580\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' tests\course_integration.py
```

课程隔离验收覆盖同一文件跨课程上传、跨课程读取/删除/会话/提问拦截、无资料通用问答和来源归属；记录位于 `tests/course-integration-results.json`。浏览器验收和页面截图位于 `tests/ui-evidence/`。

## 模型选型

在 RTX 4060 Laptop 8GB、llama.cpp、Q4_K_M 量化、4096 上下文、单并发、关闭思考模式的统一条件下，对 Qwen3.5-4B、Qwen3-4B、Phi-4-mini-instruct执行60道选择题、6道资料依据题、6道出题结构题、8道答题评测题和3次性能请求。Qwen3.5-4B综合分为78.62，略高于Qwen3-4B的78.37；Phi-4-mini-instruct虽具有最快首Token时间和生成速度，但中文专业任务得分明显偏低。项目选择Qwen3.5-4B作为默认模型，保留Qwen3-4B作为低延迟备选。完整脚本、原始响应、汇总CSV和截图在 `tests/model-benchmark/`。

## 分工

| 模块 | 负责内容 |
|---|---|
| 课程资料智能问答 | 聊天页面、SSE 交互、资料检索、会话与引用记录 |
| 考频统计与模拟出题 | 试题提取、知识点统计、模拟题生成与展示 |
| 在线答题与智能评测 | 答题页面、客观题判分、简答题评分与反馈 |

## 参与开发

组员先通过 GitHub Issues 提交问题或功能方案，再从 `main` 创建个人功能分支并提交 Pull Request。仓库提供缺陷报告、功能建议模板和 Windows CI；每次推送会自动验证 Java 测试和 C++ 编译。具体约定见 `CONTRIBUTING.md`。

## 学习资料与注释

- `学习路线与源码导读.md`：适合在编辑器中配合源码阅读，说明请求链、关键函数和修改顺序；
- `deliverables/知序系统开发学习手册.docx`：适合打印或答辩前复习，涵盖架构、RAG、BM25、Agent、3NF、事务、模型部署和测试；
- 源码中已在 `frontend/app.js`、`ApiController.java`、`PlanService.java`、`AiClient.java`、`LocalOriginFilter.java` 与 `ai/src/main.cpp` 补充关键流程的中文注释。

## 已知范围

仅支持可提取文字的 PDF，不处理扫描件；模型服务只面向本机单用户演示，默认一个生成请求执行、其余排队；系统不包含登录、测验或自动提醒。
