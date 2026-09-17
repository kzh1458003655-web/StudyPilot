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
export async function listProjects(
  includeArchived = false,
): Promise<StudyProject[]> {
  const response = await http.get<unknown>("/projects", {
    params: includeArchived ? { includeArchived: true } : undefined,
  });
  return projectListResponseSchema.parse(response.data).data;
}

/** Archives a course without deleting its materials or learning records. */
export async function archiveProject(projectId: number): Promise<void> {
  await http.post(`/projects/${projectId}/archive`);
}

/** Makes a previously archived course available in the workspace again. */
export async function restoreProject(projectId: number): Promise<void> {
  await http.post(`/projects/${projectId}/restore`);
}
