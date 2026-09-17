import { describe, expect, it, vi } from "vitest";
import { http } from "@/shared/api/http";
import { generateMockExam } from "./requests";

describe("generateMockExam", () => {
  it("allows enough time for local-model exam generation", async () => {
    const post = vi.spyOn(http, "post").mockResolvedValue({
      data: {
        data: { examId: 18, itemCount: 4 },
        requestId: "request-18",
      },
    });

    await expect(generateMockExam(6, "生成四道数据库题")).resolves.toBe(18);
    expect(post).toHaveBeenCalledWith(
      "/exams/generate",
      { instructions: "生成四道数据库题" },
      { params: { projectId: 6 }, timeout: 120_000 },
    );
  });
});
