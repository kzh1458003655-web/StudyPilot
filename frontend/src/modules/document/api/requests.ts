import { http } from "@/shared/api/http";
import { z } from "zod";

const importResponseSchema = z.object({
  data: z.object({
    id: z.number().int().positive(),
    indexId: z.string(),
    pageCount: z.number().int(),
    chunkCount: z.number().int(),
  }),
  requestId: z.string(),
});

export async function uploadDocument(
  projectId: number,
  documentType: string,
  file: File,
) {
  const body = new FormData();
  body.append("projectId", String(projectId));
  body.append("documentType", documentType);
  body.append("file", file);
  const response = await http.post<unknown>("/documents", body);
  return importResponseSchema.parse(response.data).data;
}
