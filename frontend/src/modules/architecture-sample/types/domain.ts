export interface ArchitectureStatus {
  status: string;
  requestId: string;
  databaseConfigured: boolean;
  ai: {
    service: string;
    modelReady: boolean;
    chunks: number;
    pending: number;
    completed: number;
  };
}
