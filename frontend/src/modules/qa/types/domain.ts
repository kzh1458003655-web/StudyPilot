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
