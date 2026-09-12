package cn.studypilot.qa.model;

/** Immutable citation snapshot shown when a saved answer is reopened. */
public record QaHistoryCitation(String documentName, int pageNumber, String excerpt, double score) {}
