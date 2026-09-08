import { http } from "@/shared/api/http";
import { projectResponseSchema } from "../schemas/apiSchema";
import type { StudyProject } from "../types/domain";

export async function createProject(
  name: string,
  description: string,
): Promise<StudyProject> {
  const response = await http.post<unknown>("/projects", { name, description });
  return projectResponseSchema.parse(response.data).data;
}
