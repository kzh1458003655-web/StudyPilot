package cn.studypilot.exam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.studypilot.common.exception.BusinessException;
import cn.studypilot.common.exception.ErrorCode;
import cn.studypilot.exam.model.GeneratedExamItem;
import java.util.List;
import org.springframework.stereotype.Component;

/** Parses only a JSON array; explanatory prose from a model is treated as invalid output. */
@Component
public class GeneratedExamItemParser {
  private final ObjectMapper json;
  public GeneratedExamItemParser(ObjectMapper json) { this.json = json; }
  public List<GeneratedExamItem> parse(String content) {
    try { return json.readValue(content, new TypeReference<List<GeneratedExamItem>>() {}); }
    catch (Exception error) { throw new BusinessException(ErrorCode.MODEL_OUTPUT_INVALID, "模型没有返回可解析的题目 JSON 数组"); }
  }
}
