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
export async function generateMockExam(
  projectId: number,
  instructions: string,
): Promise<number> {
  const response = await http.post<unknown>(
    "/exams/generate",
    { instructions },
    {
      params: { projectId },
      // Local generation can take 20–60 seconds even with GPU acceleration.
      timeout: 120_000,
    },
  );
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
export async function listMockExams(
  projectId: number,
): Promise<Array<{ id: number; title: string; itemCount: number }>> {
  const response = await http.get<{
    data: Array<{ id: number; title: string; itemCount: number }>;
  }>("/exams", { params: { projectId } });
  return response.data.data;
}

export async function deleteMockExam(
  projectId: number,
  examId: number,
): Promise<void> {
  await http.delete(`/exams/${examId}`, { params: { projectId } });
}
