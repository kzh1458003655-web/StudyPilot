# 参与开发

请先在 Issues 中说明准备修改的功能或问题，确认模块边界后再开始编码。

## Issue 内容

- 写清楚问题出现在哪个课程、页面或接口。
- 提供可重复的操作步骤和预期结果。
- 功能建议需要说明使用场景、输入和输出。
- 不要上传 `config.local.json`、数据库文件、本地日志、模型文件或包含个人信息的课程材料。

## 分支与提交

1. 先同步主分支：`git switch main`、`git pull`。
2. 从 `main` 创建功能分支，例如 `git switch -c feature/exam-frequency`。
3. 建议王梦阳使用 `feature/wang-mengyang-*`，王云舟使用 `feature/wang-yunzhou-*`，考振豪使用 `feature/kao-zhenhao-*`。
4. 完成后依次运行 `.\scripts\build.ps1` 和 `.\scripts\test.ps1 -SkipBrowser`。
5. 推送分支并发起 Pull Request；不要直接向 `main` 推送功能代码。

提交信息使用简短的中文动词开头，例如 `新增模拟题生成接口`、`修复课程资料串读`。

## 合并要求

- Pull Request 必须关联对应 Issue，并写明修改范围、测试方法和截图。
- 至少由另一名组员查看代码后再合并。
- GitHub Actions 中 Java 测试和 C++ 编译必须通过。
- 同时修改接口与页面时，在同一个 Pull Request 中提交，避免接口版本不一致。

## 本地文件

以下内容只保存在个人电脑，不得加入 Git：`.local/`、`config.local.json`、`data/`、`logs/`、`*.gguf` 和课程提交材料。

<!-- 原有原则继续保留 -->
<!--
2. 一次提交只处理一个相对完整的问题。
3. 修改前后端接口时，同步更新接口调用和必要注释。
4. 提交 Pull Request 时关联对应 Issue，并填写验证方法。
-->

## 基础检查

- Java 后端可以完成构建。
- C++ 服务可以编译并通过健康检查。
- 前端页面无未捕获异常。
- 课程数据不会跨课程读取。
- 新增模型功能应保留可复现的输入、参数和测试结果。
