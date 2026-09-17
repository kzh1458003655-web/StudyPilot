export type MockDocumentType =
  "TEXTBOOK" | "LECTURE" | "KNOWLEDGE" | "PAST_EXAM" | "REFERENCE_ANSWER";

export type MockDocumentStatus = "PENDING" | "PROCESSING" | "READY" | "FAILED";

export interface MockDocumentRecord {
  id: number;
  indexId: string;
  displayName: string;
  documentType: MockDocumentType;
  status: MockDocumentStatus;
  pageCount: number;
  chunkCount: number;
}

export interface MockCitationRecord {
  documentName: string;
  pageNumber: number;
  excerpt: string;
  score: number;
}

export interface MockQaTurnRecord {
  question: string;
  status: "ANSWERED" | "INSUFFICIENT_EVIDENCE";
  answer: string;
  citations: MockCitationRecord[];
}

export interface MockQaSessionRecord {
  id: number;
  turns: MockQaTurnRecord[];
}

export interface MockKnowledgePointRecord {
  knowledgePoint: string;
  questionCount: number;
}

export interface MockPastPaperAnalysisRecord {
  documentId: number;
  detectedQuestions: number;
  analyzedQuestions: number;
  needsReviewQuestions: number;
}

export interface MockExamItemRecord {
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

export interface MockExamRecord {
  id: number;
  title: string;
  items: MockExamItemRecord[];
}

export interface MockAttemptAnswerRecord {
  itemId: number;
  answer: string;
  score: number;
  feedback: string;
  gradingStatus: "PENDING" | "RULE_GRADED" | "MODEL_GRADED" | "FAILED";
}

export interface MockAttemptRecord {
  id: number;
  examId: number;
  status: "IN_PROGRESS" | "SUBMITTED" | "GRADED";
  totalScore: number | null;
  answers: MockAttemptAnswerRecord[];
}

export type MockModuleName = "documents" | "qa" | "exams" | "assessment";

export interface MockProjectBehavior {
  latencyMs?: number;
  failingModules?: MockModuleName[];
}

export interface MockProjectRecord {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  archivedAt: string | null;
  documents: MockDocumentRecord[];
  qaSession: MockQaSessionRecord | null;
  knowledgePoints: MockKnowledgePointRecord[];
  analyses: MockPastPaperAnalysisRecord[];
  exams: MockExamRecord[];
  attempts: MockAttemptRecord[];
  behavior?: MockProjectBehavior;
}

export interface MockBackendState {
  nextProjectId: number;
  nextDocumentId: number;
  nextSessionId: number;
  nextExamId: number;
  nextExamItemId: number;
  nextAttemptId: number;
  projects: MockProjectRecord[];
}
