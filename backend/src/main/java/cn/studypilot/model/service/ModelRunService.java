package cn.studypilot.model.service;

import cn.studypilot.model.dto.ModelTaskType;
import java.time.Duration;

/** 业务工作流完成后记录运行摘要的契约，不保存完整提示词或资料全文。 */
public interface ModelRunService {
  void record(ModelTaskType taskType, String modelName, Duration elapsed, boolean succeeded, String endReason);
}
