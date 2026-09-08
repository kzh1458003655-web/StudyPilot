package cn.studypilot.exam.model;

/** Frequency is counted by distinct source-question identity, never word occurrence. */
public record KnowledgePointFrequency(String knowledgePoint, long questionCount) {}
