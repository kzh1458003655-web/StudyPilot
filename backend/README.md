# StudyPilot 后端骨架

这是 Java 21、Spring Boot 3.5 的模块化单体骨架。当前仅提供公共响应与错误格式、版本化健康检查、模型与检索 Gateway 契约，以及 document、qa、exam、assessment 的工作流入口；不含旧课程版 Controller、MySQL、运行时建表或完整业务功能。

## 构建

需要 Java 21。无需启动 PostgreSQL、C++ 服务或本地模型：

```powershell
.\mvnw.cmd clean test
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

数据库变量仅为后续 `DB-001` 预留：`STUDYPILOT_DB_URL`、`STUDYPILOT_DB_USERNAME`、`STUDYPILOT_DB_PASSWORD`。不得把它们写入仓库。

模块只能通过公开的 Service 或 Gateway 跨边界调用；Controller 不直连 Repository，业务模块不访问其他模块的 Repository。完整规则见 [后端框架设计](docs/后端框架设计.md)。
