export interface QaCitation {
  documentName: string;
  pageNumber: number;
  excerpt: string;
  score: number;
}

export interface QaAnswer {
  sessionId: number;
  status: "ANSWERED" | "INSUFFICIENT_EVIDENCE";
  answer: string;
  citations: QaCitation[];
}

export interface QaHistory {
  sessionId: number | null;
  turns: Array<{ question: string; answer: QaAnswer }>;
}
