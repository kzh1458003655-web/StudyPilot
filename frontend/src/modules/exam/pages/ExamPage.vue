<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { toAppError } from "@/shared/api/http";
import {
  analyzePastPaper,
  generateMockExam,
  getKnowledgePoints,
} from "../api/requests";
import type {
  KnowledgePointFrequency,
  PastPaperAnalysis,
} from "../types/domain";

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));
const documentId = ref<number>();
const frequencies = ref<KnowledgePointFrequency[]>([]);
const analysis = ref<PastPaperAnalysis>();
const instructions = ref("");
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
    const id = await generateMockExam(
      projectId.value,
      instructions.value.trim(),
    );
    await router.push(`/projects/${projectId.value}/exams/${id}/take`);
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
        <p class="eyebrow">本课程 · 模拟考 Skill</p>
        <h1>生成一份模拟卷</h1>
      </div>
      <RouterLink :to="`/projects/${projectId}/qa`">课程问答</RouterLink>
    </div>
    <p class="subtle">
      直接描述你想练习的内容即可。已有课程资料会作为补充依据；没有资料时，模型也会根据课程主题和你的要求出题。
    </p>
    <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>

    <div class="exam-grid">
      <article class="exam-card">
        <h2>模拟出题</h2>
        <p>
          例如：“出一套操作系统期末模拟题，难度中等，侧重进程管理。”不填写也可以直接生成默认模拟卷。
        </p>
        <textarea
          v-model="instructions"
          maxlength="1000"
          placeholder="输入题量、难度、题型或知识范围；不填写也可以直接生成"
        />
        <button class="generate-exam" :disabled="generating" @click="generate">
          {{ generating ? "正在生成…" : "生成并开始作答" }}
        </button>
      </article>
      <article class="exam-card">
        <h2>考频分析</h2>
        <p>
          上传历年试卷后，可在这里输入资料编号进行考频统计；这一步不会影响模拟卷直接生成。
        </p>
        <div class="inline-form">
          <input
            v-model.number="documentId"
            min="1"
            type="number"
            placeholder="历年试卷资料编号"
          />
          <button :disabled="analyzing || !documentId" @click="analyze">
            {{ analyzing ? "分析中…" : "分析考频" }}
          </button>
        </div>
        <p v-if="analysis" class="success">
          识别 {{ analysis.detectedQuestions }} 题，归一
          {{ analysis.analyzedQuestions }} 题，待人工补充
          {{ analysis.needsReviewQuestions }} 题。
        </p>
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
  </section>
</template>
