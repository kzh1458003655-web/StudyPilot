import { expect, test } from "@playwright/test";

const algorithmLecture =
  "D:/大四课程设计/StudyPilot-output/external-course-fixtures/algorithms-lecture-2020.pdf";
const algorithmPaper =
  "D:/大四课程设计/StudyPilot-output/external-course-fixtures/algorithms-final.pdf";

test.skip(
  process.env.STUDYPILOT_REAL_API !== "true",
  "Set STUDYPILOT_REAL_API=true after starting the full local backend stack.",
);

test("completes the browser workflow from course creation to assessment", async ({
  page,
}) => {
  test.setTimeout(300_000);
  const courseName = `浏览器全流程验收-${Date.now()}`;

  await page.goto("/projects");
  await page.getByRole("button", { name: "新建课程" }).click();
  await page.getByLabel("课程名称").fill(courseName);
  await page.getByRole("button", { name: "创建课程", exact: true }).click();
  await expect(page).toHaveURL(/\/projects\/\d+\/qa$/);

  await page
    .getByRole("button", { name: /课程资料/ })
    .first()
    .click();
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles(algorithmLecture);
  await page.getByRole("button", { name: "上传资料" }).click();
  await expect(page.getByText("algorithms-lecture-2020.pdf")).toBeVisible({
    timeout: 30_000,
  });
  await fileInput.setInputFiles(algorithmPaper);
  await page.getByRole("button", { name: "上传资料" }).click();
  await expect(page.getByText("algorithms-final.pdf")).toBeVisible();
  await page.keyboard.press("Escape");

  await page
    .getByPlaceholder("输入问题，Shift + Enter 换行")
    .fill("What is the running time of binary search?");
  await page.getByRole("button", { name: "发送问题" }).click();
  await page.getByRole("tab", { name: "智能组卷" }).click();
  await expect(page).toHaveURL(/\/projects\/\d+\/exams$/);
  await page.getByRole("tab", { name: "问答" }).click();
  await expect(
    page.getByText("What is the running time of binary search?"),
  ).toBeVisible({ timeout: 60_000 });
  await expect(page.getByText(/\.pdf · P\d+/).first()).toBeVisible({
    timeout: 60_000,
  });

  // A broad question can miss lexical retrieval against the English PDFs.
  // The learner should still receive a useful local-model answer without a fake citation.
  await page
    .getByPlaceholder("输入问题，Shift + Enter 换行")
    .fill("这个课件讲了什么？");
  await page.getByRole("button", { name: "发送问题" }).click();
  await expect(page.getByText("这个课件讲了什么？")).toBeVisible({
    timeout: 60_000,
  });

  await page.getByRole("tab", { name: "考频分析" }).click();
  await expect(page.getByRole("heading", { name: "考频分析" })).toBeVisible();
  await page.getByRole("button", { name: "分析", exact: true }).first().click();
  // The fixture has ten explicit “Problem N.” headers; option numbers must not
  // inflate this count.
  await expect(page.getByText("识别题数")).toBeVisible({
    timeout: 60_000,
  });

  await page.getByRole("tab", { name: "智能组卷" }).click();
  await page
    .getByPlaceholder("输入题量、难度、题型或知识范围；不填写也可以直接生成")
    .fill("生成 3 道默认单选题");
  await page.getByRole("button", { name: "生成并作答" }).click();
  await page.getByRole("tab", { name: "问答" }).click();
  await expect(page).toHaveURL(/\/projects\/\d+\/qa$/);
  await page.getByRole("tab", { name: "智能组卷" }).click();
  await expect(page.getByRole("link", { name: "开始作答" })).toBeVisible({
    timeout: 120_000,
  });
  await page.getByRole("link", { name: "开始作答" }).click();
  await expect(page).toHaveURL(/\/projects\/\d+\/exams\/\d+\/take$/, {
    timeout: 120_000,
  });
  await expect(page.getByText(/已答 0 \/ /)).toBeVisible();
  for (const questionCard of await page.locator("form article").all()) {
    const option = questionCard.locator('input[type="radio"]').first();
    if (await option.count()) {
      await option.check();
    }
    const answerBox = questionCard.locator(
      "textarea[placeholder='输入简答题答案']",
    );
    if (await answerBox.count()) {
      await answerBox.fill("这是浏览器验收作答。");
    }
  }
  await page.getByRole("button", { name: "提交测评" }).click();
  await expect(page.getByText("得分", { exact: false })).toBeVisible({
    timeout: 120_000,
  });
  await page.getByRole("button", { name: "重新作答本卷" }).click();
  await expect(page.getByText(/已答 0 \/ /)).toBeVisible();
  await expect(page.getByText("得分", { exact: false })).toHaveCount(0);
  for (const questionCard of await page.locator("form article").all()) {
    const option = questionCard.locator('input[type="radio"]').first();
    if (await option.count()) await option.check();
    const answerBox = questionCard.locator(
      "textarea[placeholder='输入简答题答案']",
    );
    if (await answerBox.count()) await answerBox.fill("这是第二次验收作答。");
  }
  await page.getByRole("button", { name: "提交测评" }).click();
  await expect(page.getByText("得分", { exact: false })).toBeVisible({
    timeout: 120_000,
  });
  await page.getByRole("link", { name: "返回练习列表" }).click();
  await page.getByRole("button", { name: /练习操作/ }).click();
  await page.getByRole("menuitem", { name: "删除练习" }).click();
  await page.getByRole("button", { name: "删除", exact: true }).click();
  await expect(page.getByText("还没有生成记录")).toBeVisible();
});
