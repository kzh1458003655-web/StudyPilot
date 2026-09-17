import { z } from "zod";

export const projectResponseSchema = z.object({
  data: z.object({
    id: z.number().int().positive(),
    name: z.string().min(1),
    description: z.string(),
    createdAt: z.string(),
    archivedAt: z.string().nullable(),
  }),
  requestId: z.string().min(1),
});

export const projectListResponseSchema = z.object({
  data: z.array(projectResponseSchema.shape.data),
  requestId: z.string().min(1),
});
