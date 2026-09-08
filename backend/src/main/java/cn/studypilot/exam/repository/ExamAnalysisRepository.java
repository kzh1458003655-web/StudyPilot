package cn.studypilot.exam.repository;

import cn.studypilot.exam.model.KnowledgePointFrequency;
import cn.studypilot.exam.model.ParsedSourceQuestion;
import java.util.List;
import java.util.Map;

public interface ExamAnalysisRepository {
  void replaceAnalysis(long projectId, long documentId, List<ParsedSourceQuestion> questions, Map<Integer, List<String>> knowledgePoints);
  List<KnowledgePointFrequency> frequencies(long projectId);
}
