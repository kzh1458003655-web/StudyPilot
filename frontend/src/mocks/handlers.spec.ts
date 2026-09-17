import { setupServer } from "msw/node";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { projectListResponseSchema } from "@/modules/project/schemas/apiSchema";
import {
  generatedExamSchema,
  knowledgePointsSchema,
} from "@/modules/exam/schemas/apiSchema";
import { resetMockBackendState } from "./database";
import { mockHandlers } from "./handlers";

const server = setupServer(...mockHandlers);
const api = (path: string) => `http://localhost/api/v1${path}`;

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
beforeEach(() => resetMockBackendState());
afterAll(() => server.close());

describe("frontend mock backend", () => {
  it("returns seeded projects through the production response contract", async () => {
    const response = await fetch(api("/projects"));
    const payload = projectListResponseSchema.parse(await response.json());

    expect(payload.data).toHaveLength(7);
    expect(payload.data.some((project) => project.archivedAt !== null)).toBe(
      false,
    );

    const archivedResponse = await fetch(api("/projects?includeArchived=true"));
    const archivedPayload = projectListResponseSchema.parse(
      await archivedResponse.json(),
    );
    expect(archivedPayload.data).toHaveLength(8);
    expect(archivedPayload.data.at(-1)?.id).toBe(108);
  });

  it("keeps exams and frequency data isolated by project", async () => {
    const examResponse = await fetch(api("/exams/601?projectId=105"));
    expect(examResponse.status).toBe(200);
    const exam = generatedExamSchema.parse(await examResponse.json());
    expect("items" in exam.data && exam.data.items).toHaveLength(4);

    const crossProjectResponse = await fetch(api("/exams/601?projectId=104"));
    expect(crossProjectResponse.status).toBe(404);

    const frequencyResponse = await fetch(
      api("/exams/knowledge-points?projectId=104"),
    );
    const frequency = knowledgePointsSchema.parse(
      await frequencyResponse.json(),
    );
    expect(frequency.data).toHaveLength(8);
  });

  it("persists mutations through the same API routes", async () => {
    const createResponse = await fetch(api("/projects"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: "样式实验项目",
        description: "临时视觉检查",
      }),
    });
    expect(createResponse.status).toBe(201);

    const created = (await createResponse.json()) as {
      data: { id: number };
    };
    const listResponse = await fetch(api("/projects"));
    const list = projectListResponseSchema.parse(await listResponse.json());
    expect(list.data[0]?.id).toBe(created.data.id);
  });

  it("supports a complete exam attempt and deterministic grading", async () => {
    const startResponse = await fetch(
      api("/assessments/attempts?projectId=105&examId=601"),
      { method: "POST" },
    );
    const started = (await startResponse.json()) as {
      data: { attemptId: number };
    };

    const submitResponse = await fetch(
      api(
        `/assessments/attempts/${started.data.attemptId}/submit?projectId=105`,
      ),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          answers: [
            { itemId: 7001, answer: "B" },
            { itemId: 7002, answer: "A" },
            {
              itemId: 7003,
              answer:
                "B+树内部节点只存键且叶子有序相连，定位范围起点后可以顺序扫描。",
            },
            { itemId: 7004, answer: "" },
          ],
        }),
      },
    );
    const report = (await submitResponse.json()) as {
      data: { totalScore: number; maxScore: number; answers: unknown[] };
    };
    expect(report.data).toMatchObject({ maxScore: 100 });
    expect(report.data.totalScore).toBeGreaterThan(0);
    expect(report.data.totalScore).toBeLessThan(100);
    expect(report.data.answers).toHaveLength(4);
  });

  it("provides an intentional module failure for error-state styling", async () => {
    const response = await fetch(api("/exams?projectId=107"));
    const payload = (await response.json()) as {
      code: string;
      message: string;
    };

    expect(response.status).toBe(503);
    expect(payload.code).toBe("MOCK_MODULE_UNAVAILABLE");
    expect(payload.message).toContain("预设");
  });
});
