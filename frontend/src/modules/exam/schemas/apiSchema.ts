import { z } from "zod";

const envelope = <T extends z.ZodTypeAny>(data: T) =>
  z.object({ data, requestId: z.string().min(1) });
export const pastPaperAnalysisSchema = envelope(
  z.object({
    detectedQuestions: z.number().int().nonnegative(),
    analyzedQuestions: z.number().int().nonnegative(),
    needsReviewQuestions: z.number().int().nonnegative(),
  }),
);
export const knowledgePointsSchema = envelope(
  z.array(
    z.object({
      knowledgePoint: z.string().min(1),
      questionCount: z.number().int().nonnegative(),
    }),
  ),
);
export const generatedExamSchema = envelope(
  z
    .object({
      examId: z.number().int().positive().optional(),
      itemCount: z.number().int().positive().optional(),
      id: z.number().int().positive().optional(),
      title: z.string().min(1).optional(),
      items: z
        .array(
          z.object({
            id: z.number().int().positive(),
            ordinal: z.number().int().positive(),
            type: z.enum(["SINGLE_CHOICE", "SHORT_ANSWER"]),
            prompt: z.string().min(1),
            // Short-answer items have no choices. The database represents that as
            // null, while the page can use one stable empty-array shape.
            options: z
              .array(z.string())
              .nullable()
              .transform((options) => options ?? []),
            answer: z.string(),
            analysis: z.string(),
            knowledgePoint: z.string(),
            score: z.number().positive(),
          }),
        )
        .optional(),
    })
    .transform((data) =>
      data.items
        ? {
            id: data.id!,
            title: data.title!,
            items: data.items,
          }
        : data,
    ),
);
