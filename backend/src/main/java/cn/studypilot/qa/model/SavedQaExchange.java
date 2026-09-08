package cn.studypilot.qa.model;

/** Database identities assigned to a completed question-and-answer exchange. */
public record SavedQaExchange(long userMessageId, long assistantMessageId) {}
