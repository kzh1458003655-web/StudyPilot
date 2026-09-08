import { z } from "zod";
export const architectureStatusSchema = z.object({
  status: z.string().min(1),
  request_id: z.string().min(1),
});
