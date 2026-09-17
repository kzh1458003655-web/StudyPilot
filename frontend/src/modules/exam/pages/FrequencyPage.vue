<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import VChart from "vue-echarts";
import { BarChart3Icon, CheckIcon, FileSearchIcon } from "@lucide/vue";
import {
  listDocuments,
  type CourseDocument,
} from "@/modules/document/api/requests";
import { toAppError } from "@/shared/api/http";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { analyzePastPaper, getKnowledgePoints } from "../api/requests";
import type {
  KnowledgePointFrequency,
  PastPaperAnalysis,
} from "../types/domain";
import { createFrequencyChartOption, sortFrequencies } from "./frequencyChart";

use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const papers = ref<CourseDocument[]>([]);
const frequencies = ref<KnowledgePointFrequency[]>([]);
const analysis = ref<PastPaperAnalysis>();
const analyzingId = ref<number>();
const analysisStage = ref(0);
const loadError = ref("");
const analysisError = ref("");
let stageTimer: ReturnType<typeof setInterval> | undefined;

const stages = ["识别题目", "归一知识点", "汇总考频"];
const sortedFrequencies = computed(() => sortFrequencies(frequencies.value));
const processingPapers = computed(() =>
  papers.value.filter(
    (paper) => paper.status !== "READY" && paper.status !== "FAILED",
  ),
);
const chartOption = computed(() =>
  createFrequencyChartOption(frequencies.value),
);

async function load() {
  const targetProject = projectId.value;
  loadError.value = "";
  try {
    const [documents, points] = await Promise.all([
      listDocuments(targetProject),
      getKnowledgePoints(targetProject),
    ]);
    if (targetProject !== projectId.value) return;
    papers.value = documents.filter(
      (document) => document.documentType === "PAST_EXAM",
    );
    frequencies.value = points;
  } catch (reason) {
    loadError.value = toAppError(reason).message;
  }
}

async function analyze(documentId: number) {
  const targetProject = projectId.value;
  analyzingId.value = documentId;
  analysisStage.value = 0;
  analysisError.value = "";
  stageTimer = setInterval(() => {
    analysisStage.value = Math.min(analysisStage.value + 1, 2);
  }, 900);
  try {
    const result = await analyzePastPaper(targetProject, documentId);
    if (targetProject !== projectId.value) return;
    analysis.value = result;
    await load();
  } catch (reason) {
    analysisError.value = toAppError(reason).message;
  } finally {
    if (stageTimer) clearInterval(stageTimer);
    analyzingId.value = undefined;
  }
}

onMounted(load);
onUnmounted(() => {
  if (stageTimer) clearInterval(stageTimer);
});
watch(projectId, () => {
  papers.value = [];
  frequencies.value = [];
  analysis.value = undefined;
  loadError.value = "";
  analysisError.value = "";
  void load();
});
</script>

<template>
  <section class="mx-auto max-w-5xl px-4 py-9 sm:px-8 sm:py-12">
    <header class="mb-8">
      <h1 class="editorial-title text-3xl font-semibold sm:text-4xl">
        考频分析
      </h1>
    </header>

    <p
      v-if="loadError"
      class="mb-5 rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive"
      role="alert"
    >
      课程数据加载失败：{{ loadError }}
    </p>

    <section>
      <div class="mb-3 flex items-center justify-between">
        <h2 class="text-sm font-semibold">历年试卷</h2>
        <Badge variant="secondary">{{ papers.length }}</Badge>
      </div>
      <div
        v-if="!papers.length && !loadError"
        class="rounded-xl border border-dashed py-10 text-center text-sm text-muted-foreground"
      >
        尚未识别到历年试卷
      </div>
      <div v-else class="divide-y border-y">
        <article
          v-for="paper in papers"
          :key="paper.id"
          class="flex items-center gap-4 py-4"
        >
          <FileSearchIcon class="size-5 shrink-0 text-muted-foreground" />
          <div class="min-w-0 flex-1">
            <h3
              class="break-words font-sans text-sm font-medium tracking-normal"
            >
              {{ paper.displayName }}
            </h3>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ paper.pageCount }} 页 ·
              {{
                paper.status === "READY"
                  ? "可分析"
                  : paper.status === "FAILED"
                    ? "识别失败"
                    : "正在识别"
              }}
            </p>
          </div>
          <Button
            size="sm"
            variant="outline"
            :disabled="Boolean(analyzingId) || paper.status !== 'READY'"
            @click="analyze(paper.id)"
          >
            {{ analyzingId === paper.id ? "分析中" : "分析" }}
          </Button>
        </article>
      </div>
      <p
        v-if="processingPapers.length"
        class="mt-3 text-xs text-muted-foreground"
      >
        {{ processingPapers.length }} 份试卷仍在识别，完成后即可分析。
      </p>
    </section>

    <section
      v-if="analyzingId"
      class="mt-8 rounded-2xl border bg-card p-5"
      aria-live="polite"
    >
      <div class="flex flex-wrap gap-3">
        <div
          v-for="(stage, index) in stages"
          :key="stage"
          class="flex items-center gap-2 text-sm"
          :class="
            index <= analysisStage
              ? 'text-foreground'
              : 'text-muted-foreground/60'
          "
        >
          <span
            class="grid size-6 place-items-center rounded-full border"
            :class="
              index < analysisStage
                ? 'border-primary bg-primary text-primary-foreground'
                : index === analysisStage
                  ? 'animate-pulse border-primary text-primary'
                  : ''
            "
            ><CheckIcon v-if="index < analysisStage" class="size-3.5" /><span
              v-else
              >{{ index + 1 }}</span
            ></span
          >{{ stage }}
        </div>
      </div>
      <div class="mt-7 space-y-3">
        <Skeleton class="h-7 w-[82%]" /><Skeleton
          class="h-7 w-[64%]"
        /><Skeleton class="h-7 w-[48%]" /><Skeleton class="h-7 w-[31%]" />
      </div>
    </section>

    <p
      v-if="analysisError"
      class="mt-6 rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive"
      role="alert"
    >
      分析失败：{{ analysisError }}
    </p>

    <div v-if="analysis" class="mt-8 grid grid-cols-3 gap-3">
      <div class="rounded-xl border bg-card p-4">
        <strong class="block text-2xl">{{ analysis.detectedQuestions }}</strong
        ><span class="text-xs text-muted-foreground">识别题数</span>
      </div>
      <div class="rounded-xl border bg-card p-4">
        <strong class="block text-2xl">{{ analysis.analyzedQuestions }}</strong
        ><span class="text-xs text-muted-foreground">归一题数</span>
      </div>
      <div class="rounded-xl border bg-card p-4">
        <strong class="block text-2xl">{{
          analysis.needsReviewQuestions
        }}</strong
        ><span class="text-xs text-muted-foreground">待检查</span>
      </div>
    </div>

    <section class="mt-10">
      <div class="mb-4 flex items-center gap-2">
        <BarChart3Icon class="size-5 text-primary" />
        <h2 class="editorial-title text-xl font-semibold">知识点分布</h2>
      </div>
      <div
        v-if="!frequencies.length && !analyzingId"
        class="rounded-xl border border-dashed py-14 text-center text-sm text-muted-foreground"
      >
        完成试卷分析后显示考点分布
      </div>
      <div
        v-else-if="frequencies.length"
        class="rounded-2xl border bg-card p-3 sm:p-5"
      >
        <div class="frequency-chart h-[360px] w-full">
          <VChart
            :option="chartOption"
            autoresize
            aria-label="知识点考频横向柱状图"
          />
        </div>
        <ol class="sr-only">
          <li v-for="item in sortedFrequencies" :key="item.knowledgePoint">
            {{ item.knowledgePoint }}：{{ item.questionCount }} 题
          </li>
        </ol>
      </div>
    </section>
  </section>
</template>

<style scoped>
.frequency-chart :deep(.echarts-host) {
  width: 100%;
  height: 100%;
}
</style>
