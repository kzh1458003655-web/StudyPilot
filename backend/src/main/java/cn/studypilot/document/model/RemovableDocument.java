package cn.studypilot.document.model;

/** Internal deletion descriptor; no controller receives the local path. */
public record RemovableDocument(long id, String indexId, String filePath) {}
