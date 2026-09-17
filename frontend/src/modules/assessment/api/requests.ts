import { http } from "@/shared/api/http";
export interface AttemptAnswer {
  itemId: number;
  answer: string;
}
export async function startAttempt(
  projectId: number,
  examId: number,
): Promise<number> {
  const r = await http.post<unknown>("/assessments/attempts", undefined, {
    params: { projectId, examId },
  });
  return (r.data as { data: { attemptId: number } }).data.attemptId;
}
export async function submitAttempt(
  projectId: number,
  attemptId: number,
  answers: AttemptAnswer[],
) {
  const r = await http.post<unknown>(
    `/assessments/attempts/${attemptId}/submit`,
    { answers },
    { params: { projectId } },
  );
  return (
    r.data as {
      data: {
        totalScore: number;
        maxScore: number;
        answers: Array<{ itemId: number; score: number; feedback: string }>;
      };
    }
  ).data;
}
