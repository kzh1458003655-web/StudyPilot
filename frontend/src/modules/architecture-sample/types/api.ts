export interface ArchitectureStatusDto {
  data: {
    status: string;
    databaseConfigured: boolean;
    ai: {
      service: string;
      modelReady: boolean;
      chunks: number;
      pending: number;
      completed: number;
    };
  };
  requestId: string;
}
