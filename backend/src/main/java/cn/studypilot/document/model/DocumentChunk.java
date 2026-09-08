package cn.studypilot.document.model;

/** A retrieval-sized section of one extracted page. */
public record DocumentChunk(int pageNumber, int chunkIndex, String text, int tokenEstimate) {}
