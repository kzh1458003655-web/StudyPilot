import type { ArchitectureStatusDto } from "../types/api";
import type { ArchitectureStatus } from "../types/domain";
export function mapArchitectureStatus(
  dto: ArchitectureStatusDto,
): ArchitectureStatus {
  return { status: dto.status, requestId: dto.request_id };
}
