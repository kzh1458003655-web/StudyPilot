package cn.studypilot.exam.model;

import java.util.List;

/** Structured candidate returned by the local model before validation and persistence. */
public record GeneratedExamItem(String type, String prompt, List<String> options, String answer, String analysis,
                                String knowledgePoint, double score, List<String> sourceDocumentIds) {}
