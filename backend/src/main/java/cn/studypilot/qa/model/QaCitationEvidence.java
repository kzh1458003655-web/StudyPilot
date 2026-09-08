package cn.studypilot.qa.model;

/** Immutable retrieval evidence saved with an answer and returned to the user. */
public record QaCitationEvidence(String documentIndexId, String documentName, int pageNumber, String excerpt,
                                 double score, int rankPosition) {}
