<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeftIcon, MessageCircleIcon, RotateCcwIcon } from "@lucide/vue";
import { toAppError } from "@/shared/api/http";
import { getMockExam } from "@/modules/exam/api/requests";
import type { MockExam } from "@/modules/exam/types/domain";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Progress } from "@/shared/ui/progress";
import { Textarea } from "@/shared/ui/textarea";
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

const answeredCount = computed(
  () =>
    exam.value?.items.filter((item) => Boolean(answers.value[item.id]?.trim()))
      .length ?? 0,
);
const progress = computed(() =>
  exam.value?.items.length
    ? (answeredCount.value / exam.value.items.length) * 100
    : 0,
);

async function load() {
  if (!Number.isInteger(examId.value) || examId.value <= 0) {
    error.value = "请从智能组卷页面进入作答。";
    return;
  }
  try {
    exam.value = await getMockExam(projectId.value, examId.value);
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
  error.value = "";
  try {
    result.value = await submitAttempt(
      projectId.value,
      attemptId.value,
      exam.value.items.map((item) => ({
        itemId: item.id,
        answer: answers.value[item.id] ?? "",
      })),
    );
  } catch (e) {
    error.value = toAppError(e).message;
  } finally {
    submitting.value = false;
  }
}

async function repeatAttempt() {
  result.value = undefined;
  answers.value = {};
  attemptId.value = undefined;
  await start();
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

function feedbackFor(itemId: number) {
  return result.value?.answers.find((item) => item.itemId === itemId);
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-3xl px-4 py-8 sm:px-8 sm:py-10">
    <Button
      as-child
      variant="ghost"
      size="sm"
      class="mb-6 -ml-3 text-muted-foreground"
    >
      <RouterLink :to="`/projects/${projectId}/exams`"
        ><ArrowLeftIcon />返回练习列表</RouterLink
      >
    </Button>
    <p
      v-if="error"
      class="mb-5 rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive"
      role="alert"
    >
      {{ error }}
    </p>

    <template v-if="exam">
      <header class="mb-8">
        <h1
          class="editorial-title text-balance text-3xl font-semibold sm:text-4xl"
        >
          {{ exam.title }}
        </h1>
        <div class="mt-5 flex items-center gap-4">
          <Progress :model-value="progress" class="h-1.5 flex-1" />
          <span class="shrink-0 text-xs text-muted-foreground"
            >已答 {{ answeredCount }} / {{ exam.items.length }}</span
          >
        </div>
      </header>

      <form class="pb-24" @submit.prevent="submit">
        <article
          v-for="item in exam.items"
          :key="item.id"
          class="border-t py-8 first:border-t-0 first:pt-0"
        >
          <div class="mb-5 flex items-start gap-3">
            <span
              class="mt-0.5 grid size-7 shrink-0 place-items-center rounded-full bg-secondary text-xs font-semibold"
              >{{ item.ordinal }}</span
            >
            <div class="min-w-0 flex-1">
              <h2
                class="font-sans text-base font-medium leading-7 tracking-normal"
              >
                {{ item.prompt }}
              </h2>
              <div class="mt-2 flex flex-wrap gap-2">
                <Badge variant="outline">{{ item.score }} 分</Badge
                ><Badge variant="secondary">{{ item.knowledgePoint }}</Badge>
              </div>
            </div>
          </div>

          <div
            v-if="item.type === 'SINGLE_CHOICE'"
            class="ml-0 grid gap-2 sm:ml-10"
          >
            <label
              v-for="(option, index) in item.options"
              :key="option"
              class="flex cursor-pointer items-start gap-3 rounded-xl border bg-card px-4 py-3.5 transition-colors hover:bg-accent/40 has-[:checked]:border-primary has-[:checked]:bg-primary/5"
            >
              <input
                v-model="answers[item.id]"
                :disabled="Boolean(result)"
                type="radio"
                :name="`item-${item.id}`"
                :value="String.fromCharCode(65 + index)"
                class="mt-1 size-4 accent-[var(--primary)]"
              />
              <span class="text-sm leading-6"
                ><strong class="mr-1"
                  >{{ String.fromCharCode(65 + index) }}.</strong
                >{{ optionText(option, index) }}</span
              >
            </label>
          </div>
          <Textarea
            v-else
            v-model="answers[item.id]"
            :disabled="Boolean(result)"
            placeholder="输入简答题答案"
            class="ml-0 min-h-28 resize-y bg-card sm:ml-10 sm:w-[calc(100%-2.5rem)]"
          />

          <div
            v-if="feedbackFor(item.id)"
            class="ml-0 mt-5 rounded-xl bg-secondary/70 px-4 py-3 text-sm sm:ml-10"
          >
            <strong
              >得分 {{ feedbackFor(item.id)?.score }} / {{ item.score }}</strong
            >
            <p class="mt-1 leading-6 text-muted-foreground">
              {{ feedbackFor(item.id)?.feedback }}
            </p>
          </div>
        </article>

        <div
          v-if="!result"
          class="sticky bottom-4 z-10 mt-4 flex items-center justify-between rounded-xl border bg-card/95 p-3 shadow-[0_8px_28px_rgba(45,36,28,0.08)] backdrop-blur"
        >
          <span class="pl-2 text-xs text-muted-foreground"
            >未作答题目也可提交</span
          >
          <Button type="submit" :disabled="!attemptId || submitting">{{
            submitting ? "正在测评…" : "提交测评"
          }}</Button>
        </div>
      </form>
    </template>

    <section v-if="result" class="mb-12 rounded-2xl border bg-card p-6 sm:p-8">
      <p class="text-sm text-muted-foreground">本次得分</p>
      <h2 class="editorial-title mt-2 text-4xl font-semibold">
        得分 {{ result.totalScore }} / {{ result.maxScore }}
      </h2>
      <div class="mt-6 flex flex-col gap-3 sm:flex-row">
        <Button variant="outline" @click="askAboutResult"
          ><MessageCircleIcon />根据结果提问</Button
        >
        <Button @click="repeatAttempt"><RotateCcwIcon />重新作答本卷</Button>
      </div>
    </section>
  </section>
</template>
