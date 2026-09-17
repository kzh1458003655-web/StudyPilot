package cn.studypilot.exam.model;

import java.util.List;

/** A project-scoped mock exam and its generated questions. */
public record MockExamDetail(long id, String title, List<MockExamItemView> items) {
}
