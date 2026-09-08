import { http } from "@/shared/api/http";
import {
  generatedExamSchema,
  knowledgePointsSchema,
  pastPaperAnalysisSchema,
} from "../schemas/apiSchema";
import type {
  MockExam,
  PastPaperAnalysis,
  KnowledgePointFrequency,
} from "../types/domain";

export async function analyzePastPaper(
  projectId: number,
  documentId: number,
): Promise<PastPaperAnalysis> {
  const response = await http.post<unknown>(
    `/exams/past-papers/${documentId}/analysis`,
    undefined,
    { params: { projectId } },
  );
  return pastPaperAnalysisSchema.parse(response.data).data;
}
export async function getKnowledgePoints(
  projectId: number,
): Promise<KnowledgePointFrequency[]> {
  const response = await http.get<unknown>("/exams/knowledge-points", {
    params: { projectId },
  });
  return knowledgePointsSchema.parse(response.data).data;
}
export async function generateMockExam(projectId: number): Promise<number> {
  const response = await http.post<unknown>("/exams/generate", undefined, {
    params: { projectId },
  });
  return generatedExamSchema.parse(response.data).data.examId;
}
export async function getMockExam(
  projectId: number,
  examId: number,
): Promise<MockExam> {
  const response = await http.get<unknown>(`/exams/${examId}`, {
    params: { projectId },
  });
  return generatedExamSchema.parse(response.data).data;
}
