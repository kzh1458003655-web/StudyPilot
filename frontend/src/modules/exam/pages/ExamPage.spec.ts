import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { createPinia } from "pinia";
import { describe, expect, it, vi } from "vitest";
import ExamPage from "./ExamPage.vue";

const { generateMockExam } = vi.hoisted(() => ({ generateMockExam: vi.fn() }));
vi.mock("../api/requests", () => ({
  analyzePastPaper: vi.fn(),
  generateMockExam,
  deleteMockExam: vi.fn(),
  getKnowledgePoints: vi.fn().mockResolvedValue([]),
  getMockExam: vi.fn(),
  listMockExams: vi.fn().mockResolvedValue([]),
}));

describe("ExamPage", () => {
  it("lets a learner generate an exam without uploading a reference paper", async () => {
    generateMockExam.mockResolvedValue(18);
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: "/projects/:projectId/exams", component: ExamPage },
        {
          path: "/projects/:projectId/exams/:examId/take",
          component: { template: "<p>答题页</p>" },
        },
      ],
    });
    await router.push("/projects/6/exams");
    await router.isReady();
    const wrapper = mount(ExamPage, {
      global: { plugins: [createPinia(), router] },
    });

    expect(wrapper.get("textarea").attributes("placeholder")).toContain(
      "不填写也可以直接生成",
    );
    await wrapper.get("button.generate-exam").trigger("click");
    await flushPromises();

    expect(generateMockExam).toHaveBeenCalledWith(6, "");
    expect(router.currentRoute.value.path).toBe("/projects/6/exams/18/take");
  });
});
