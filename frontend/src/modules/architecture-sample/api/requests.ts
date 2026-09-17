import { http } from "@/shared/api/http";
import { architectureStatusSchema } from "../schemas/apiSchema";
import { mapArchitectureStatus } from "./mappers";
import type { ArchitectureStatus } from "../types/domain";
export async function fetchArchitectureStatus(
  signal?: AbortSignal,
): Promise<ArchitectureStatus> {
  const response = await http.get<unknown>("/health/dependencies", { signal });
  return mapArchitectureStatus(architectureStatusSchema.parse(response.data));
}
