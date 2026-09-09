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
    DICTIONARY.put("算法复杂度", List.of("复杂度", "time complexity", "running time", "asymptotic", "big o", "polynomial"));
    DICTIONARY.put("排序算法", List.of("sorting", "insertion sort", "merge sort", "radix sort", "heap sort", "counting sort"));
    DICTIONARY.put("查找与二分", List.of("binary search", "二分查找", "search tree", "查找"));
    DICTIONARY.put("平衡树", List.of("avl", "balanced tree", "平衡树"));
    DICTIONARY.put("图搜索", List.of("breadth-first", "breadth first", "bfs", "深度优先", "depth-first", "depth first", "dfs"));
    DICTIONARY.put("图算法", List.of("graph", "图", "shortest path", "最短路"));
    DICTIONARY.put("动态规划", List.of("dynamic programming", "动态规划", "memoization", "递推"));
    DICTIONARY.put("计算复杂性", List.of("np-complete", "np-hard", "np complete", "np hard", "complexity class"));
    DICTIONARY.put("极限", List.of("limx", "limit x", "极限"));
    DICTIONARY.put("导数", List.of("derivative", "differentiable", "differentiation", "导数", "可导"));
    DICTIONARY.put("积分", List.of("integral", "integrable", "antiderivative", "积分", "可积"));
    DICTIONARY.put("级数与收敛", List.of("series", "convergence", "converge", "diverge", "级数", "收敛"));
    DICTIONARY.put("幂级数", List.of("power series", "radius of convergence", "幂级数", "收敛半径"));
    DICTIONARY.put("连续性", List.of("continuous", "continuity", "连续函数", "连续性"));
    DICTIONARY.put("一致收敛", List.of("uniform convergence", "converges uniformly", "一致收敛"));
    DICTIONARY.put("实变函数与可积性", List.of("riemann", "step function", "real analysis", "实变", "黎曼"));
    DICTIONARY.put("序列与子序列", List.of("subsequence", "sequence {", "数列", "子列"));
    DICTIONARY.put("数学归纳法", List.of("induction", "归纳法"));
    DICTIONARY.put("进程与线程", List.of("进程", "线程", "process", "thread"));
    DICTIONARY.put("进程调度", List.of("调度", "时间片", "scheduler"));
    DICTIONARY.put("死锁", List.of("死锁", "deadlock"));
    DICTIONARY.put("内存管理", List.of("分页", "分段", "虚拟内存", "page replacement"));
    DICTIONARY.put("数据库事务", List.of("数据库", "事务", "隔离级别", "acid", "transaction isolation"));
    DICTIONARY.put("SQL 查询", List.of("select", "join", "sql", "查询"));
  }

  public List<String> extractAndNormalize(String text) {
    String lower = text.toLowerCase(Locale.ROOT);
    return DICTIONARY.entrySet().stream()
        .filter(entry -> entry.getValue().stream().anyMatch(lower::contains))
        .map(Map.Entry::getKey).toList();
  }
}
