package cn.studypilot.document.dto;

public record DocumentImportResponse(long id, String indexId, int pageCount, int chunkCount) {}
