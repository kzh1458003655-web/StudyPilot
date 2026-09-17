package cn.studypilot.document.dto;

public record DocumentListResponse(long id, String displayName, String documentType, String status, int pageCount, int chunkCount) {}
