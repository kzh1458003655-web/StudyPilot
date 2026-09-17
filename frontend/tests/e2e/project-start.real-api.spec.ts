import { expect, test } from "@playwright/test";

const databasePaper =
  "D:/大四课程设计/StudyPilot/samples/数据库原理历年试卷测试样卷.pdf";

// This scenario is opt-in because it intentionally sends a request to a running local backend.
// It verifies the browser, Vite proxy, Spring MVC and PostgreSQL path together instead of
// replacing the API with a mocked response as the ordinary UI test does.
test.skip(
  process.env.STUDYPILOT_REAL_API !== "true",
  "Set STUDYPILOT_REAL_API=true after starting the local demo or target backend.",
);

test("creates a project through the running local API", async ({ page }) => {
  test.setTimeout(120_000);
  const projectName = `浏览器验收-${Date.now()}`;

  await page.goto("/projects");
  await page.getByRole("button", { name: "新建课程" }).click();
  await page.getByLabel("课程名称").fill(projectName);
  await page.getByRole("button", { name: "创建课程", exact: true }).click();

  await expect(page).toHaveURL(/\/projects\/\d+\/qa$/);
  await expect(page.getByRole("heading", { name: "知识问答" })).toBeVisible();
  await expect(page.getByRole("button", { name: projectName })).toBeVisible();

  await page.getByRole("button", { name: `课程操作：${projectName}` }).click();
  await page.getByRole("menuitem", { name: "归档课程" }).click();
  await page.getByRole("button", { name: "归档", exact: true }).click();
  await expect(page).toHaveURL(/\/projects$/);
  await page.getByRole("button", { name: /归档课程/ }).click();
  const archiveDialog = page.getByRole("dialog", { name: "归档课程" });
  await expect(archiveDialog.getByText(projectName)).toBeVisible();
  await archiveDialog.getByRole("button", { name: "恢复课程" }).click();
  await expect(page).toHaveURL(/\/projects$/);
  await page.getByRole("button", { name: projectName }).click();

  await page
    .getByPlaceholder("输入问题，Shift + Enter 换行")
    .fill("数据库事务的 ACID 分别指什么？");
  await page.getByRole("button", { name: "发送问题" }).click();
  await expect(page.getByText("数据库事务的 ACID 分别指什么？")).toBeVisible();

  await page
    .getByRole("button", { name: /课程资料/ })
    .first()
    .click();
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles(databasePaper);
  await page.getByRole("button", { name: "上传资料" }).click();
  await expect(page.getByText("数据库原理历年试卷测试样卷.pdf")).toBeVisible();

  await page
    .getByRole("button", { name: /资料操作：数据库原理历年试卷测试样卷/ })
    .click();
  await page.getByRole("menuitem", { name: "删除资料" }).click();
  await page.getByRole("button", { name: "删除", exact: true }).click();
  await expect(page.getByText("数据库原理历年试卷测试样卷.pdf")).toHaveCount(0);
});
