import { z } from "zod";

export const qaAnswerSchema = z.object({
  data: z.object({
    sessionId: z.number().int().positive(),
    status: z.enum(["ANSWERED", "INSUFFICIENT_EVIDENCE"]),
    answer: z.string(),
    citations: z.array(
      z.object({
        documentName: z.string().min(1),
        pageNumber: z.number().int().positive(),
        excerpt: z.string().min(1),
        score: z.number(),
      }),
    ),
  }),
  requestId: z.string().min(1),
});
