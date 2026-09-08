import axios from "axios";
import type { AppError } from "./errors";
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api/v1",
  timeout: 10_000,
});
export function toAppError(error: unknown): AppError {
  if (axios.isCancel(error))
    return { kind: "cancelled", message: "请求已取消" };
  if (axios.isAxiosError(error))
    return {
      kind: error.code === "ECONNABORTED" ? "timeout" : "network",
      message: error.message,
      code: error.code,
      details: error.response?.data,
    };
  return { kind: "unknown", message: "发生未预期错误", details: error };
}
