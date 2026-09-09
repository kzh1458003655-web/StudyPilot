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

const documentListResponseSchema = z.object({
  data: z.array(
    z.object({
      id: z.number().int().positive(),
      displayName: z.string().min(1),
      documentType: z.string(),
      status: z.string(),
      pageCount: z.number().int(),
      chunkCount: z.number().int(),
    }),
  ),
  requestId: z.string(),
});

export type CourseDocument = z.infer<
  typeof documentListResponseSchema
>["data"][number];

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

/** Reads only the material records belonging to the selected course. */
export async function listDocuments(
  projectId: number,
): Promise<CourseDocument[]> {
  const response = await http.get<unknown>("/documents", {
    params: { projectId },
  });
  return documentListResponseSchema.parse(response.data).data;
}

/** Removes the file and its local index from the active course. */
export async function deleteDocument(
  projectId: number,
  documentId: number,
): Promise<void> {
  await http.delete(`/documents/${documentId}`, { params: { projectId } });
}
