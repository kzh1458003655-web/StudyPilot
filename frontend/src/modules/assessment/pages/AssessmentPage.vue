<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { toAppError } from "@/shared/api/http";
import { getMockExam } from "@/modules/exam/api/requests";
import type { MockExam } from "@/modules/exam/types/domain";
import { startAttempt, submitAttempt } from "../api/requests";
const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));
const examId = computed(() =>
  Number(route.params.examId ?? route.query.examId),
);
const exam = ref<MockExam>();
const attemptId = ref<number>();
const answers = ref<Record<number, string>>({});
const submitting = ref(false);
const error = ref("");
const result = ref<{
  totalScore: number;
  maxScore: number;
  answers: Array<{ itemId: number; score: number; feedback: string }>;
}>();
async function load() {
  if (!Number.isInteger(examId.value) || examId.value <= 0) {
    error.value = "请从智能组卷页面进入作答。";
    return;
  }
  try {
    exam.value = await getMockExam(projectId.value, examId.value);
    // 作答页打开即创建一次作答记录，用户可以直接选择答案。
    await start();
  } catch (e) {
    error.value = toAppError(e).message;
  }
}
async function start() {
  try {
    error.value = "";
    attemptId.value = await startAttempt(projectId.value, examId.value);
  } catch (e) {
    error.value = toAppError(e).message;
  }
}
async function submit() {
  if (!attemptId.value || !exam.value) return;
  submitting.value = true;
  try {
    result.value = await submitAttempt(
      projectId.value,
      attemptId.value,
      exam.value.items.map((i) => ({
        itemId: i.id,
        answer: answers.value[i.id] ?? "",
      })),
    );
  } catch (e) {
    error.value = toAppError(e).message;
  } finally {
    submitting.value = false;
  }
}
async function askAboutResult() {
  if (!result.value) return;
  await router.push({
    path: `/projects/${projectId.value}/qa`,
    query: { examId: String(examId.value), followUp: "assessment" },
  });
}
function optionText(option: string, index: number) {
  const letter = String.fromCharCode(65 + index);
  return option
    .replace(new RegExp(`^\\s*${letter}[.、)）]\\s*`, "i"), "")
    .trim();
}
onMounted(load);
</script>
<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">本课程 · 作答评测</p>
        <h1>在线作答与测评</h1>
      </div>
      <RouterLink :to="`/projects/${projectId}/exams`">返回智能组卷</RouterLink>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="exam">
      <article class="exam-card">
        <h2>{{ exam.title }}</h2>
        <p class="subtle">作答记录已准备好，选择答案后可以直接提交测评。</p>
      </article>
      <form class="form-stack" @submit.prevent="submit">
        <article v-for="item in exam.items" :key="item.id" class="exam-card">
          <p>
            <strong>{{ item.ordinal }}. {{ item.prompt }}</strong
            >（{{ item.score }} 分）
          </p>
          <div v-if="item.type === 'SINGLE_CHOICE'" class="exam-options">
            <label
              v-for="(option, index) in item.options"
              :key="option"
              class="exam-option"
            >
              <input
                v-model="answers[item.id]"
                type="radio"
                :name="`item-${item.id}`"
                :value="String.fromCharCode(65 + index)"
              />
              <span
                >{{ String.fromCharCode(65 + index) }}.
                {{ optionText(option, index) }}</span
              >
            </label>
          </div>
          <textarea
            v-else
            v-model="answers[item.id]"
            placeholder="输入简答题答案"
          />
          <p class="subtle">知识点：{{ item.knowledgePoint }}</p>
        </article>
        <button type="submit" :disabled="!attemptId || submitting">
          {{ submitting ? "正在测评…" : "提交并生成测评" }}
        </button>
      </form>
    </template>
    <article v-if="result" class="exam-card">
      <h2>得分 {{ result.totalScore }} / {{ result.maxScore }}</h2>
      <p v-for="item in result.answers" :key="item.itemId">
        第 {{ item.itemId }} 题：{{ item.score }} 分。{{ item.feedback }}
      </p>
      <button type="button" @click="askAboutResult">
        根据本次结果继续提问
      </button>
    </article>
  </section>
</template>
