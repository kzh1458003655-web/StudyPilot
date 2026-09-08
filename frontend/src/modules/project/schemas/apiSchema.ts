import { z } from "zod";

export const projectResponseSchema = z.object({
  data: z.object({
    id: z.number().int().positive(),
    name: z.string().min(1),
    description: z.string(),
    createdAt: z.string(),
  }),
  requestId: z.string().min(1),
});
