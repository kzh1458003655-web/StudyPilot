<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowRightIcon,
  MoreHorizontalIcon,
  SparklesIcon,
  Trash2Icon,
} from "@lucide/vue";
import { toast } from "vue-sonner";
import { toAppError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";
import { Textarea } from "@/shared/ui/textarea";
import { Badge } from "@/shared/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import { Shimmer } from "@/shared/shimmer";
import { Suggestion, Suggestions } from "@/shared/suggestion";
import { deleteMockExam, listMockExams } from "../api/requests";
import { useExamTaskStore } from "../stores/examTaskStore";

interface ExamSummary {
  id: number;
  title: string;
  itemCount: number;
}

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));
const instructions = ref("");
const tasks = useExamTaskStore();
const generating = computed(() => Boolean(tasks.active[projectId.value]));
const errorMessage = ref("");
const history = ref<ExamSummary[]>([]);
const pendingDelete = ref<ExamSummary>();
let mounted = true;

async function loadHistory() {
  const targetProject = projectId.value;
  const result = await listMockExams(targetProject);
  if (mounted && targetProject === projectId.value) history.value = result;
}

async function restoreHistory() {
  const running = tasks.current(projectId.value);
  try {
    await loadHistory();
    if (running) {
      try {
        await running;
      } catch (error) {
        if (mounted) errorMessage.value = toAppError(error).message;
      }
      if (mounted) await loadHistory();
    }
  } catch (error) {
    if (mounted) errorMessage.value = toAppError(error).message;
  }
}

async function generate() {
  const targetProject = projectId.value;
  errorMessage.value = "";
  try {
    const id = await tasks.generate(targetProject, instructions.value.trim());
    if (!mounted || targetProject !== projectId.value) return;
    await loadHistory();
    await router.push(`/projects/${targetProject}/exams/${id}/take`);
  } catch (error) {
    if (mounted && targetProject === projectId.value)
      errorMessage.value = toAppError(error).message;
  }
}

async function remove(exam: ExamSummary) {
  try {
    await deleteMockExam(projectId.value, exam.id);
    pendingDelete.value = undefined;
    await loadHistory();
    toast.success("练习卷已删除");
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  }
}

onMounted(() => {
  mounted = true;
  void restoreHistory();
});
onUnmounted(() => {
  mounted = false;
});
watch(projectId, () => {
  history.value = [];
  instructions.value = "";
  errorMessage.value = "";
  void restoreHistory();
});
</script>

<template>
  <section class="mx-auto max-w-4xl px-4 py-9 sm:px-8 sm:py-12">
    <header class="mb-8">
      <h1 class="editorial-title text-3xl font-semibold sm:text-4xl">
        生成练习
      </h1>
    </header>

    <div
      class="rounded-2xl border bg-card p-3 shadow-[0_10px_32px_rgba(45,36,28,0.04)] sm:p-4"
    >
      <Textarea
        v-model="instructions"
        maxlength="1000"
        class="min-h-[84px] resize-none border-0 bg-transparent p-2 shadow-none focus-visible:ring-0"
        placeholder="描述题量、难度、题型或知识范围；不填写也可以直接生成"
      />
      <div
        class="mt-2 flex flex-col gap-3 border-t pt-3 sm:flex-row sm:items-end sm:justify-between"
      >
        <Suggestions class="gap-2 overflow-x-auto pb-1 sm:flex-wrap">
          <Suggestion
            suggestion="生成一套综合复习题"
            @click="instructions = $event"
            >综合复习</Suggestion
          >
          <Suggestion
            suggestion="侧重本课程高频考点出题"
            @click="instructions = $event"
            >高频考点</Suggestion
          >
          <Suggestion
            suggestion="生成一套中等难度练习题"
            @click="instructions = $event"
            >中等难度</Suggestion
          >
        </Suggestions>
        <Button
          class="generate-exam shrink-0"
          :disabled="generating"
          @click="generate"
        >
          <SparklesIcon />{{ generating ? "生成中" : "生成并作答" }}
        </Button>
      </div>
    </div>

    <div
      v-if="generating"
      class="mt-4 flex items-center gap-3 rounded-xl border bg-card px-4 py-3 text-sm"
      role="status"
    >
      <span class="size-2 animate-pulse rounded-full bg-primary" />
      <Shimmer>正在根据课程内容组织题目，历史试卷仍可使用…</Shimmer>
    </div>
    <p v-if="errorMessage" class="mt-4 text-sm text-destructive" role="alert">
      {{ errorMessage }}
    </p>

    <section class="mt-12">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="editorial-title text-xl font-semibold">历史练习</h2>
        <Badge variant="secondary">{{ history.length }}</Badge>
      </div>
      <p
        v-if="!history.length"
        class="rounded-xl border border-dashed py-12 text-center text-sm text-muted-foreground"
      >
        还没有生成记录
      </p>
      <div v-else class="divide-y border-y">
        <article
          v-for="exam in history"
          :key="exam.id"
          class="flex items-center gap-3 py-4"
        >
          <div class="min-w-0 flex-1">
            <h3
              class="break-words font-sans text-sm font-medium tracking-normal sm:text-base"
            >
              {{ exam.title }}
            </h3>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ exam.itemCount }} 道题
            </p>
          </div>
          <Button as-child variant="outline" size="sm">
            <RouterLink :to="`/projects/${projectId}/exams/${exam.id}/take`"
              >开始作答<ArrowRightIcon
            /></RouterLink>
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger as-child
              ><Button
                variant="ghost"
                size="icon-sm"
                :aria-label="`练习操作：${exam.title}`"
                ><MoreHorizontalIcon /></Button
            ></DropdownMenuTrigger>
            <DropdownMenuContent align="end"
              ><DropdownMenuItem
                class="text-destructive"
                @select="pendingDelete = exam"
                ><Trash2Icon />删除练习</DropdownMenuItem
              ></DropdownMenuContent
            >
          </DropdownMenu>
        </article>
      </div>
    </section>

    <AlertDialog
      :open="Boolean(pendingDelete)"
      @update:open="(open) => !open && (pendingDelete = undefined)"
    >
      <AlertDialogContent>
        <AlertDialogHeader
          ><AlertDialogTitle
            >删除“{{ pendingDelete?.title }}”？</AlertDialogTitle
          ><AlertDialogDescription
            >相关作答记录也会一并删除。</AlertDialogDescription
          ></AlertDialogHeader
        >
        <AlertDialogFooter
          ><AlertDialogCancel @click="pendingDelete = undefined"
            >取消</AlertDialogCancel
          ><AlertDialogAction
            class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            @click="pendingDelete && remove(pendingDelete)"
            >删除</AlertDialogAction
          ></AlertDialogFooter
        >
      </AlertDialogContent>
    </AlertDialog>
  </section>
</template>
