package cn.studypilot.common.repository;

import java.time.Instant;

/** Database-facing project value; controllers must map it to module DTOs before returning it. */
public record ProjectRecord(
    long id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt,
    Instant archivedAt) {}
