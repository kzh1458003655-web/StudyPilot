package cn.studypilot.qa.dto;

/** Citation snapshot never depends on a source file continuing to exist. */
public record QaCitationResponse(String documentName, int pageNumber, String excerpt, double score) {}
