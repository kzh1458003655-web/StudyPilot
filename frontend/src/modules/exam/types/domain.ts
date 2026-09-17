export interface PastPaperAnalysis {
  detectedQuestions: number;
  analyzedQuestions: number;
  needsReviewQuestions: number;
}
export interface KnowledgePointFrequency {
  knowledgePoint: string;
  questionCount: number;
}
export interface MockExamItem {
  id: number;
  ordinal: number;
  type: "SINGLE_CHOICE" | "SHORT_ANSWER";
  prompt: string;
  options: string[];
  answer: string;
  analysis: string;
  knowledgePoint: string;
  score: number;
}
export interface MockExam {
  id: number;
  title: string;
  items: MockExamItem[];
}
