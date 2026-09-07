# 本地小模型量化 Benchmark

比较模型：Qwen3.5-4B、Qwen3-4B、Phi-4-mini-instruct，均使用Q4_K_M GGUF版本。

- 60道计算机专业选择题：自动计算正确率和严格JSON格式率。
- 6道资料依据题：自动检查答案、页码和资料不足时的拒答。
- 6道模拟出题题：自动检查JSON、题数、字段、四个选项和答案格式。
- 8道答题评测题：与预先给定的标准分、评分点比较，计算平均绝对误差。
- 3次固定长回答：测量首Token时间、生成速度和GPU显存。

运行示例：

```powershell
& 'D:\Python environment\python.exe' .\benchmark.py --model D:\StudyPilot-runtime\Qwen3-4B-Q4_K_M.gguf --name Qwen3-4B
& 'D:\Python environment\python.exe' .\summarize.py
```

综合分权重：选择题45%，资料依据15%，出题结构15%，评分准确性15%，性能8%，成功率2%。
