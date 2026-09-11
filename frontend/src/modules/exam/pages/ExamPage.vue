<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { toAppError } from "@/shared/api/http";
import { generateMockExam, listMockExams } from "../api/requests";

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));
const instructions = ref("");
const generating = ref(false);
const errorMessage = ref("");
const history = ref<Array<{ id: number; title: string; itemCount: number }>>(
  [],
);

async function loadHistory() {
  history.value = await listMockExams(projectId.value);
}
onMounted(() =>
  loadHistory().catch(() => {
    /* The empty history message remains usable. */
  }),
);
async function generate() {
  generating.value = true;
  errorMessage.value = "";
  try {
    const id = await generateMockExam(
      projectId.value,
      instructions.value.trim(),
    );
    await loadHistory();
    await router.push(`/projects/${projectId.value}/exams/${id}/take`);
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  } finally {
    generating.value = false;
  }
}
</script>

<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">本课程 · 模拟考 Skill</p>
        <h1>生成一份模拟卷</h1>
      </div>
      <div class="workspace-links">
        <RouterLink :to="`/projects/${projectId}/frequency`"
          >考频分析</RouterLink
        >
        <RouterLink :to="`/projects/${projectId}/qa`">课程问答</RouterLink>
      </div>
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
          {{ generating ? "正在生成，请稍候…" : "生成并开始作答" }}
        </button>
      </article>
    </div>
    <article class="exam-card">
      <h2>已生成的模拟卷</h2>
      <p v-if="!history.length" class="subtle">本课程还没有生成记录。</p>
      <div v-for="exam in history" :key="exam.id" class="frequency-paper">
        <span
          ><strong>{{ exam.title }}</strong
          ><small>{{ exam.itemCount }} 道题</small></span
        >
        <RouterLink :to="`/projects/${projectId}/exams/${exam.id}/take`"
          >继续作答</RouterLink
        >
      </div>
    </article>
  </section>
</template>
