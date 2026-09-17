package cn.studypilot.document.dto;

import java.time.Instant;
public record ProjectResponse(long id, String name, String description, Instant createdAt, Instant archivedAt) {}
