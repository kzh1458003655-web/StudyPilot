package cn.studypilot.exam.model;

/** A temporary text question detected from a past-paper page; it is not yet an answerable item. */
public record ParsedSourceQuestion(int ordinal, int pageNumber, String text) {}
