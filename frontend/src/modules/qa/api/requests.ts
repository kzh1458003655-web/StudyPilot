import { http } from "@/shared/api/http";
import { qaAnswerSchema } from "../schemas/apiSchema";
import type { QaAnswer } from "../types/domain";

export async function askQuestion(
  projectId: number,
  question: string,
  sessionId?: number,
): Promise<QaAnswer> {
  const response = await http.post<unknown>("/qa", {
    projectId,
    sessionId,
    question,
  });
  return qaAnswerSchema.parse(response.data).data;
}
