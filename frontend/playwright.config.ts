import { defineConfig, devices } from "@playwright/test";

const outputRoot =
  process.env.STUDYPILOT_OUTPUT_ROOT ??
  "D:/大四课程设计/StudyPilot-output/playwright";
const externalBaseUrl = process.env.STUDYPILOT_E2E_BASE_URL;

export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: [
    ["list"],
    ["html", { outputFolder: `${outputRoot}/report`, open: "never" }],
    ["json", { outputFile: `${outputRoot}/results.json` }],
  ],
  outputDir: `${outputRoot}/artifacts`,
  use: {
    baseURL: externalBaseUrl ?? "http://127.0.0.1:5174",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: externalBaseUrl
    ? undefined
    : {
        command: "pnpm exec vite --host 127.0.0.1 --port 5174",
        url: "http://127.0.0.1:5174",
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
