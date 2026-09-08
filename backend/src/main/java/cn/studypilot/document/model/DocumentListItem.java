package cn.studypilot.document.model;

/** Safe metadata used by the resource centre; file paths and hashes never leave the backend. */
public record DocumentListItem(long id, String displayName, String documentType, String status, int pageCount, int chunkCount) {}
