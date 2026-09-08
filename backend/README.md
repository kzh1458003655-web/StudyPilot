# StudyPilot 后端骨架

这是 Java 21、Spring Boot 3.5 的模块化单体骨架。当前提供公共响应与错误格式、版本化健康检查、模型与检索 Gateway 契约，以及 document、qa、exam、assessment 的工作流入口。数据库使用 PostgreSQL 的 `studypilot` Schema，不含旧课程版 Controller、MySQL、运行时建表或完整业务接口。

## 构建

需要 Java 21。日常构建无需启动 PostgreSQL、C++ 服务或本地模型：

```powershell
.\mvnw.cmd clean test
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

完整测试会在测试进程内启动并停止一个隔离的 PostgreSQL 实例，真实执行七份 DDL；它不连接 Supabase，也不会读取数据库变量。首次执行会下载测试范围的 PostgreSQL 二进制依赖。

部署到本地或受控测试库时，显式设置 `STUDYPILOT_DB_URL`、`STUDYPILOT_DB_USERNAME`、`STUDYPILOT_DB_PASSWORD`。三者缺少任一个时不会创建数据源；不得把它们写入仓库。

拥有独立 PostgreSQL 测试库时，可执行 `powershell -ExecutionPolicy Bypass -File .\scripts\verify-schema.ps1` 按固定顺序验证全部 DDL。该脚本拒绝缺少连接串的执行，且不保存任何凭据。操作前请确认目标是可清理的测试库；它只做幂等建表和索引，不会删除已有表或数据。

模块只能通过公开的 Service 或 Gateway 跨边界调用；Controller 不直连 Repository，业务模块不访问其他模块的 Repository。完整规则见 [后端框架设计](docs/后端框架设计.md)，表关系和验证原理见 [数据库实现说明](docs/数据库实现说明.md)。
