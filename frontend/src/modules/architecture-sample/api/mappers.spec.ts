import { describe, expect, it } from "vitest";
import { mapArchitectureStatus } from "./mappers";
describe("mapArchitectureStatus", () => {
  it("maps external snake case into the module model", () => {
    expect(mapArchitectureStatus({ status: "UP", request_id: "r-1" })).toEqual({
      status: "UP",
      requestId: "r-1",
    });
  });
});
