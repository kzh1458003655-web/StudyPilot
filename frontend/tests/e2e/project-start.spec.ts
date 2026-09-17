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
  await page.route("**/api/v1/qa/history?projectId=42", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: { sessionId: null, turns: [] },
        requestId: "playwright-qa-history",
      }),
    });
  });

  await page.goto("/projects");
  await page.getByRole("button", { name: "新建课程" }).click();
  await page.getByLabel("课程名称").fill("操作系统期末复习");
  await page.getByRole("button", { name: "创建课程", exact: true }).click();

  await expect(page).toHaveURL(/\/projects\/42\/qa$/);
  await expect(page.getByRole("heading", { name: "知识问答" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "操作系统期末复习", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("tab", { name: "问答" })).toHaveAttribute(
    "data-state",
    "active",
  );
});

test("blocks archived course URLs and restores without opening the course", async ({
  page,
}) => {
  let archivedAt: string | null = "2026-09-01T06:30:00.000Z";
  await page.route("**/api/v1/projects*", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: [
          {
            id: 42,
            name: "已归档课程",
            description: "",
            createdAt: "2026-08-01T00:00:00Z",
            archivedAt,
          },
        ],
        requestId: "playwright-archived-project",
      }),
    });
  });
  await page.route("**/api/v1/projects/42/restore", async (route) => {
    archivedAt = null;
    await route.fulfill({ status: 204 });
  });
  await page.route("**/api/v1/documents?projectId=42", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ data: [], requestId: "documents" }),
    }),
  );
  await page.route("**/api/v1/qa/history?projectId=42", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        data: { sessionId: null, turns: [] },
        requestId: "history",
      }),
    }),
  );

  await page.goto("/projects/42/qa");
  await expect(page).toHaveURL(/\/projects\?archive=1$/);
  const archiveDialog = page.getByRole("dialog", { name: "归档课程" });
  await expect(archiveDialog.getByText("已归档课程")).toBeVisible();
  await archiveDialog.getByRole("button", { name: "恢复课程" }).click();

  await expect(page).toHaveURL(/\/projects\?archive=1$/);
  await archiveDialog.getByRole("button", { name: "关闭" }).click();
  await expect(
    page.getByRole("button", { name: "已归档课程", exact: true }),
  ).toBeVisible();
});
