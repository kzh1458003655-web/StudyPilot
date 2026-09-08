<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { toAppError } from "@/shared/api/http";
import {
  analyzePastPaper,
  generateMockExam,
  getKnowledgePoints,
  getMockExam,
} from "../api/requests";
import type {
  KnowledgePointFrequency,
  MockExam,
  PastPaperAnalysis,
} from "../types/domain";

const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const documentId = ref<number>();
const frequencies = ref<KnowledgePointFrequency[]>([]);
const analysis = ref<PastPaperAnalysis>();
const exam = ref<MockExam>();
const analyzing = ref(false);
const generating = ref(false);
const errorMessage = ref("");

async function refreshFrequencies() {
  if (!Number.isInteger(projectId.value) || projectId.value <= 0) return;
  frequencies.value = await getKnowledgePoints(projectId.value);
}
async function analyze() {
  if (!documentId.value || !Number.isInteger(documentId.value)) return;
  analyzing.value = true;
  errorMessage.value = "";
  try {
    analysis.value = await analyzePastPaper(projectId.value, documentId.value);
    await refreshFrequencies();
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  } finally {
    analyzing.value = false;
  }
}
async function generate() {
  generating.value = true;
  errorMessage.value = "";
  try {
    const id = await generateMockExam(projectId.value);
    exam.value = await getMockExam(projectId.value, id);
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  } finally {
    generating.value = false;
  }
}
onMounted(async () => {
  try {
    await refreshFrequencies();
  } catch {
    /* Empty or unavailable data is shown after an explicit action. */
  }
});
</script>

<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">项目 {{ projectId }}</p>
        <h1>真题分析与模拟考</h1>
      </div>
      <RouterLink :to="`/projects/${projectId}/resources`">管理资料</RouterLink>
    </div>
    <p class="subtle">
      先上传“历年真题”PDF。填写上传完成后显示的资料编号，系统会提取题目、归一知识点并统计考频；模拟卷仅依据已就绪的教材、讲义和知识点资料生成。
    </p>
    <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>

    <div class="exam-grid">
      <article class="exam-card">
        <h2>1. 分析历年真题</h2>
        <div class="inline-form">
          <input
            v-model.number="documentId"
            min="1"
            type="number"
            placeholder="历年真题资料编号"
          /><button :disabled="analyzing || !documentId" @click="analyze">
            {{ analyzing ? "分析中…" : "开始分析" }}
          </button>
        </div>
        <p v-if="analysis" class="success">
          识别 {{ analysis.detectedQuestions }} 题，归一
          {{ analysis.analyzedQuestions }} 题，待人工补充
          {{ analysis.needsReviewQuestions }} 题。
        </p>
      </article>
      <article class="exam-card">
        <h2>2. 生成模拟卷</h2>
        <p>
          固定生成 2 道单选题和 2
          道简答题。生成内容必须通过结构和资料来源校验，才会保存。
        </p>
        <button :disabled="generating" @click="generate">
          {{ generating ? "正在生成…" : "生成一份模拟卷" }}
        </button>
      </article>
    </div>

    <article class="exam-card">
      <h2>知识点考频</h2>
      <p v-if="!frequencies.length" class="subtle">尚无已分析的真题。</p>
      <ol v-else class="frequency-list">
        <li v-for="item in frequencies" :key="item.knowledgePoint">
          <span>{{ item.knowledgePoint }}</span
          ><strong>{{ item.questionCount }} 题</strong>
        </li>
      </ol>
    </article>
    <article v-if="exam" class="exam-card generated-paper">
      <h2>{{ exam.title }}</h2>
      <section v-for="item in exam.items" :key="item.id" class="question-card">
        <p>
          <strong>{{ item.ordinal }}. {{ item.prompt }}</strong
          >（{{ item.score }} 分）
        </p>
        <ol v-if="item.type === 'SINGLE_CHOICE'" type="A">
          <li v-for="option in item.options" :key="option">{{ option }}</li>
        </ol>
        <p class="subtle">知识点：{{ item.knowledgePoint }}</p>
        <RouterLink :to="`/projects/${projectId}/assessment?examId=${exam.id}`">
          开始在线作答
        </RouterLink>
        <details>
          <summary>展开参考答案与解析</summary>
          <p>答案：{{ item.answer }}</p>
          <p v-if="item.analysis">解析：{{ item.analysis }}</p>
        </details>
      </section>
    </article>
  </section>
</template>
