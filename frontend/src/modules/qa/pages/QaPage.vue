<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { toAppError } from "@/shared/api/http";
import { askQuestion } from "../api/requests";
import type { QaAnswer } from "../types/domain";
interface Turn {
  question: string;
  answer: QaAnswer;
}
const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const question = ref("");
const sessionId = ref<number>();
const turns = ref<Turn[]>([]);
const asking = ref(false);
const errorMessage = ref("");
async function ask() {
  if (
    !question.value.trim() ||
    !Number.isInteger(projectId.value) ||
    projectId.value <= 0
  )
    return;
  const currentQuestion = question.value.trim();
  asking.value = true;
  errorMessage.value = "";
  try {
    const answer = await askQuestion(
      projectId.value,
      currentQuestion,
      sessionId.value,
    );
    sessionId.value = answer.sessionId;
    turns.value.push({ question: currentQuestion, answer });
    question.value = "";
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  } finally {
    asking.value = false;
  }
}
</script>
<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">项目 {{ projectId }}</p>
        <h1>知识问答</h1>
      </div>
      <div class="workspace-links">
        <RouterLink :to="`/projects/${projectId}/resources`">
          管理资料
        </RouterLink>
        <RouterLink :to="`/projects/${projectId}/exams`">
          真题分析与模拟考
        </RouterLink>
      </div>
    </div>
    <p class="subtle">
      回答只使用本项目已处理完成的教材、讲义和知识点资料，并显示页码依据。
    </p>
    <div v-if="!turns.length" class="empty-state">
      上传资料后，试着问一个具体问题。例如：“进程和线程有什么区别？”
    </div>
    <article v-for="turn in turns" :key="turn.question" class="qa-turn">
      <p class="question">{{ turn.question }}</p>
      <p class="answer">{{ turn.answer.answer }}</p>
      <p v-if="turn.answer.status === 'INSUFFICIENT_EVIDENCE'" class="notice">
        这次回答没有使用模型推断，因为资料证据不足。
      </p>
      <details
        v-for="citation in turn.answer.citations"
        :key="`${citation.documentName}-${citation.pageNumber}-${citation.excerpt}`"
        class="citation"
      >
        <summary>
          {{ citation.documentName }} · 第 {{ citation.pageNumber }} 页
        </summary>
        <p>{{ citation.excerpt }}</p>
      </details>
    </article>
    <form class="ask-box" @submit.prevent="ask">
      <textarea
        v-model="question"
        maxlength="1000"
        placeholder="输入你的问题"
        :disabled="asking"
      />
      <div>
        <span v-if="errorMessage" class="error" role="alert">{{
          errorMessage
        }}</span
        ><button type="submit" :disabled="asking || !question.trim()">
          {{ asking ? "正在检索与回答…" : "发送问题" }}
        </button>
      </div>
    </form>
  </section>
</template>
