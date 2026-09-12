<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { toAppError } from "@/shared/api/http";
import { deleteMockExam, listMockExams } from "../api/requests";
import { useExamTaskStore } from "../stores/examTaskStore";

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));
const instructions = ref("");
const tasks = useExamTaskStore();
const generating = computed(() => Boolean(tasks.active[projectId.value]));
const errorMessage = ref("");
const history = ref<Array<{ id: number; title: string; itemCount: number }>>(
  [],
);
let mounted = true;

async function loadHistory() {
  const targetProject = projectId.value;
  const result = await listMockExams(targetProject);
  if (mounted && targetProject === projectId.value) history.value = result;
}
async function restoreHistory() {
  try {
    await loadHistory();
    const running = tasks.current(projectId.value);
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
async function remove(exam: { id: number; title: string }) {
  if (!window.confirm(`删除“${exam.title}”吗？相关作答记录也会删除。`)) return;
  try {
    await deleteMockExam(projectId.value, exam.id);
    await loadHistory();
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  }
}
</script>

<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">本课程 · 智能组卷</p>
        <h1>创建一套练习</h1>
      </div>
      <div class="workspace-links">
        <RouterLink :to="`/projects/${projectId}/frequency`">
          考频分析
        </RouterLink>
        <RouterLink :to="`/projects/${projectId}/qa`"> 课程问答 </RouterLink>
      </div>
    </div>
    <p class="subtle">
      直接描述你想练习的内容即可。已有课程资料会作为补充依据；没有资料时，模型也会根据课程主题和你的要求出题。
    </p>
    <p v-if="generating" class="task-banner" role="status">
      正在后台生成练习。你可以切换到其他模块，完成后会自动出现在历史记录中。
    </p>
    <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>

    <div class="exam-grid">
      <article class="exam-card generator-card">
        <h2>智能出题</h2>
        <p>
          例如：“出一套操作系统期末练习题，难度中等，侧重进程管理。”不填写也可以直接生成默认练习卷。
        </p>
        <textarea
          v-model="instructions"
          maxlength="1000"
          placeholder="输入题量、难度、题型或知识范围；不填写也可以直接生成"
        />
        <button class="generate-exam" :disabled="generating" @click="generate">
          {{ generating ? "正在生成，请稍候…" : "生成并开始作答" }}
        </button>
      </article>
    </div>
    <article class="exam-card exam-history-card">
      <h2>已生成的练习卷</h2>
      <p v-if="!history.length" class="subtle">本课程还没有生成记录。</p>
      <div v-for="exam in history" :key="exam.id" class="frequency-paper">
        <span
          ><strong>{{ exam.title }}</strong
          ><small>{{ exam.itemCount }} 道题</small></span
        >
        <div class="exam-history-actions">
          <RouterLink :to="`/projects/${projectId}/exams/${exam.id}/take`">
            重新作答
          </RouterLink>
          <button
            type="button"
            :aria-label="`删除 ${exam.title}`"
            @click="remove(exam)"
          >
            删除
          </button>
        </div>
      </div>
    </article>
  </section>
</template>
