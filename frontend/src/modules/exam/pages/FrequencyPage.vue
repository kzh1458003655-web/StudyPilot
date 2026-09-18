<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { BarChart, GraphChart, RadarChart, ScatterChart } from "echarts/charts";
import {
  GridComponent,
  RadarComponent,
  TooltipComponent,
} from "echarts/components";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import VChart from "vue-echarts";
import {
  BarChart3Icon,
  FileSearchIcon,
  NetworkIcon,
  RefreshCwIcon,
  ScanLineIcon,
} from "@lucide/vue";
import type { CourseDocument } from "@/modules/document/api/requests";
import { Shimmer } from "@/shared/shimmer";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Progress } from "@/shared/ui/progress";
import {
  EXAM_TOPIC_INSIGHTS,
  createFrequencyQuadrantOption,
  createFrequencyRelationOption,
  createPriorityRadarOption,
  createScoreFrequencyChartOption,
} from "./frequencyChart";

use([
  BarChart,
  GraphChart,
  RadarChart,
  ScatterChart,
  GridComponent,
  RadarComponent,
  TooltipComponent,
  CanvasRenderer,
]);

const ANALYSIS_DURATION = 8_000;
const FIXED_ANALYSIS_PAPERS: CourseDocument[] = [
  {
    id: 401,
    displayName: "2012—2013 学年中文卷（含答案）.pdf",
    documentType: "PAST_EXAM",
    status: "READY",
    pageCount: 6,
    chunkCount: 21,
  },
  {
    id: 402,
    displayName: "2012—2013 学年双语期中 A 卷（含答案）.pdf",
    documentType: "PAST_EXAM",
    status: "READY",
    pageCount: 10,
    chunkCount: 31,
  },
  {
    id: 403,
    displayName: "2014—2015 学年试题（OCR 版）.pdf",
    documentType: "PAST_EXAM",
    status: "READY",
    pageCount: 16,
    chunkCount: 15,
  },
];
const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const papers = FIXED_ANALYSIS_PAPERS;
const isAnalyzing = ref(false);
const showResults = ref(false);
const analysisElapsed = ref(0);
let progressTimer: ReturnType<typeof setInterval> | undefined;

const analysisStages = [
  { title: "读取资料", start: 0, end: 1_500 },
  { title: "整理题目", start: 1_500, end: 3_500 },
  { title: "汇总考频", start: 3_500, end: 5_800 },
  { title: "生成图表", start: 5_800, end: 7_500 },
  { title: "分析完成", start: 7_500, end: 8_000 },
] as const;

const readyPapers = computed(() =>
  papers.filter((paper) => paper.status === "READY"),
);
const analysisPapers = computed(() => readyPapers.value.slice(0, 3));
const analysisPaperCount = computed(() => analysisPapers.value.length);
const progress = computed(() =>
  Math.min(100, Math.round((analysisElapsed.value / ANALYSIS_DURATION) * 100)),
);
const currentStageIndex = computed(() => {
  const index = analysisStages.findIndex(
    (stage) =>
      analysisElapsed.value >= stage.start && analysisElapsed.value < stage.end,
  );
  return index === -1 ? analysisStages.length - 1 : index;
});
const currentStage = computed(
  () => analysisStages[currentStageIndex.value] ?? analysisStages[0],
);
const revealedPaperCount = computed(() =>
  analysisPaperCount.value === 0
    ? 0
    : Math.min(
        analysisPaperCount.value,
        Math.max(1, Math.ceil(analysisElapsed.value / 500)),
      ),
);
const analysisDetail = computed(() => {
  if (currentStageIndex.value === 0) {
    return `已读取 ${revealedPaperCount.value}/${analysisPaperCount.value} 份试卷`;
  }
  if (currentStageIndex.value === 1) {
    const stageProgress = Math.min(1, (analysisElapsed.value - 1_500) / 2_000);
    return `已扫描 ${Math.max(1, Math.round(stageProgress * 32))} 页 · 正在归并考点标签`;
  }
  if (currentStageIndex.value === 2) {
    const index = Math.min(
      2,
      Math.floor(((analysisElapsed.value - 3_500) / 2_300) * 3),
    );
    return ["关系规范化 36.0%", "关系代数 21.7%", "E-R 建模 13.7%"][index];
  }
  if (currentStageIndex.value === 3) {
    return "正在生成 4 项核心可视化";
  }
  return `${analysisPaperCount.value}套有效试卷 · 8类考点 · 3个高频模块`;
});

const scoreFrequencyOption = createScoreFrequencyChartOption();
const priorityRadarOption = createPriorityRadarOption();
const quadrantOption = createFrequencyQuadrantOption();
const relationOption = createFrequencyRelationOption();

function wait(duration: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, duration);
  });
}

function paperShortName(name: string, index: number) {
  if (name.includes("双语")) return "双语 A 卷";
  if (name.includes("2014") || name.includes("2015")) return "2014—2015 卷";
  if (name.includes("中文")) return "中文卷";
  return `试卷 ${index + 1}`;
}

async function runAnalysis() {
  const targetProject = projectId.value;
  if (!analysisPapers.value.length || isAnalyzing.value) return;

  isAnalyzing.value = true;
  showResults.value = false;
  analysisElapsed.value = 0;
  const startedAt = Date.now();
  progressTimer = setInterval(() => {
    analysisElapsed.value = Math.min(ANALYSIS_DURATION, Date.now() - startedAt);
  }, 100);

  try {
    await wait(ANALYSIS_DURATION);
    if (targetProject !== projectId.value) return;
    analysisElapsed.value = ANALYSIS_DURATION;
    showResults.value = true;
  } finally {
    if (progressTimer) clearInterval(progressTimer);
    progressTimer = undefined;
    isAnalyzing.value = false;
  }
}

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer);
});
watch(projectId, () => {
  if (progressTimer) clearInterval(progressTimer);
  progressTimer = undefined;
  isAnalyzing.value = false;
  showResults.value = false;
  analysisElapsed.value = 0;
});
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 py-9 sm:px-8 sm:py-12">
    <header
      class="mb-8 flex flex-col gap-4 border-b border-border/80 pb-7 sm:flex-row sm:items-end sm:justify-between"
    >
      <div>
        <h1 class="editorial-title text-3xl font-semibold sm:text-4xl">
          考频分析
        </h1>
      </div>
      <Button
        :disabled="isAnalyzing || readyPapers.length === 0"
        class="self-start sm:self-auto"
        @click="runAnalysis"
      >
        <RefreshCwIcon
          class="size-4"
          :class="isAnalyzing ? 'animate-spin' : ''"
        />
        {{ showResults ? "重新分析" : "开始分析" }}
      </Button>
    </header>

    <section aria-labelledby="paper-heading">
      <div class="mb-3 flex items-center justify-between">
        <h2 id="paper-heading" class="text-sm font-semibold">分析资料</h2>
        <Badge variant="secondary">{{ papers.length }} 份</Badge>
      </div>
      <div class="grid gap-3 md:grid-cols-3">
        <article
          v-for="paper in papers.slice(0, 3)"
          :key="paper.id"
          class="flex min-w-0 items-center gap-3 rounded-xl border bg-card px-4 py-3"
        >
          <span
            class="grid size-9 shrink-0 place-items-center rounded-lg bg-secondary text-muted-foreground"
          >
            <FileSearchIcon class="size-4" />
          </span>
          <div class="min-w-0">
            <h3 class="truncate text-sm font-medium">
              {{ paper.displayName }}
            </h3>
            <p class="mt-0.5 text-xs text-muted-foreground">
              {{ paper.pageCount }} 页 ·
              {{ paper.status === "READY" ? "可分析" : "正在识别" }}
            </p>
          </div>
        </article>
      </div>
    </section>

    <section
      v-if="isAnalyzing"
      class="analysis-panel mt-7 overflow-hidden rounded-2xl border bg-card px-4 py-5 sm:px-7 sm:py-6"
      aria-live="polite"
    >
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="flex items-center gap-2 text-sm font-semibold">
            <ScanLineIcon class="size-4 text-primary" />
            正在分析试卷
          </div>
          <p class="mt-1 text-xs text-muted-foreground">
            统一题目、考点与分值口径
          </p>
        </div>
        <span class="font-mono text-xs text-muted-foreground">
          {{ Math.min(8, Math.floor(analysisElapsed / 1000)) }}s
        </span>
      </div>

      <div class="mt-3 overflow-hidden" aria-hidden="true">
        <svg class="analysis-graph w-full" viewBox="0 0 760 260" role="img">
          <path
            v-for="y in [55, 130, 205].slice(0, analysisPaperCount)"
            :key="`source-edge-${y}`"
            :d="`M 164 ${y} C 194 ${y}, 202 130, 232 130`"
            class="graph-edge"
            :class="
              revealedPaperCount >= [55, 130, 205].indexOf(y) + 1
                ? 'is-done'
                : ''
            "
          />
          <path
            v-for="(edge, index) in [
              [348, 368],
              [436, 472],
              [540, 576],
              [644, 680],
            ]"
            :key="`stage-edge-${edge[0]}`"
            :d="`M ${edge[0]} 130 L ${edge[1]} 130`"
            class="graph-edge"
            :class="currentStageIndex > index ? 'is-done' : ''"
          />

          <g
            v-for="(paper, index) in analysisPapers"
            :key="paper.id"
            class="graph-node source-node"
            :class="revealedPaperCount > index ? 'is-done' : ''"
            :transform="`translate(18 ${31 + index * 75})`"
          >
            <rect width="146" height="48" rx="12" />
            <text x="18" y="21">
              {{ paperShortName(paper.displayName, index) }}
            </text>
            <text class="node-caption" x="18" y="36">
              {{ paper.pageCount }} 页
            </text>
          </g>

          <g
            class="graph-node core-node"
            :class="currentStageIndex === 0 ? 'is-active' : 'is-done'"
            transform="translate(290 130)"
          >
            <circle r="58" />
            <text text-anchor="middle" y="-3">读取</text>
            <text class="node-caption" text-anchor="middle" y="16">
              {{ analysisPaperCount }} 份试卷
            </text>
          </g>

          <g
            v-for="(label, index) in ['整理', '汇总', '图表', '完成']"
            :key="label"
            class="graph-node stage-node"
            :class="{
              'is-active': currentStageIndex === index + 1,
              'is-done': currentStageIndex > index + 1,
            }"
            :transform="`translate(${402 + index * 104} 130)`"
          >
            <circle r="34" />
            <text text-anchor="middle" y="4">{{ label }}</text>
          </g>
        </svg>
      </div>

      <div class="mx-auto mt-1 max-w-2xl text-center">
        <p class="text-sm font-medium">{{ currentStage.title }}</p>
        <Shimmer class="mt-1 text-xs" :duration="1.8">
          {{ analysisDetail }}
        </Shimmer>
        <Progress :model-value="progress" class="mt-4 h-1.5 bg-secondary/80" />
      </div>
    </section>

    <section v-if="showResults && !isAnalyzing" class="mt-9">
      <div
        class="mb-5 flex flex-col gap-3 border-b border-border/80 pb-5 sm:flex-row sm:items-end sm:justify-between"
      >
        <div>
          <div class="flex items-center gap-2">
            <BarChart3Icon class="size-5 text-primary" />
            <h2 class="editorial-title text-2xl font-semibold">分析结果</h2>
          </div>
          <p class="mt-2 text-sm text-muted-foreground">
            关系规范化、关系代数与 E-R 建模合计占 71.4%。
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <Badge variant="secondary">{{ analysisPaperCount }} 套有效试卷</Badge>
          <Badge variant="secondary">8 类考点</Badge>
          <Badge>3 个高频模块</Badge>
        </div>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <article class="chart-card lg:col-span-2">
          <div class="chart-heading">
            <div>
              <h3>考频统计</h3>
              <p>按 {{ analysisPaperCount }} 套试卷中的分值占比排序</p>
            </div>
            <Badge variant="outline">300 分样本</Badge>
          </div>
          <div class="h-[350px] w-full">
            <VChart
              :option="scoreFrequencyOption"
              autoresize
              aria-label="数据库系统原理考点分值占比横向柱状图"
            />
          </div>
        </article>

        <article class="chart-card">
          <div class="chart-heading">
            <div>
              <h3>复习优先级</h3>
              <p>综合覆盖频率与单次分值影响</p>
            </div>
          </div>
          <div class="h-[330px] w-full">
            <VChart
              :option="priorityRadarOption"
              autoresize
              aria-label="数据库系统原理复习优先级雷达图"
            />
          </div>
        </article>

        <article class="chart-card">
          <div class="chart-heading">
            <div>
              <h3>高频—高分四象限</h3>
              <p>右上优先，左上警惕低频高分题</p>
            </div>
          </div>
          <div class="h-[330px] w-full">
            <VChart
              :option="quadrantOption"
              autoresize
              aria-label="考点试卷覆盖率与平均分值四象限图"
            />
          </div>
        </article>

        <article class="chart-card lg:col-span-2">
          <div class="chart-heading">
            <div>
              <h3 class="flex items-center gap-2">
                <NetworkIcon class="size-4 text-primary" />
                考频关系图
              </h3>
              <p>展示核心考点之间的先修与题型联系</p>
            </div>
          </div>
          <div class="h-[360px] w-full">
            <VChart
              :option="relationOption"
              autoresize
              aria-label="数据库系统原理高频考点关系图"
            />
          </div>
        </article>
      </div>

      <table class="sr-only">
        <caption>
          {{
            analysisPaperCount
          }}
          套试卷考点统计
        </caption>
        <thead>
          <tr>
            <th>考点</th>
            <th>覆盖试卷</th>
            <th>估算分值</th>
            <th>占比</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in EXAM_TOPIC_INSIGHTS" :key="item.name">
            <td>{{ item.name }}</td>
            <td>{{ item.paperCount }}/3</td>
            <td>{{ item.score }}</td>
            <td>{{ item.share }}%</td>
          </tr>
        </tbody>
      </table>
    </section>
  </section>
</template>

<style scoped>
.analysis-panel {
  background:
    radial-gradient(circle at 50% 44%, rgb(201 108 77 / 7%), transparent 36%),
    var(--card);
}

.analysis-graph {
  height: clamp(210px, 28vw, 270px);
}

.graph-edge {
  fill: none;
  stroke: #ddd4cb;
  stroke-width: 1.5;
  transition: stroke 240ms ease;
}

.graph-edge.is-done {
  stroke: #c96c4d;
}

.graph-node rect,
.graph-node circle {
  fill: #f3eee8;
  stroke: #d8cec4;
  stroke-width: 1.5;
  transition:
    fill 220ms ease,
    stroke 220ms ease,
    opacity 220ms ease;
}

.graph-node text {
  fill: #766f67;
  font-size: 13px;
  font-weight: 600;
}

.graph-node .node-caption {
  font-size: 10px;
  font-weight: 400;
}

.source-node {
  opacity: 0.42;
  transition: opacity 220ms ease;
}

.source-node.is-done {
  opacity: 1;
}

.graph-node.is-active circle {
  fill: #fff7f2;
  stroke: #c96c4d;
  stroke-width: 2;
}

.graph-node.is-active text {
  fill: #8f452e;
}

.graph-node.is-done circle,
.graph-node.is-done rect {
  fill: #c96c4d;
  stroke: #c96c4d;
}

.graph-node.is-done text {
  fill: #fffaf5;
}

.stage-node.is-active circle {
  animation: node-breathe 1.6s ease-in-out infinite;
}

.chart-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: calc(var(--radius) + 6px);
  background: var(--card);
  padding: 1rem;
}

.chart-heading {
  display: flex;
  min-height: 48px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.25rem 0.25rem 0.75rem;
}

.chart-heading h3 {
  font-size: 0.875rem;
  font-weight: 650;
}

.chart-heading p {
  margin-top: 0.25rem;
  color: var(--muted-foreground);
  font-size: 0.75rem;
}

@keyframes node-breathe {
  0%,
  100% {
    stroke-width: 2;
  }
  50% {
    stroke-width: 4;
  }
}

@media (max-width: 640px) {
  .analysis-graph {
    min-width: 640px;
    transform: translateX(-42px);
  }

  .chart-card {
    padding: 0.75rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stage-node.is-active circle {
    animation: none;
  }
}
</style>
