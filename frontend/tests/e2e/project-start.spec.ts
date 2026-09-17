import { expect, test } from "@playwright/test";

test("creates a project and enters its isolated Q&A workspace", async ({
  page,
}) => {
  await page.route("**/api/v1/projects*", async (route) => {
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
              archivedAt: null,
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
          archivedAt: null,
        },
        requestId: "playwright-project-start",
      }),
    });
  });
  await page.route("**/api/v1/documents?projectId=42", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ data: [], requestId: "playwright-document-list" }),
    });
  });

  await page.goto("/projects");
  await page.getByRole("button", { name: "新建课程" }).click();
  await page.getByLabel("课程名称").fill("操作系统期末复习");
  await page.getByRole("button", { name: "创建课程", exact: true }).click();

  await expect(page).toHaveURL(/\/projects\/42\/qa$/);
  await expect(page.getByRole("heading", { name: "知识问答" })).toBeVisible();
  await expect(page.getByText("本课程 · 可追溯回答")).toBeVisible();
  await expect(
    page.locator(".course-item", { hasText: "操作系统期末复习" }),
  ).toBeVisible();
});
