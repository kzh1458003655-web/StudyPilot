package cn.studypilot.retrieval.dto;

import java.util.List;

/** 资料 ID 由业务模块先按备考项目范围过滤，再交给检索 Gateway。 */
public record RetrievalRequest(String projectId, String query, List<String> documentIds, int limit) {}
