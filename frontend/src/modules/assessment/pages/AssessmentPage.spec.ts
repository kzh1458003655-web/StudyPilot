import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AssessmentPage from "./AssessmentPage.vue";

const { getMockExam, startAttempt, submitAttempt } = vi.hoisted(() => ({
  getMockExam: vi.fn(),
  startAttempt: vi.fn(),
  submitAttempt: vi.fn(),
}));

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: { projectId: "7" }, query: { examId: "9" } }),
  useRouter: () => ({ push: vi.fn() }),
}));
vi.mock("@/modules/exam/api/requests", () => ({ getMockExam }));
vi.mock("../api/requests", () => ({ startAttempt, submitAttempt }));

describe("AssessmentPage", () => {
  beforeEach(() => {
    getMockExam.mockResolvedValue({
      id: 9,
      title: "操作系统模拟卷",
      items: [
        {
          id: 101,
          ordinal: 1,
          type: "SINGLE_CHOICE",
          prompt: "进程是什么？",
          options: ["执行中的程序", "静态文件"],
          answer: "A",
          analysis: "",
          knowledgePoint: "进程",
          score: 5,
        },
        {
          id: 102,
          ordinal: 2,
          type: "SHORT_ANSWER",
          prompt: "说明线程。",
          options: [],
          answer: "执行单位",
          analysis: "",
          knowledgePoint: "线程",
          score: 10,
        },
      ],
    });
    startAttempt.mockResolvedValue(23);
    submitAttempt.mockResolvedValue({
      totalScore: 12,
      maxScore: 15,
      answers: [
        { itemId: 101, score: 5, feedback: "正确" },
        { itemId: 102, score: 7, feedback: "定义基本完整" },
      ],
    });
  });

  it("loads a project exam, submits every answer, and displays the report", async () => {
    const wrapper = mount(AssessmentPage, {
      global: { stubs: { RouterLink: true } },
    });
    await flushPromises();
    expect(getMockExam).toHaveBeenCalledWith(7, 9);

    await wrapper.get("button").trigger("click");
    await wrapper.get('input[value="A"]').setValue();
    await wrapper.get("textarea").setValue("线程是进程中的执行单位");
    await wrapper.get("form").trigger("submit.prevent");
    await flushPromises();

    expect(submitAttempt).toHaveBeenCalledWith(7, 23, [
      { itemId: 101, answer: "A" },
      { itemId: 102, answer: "线程是进程中的执行单位" },
    ]);
    expect(wrapper.text()).toContain("得分 12 / 15");
    expect(wrapper.text()).toContain("定义基本完整");
  });
});
