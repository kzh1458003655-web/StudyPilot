package cn.studypilot.document.model;

/** Page text exposed only to the exam analysis workflow after project/type/status checks. */
public record DocumentPageForAnalysis(long documentId, int pageNumber, String text) {}
