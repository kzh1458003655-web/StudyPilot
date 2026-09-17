import { z } from "zod";
export const architectureStatusSchema = z.object({
  data: z.object({
    status: z.string().min(1),
    databaseConfigured: z.boolean(),
    ai: z.object({
      service: z.string().min(1),
      modelReady: z.boolean(),
      chunks: z.number().int().nonnegative(),
      pending: z.number().int().nonnegative(),
      completed: z.number().int().nonnegative(),
    }),
  }),
  requestId: z.string().min(1),
});
