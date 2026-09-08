package cn.studypilot.retrieval.dto;

/** 检索返回的可引用片段；C++ 内部索引字段不穿透到业务模块。 */
public record RetrievalHit(String documentId, int pageNumber, String excerpt, double score) {}
