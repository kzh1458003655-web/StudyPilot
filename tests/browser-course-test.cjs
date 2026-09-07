/**
 * 真实浏览器验收：通过页面创建课程、上传 PDF、切换课程；不伪造接口或模型回答。
 * 运行：NODE_PATH 指向包含 playwright 的目录，node tests/browser-course-test.cjs
 * 可选：STUDYPILOT_UI_CHAT=1 测试真实模型问答；STUDYPILOT_UI_KEEP=1 保留测试课程。
 */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
let playwright;
try { playwright = require('playwright'); }
catch { playwright = require(path.join(process.env.USERPROFILE, '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright')); }
const { chromium } = playwright;
const base = process.env.STUDYPILOT_URL || 'http://127.0.0.1:18080';
const evidence = path.join(__dirname, 'ui-evidence');
fs.mkdirSync(evidence, { recursive: true });
const report = { started_at: new Date().toISOString(), base, mocked: false, checks: [], console_errors: [] };
const stamp = Date.now().toString().slice(-6);
const courseNames = [`数据库原理 · 验收${stamp}`, `计算机网络 · 验收${stamp}`];
const ownedCourses = [];
let browser, page, context;
async function check(name, fn) {
 const start = Date.now();
 try { await fn(); report.checks.push({ name, passed: true, elapsed_ms: Date.now()-start }); console.log('PASS', name); }
 catch (error) { report.checks.push({ name, passed: false, error: String(error), elapsed_ms: Date.now()-start }); throw error; }
}
async function screenshot(name) { await page.screenshot({ path: path.join(evidence, name+'.png'), fullPage: true }); }
async function waitCourse(name) {
 await page.locator('.course-item.selected').filter({ hasText: name }).waitFor();
 await page.locator('.course-loading').waitFor({ state:'hidden' });
}
(async () => {
 // 若没有 Playwright 自带浏览器，使用系统 Edge；这两个入口均不联网下载。
 try { browser = await chromium.launch({ headless: true }); }
 catch { browser = await chromium.launch({ headless: true, channel: 'msedge' }); }
 context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
 page = await context.newPage();
 page.setDefaultTimeout(15000);
 page.on('pageerror', e => report.console_errors.push(String(e)));
 await check('页面加载并显示课程空间', async () => {
  await page.goto(base, { waitUntil: 'networkidle' });
  await page.locator('#app:not([v-cloak]) .brand').waitFor();
  await page.getByRole('button', { name: '＋ 新建课程', exact: true }).waitFor();
 });
 await check('通过页面创建两门课程', async () => {
  for (const name of courseNames) {
   await page.getByRole('button', { name: '＋ 新建课程', exact:true }).click();
   await page.getByLabel('课程名称', { exact:true }).fill(name);
   await page.getByRole('button', { name: '创建课程', exact:true }).click();
   await waitCourse(name);
  }
  const response = await context.request.get(base+'/api/courses');
  assert.equal(response.status(), 200);
  const courses = await response.json();
  ownedCourses.push(...courses.filter(c => courseNames.includes(c.name)).map(c=>c.id));
  assert.equal(ownedCourses.length, 2);
 });
 await check('课程 A 上传 PDF，课程 B 文件保持为空', async () => {
  await page.locator('.course-item').filter({ hasText: courseNames[0] }).click(); await waitCourse(courseNames[0]);
  await page.locator('input[type=file]').setInputFiles(path.join(__dirname,'../samples/数据库课程示例讲义.pdf'));
  await page.locator('.document-info a').filter({hasText:'数据库课程示例讲义.pdf'}).waitFor({timeout:30000});
  await screenshot('01-course-a-pdf');
  await page.locator('.course-item').filter({ hasText: courseNames[1] }).click(); await waitCourse(courseNames[1]);
  assert.equal(await page.locator('.document').count(),0);
  assert.match(await page.locator('.reference-hint').innerText(),/没有资料/);
  await screenshot('02-course-b-isolated');
 });
 await check('会话按课程隔离且切换后恢复', async () => {
  await page.getByRole('button',{name:'＋ 新对话',exact:true}).click();
  await page.locator('.session-list button').first().waitFor();
  assert.equal(await page.locator('.session-list button').count(),1);
  await page.locator('.course-item').filter({hasText:courseNames[0]}).click(); await waitCourse(courseNames[0]);
  assert.equal(await page.locator('.session-list button').count(),0);
  assert.equal(await page.locator('.document').count(),1);
  await page.locator('.course-item').filter({hasText:courseNames[1]}).click(); await waitCourse(courseNames[1]);
  assert.equal(await page.locator('.session-list button').count(),1);
 });
 await check('参考开关与随机鼓励语显示正确', async () => {
  const quote = await page.locator('.study-quote').innerText(); assert.ok(quote.length>10);
  await page.getByLabel('参考课程资料',{exact:true}).uncheck();
  assert.match(await page.locator('.reference-hint').innerText(),/通用问答/);
  await page.locator('.course-item').filter({hasText:courseNames[0]}).click(); await waitCourse(courseNames[0]);
  assert.notEqual(await page.locator('.study-quote').innerText(),quote);
  await page.getByLabel('参考课程资料',{exact:true}).check();
 });
 if (process.env.STUDYPILOT_UI_CHAT === '1') {
  await check('无文件课程真实模型问答', async () => {
   await page.locator('.course-item').filter({hasText:courseNames[1]}).click(); await waitCourse(courseNames[1]);
   await page.getByLabel('输入课程问题').fill('请用两句话解释什么是计算机网络。');
   await page.getByRole('button',{name:'发送问题 ↑',exact:true}).click();
   await page.getByRole('button',{name:'停止生成',exact:true}).waitFor();
   await page.getByRole('button',{name:'发送问题 ↑',exact:true}).waitFor({timeout:240000});
   const answer = await page.locator('.message.assistant .message-text').last().innerText();
   assert.ok(answer.length>15 && !/本次没有生成内容/.test(answer));
   assert.ok(!/生成失败/.test(await page.locator('.message.assistant .message-label').last().innerText()));
   report.general_answer=answer; await screenshot('03-general-answer');
  });
 }
 await check('390px 窄屏仍可使用课程和资料入口', async () => {
  await page.setViewportSize({width:390,height:844});
  await page.getByRole('button',{name:'＋ 新建课程',exact:true}).waitFor();
  const sidebar = await page.locator('.sidebar').boundingBox(); assert.ok(sidebar.width<=390);
  const main = await page.locator('main').boundingBox(); assert.ok(main.x>=0 && main.width<=390);
  await screenshot('04-mobile-390');
 });
 await check('没有浏览器未捕获异常', async()=>assert.deepEqual(report.console_errors,[]));
})().catch(async error => { console.error(error); process.exitCode=1; if(page) await screenshot('failure').catch(()=>{}); }).finally(async()=>{
 // 只清理本次脚本创建的课程；不触及用户原有课程或资料。
 if(context && process.env.STUDYPILOT_UI_KEEP!=='1') for(const id of ownedCourses) {
  const r = await context.request.delete(base+'/api/courses/'+id).catch(()=>null);
  if(!r?.ok()) report.cleanup_warning = '部分测试课程未清理，请按验收后缀检查';
 }
 report.finished_at=new Date().toISOString();
 fs.writeFileSync(path.join(evidence,'browser-results.json'),JSON.stringify(report,null,2));
 await browser?.close();
});
