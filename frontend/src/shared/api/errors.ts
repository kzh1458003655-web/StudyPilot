export interface AppError {
  kind:
    "network" | "timeout" | "cancelled" | "validation" | "business" | "unknown";
  message: string;
  code?: string;
  details?: unknown;
}
