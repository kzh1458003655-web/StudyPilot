package cn.studypilot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 考频、组卷和评测的业务边界。
 *
 * <p>分数和参考答案始终留在服务端：网页只获得题目、选项和评测结果，不能通过修改请求伪造得分。
 * 所有查询都以 courseId 为条件，使同一个用户的不同课程题库和答卷彼此隔离。</p>
 */
@Service
@DependsOnDatabaseInitialization
public class AssessmentService {
  private final JdbcTemplate db;
  private final ObjectMapper mapper;
  private final CourseService courses;

  public AssessmentService(JdbcTemplate db, ObjectMapper mapper, CourseService courses) {
    this.db = db;
    this.mapper = mapper;
    this.courses = courses;
  }

  /** 建表同时兼容已有的 H2 本地数据库；课程表由 CourseService 先完成迁移。 */
  @PostConstruct
  public void migrate() {
    db.execute("CREATE TABLE IF NOT EXISTS exam_topics("
        + "id VARCHAR(36) PRIMARY KEY,course_id VARCHAR(36) NOT NULL,name VARCHAR(120) NOT NULL,"
        + "frequency INT NOT NULL,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    createIndex("idx_exam_topics_course", "exam_topics", "course_id");
    db.execute("CREATE TABLE IF NOT EXISTS exam_questions("
        + "id VARCHAR(36) PRIMARY KEY,course_id VARCHAR(36) NOT NULL,topic_id VARCHAR(36) NOT NULL,"
        + "stem VARCHAR(2000) NOT NULL,question_type VARCHAR(16) NOT NULL,options_json MEDIUMTEXT,"
        + "answer_key VARCHAR(1000) NOT NULL,rubric VARCHAR(2000),source_type VARCHAR(24) NOT NULL,"
        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    createIndex("idx_exam_questions_course", "exam_questions", "course_id");
    db.execute("CREATE TABLE IF NOT EXISTS mock_papers("
        + "id VARCHAR(36) PRIMARY KEY,course_id VARCHAR(36) NOT NULL,title VARCHAR(160) NOT NULL,"
        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    db.execute("CREATE TABLE IF NOT EXISTS mock_paper_questions("
        + "paper_id VARCHAR(36) NOT NULL,question_id VARCHAR(36) NOT NULL,ordinal INT NOT NULL,"
        + "PRIMARY KEY(paper_id,question_id))");
    db.execute("CREATE TABLE IF NOT EXISTS exam_attempts("
        + "id VARCHAR(36) PRIMARY KEY,paper_id VARCHAR(36) NOT NULL UNIQUE,course_id VARCHAR(36) NOT NULL,"
        + "score INT NOT NULL,total_score INT NOT NULL,weak_summary VARCHAR(1000) NOT NULL,"
        + "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    createIndex("idx_exam_attempts_course", "exam_attempts", "course_id");
    db.execute("CREATE TABLE IF NOT EXISTS exam_answers("
        + "id VARCHAR(36) PRIMARY KEY,attempt_id VARCHAR(36) NOT NULL,question_id VARCHAR(36) NOT NULL,"
        + "answer_text VARCHAR(4000) NOT NULL,score INT NOT NULL,feedback VARCHAR(2000) NOT NULL)");
  }

  /** 返回当前课程的考点。frequency 是教材、历年题等资料整理后的出现次数。 */
  public List<Map<String, Object>> topics(String courseId) {
    courseId = courses.require(courseId);
    return db.queryForList("SELECT id,name,frequency FROM exam_topics WHERE course_id=? "
        + "ORDER BY frequency DESC,name", courseId);
  }

  /**
   * 填入一套可立即演示的数据库基础题库。实际使用时可由资料整理流程替换为课程专属题目；
   * 幂等设计保证重复点击不会产生相同的题目和考点。
   */
  @Transactional
  public Map<String, Object> seed(String courseId) {
    courseId = courses.require(courseId);
    Integer count = db.queryForObject("SELECT COUNT(*) FROM exam_questions WHERE course_id=?", Integer.class, courseId);
    if (count != null && count > 0) return Map.of("created", false, "topics", topics(courseId).size(), "questions", count);

    Map<String, String> topicIds = new LinkedHashMap<>();
    addTopic(courseId, topicIds, "关系模型与范式", 28);
    addTopic(courseId, topicIds, "SQL 与索引", 24);
    addTopic(courseId, topicIds, "事务与并发控制", 21);
    addTopic(courseId, topicIds, "数据完整性", 17);
    addChoice(courseId, topicIds.get("关系模型与范式"), "关系模型中，为满足第一范式，属性值应当满足哪项要求？",
        List.of("保持原子性，不再分解", "必须全部是数字", "只能建立一个索引", "不允许出现主键"), "A");
    addChoice(courseId, topicIds.get("SQL 与索引"), "分析一条 SQL 是否使用索引、扫描多少行时，优先使用哪个命令？",
        List.of("GRANT", "EXPLAIN", "COMMIT", "DROP"), "B");
    addChoice(courseId, topicIds.get("事务与并发控制"), "下列哪项最能体现事务的原子性？",
        List.of("数据可长期保存", "多个用户可同时查询", "操作要么全部完成，要么全部回滚", "字段必须唯一"), "C");
    addChoice(courseId, topicIds.get("SQL 与索引"), "关于索引的说法，哪一项正确？",
        List.of("每个字段都应建立索引", "索引只会增加写入速度", "索引不占用存储空间", "索引可提升查询速度，但会带来写入维护成本"), "D");
    addShort(courseId, topicIds.get("事务与并发控制"), "请说明为什么创建复习计划和任务时应放在同一个事务中。",
        List.of("原子性", "一致性", "全部成功", "全部回滚"));
    return Map.of("created", true, "topics", topicIds.size(), "questions", 5);
  }

  /** 依据当前课程的高频考点创建一份固定试卷，同一份试卷只允许提交一次。 */
  @Transactional
  public Map<String, Object> createPaper(String courseId, int requestedCount) {
    courseId = courses.require(courseId);
    if (requestedCount < 1 || requestedCount > 10) throw new IllegalArgumentException("题目数量应在1到10之间");
    List<Map<String, Object>> questions = db.queryForList(
        "SELECT q.id,q.stem,q.question_type,q.options_json,t.name AS topic,q.created_at "
            + "FROM exam_questions q JOIN exam_topics t ON q.topic_id=t.id "
            + "WHERE q.course_id=? ORDER BY t.frequency DESC,q.created_at,q.id LIMIT ?", courseId, requestedCount);
    if (questions.isEmpty()) throw new IllegalStateException("当前课程尚无题库，请先载入演示题库");
    String paperId = PlanService.id();
    db.update("INSERT INTO mock_papers(id,course_id,title) VALUES(?,?,?)", paperId, courseId,
        "模拟测验 · " + questions.size() + " 题");
    for (int index = 0; index < questions.size(); index++)
      db.update("INSERT INTO mock_paper_questions(paper_id,question_id,ordinal) VALUES(?,?,?)",
          paperId, questions.get(index).get("id"), index + 1);
    return paper(paperId, courseId);
  }

  /** 获取试卷时刻意不查询 answer_key 和 rubric，避免答案随接口泄漏到浏览器。 */
  public Map<String, Object> paper(String paperId, String courseId) {
    courseId = courses.require(courseId);
    List<Map<String, Object>> papers = db.queryForList(
        "SELECT id,title,created_at FROM mock_papers WHERE id=? AND course_id=?", paperId, courseId);
    if (papers.isEmpty()) throw new NoSuchElementException("当前课程中不存在该试卷");
    Map<String, Object> paper = new LinkedHashMap<>(papers.getFirst());
    List<Map<String, Object>> questions = db.queryForList(
        "SELECT q.id,q.stem,q.question_type,q.options_json,t.name AS topic,pq.ordinal "
            + "FROM mock_paper_questions pq JOIN exam_questions q ON pq.question_id=q.id "
            + "JOIN exam_topics t ON q.topic_id=t.id WHERE pq.paper_id=? ORDER BY pq.ordinal", paperId);
    for (Map<String, Object> question : questions) {
      question.put("type", question.remove("question_type"));
      question.put("options", parseOptions((String) question.remove("options_json")));
    }
    paper.put("questions", questions);
    return paper;
  }

  /** 按选择题答案和简答题关键词评分，并保存每题反馈与薄弱考点。 */
  @Transactional
  public Map<String, Object> submit(String paperId, String courseId, List<Map<String, String>> answers) {
    courseId = courses.require(courseId);
    if (db.queryForList("SELECT id FROM mock_papers WHERE id=? AND course_id=?", paperId, courseId).isEmpty())
      throw new NoSuchElementException("当前课程中不存在该试卷");
    if (!db.queryForList("SELECT id FROM exam_attempts WHERE paper_id=?", paperId).isEmpty())
      throw new IllegalStateException("这份试卷已经提交，不能重复计分");
    if (answers == null || answers.isEmpty()) throw new IllegalArgumentException("请至少提交一题答案");

    Map<String, String> supplied = new HashMap<>();
    for (Map<String, String> answer : answers) {
      String questionId = answer.get("question_id");
      String text = answer.get("answer");
      if (questionId == null || text == null || text.isBlank() || text.length() > 4000)
        throw new IllegalArgumentException("答案格式不正确");
      if (supplied.put(questionId, text.strip()) != null) throw new IllegalArgumentException("同一题只能提交一个答案");
    }

    List<Map<String, Object>> questions = db.queryForList(
        "SELECT q.id,q.stem,q.question_type,q.options_json,q.answer_key,q.rubric,t.id AS topic_id,t.name AS topic "
            + "FROM mock_paper_questions pq JOIN exam_questions q ON pq.question_id=q.id "
            + "JOIN exam_topics t ON q.topic_id=t.id WHERE pq.paper_id=? ORDER BY pq.ordinal", paperId);
    if (supplied.size() != questions.size()) throw new IllegalArgumentException("请完成全部题目后再提交");
    Set<String> questionIds = questions.stream().map(q -> q.get("id").toString()).collect(Collectors.toSet());
    if (!questionIds.equals(supplied.keySet())) throw new IllegalArgumentException("提交的题目不属于该试卷");

    int each = 100 / questions.size(), total = each * questions.size(), score = 0;
    Map<String, int[]> perTopic = new LinkedHashMap<>();
    List<Map<String, Object>> results = new ArrayList<>();
    for (Map<String, Object> question : questions) {
      String answer = supplied.get(question.get("id").toString());
      int itemScore = score(question, answer, each);
      score += itemScore;
      int[] topicScore = perTopic.computeIfAbsent(question.get("topic").toString(), ignored -> new int[2]);
      topicScore[0] += itemScore;
      topicScore[1] += each;
      results.add(Map.of("question_id", question.get("id").toString(), "score", itemScore,
          "feedback", feedback(question, answer, itemScore, each)));
    }
    String weak = weakSummary(perTopic);
    String attemptId = PlanService.id();
    db.update("INSERT INTO exam_attempts(id,paper_id,course_id,score,total_score,weak_summary) VALUES(?,?,?,?,?,?)",
        attemptId, paperId, courseId, score, total, weak);
    for (Map<String, Object> result : results) {
      String questionId = result.get("question_id").toString();
      db.update("INSERT INTO exam_answers(id,attempt_id,question_id,answer_text,score,feedback) VALUES(?,?,?,?,?,?)",
          PlanService.id(), attemptId, questionId, supplied.get(questionId), result.get("score"), result.get("feedback"));
    }
    return Map.of("id", attemptId, "score", score, "total_score", total, "weak_summary", weak, "results", results);
  }

  /** 供评测页展示本课程的历史测验，不暴露作答原文以外的参考答案。 */
  public List<Map<String, Object>> attempts(String courseId) {
    return db.queryForList("SELECT id,paper_id,score,total_score,weak_summary,submitted_at FROM exam_attempts "
        + "WHERE course_id=? ORDER BY submitted_at DESC", courses.require(courseId));
  }

  /** 删除课程时由控制器调用，确保题库、试卷、答卷都不遗留孤立记录。 */
  @Transactional
  public void removeCourseData(String courseId) {
    db.update("DELETE FROM exam_answers WHERE attempt_id IN (SELECT id FROM exam_attempts WHERE course_id=?)", courseId);
    db.update("DELETE FROM exam_attempts WHERE course_id=?", courseId);
    db.update("DELETE FROM mock_paper_questions WHERE paper_id IN (SELECT id FROM mock_papers WHERE course_id=?)", courseId);
    db.update("DELETE FROM mock_papers WHERE course_id=?", courseId);
    db.update("DELETE FROM exam_questions WHERE course_id=?", courseId);
    db.update("DELETE FROM exam_topics WHERE course_id=?", courseId);
  }

  private void addTopic(String courseId, Map<String, String> ids, String name, int frequency) {
    String id = PlanService.id();
    ids.put(name, id);
    db.update("INSERT INTO exam_topics(id,course_id,name,frequency) VALUES(?,?,?,?)", id, courseId, name, frequency);
  }
  /** MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，故先查询元数据再创建，迁移可以安全重跑。 */
  private void createIndex(String index, String table, String column) {
    db.execute((java.sql.Connection connection) -> {
      try (var indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, table, false, false)) {
        while (indexes.next())
          if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return null;
      }
      try (var statement = connection.createStatement()) {
        statement.execute("CREATE INDEX " + index + " ON " + table + "(" + column + ")");
      }
      return null;
    });
  }
  private void addChoice(String courseId, String topicId, String stem, List<String> options, String answer) {
    addQuestion(courseId, topicId, stem, "choice", options, answer, null);
  }
  private void addShort(String courseId, String topicId, String stem, List<String> rubric) {
    addQuestion(courseId, topicId, stem, "short", List.of(), "", String.join("|", rubric));
  }
  private void addQuestion(String courseId, String topicId, String stem, String type, List<String> options,
      String answer, String rubric) {
    try {
      db.update("INSERT INTO exam_questions(id,course_id,topic_id,stem,question_type,options_json,answer_key,rubric,source_type) "
          + "VALUES(?,?,?,?,?,?,?,?,?)", PlanService.id(), courseId, topicId, stem, type,
          mapper.writeValueAsString(options), answer, rubric, "demo");
    } catch (Exception e) {
      throw new IllegalStateException("题库初始化失败", e);
    }
  }
  private List<String> parseOptions(String json) {
    if (json == null || json.isBlank()) return List.of();
    try { return mapper.readValue(json, new TypeReference<List<String>>() {}); }
    catch (Exception e) { throw new IllegalStateException("题目选项数据损坏", e); }
  }
  private int score(Map<String, Object> question, String answer, int each) {
    if ("choice".equals(question.get("question_type"))) {
      String normalized = answer.strip().toUpperCase(Locale.ROOT);
      if (!List.of("A", "B", "C", "D").contains(normalized)) throw new IllegalArgumentException("选择题答案必须为 A、B、C 或 D");
      return normalized.equals(question.get("answer_key").toString()) ? each : 0;
    }
    String[] rules = Optional.ofNullable((String) question.get("rubric")).orElse("").split("\\|");
    String normalized = answer.replaceAll("\\s+", "");
    int hits = (int) Arrays.stream(rules).filter(rule -> !rule.isBlank() && normalized.contains(rule)).count();
    return rules.length == 0 ? 0 : Math.round((float) each * hits / rules.length);
  }
  private String feedback(Map<String, Object> question, String answer, int score, int each) {
    if ("choice".equals(question.get("question_type")))
      return score == each ? "回答正确。" : "回答不正确，请回到对应考点复习后再做同类题。";
    String rubric = Optional.ofNullable((String) question.get("rubric")).orElse("").replace('|', '、');
    return score == each ? "要点完整，说明清楚。" : "可围绕“" + rubric + "”补充论证，再说明它们之间的关系。";
  }
  private String weakSummary(Map<String, int[]> scores) {
    List<String> weak = scores.entrySet().stream().filter(e -> e.getValue()[0] * 10 < e.getValue()[1] * 6)
        .map(Map.Entry::getKey).toList();
    return weak.isEmpty() ? "暂无明显薄弱考点，继续巩固高频内容。" : "建议优先复习：" + String.join("、", weak) + "。";
  }
}
