package cn.studypilot.exam.algorithm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Deterministic starter dictionary; model extraction can add candidates but cannot bypass normalization. */
@Component
public class KnowledgePointNormalizer {
  private static final Map<String, List<String>> DICTIONARY = new LinkedHashMap<>();
  static {
    DICTIONARY.put("进程与线程", List.of("进程", "线程", "process", "thread"));
    DICTIONARY.put("进程调度", List.of("调度", "时间片", "scheduler"));
    DICTIONARY.put("死锁", List.of("死锁", "deadlock"));
    DICTIONARY.put("内存管理", List.of("分页", "分段", "虚拟内存", "page replacement"));
    DICTIONARY.put("数据库事务", List.of("事务", "隔离级别", "acid", "transaction"));
    DICTIONARY.put("SQL 查询", List.of("select", "join", "sql", "查询"));
  }

  public List<String> extractAndNormalize(String text) {
    String lower = text.toLowerCase(Locale.ROOT);
    return DICTIONARY.entrySet().stream()
        .filter(entry -> entry.getValue().stream().anyMatch(lower::contains))
        .map(Map.Entry::getKey).toList();
  }
}
