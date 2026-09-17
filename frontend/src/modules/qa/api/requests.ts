import { http } from "@/shared/api/http";
import { qaAnswerSchema, qaHistorySchema } from "../schemas/apiSchema";
import type { QaAnswer, QaHistory } from "../types/domain";

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

export async function getQaHistory(projectId: number): Promise<QaHistory> {
  const response = await http.get<unknown>("/qa/history", {
    params: { projectId },
  });
  const history = qaHistorySchema.parse(response.data).data;
  return {
    sessionId: history.sessionId,
    turns: history.turns.map((turn) => ({
      question: turn.question,
      answer: {
        sessionId: history.sessionId ?? 0,
        status: "ANSWERED",
        answer: turn.answer,
        citations: turn.citations,
      },
    })),
  };
}
