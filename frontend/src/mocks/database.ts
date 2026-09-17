import { createMockBackendSeed } from "./fixtures";
import type { MockBackendState, MockProjectRecord } from "./types";

const STORAGE_KEY = "studypilot.mock.database";

let memoryState: MockBackendState | undefined;

function storageAvailable(): boolean {
  return (
    typeof window !== "undefined" && typeof window.localStorage !== "undefined"
  );
}

function readStoredState(): MockBackendState | undefined {
  if (!storageAvailable()) return undefined;
  const serialized = window.localStorage.getItem(STORAGE_KEY);
  if (!serialized) return undefined;
  try {
    const parsed = JSON.parse(serialized) as MockBackendState;
    return Array.isArray(parsed.projects) ? parsed : undefined;
  } catch {
    return undefined;
  }
}

export function getMockBackendState(): MockBackendState {
  memoryState ??= readStoredState() ?? createMockBackendSeed();
  return memoryState;
}

export function saveMockBackendState(): void {
  if (storageAvailable()) {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(getMockBackendState()),
    );
  }
}

export function resetMockBackendState(): MockBackendState {
  memoryState = createMockBackendSeed();
  saveMockBackendState();
  return memoryState;
}

export function findMockProject(
  projectId: number,
): MockProjectRecord | undefined {
  return getMockBackendState().projects.find(
    (project) => project.id === projectId,
  );
}
