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
        <p class="eyebrow">本课程 · 可追溯回答</p>
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
    <div class="qa-layout">
      <section class="chat-panel">
        <div class="messages">
          <div v-if="!turns.length" class="welcome">
            <div class="welcome-mark">✳</div>
            <h2>今天想弄懂什么？</h2>
            <p>直接提问，或先添加本课程 PDF，让回答更贴近课堂。</p>
            <div class="suggestions">
              <button
                type="button"
                @click="question = '请用一个简单例子解释这门课的核心概念。'"
              >
                解释一个知识点 ↗
              </button>
              <button
                type="button"
                @click="question = '请帮我梳理这门课的复习思路。'"
              >
                梳理重点与区别 ↗
              </button>
            </div>
            <small>每次弄懂一点，都是进步。</small>
          </div>
          <article v-for="turn in turns" :key="turn.question" class="qa-turn">
            <p class="question">你</p>
            <p class="answer">{{ turn.question }}</p>
            <p class="question assistant-label">StudyPilot</p>
            <p class="answer">{{ turn.answer.answer }}</p>
            <p
              v-if="turn.answer.status === 'INSUFFICIENT_EVIDENCE'"
              class="notice"
            >
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
        </div>
        <form class="composer" @submit.prevent="ask">
          <textarea
            v-model="question"
            maxlength="1000"
            placeholder="输入问题，或说说你想弄懂的知识点…"
            :disabled="asking"
          />
          <div class="composer-foot">
            <span>仅使用当前课程资料</span>
            <span v-if="errorMessage" class="error" role="alert">{{
              errorMessage
            }}</span>
            <button type="submit" :disabled="asking || !question.trim()">
              {{ asking ? "正在检索与回答…" : "发送问题 ↑" }}
            </button>
          </div>
        </form>
      </section>
      <aside class="qa-sidecard">
        <div class="panel-heading">
          <h3>本课程资料</h3>
          <span>独立存储</span>
        </div>
        <p>教材、讲义和知识点资料只会用于当前课程的回答。</p>
        <RouterLink :to="`/projects/${projectId}/resources`">
          管理课程资料 →
        </RouterLink>
        <div class="side-note">
          <strong>让回答更有依据</strong><br />添加文字型 PDF
          后，回答会附上页码和原文片段。
        </div>
      </aside>
    </div>
  </section>
</template>
