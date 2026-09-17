import { delay, http, HttpResponse } from "msw";
import {
  findMockProject,
  getMockBackendState,
  resetMockBackendState,
  saveMockBackendState,
} from "./database";
import { createGeneratedExam } from "./fixtures";
import type {
  MockDocumentType,
  MockExamItemRecord,
  MockModuleName,
  MockProjectRecord,
} from "./types";

const API = "*/api/v1";

function requestId(): string {
  return `mock-${crypto.randomUUID()}`;
}

function ok<T>(data: T, init?: ResponseInit) {
  return HttpResponse.json({ data, requestId: requestId() }, init);
}

function fail(status: number, code: string, message: string) {
  return HttpResponse.json(
    {
      code,
      message,
      requestId: requestId(),
      timestamp: new Date().toISOString(),
    },
    { status },
  );
}

function positiveInteger(value: string | null): number | undefined {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined;
}

function projectFromRequest(request: Request): MockProjectRecord | undefined {
  return findMockProject(
    positiveInteger(new URL(request.url).searchParams.get("projectId")) ?? -1,
  );
}

async function projectDelay(project?: MockProjectRecord): Promise<void> {
  await delay(project?.behavior?.latencyMs ?? 120);
}

function moduleFailure(
  project: MockProjectRecord,
  module: MockModuleName,
): Response | undefined {
  if (!project.behavior?.failingModules?.includes(module)) return undefined;
  return fail(
    503,
    "MOCK_MODULE_UNAVAILABLE",
    `这是“${project.name}”预设的 ${module} 模块异常场景。`,
  );
}

function publicProject(project: MockProjectRecord) {
  return {
    id: project.id,
    name: project.name,
    description: project.description,
    createdAt: project.createdAt,
    archivedAt: project.archivedAt,
  };
}

function publicDocument(document: MockProjectRecord["documents"][number]) {
  return {
    id: document.id,
    displayName: document.displayName,
    documentType: document.documentType,
    status: document.status,
    pageCount: document.pageCount,
    chunkCount: document.chunkCount,
  };
}

function publicExamItem(item: MockExamItemRecord) {
  return {
    ...item,
    options: item.type === "SHORT_ANSWER" ? null : item.options,
  };
}

function inferDocumentType(fileName: string): MockDocumentType {
  const normalized = fileName.toLowerCase();
  if (/真题|试卷|past|exam/.test(normalized)) return "PAST_EXAM";
  if (/答案|answer/.test(normalized)) return "REFERENCE_ANSWER";
  if (/讲义|lecture/.test(normalized)) return "LECTURE";
  if (/知识|提纲|knowledge/.test(normalized)) return "KNOWLEDGE";
  return "TEXTBOOK";
}

function findProjectOrError(
  request: Request,
):
  | { project: MockProjectRecord; error?: never }
  | { project?: never; error: Response } {
  const project = projectFromRequest(request);
  return project
    ? { project }
    : { error: fail(404, "PROJECT_NOT_FOUND", "未找到指定课程项目。") };
}

export const mockHandlers = [
  http.get(`${API}/health`, () => ok({ status: "UP", mode: "FRONTEND_MOCK" })),

  http.get(`${API}/health/dependencies`, () =>
    ok({
      application: "UP",
      database: "MOCKED",
      vectorStore: "SKIPPED",
      model: "SKIPPED",
    }),
  ),

  http.get(`${API}/projects`, async ({ request }) => {
    await delay(100);
    const includeArchived =
      new URL(request.url).searchParams.get("includeArchived") === "true";
    return ok(
      getMockBackendState()
        .projects.filter(
          (project) => includeArchived || project.archivedAt === null,
        )
        .map(publicProject),
    );
  }),

  http.post(`${API}/projects`, async ({ request }) => {
    const body = (await request.json()) as {
      name?: unknown;
      description?: unknown;
    };
    if (typeof body.name !== "string" || !body.name.trim()) {
      return fail(
        400,
        "INVALID_PROJECT_NAME",
        "课程名称不能为空。用户界面可展示此校验错误。 ",
      );
    }
    const state = getMockBackendState();
    const project: MockProjectRecord = {
      id: state.nextProjectId++,
      name: body.name.trim(),
      description:
        typeof body.description === "string" ? body.description.trim() : "",
      createdAt: new Date().toISOString(),
      archivedAt: null,
      documents: [],
      qaSession: null,
      knowledgePoints: [],
      analyses: [],
      exams: [],
      attempts: [],
    };
    state.projects.unshift(project);
    saveMockBackendState();
    await delay(160);
    return ok(publicProject(project), { status: 201 });
  }),

  http.post(`${API}/projects/:projectId/archive`, async ({ params }) => {
    const project = findMockProject(
      positiveInteger(String(params.projectId)) ?? -1,
    );
    if (!project) return fail(404, "PROJECT_NOT_FOUND", "未找到指定课程项目。");
    project.archivedAt = new Date().toISOString();
    saveMockBackendState();
    await projectDelay(project);
    return ok(publicProject(project));
  }),

  http.post(`${API}/projects/:projectId/restore`, async ({ params }) => {
    const project = findMockProject(
      positiveInteger(String(params.projectId)) ?? -1,
    );
    if (!project) return fail(404, "PROJECT_NOT_FOUND", "未找到指定课程项目。");
    project.archivedAt = null;
    saveMockBackendState();
    await projectDelay(project);
    return ok(publicProject(project));
  }),

  http.get(`${API}/documents`, async ({ request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    return (
      moduleFailure(result.project, "documents") ??
      ok(result.project.documents.map(publicDocument))
    );
  }),

  http.post(`${API}/documents`, async ({ request }) => {
    const form = await request.formData();
    const projectId = positiveInteger(String(form.get("projectId") ?? ""));
    const project = findMockProject(projectId ?? -1);
    if (!project) return fail(404, "PROJECT_NOT_FOUND", "未找到指定课程项目。");
    await projectDelay(project);
    const simulatedFailure = moduleFailure(project, "documents");
    if (simulatedFailure) return simulatedFailure;
    const file = form.get("file");
    if (!(file instanceof File)) {
      return fail(400, "FILE_REQUIRED", "请选择需要导入的 PDF 文件。");
    }
    const state = getMockBackendState();
    const document = {
      id: state.nextDocumentId++,
      indexId: crypto.randomUUID(),
      displayName: file.name,
      documentType: inferDocumentType(file.name),
      status: "READY" as const,
      pageCount: Math.max(1, Math.ceil(file.size / 12_000)),
      chunkCount: Math.max(3, Math.ceil(file.size / 4_000)),
    };
    project.documents.unshift(document);
    saveMockBackendState();
    return ok(
      {
        id: document.id,
        indexId: document.indexId,
        pageCount: document.pageCount,
        chunkCount: document.chunkCount,
      },
      { status: 201 },
    );
  }),

  http.delete(`${API}/documents/:documentId`, async ({ params, request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    const simulatedFailure = moduleFailure(result.project, "documents");
    if (simulatedFailure) return simulatedFailure;
    const documentId = positiveInteger(String(params.documentId));
    const index = result.project.documents.findIndex(
      (item) => item.id === documentId,
    );
    if (index < 0) {
      return fail(404, "DOCUMENT_NOT_FOUND", "当前课程中不存在该资料。");
    }
    result.project.documents.splice(index, 1);
    saveMockBackendState();
    return new HttpResponse(null, { status: 204 });
  }),

  http.get(`${API}/qa/history`, async ({ request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    const simulatedFailure = moduleFailure(result.project, "qa");
    if (simulatedFailure) return simulatedFailure;
    return ok({
      sessionId: result.project.qaSession?.id ?? null,
      turns:
        result.project.qaSession?.turns.map(
          ({ question, answer, citations }) => ({
            question,
            answer,
            citations,
          }),
        ) ?? [],
    });
  }),

  http.post(`${API}/qa`, async ({ request }) => {
    const body = (await request.json()) as {
      projectId?: unknown;
      sessionId?: unknown;
      question?: unknown;
    };
    const project = findMockProject(
      typeof body.projectId === "number" ? body.projectId : -1,
    );
    if (!project) return fail(404, "PROJECT_NOT_FOUND", "未找到指定课程项目。");
    await projectDelay(project);
    const simulatedFailure = moduleFailure(project, "qa");
    if (simulatedFailure) return simulatedFailure;
    if (typeof body.question !== "string" || !body.question.trim()) {
      return fail(400, "QUESTION_REQUIRED", "问题不能为空。");
    }
    const readyDocument = project.documents.find(
      (item) => item.status === "READY",
    );
    const insufficient =
      !readyDocument || /没有|未提及|超出|不存在|量子/.test(body.question);
    const state = getMockBackendState();
    project.qaSession ??= { id: state.nextSessionId++, turns: [] };
    const turn = insufficient
      ? {
          question: body.question.trim(),
          status: "INSUFFICIENT_EVIDENCE" as const,
          answer:
            "当前课程资料中没有找到足够证据回答这个问题。请补充相关资料或换一种问法。",
          citations: [],
        }
      : {
          question: body.question.trim(),
          status: "ANSWERED" as const,
          answer:
            "这是由前端模拟服务生成的示例回答。它保持了真实接口结构，可用于检查问答气泡、加载状态、引用卡片和长文本排版。",
          citations: [
            {
              documentName: readyDocument.displayName,
              pageNumber: Math.min(Math.max(readyDocument.pageCount, 1), 18),
              excerpt:
                "这是与问题相关的模拟资料片段，用于呈现引用来源、页码、相关度和展开状态。",
              score: 0.9235,
            },
          ],
        };
    project.qaSession.turns.push(turn);
    saveMockBackendState();
    return ok({
      sessionId: project.qaSession.id,
      status: turn.status,
      answer: turn.answer,
      citations: turn.citations,
    });
  }),

  http.get(`${API}/exams/knowledge-points`, async ({ request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    return (
      moduleFailure(result.project, "exams") ??
      ok(result.project.knowledgePoints)
    );
  }),

  http.post(
    `${API}/exams/past-papers/:documentId/analysis`,
    async ({ params, request }) => {
      const result = findProjectOrError(request);
      if (result.error) return result.error;
      await projectDelay(result.project);
      const simulatedFailure = moduleFailure(result.project, "exams");
      if (simulatedFailure) return simulatedFailure;
      const documentId = positiveInteger(String(params.documentId));
      const document = result.project.documents.find(
        (item) => item.id === documentId && item.documentType === "PAST_EXAM",
      );
      if (!document) {
        return fail(
          404,
          "PAST_PAPER_NOT_FOUND",
          "当前课程中不存在该真题资料。",
        );
      }
      if (document.status !== "READY") {
        return fail(
          409,
          "DOCUMENT_NOT_READY",
          "资料仍在处理中，暂时不能分析。",
        );
      }
      let analysis = result.project.analyses.find(
        (item) => item.documentId === document.id,
      );
      if (!analysis) {
        analysis = {
          documentId: document.id,
          detectedQuestions: Math.max(8, Math.round(document.chunkCount * 0.7)),
          analyzedQuestions: Math.max(
            7,
            Math.round(document.chunkCount * 0.65),
          ),
          needsReviewQuestions: 1,
        };
        result.project.analyses.push(analysis);
        if (result.project.knowledgePoints.length === 0) {
          result.project.knowledgePoints.push(
            { knowledgePoint: "基础概念", questionCount: 8 },
            { knowledgePoint: "综合应用", questionCount: 5 },
            { knowledgePoint: "计算与分析", questionCount: 3 },
          );
        }
        saveMockBackendState();
      }
      return ok({
        detectedQuestions: analysis.detectedQuestions,
        analyzedQuestions: analysis.analyzedQuestions,
        needsReviewQuestions: analysis.needsReviewQuestions,
      });
    },
  ),

  http.post(`${API}/exams/generate`, async ({ request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    const simulatedFailure = moduleFailure(result.project, "exams");
    if (simulatedFailure) return simulatedFailure;
    const body = (await request.json()) as { instructions?: unknown };
    const state = getMockBackendState();
    const exam = createGeneratedExam(
      state.nextExamId++,
      state.nextExamItemId,
      result.project.name,
      typeof body.instructions === "string" ? body.instructions.trim() : "",
    );
    state.nextExamItemId += exam.items.length;
    result.project.exams.unshift(exam);
    saveMockBackendState();
    return ok(
      { examId: exam.id, itemCount: exam.items.length },
      { status: 201 },
    );
  }),

  http.get(`${API}/exams`, async ({ request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    return (
      moduleFailure(result.project, "exams") ??
      ok(
        result.project.exams.map((exam) => ({
          id: exam.id,
          title: exam.title,
          itemCount: exam.items.length,
        })),
      )
    );
  }),

  http.get(`${API}/exams/:examId`, async ({ params, request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    const simulatedFailure = moduleFailure(result.project, "exams");
    if (simulatedFailure) return simulatedFailure;
    const exam = result.project.exams.find(
      (item) => item.id === positiveInteger(String(params.examId)),
    );
    if (!exam) return fail(404, "EXAM_NOT_FOUND", "当前课程中不存在该试卷。");
    return ok({
      id: exam.id,
      title: exam.title,
      items: exam.items.map(publicExamItem),
    });
  }),

  http.delete(`${API}/exams/:examId`, async ({ params, request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    const simulatedFailure = moduleFailure(result.project, "exams");
    if (simulatedFailure) return simulatedFailure;
    const examId = positiveInteger(String(params.examId));
    const index = result.project.exams.findIndex((item) => item.id === examId);
    if (index < 0)
      return fail(404, "EXAM_NOT_FOUND", "当前课程中不存在该试卷。");
    result.project.exams.splice(index, 1);
    result.project.attempts = result.project.attempts.filter(
      (attempt) => attempt.examId !== examId,
    );
    saveMockBackendState();
    return new HttpResponse(null, { status: 204 });
  }),

  http.post(`${API}/assessments/attempts`, async ({ request }) => {
    const result = findProjectOrError(request);
    if (result.error) return result.error;
    await projectDelay(result.project);
    const simulatedFailure = moduleFailure(result.project, "assessment");
    if (simulatedFailure) return simulatedFailure;
    const examId = positiveInteger(
      new URL(request.url).searchParams.get("examId"),
    );
    if (!result.project.exams.some((exam) => exam.id === examId)) {
      return fail(404, "EXAM_NOT_FOUND", "当前课程中不存在该试卷。");
    }
    const state = getMockBackendState();
    const attempt = {
      id: state.nextAttemptId++,
      examId,
      status: "IN_PROGRESS" as const,
      totalScore: null,
      answers: [],
    };
    result.project.attempts.push(attempt);
    saveMockBackendState();
    return ok({ attemptId: attempt.id }, { status: 201 });
  }),

  http.post(
    `${API}/assessments/attempts/:attemptId/submit`,
    async ({ params, request }) => {
      const result = findProjectOrError(request);
      if (result.error) return result.error;
      await projectDelay(result.project);
      const simulatedFailure = moduleFailure(result.project, "assessment");
      if (simulatedFailure) return simulatedFailure;
      const attempt = result.project.attempts.find(
        (item) => item.id === positiveInteger(String(params.attemptId)),
      );
      if (!attempt) {
        return fail(404, "ATTEMPT_NOT_FOUND", "当前课程中不存在该答题记录。");
      }
      const exam = result.project.exams.find(
        (item) => item.id === attempt.examId,
      );
      if (!exam) return fail(404, "EXAM_NOT_FOUND", "试卷已不存在。");
      const body = (await request.json()) as {
        answers?: Array<{ itemId?: unknown; answer?: unknown }>;
      };
      const submitted = Array.isArray(body.answers) ? body.answers : [];
      attempt.answers = exam.items.map((item) => {
        const rawAnswer = submitted.find(
          (answer) => answer.itemId === item.id,
        )?.answer;
        const answer = typeof rawAnswer === "string" ? rawAnswer.trim() : "";
        const exact = answer.toUpperCase() === item.answer.toUpperCase();
        const score =
          item.type === "SINGLE_CHOICE"
            ? exact
              ? item.score
              : 0
            : answer.length === 0
              ? 0
              : answer.length >= 30
                ? Math.round(item.score * 0.8)
                : Math.round(item.score * 0.5);
        return {
          itemId: item.id,
          answer,
          score,
          feedback:
            answer.length === 0
              ? "未作答。建议先写出关键概念和推理步骤。"
              : score === item.score
                ? "回答正确，关键点完整。"
                : item.type === "SINGLE_CHOICE"
                  ? `选择不正确，参考答案为 ${item.answer}。`
                  : "已覆盖部分关键点，可结合参考答案补充条件、步骤和结论。",
          gradingStatus:
            item.type === "SINGLE_CHOICE"
              ? ("RULE_GRADED" as const)
              : ("MODEL_GRADED" as const),
        };
      });
      attempt.status = "GRADED";
      attempt.totalScore = attempt.answers.reduce(
        (sum, answer) => sum + answer.score,
        0,
      );
      saveMockBackendState();
      return ok({
        totalScore: attempt.totalScore,
        maxScore: exam.items.reduce((sum, item) => sum + item.score, 0),
        answers: attempt.answers.map(({ itemId, score, feedback }) => ({
          itemId,
          score,
          feedback,
        })),
      });
    },
  ),

  http.post(`${API}/mock/reset`, async () => {
    resetMockBackendState();
    await delay(80);
    return ok({ reset: true });
  }),
];
