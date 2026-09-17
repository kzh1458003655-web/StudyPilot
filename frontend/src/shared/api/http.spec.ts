import { describe, expect, it } from "vitest";
import { toAppError } from "./http";

describe("toAppError", () => {
  it("shows a server-provided validation message instead of Axios status text", () => {
    const error = {
      isAxiosError: true,
      message: "Request failed with status code 422",
      response: {
        data: {
          code: "VALIDATION_ERROR",
          message: "单个 PDF 文件不得超过 25 MB",
        },
      },
    };

    expect(toAppError(error).message).toBe("单个 PDF 文件不得超过 25 MB");
  });
});
