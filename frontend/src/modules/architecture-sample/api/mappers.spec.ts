import { describe, expect, it } from "vitest";
import { mapArchitectureStatus } from "./mappers";
describe("mapArchitectureStatus", () => {
  it("maps external snake case into the module model", () => {
    expect(
      mapArchitectureStatus({
        data: {
          status: "UP",
          databaseConfigured: false,
          ai: {
            service: "StudyPilot C++",
            modelReady: true,
            chunks: 3,
            pending: 0,
            completed: 2,
          },
        },
        requestId: "r-1",
      }),
    ).toEqual({
      status: "UP",
      requestId: "r-1",
      databaseConfigured: false,
      ai: {
        service: "StudyPilot C++",
        modelReady: true,
        chunks: 3,
        pending: 0,
        completed: 2,
      },
    });
  });
});
