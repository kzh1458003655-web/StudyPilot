import { expect, test } from "@playwright/test";

// This scenario is opt-in because it intentionally sends a request to a running local backend.
// It verifies the browser, Vite proxy, Spring MVC and PostgreSQL path together instead of
// replacing the API with a mocked response as the ordinary UI test does.
test.skip(
  process.env.STUDYPILOT_REAL_API !== "true",
  "Set STUDYPILOT_REAL_API=true after starting the local demo or target backend.",
);

test("creates a project through the running local API", async ({ page }) => {
  const projectName = `浏览器验收-${Date.now()}`;

  await page.goto("/projects");
  await page.getByLabel("项目名称").fill(projectName);
  await page.getByRole("button", { name: "创建课程并进入问答" }).click();

  await expect(page).toHaveURL(/\/projects\/\d+\/qa$/);
  await expect(page.getByRole("heading", { name: "知识问答" })).toBeVisible();
  await expect(page.getByText(/项目 \d+/)).toBeVisible();
  await expect(page.getByRole("button", { name: projectName })).toBeVisible();
});
