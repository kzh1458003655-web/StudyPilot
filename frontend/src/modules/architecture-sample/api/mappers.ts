import type { ArchitectureStatusDto } from "../types/api";
import type { ArchitectureStatus } from "../types/domain";
export function mapArchitectureStatus(
  dto: ArchitectureStatusDto,
): ArchitectureStatus {
  return {
    status: dto.data.status,
    requestId: dto.requestId,
    databaseConfigured: dto.data.databaseConfigured,
    ai: dto.data.ai,
  };
}
