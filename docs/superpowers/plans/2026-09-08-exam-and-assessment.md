# Exam and Assessment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add course-scoped exam-frequency statistics, mock-paper generation, answer submission, deterministic objective grading, rubric-based short-answer assessment, and weak-topic feedback.

**Architecture:** Spring Boot owns records, validation, transactions, and objective scoring. The existing C++ model gateway is called only for question generation and short-answer feedback. Vue renders a three-stage flow: statistics and question bank, mock paper, then feedback; every record is tied to `course_id`.

**Tech Stack:** Vue 3 static frontend, Spring Boot 3.5/JDBC/MySQL, C++ local AI gateway, Qwen3.5 GGUF, JUnit 5, Python real-service integration tests, Playwright browser tests.

---

### Task 1: Persist course-scoped assessment data

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Create: `backend/src/test/java/cn/studypilot/AssessmentServiceTest.java`
- Create: `backend/src/main/java/cn/studypilot/AssessmentService.java`

- [ ] **Step 1: Write failing tests** for course isolation, answer validation, objective scoring, score aggregation, and completed-record lookup.
- [ ] **Step 2: Run tests** and confirm compilation fails because `AssessmentService` does not exist.
- [ ] **Step 3: Create tables** for topics, question bank, papers, paper questions, attempts, and answers, all with course ownership.
- [ ] **Step 4: Implement service** methods with transactional submit logic and deterministic single-choice scoring.
- [ ] **Step 5: Run Java tests** and confirm they pass.

### Task 2: Expose a stable assessment API

**Files:**
- Modify: `backend/src/main/java/cn/studypilot/ApiController.java`
- Modify: `backend/src/main/java/cn/studypilot/CourseService.java`
- Test: `backend/src/test/java/cn/studypilot/AssessmentServiceTest.java`

- [ ] **Step 1: Add failing API-facing service assertions** for invalid course IDs, invalid options, duplicate submission, and cross-course paper rejection.
- [ ] **Step 2: Add endpoints** for topic statistics, question creation, mock paper creation, paper detail, answer submission, and attempt feedback.
- [ ] **Step 3: Run Java tests** and confirm service behavior is valid.

### Task 3: Add deterministic seed data and local-model generation entry

**Files:**
- Modify: `backend/src/main/java/cn/studypilot/AssessmentService.java`
- Test: `tests/assessment_integration.py`

- [ ] **Step 1: Write an integration test** that creates a paper from fixed course questions and checks topic frequency.
- [ ] **Step 2: Implement a seed/question endpoint** for a small, explainable question bank. Use the existing AI client only for optional short-answer feedback, retaining a rule-based fallback if unavailable.
- [ ] **Step 3: Run the integration test** against the local service.

### Task 4: Build the three-stage Vue experience

**Files:**
- Modify: `frontend/index.html`
- Modify: `frontend/app.js`
- Modify: `frontend/style.css`
- Modify: `tests/browser-course-test.cjs`

- [ ] **Step 1: Add a browser test** that selects a course, creates a mock paper, submits responses, and sees score plus weak topics.
- [ ] **Step 2: Implement statistics, paper, answer and feedback views** using existing fetch helpers and course ID propagation.
- [ ] **Step 3: Run browser test** and inspect saved screenshots.

### Task 5: Verify and document the new boundary

**Files:**
- Modify: `README.md`
- Modify: `docs/项目架构图.md`
- Test: `tests/assessment_integration.py`, `tests/browser-course-test.cjs`

- [ ] **Step 1: Run full Java build** and verify all unit tests pass.
- [ ] **Step 2: Run real-service assessment integration** and validate course isolation, paper generation, objective grading, feedback and repeated-submit rejection.
- [ ] **Step 3: Run browser flow** and retain visual evidence.
- [ ] **Step 4: Update README and architecture status** so completed functionality is distinguishable from future work.
- [ ] **Step 5: Commit and push** the verified implementation.
