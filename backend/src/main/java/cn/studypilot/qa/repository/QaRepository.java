package cn.studypilot.qa.repository;

import cn.studypilot.qa.model.QaCitationEvidence;
import cn.studypilot.qa.model.QaHistoryMessage;
import cn.studypilot.qa.model.SavedQaExchange;
import java.util.List;

/** Persistence boundary owned by QA; callers never manipulate its four tables separately. */
public interface QaRepository {
  long createSession(long projectId, String title);
  boolean belongsToProject(long sessionId, long projectId);
  List<QaHistoryMessage> recentMessages(long sessionId, int limit);
  SavedQaExchange saveExchange(long projectId, long sessionId, String question, String answer,
                               List<QaCitationEvidence> evidence);
}
