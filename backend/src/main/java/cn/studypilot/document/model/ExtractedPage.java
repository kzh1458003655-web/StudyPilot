package cn.studypilot.document.model;

/** One text-bearing PDF page after extraction. Page numbers are one-based and user-visible. */
public record ExtractedPage(int pageNumber, String text) {}
