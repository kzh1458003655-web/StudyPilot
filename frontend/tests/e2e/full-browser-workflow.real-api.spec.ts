import { expect, test } from "@playwright/test";

const algorithmLecture =
  "D:/大四课程设计/StudyPilot-output/external-course-fixtures/algorithms-lecture-2020.pdf";
const algorithmPaper =
  "D:/大四课程设计/StudyPilot-output/external-course-fixtures/algorithms-final.pdf";

test("completes the browser workflow from course creation to assessment", async ({
  page,
}) => {
  test.setTimeout(180_000);
  const courseName = `浏览器全流程验收-${Date.now()}`;

  await page.goto("/projects");
  await page.getByRole("button", { name: "新建课程" }).click();
  await page.getByLabel("课程名称").fill(courseName);
  await page.getByRole("button", { name: "创建课程", exact: true }).click();
  await expect(page).toHaveURL(/\/projects\/\d+\/qa$/);

  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles(algorithmLecture);
  await page.getByRole("button", { name: "上传资料" }).click();
  await expect(page.getByText("已提取", { exact: false })).toBeVisible();
  await fileInput.setInputFiles(algorithmPaper);
  await page.getByRole("button", { name: "上传资料" }).click();
  await expect(page.getByText("algorithms-final.pdf")).toBeVisible();

  await page
    .getByPlaceholder("输入问题，或说说你想弄懂的知识点…")
    .fill("What is the running time of binary search?");
  await page.getByRole("button", { name: "发送问题 ↑" }).click();
  await expect(page.locator(".citation").first()).toBeVisible({
    timeout: 60_000,
  });

  await page.getByRole("link", { name: "考频分析" }).click();
  await expect(
    page.getByRole("heading", { name: "历年试卷考频" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "开始分析" }).click();
  // The fixture has ten explicit “Problem N.” headers; option numbers must not
  // inflate this count.
  await expect(page.locator(".success")).toContainText("识别 10 题", {
    timeout: 60_000,
  });

  await page.getByRole("link", { name: "生成模拟卷" }).click();
  await page
    .getByPlaceholder("输入题量、难度、题型或知识范围；不填写也可以直接生成")
    .fill("生成 3 道默认单选题");
  await page.getByRole("button", { name: "生成并开始作答" }).click();
  await expect(page).toHaveURL(/\/projects\/\d+\/exams\/\d+\/take$/, {
    timeout: 120_000,
  });
  await expect(
    page.getByText("作答记录已准备好", { exact: false }),
  ).toBeVisible();
  for (const questionCard of await page.locator("form .exam-card").all()) {
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
  await page.getByRole("button", { name: "提交并生成测评" }).click();
  await expect(page.getByText("得分", { exact: false })).toBeVisible({
    timeout: 120_000,
  });
});
