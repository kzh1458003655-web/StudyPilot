package cn.studypilot;

import jakarta.annotation.PostConstruct;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;

/** 课程是资料、对话和计划的共同边界；旧数据原位归入默认课程。 */
@Service
@DependsOnDatabaseInitialization
public class CourseService {
  public static final String DEFAULT = "00000000-0000-0000-0000-000000000001";
  private final JdbcTemplate db;
  public CourseService(JdbcTemplate db) { this.db=db; }
  @PostConstruct
  public void migrate() {
    db.execute("CREATE TABLE IF NOT EXISTS courses(id VARCHAR(36) PRIMARY KEY,name VARCHAR(120) NOT NULL,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    if (db.queryForList("SELECT id FROM courses WHERE id=?",DEFAULT).isEmpty())
      db.update("INSERT INTO courses(id,name) VALUES(?,?)",DEFAULT,"我的课程");
    for (String table : List.of("documents","sessions","plans","agent_events")) {
      Boolean present=db.query("SELECT * FROM "+table+" WHERE 1=0",rs->{
        for(int i=1;i<=rs.getMetaData().getColumnCount();i++)
          if(rs.getMetaData().getColumnName(i).equalsIgnoreCase("course_id"))return true;
        return false;
      });
      if (!Boolean.TRUE.equals(present)) {
        db.execute("ALTER TABLE "+table+" ADD COLUMN course_id VARCHAR(36) NOT NULL DEFAULT '"+DEFAULT+"'");
        db.execute("CREATE INDEX idx_"+table+"_course ON "+table+"(course_id)");
      }
    }
    // 旧版 sha256 全局唯一会阻止两门课上传同一资料；改为课程内去重。
    db.execute((java.sql.Connection con)->{
      Map<String,List<String>> indexes=new HashMap<>();
      try(var rs=con.getMetaData().getIndexInfo(con.getCatalog(),null,"documents",true,false)) {
        while(rs.next()) { String n=rs.getString("INDEX_NAME"),c=rs.getString("COLUMN_NAME");
          if(n!=null&&c!=null)indexes.computeIfAbsent(n,k->new ArrayList<>()).add(c); }
      }
      for(var e:indexes.entrySet()) if(e.getValue().size()==1&&e.getValue().getFirst().equalsIgnoreCase("sha256"))
        try(var s=con.createStatement()){s.execute("ALTER TABLE documents DROP INDEX `"+e.getKey().replace("`","")+"`");}
      if(indexes.keySet().stream().noneMatch(n->n.equalsIgnoreCase("uq_documents_course_sha")))
        try(var statement=con.createStatement()){statement.execute("CREATE UNIQUE INDEX uq_documents_course_sha ON documents(course_id,sha256)");}
      return null;
    });
  }
  public String require(String id) {
    String course=id==null||id.isBlank()?DEFAULT:id;
    if(db.queryForList("SELECT id FROM courses WHERE id=?",course).isEmpty())throw new NoSuchElementException("课程不存在");
    return course;
  }
  public List<String> documentIds(String course) {
    return db.queryForList("SELECT id FROM documents WHERE course_id=?",String.class,require(course));
  }
  public void owns(String table,String id,String course) {
    if(!Set.of("documents","sessions","plans").contains(table))throw new IllegalArgumentException("无效对象");
    if(db.queryForList("SELECT id FROM "+table+" WHERE id=? AND course_id=?",id,require(course)).isEmpty())
      throw new NoSuchElementException("当前课程中不存在该记录");
  }
}
