package cn.studypilot.qa.model;

/** A stored message that can be safely supplied as short conversational context. */
public record QaHistoryMessage(String role, String content) {}
