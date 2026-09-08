package cn.studypilot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** 验证模拟试卷、课程隔离与评分都由服务端决定，浏览器不能伪造分数。 */
class AssessmentServiceTest {
  JdbcTemplate db;
  CourseService courses;
  AssessmentService service;
  String other = "20000000-0000-0000-0000-000000000001";

  @BeforeEach void init() {
    var ds = new DriverManagerDataSource(
        "jdbc:h2:mem:assessment" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
    db = new JdbcTemplate(ds);
    for (String table : List.of("documents", "sessions", "plans", "agent_events"))
      db.execute("CREATE TABLE " + table + "(id VARCHAR(36) PRIMARY KEY,name VARCHAR(120),title VARCHAR(120),sha256 CHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    courses = new CourseService(db);
    courses.migrate();
    db.update("INSERT INTO courses(id,name) VALUES(?,?)", other, "计算机网络");
    service = new AssessmentService(db, new ObjectMapper(), courses);
    service.migrate();
  }

  @Test void paperAndTopicsStayWithinCourse() {
    service.seed(other);
    var paper = service.createPaper(other, 3);
    assertEquals(3, ((List<?>) paper.get("questions")).size());
    assertEquals(4, service.topics(other).size());
    assertThrows(NoSuchElementException.class, () -> service.paper(paper.get("id").toString(), CourseService.DEFAULT));
  }

  @Test void objectiveAndRubricScoresAreCalculatedOnServer() {
    service.seed(CourseService.DEFAULT);
    var paper = service.createPaper(CourseService.DEFAULT, 5);
    @SuppressWarnings("unchecked") var questions = (List<Map<String, Object>>) paper.get("questions");
    List<Map<String, String>> answers = new ArrayList<>();
    for (var question : questions) {
      String answer = "short".equals(question.get("type"))
          ? "利用原子性与一致性，使计划和任务全部成功；出现异常时全部回滚。"
          // 正确答案仅从测试数据库取得；对外的试卷对象不暴露答案，避免浏览器直接读取。
          : db.queryForObject("SELECT answer_key FROM exam_questions WHERE id=?", String.class, question.get("id"));
      answers.add(Map.of("question_id", question.get("id").toString(), "answer", answer));
    }
    var attempt = service.submit(paper.get("id").toString(), CourseService.DEFAULT, answers);
    assertEquals(100, ((Number) attempt.get("score")).intValue());
    assertTrue(((String) attempt.get("weak_summary")).contains("暂无明显薄弱考点"));
    assertThrows(IllegalStateException.class, () -> service.submit(paper.get("id").toString(), CourseService.DEFAULT, answers));
  }

  @Test void invalidQuestionAnswerIsRejected() {
    service.seed(CourseService.DEFAULT);
    var paper = service.createPaper(CourseService.DEFAULT, 1);
    @SuppressWarnings("unchecked") var question = ((List<Map<String, Object>>) paper.get("questions")).getFirst();
    assertThrows(IllegalArgumentException.class, () -> service.submit(paper.get("id").toString(), CourseService.DEFAULT,
        List.of(Map.of("question_id", question.get("id").toString(), "answer", "Z"))));
  }
}
