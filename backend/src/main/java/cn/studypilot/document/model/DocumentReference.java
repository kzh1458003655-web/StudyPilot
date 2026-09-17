package cn.studypilot.document.model;

/** A retrieval-safe document identity. It deliberately excludes local storage paths and raw text. */
public record DocumentReference(String indexId, String displayName) {}
