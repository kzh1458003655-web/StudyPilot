import { expect, test } from "@playwright/test";

test("creates a project and enters its isolated Q&A workspace", async ({
  page,
}) => {
  await page.route("**/api/v1/projects", async (route) => {
    if (route.request().method() === "GET") {
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          data: [
            {
              id: 42,
              name: "操作系统期末复习",
              description: "",
              createdAt: "2026-09-09T00:00:00Z",
            },
          ],
          requestId: "playwright-project-list",
        }),
      });
      return;
    }
    if (route.request().method() !== "POST") return route.fallback();
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: {
          id: 42,
          name: "操作系统期末复习",
          description: "",
          createdAt: "2026-09-09T00:00:00Z",
        },
        requestId: "playwright-project-start",
      }),
    });
  });

  await page.goto("/projects");
  await page.getByLabel("项目名称").fill("操作系统期末复习");
  await page.getByRole("button", { name: "创建课程并进入问答" }).click();

  await expect(page).toHaveURL(/\/projects\/42\/qa$/);
  await expect(page.getByRole("heading", { name: "知识问答" })).toBeVisible();
  await expect(page.getByText("项目 42")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "操作系统期末复习" }),
  ).toBeVisible();
});
