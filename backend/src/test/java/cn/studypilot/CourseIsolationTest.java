package cn.studypilot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/** 用真实内存数据库验证隔离，避免只断言 SQL 字符串而漏掉越课程访问。 */
class CourseIsolationTest {
  JdbcTemplate db; CourseService courses; ApiController api; AiClient ai;
  String other="10000000-0000-0000-0000-000000000001";
  // 测试只需要一个可创建 documents 子目录的构建目录；不产生 PDF 文件。
  Path temp=Path.of("target","course-test-data");
  @BeforeEach void init() throws Exception {
    var ds=new DriverManagerDataSource("jdbc:h2:mem:"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    db=new JdbcTemplate(ds);
    for(String table:new String[]{"documents","sessions","plans","agent_events"})
      db.execute("CREATE TABLE "+table+"(id VARCHAR(36) PRIMARY KEY,name VARCHAR(120),title VARCHAR(120),sha256 CHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    db.update("INSERT INTO documents(id,name) VALUES('old','旧资料')");
    courses=new CourseService(db);courses.migrate();courses.migrate();
    db.update("INSERT INTO courses(id,name) VALUES(?,?)",other,"操作系统");
    db.update("INSERT INTO documents(id,name,course_id) VALUES('other-doc','其他课程',?)",other);
    db.update("INSERT INTO sessions(id,title,course_id) VALUES('other-session','隐私会话',?)",other);
    ai=mock(AiClient.class);var mapper=new ObjectMapper();
    var plans=new PlanService(db,ai,mapper,new DataSourceTransactionManager(ds),courses);
    api=new ApiController(db,ai,mapper,plans,courses,temp.toString());
  }
  @Test void legacyDataIsRetainedAndMigrationIsRepeatable() {
    assertEquals(CourseService.DEFAULT,db.queryForObject("SELECT course_id FROM documents WHERE id='old'",String.class));
    assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM documents",Integer.class));
  }
  @Test void listsAndRetrievalAllowlistAreScoped() {
    assertEquals("old",api.documents(null).getFirst().get("id"));
    assertEquals(java.util.List.of("other-doc"),courses.documentIds(other));
    assertTrue(api.sessions(null).isEmpty());
    assertEquals(1,api.sessions(other).size());
  }
  @Test void guessedIdsCannotReadMessagesOrPlansAcrossCourses() {
    assertThrows(NoSuchElementException.class,()->api.messages("other-session",CourseService.DEFAULT));
    assertThrows(NoSuchElementException.class,()->api.plan("other-doc",CourseService.DEFAULT));
    assertThrows(NoSuchElementException.class,()->courses.owns("documents","other-doc",CourseService.DEFAULT));
  }
  @Test void chatCannotUseAnotherCoursesSession() throws Exception {
    var body=new ObjectMapper().readTree("{\"question\":\"解释数据库\",\"session_id\":\"other-session\",\"courseId\":\""+CourseService.DEFAULT+"\",\"useReferences\":false}");
    assertThrows(NoSuchElementException.class,()->api.chat(body));
  }

  @Test void generalChatCallsModelWithoutRetrieval() throws Exception {
    db.execute("CREATE TABLE messages(id VARCHAR(36),session_id VARCHAR(36),role VARCHAR(12),content TEXT,status VARCHAR(20),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    db.execute("CREATE TABLE citations(id VARCHAR(36),message_id VARCHAR(36),document_id VARCHAR(36),source_name VARCHAR(240),page_number INT,excerpt TEXT)");
    var response=mock(java.net.http.HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(new java.io.ByteArrayInputStream(("data: {\"choices\":[{\"delta\":{\"content\":\"数据库用于保存数据\"}}]}\n\ndata: [DONE]\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    when(ai.stream(any())).thenReturn(response);
    var body=new ObjectMapper().readTree("{\"question\":\"解释数据库\",\"session_id\":\"other-session\",\"courseId\":\""+other+"\",\"useReferences\":false}");
    api.chat(body);
    for(int i=0;i<100&&db.queryForObject("SELECT COUNT(*) FROM messages WHERE role='assistant'",Integer.class)==0;i++)Thread.sleep(25);
    assertEquals("数据库用于保存数据",db.queryForObject("SELECT content FROM messages WHERE role='assistant'",String.class));
    verify(ai,never()).post(eq("/retrieve"),any());
    verify(ai).stream(any());
  }
}
