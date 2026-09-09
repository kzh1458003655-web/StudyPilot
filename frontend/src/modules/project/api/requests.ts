import { http } from "@/shared/api/http";
import {
  projectListResponseSchema,
  projectResponseSchema,
} from "../schemas/apiSchema";
import type { StudyProject } from "../types/domain";

export async function createProject(
  name: string,
  description: string,
): Promise<StudyProject> {
  const response = await http.post<unknown>("/projects", { name, description });
  return projectResponseSchema.parse(response.data).data;
}

/** Returns the local courses shown in the workspace sidebar. */
export async function listProjects(): Promise<StudyProject[]> {
  const response = await http.get<unknown>("/projects");
  return projectListResponseSchema.parse(response.data).data;
}
