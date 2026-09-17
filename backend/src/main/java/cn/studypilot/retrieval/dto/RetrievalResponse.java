package cn.studypilot.retrieval.dto;

import java.util.List;
public record RetrievalResponse(List<RetrievalHit> hits) {}
