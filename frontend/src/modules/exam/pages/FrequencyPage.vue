<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import {
  listDocuments,
  type CourseDocument,
} from "@/modules/document/api/requests";
import { toAppError } from "@/shared/api/http";
import { analyzePastPaper, getKnowledgePoints } from "../api/requests";
import type {
  KnowledgePointFrequency,
  PastPaperAnalysis,
} from "../types/domain";

const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const papers = ref<CourseDocument[]>([]);
const frequencies = ref<KnowledgePointFrequency[]>([]);
const analysis = ref<PastPaperAnalysis>();
const analyzingId = ref<number>();
const error = ref("");

async function load() {
  const documents = await listDocuments(projectId.value);
  papers.value = documents.filter(
    (document) => document.documentType === "PAST_EXAM",
  );
  frequencies.value = await getKnowledgePoints(projectId.value);
}
async function analyze(documentId: number) {
  analyzingId.value = documentId;
  error.value = "";
  try {
    analysis.value = await analyzePastPaper(projectId.value, documentId);
    await load();
  } catch (reason) {
    error.value = toAppError(reason).message;
  } finally {
    analyzingId.value = undefined;
  }
}
onMounted(() =>
  load().catch((reason) => {
    error.value = toAppError(reason).message;
  }),
);
</script>
<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">本课程 · 考频分析 Skill</p>
        <h1>历年试卷考频</h1>
      </div>
      <RouterLink :to="`/projects/${projectId}/exams`">生成模拟卷</RouterLink>
    </div>
    <p class="subtle">
      系统会自动识别上传的试卷。选择下方试卷开始分析，无需填写编号。
    </p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <article class="exam-card">
      <h2>已识别的历年试卷</h2>
      <p v-if="!papers.length" class="subtle">
        尚未识别到试卷。请在课程问答右侧上传 PDF，系统会自动判断文件内容。
      </p>
      <div v-for="paper in papers" :key="paper.id" class="frequency-paper">
        <span
          ><strong>{{ paper.displayName }}</strong
          ><small
            >{{ paper.pageCount }} 页 ·
            {{ paper.status === "READY" ? "已就绪" : "处理中" }}</small
          ></span
        ><button
          :disabled="analyzingId === paper.id || paper.status !== 'READY'"
          @click="analyze(paper.id)"
        >
          {{ analyzingId === paper.id ? "分析中…" : "开始分析" }}
        </button>
      </div>
      <p v-if="analysis" class="success">
        识别 {{ analysis.detectedQuestions }} 题，归一
        {{ analysis.analyzedQuestions }} 题，待人工补充
        {{ analysis.needsReviewQuestions }} 题。
      </p>
    </article>
    <article class="exam-card">
      <h2>知识点考频</h2>
      <p v-if="!frequencies.length" class="subtle">
        完成试卷分析后，这里会显示知识点出现次数。
      </p>
      <ol v-else class="frequency-list">
        <li v-for="item in frequencies" :key="item.knowledgePoint">
          <span>{{ item.knowledgePoint }}</span
          ><strong>{{ item.questionCount }} 题</strong>
        </li>
      </ol>
    </article>
  </section>
</template>
