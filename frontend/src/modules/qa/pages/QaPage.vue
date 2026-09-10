<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { toAppError } from "@/shared/api/http";
import {
  deleteDocument,
  listDocuments,
  uploadDocument,
} from "@/modules/document/api/requests";
import type { CourseDocument } from "@/modules/document/api/requests";
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
const documents = ref<CourseDocument[]>([]);
const documentsLoading = ref(false);
const documentError = ref("");
const documentNotice = ref("");
const selectedFile = ref<File>();
const uploading = ref(false);
const fileInput = ref<HTMLInputElement>();
const MAX_PDF_BYTES = 25 * 1024 * 1024;

async function loadDocuments() {
  if (!Number.isInteger(projectId.value) || projectId.value <= 0) return;
  documentsLoading.value = true;
  documentError.value = "";
  try {
    documents.value = await listDocuments(projectId.value);
  } catch (error) {
    documentError.value = toAppError(error).message;
  } finally {
    documentsLoading.value = false;
  }
}
function selectFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0];
}
async function uploadMaterial() {
  if (!selectedFile.value) {
    documentError.value = "请选择一份文字型 PDF。";
    return;
  }
  if (selectedFile.value.size > MAX_PDF_BYTES) {
    documentError.value = "单个 PDF 文件不得超过 25 MB。";
    return;
  }
  uploading.value = true;
  documentError.value = "";
  documentNotice.value = "";
  try {
    const result = await uploadDocument(projectId.value, selectedFile.value);
    documentNotice.value = `已提取 ${result.pageCount} 页，建立 ${result.chunkCount} 个检索片段。`;
    selectedFile.value = undefined;
    if (fileInput.value) fileInput.value.value = "";
    await loadDocuments();
  } catch (error) {
    documentError.value = toAppError(error).message;
  } finally {
    uploading.value = false;
  }
}
async function removeDocument(document: CourseDocument) {
  if (
    !window.confirm(
      `删除“${document.displayName}”吗？这会移除本课程中的文件和索引。`,
    )
  )
    return;
  try {
    await deleteDocument(projectId.value, document.id);
    documentNotice.value = "资料已删除。";
    await loadDocuments();
  } catch (error) {
    documentError.value = toAppError(error).message;
  }
}
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

onMounted(loadDocuments);
watch(projectId, () => {
  sessionId.value = undefined;
  turns.value = [];
  selectedFile.value = undefined;
  documentNotice.value = "";
  loadDocuments();
});
</script>
<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">本课程 · 可追溯回答</p>
        <h1>知识问答</h1>
      </div>
      <div class="workspace-links">
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
            <span>{{
              documents.length
                ? "优先使用当前课程资料"
                : "当前无资料，将使用本地模型回答"
            }}</span>
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
          <span>{{ documents.length }} 份</span>
        </div>
        <p>
          资料只用于当前课程，回答会引用命中的原文页码。仅支持不超过 25 MB
          的文字型 PDF。
        </p>
        <form class="side-upload" @submit.prevent="uploadMaterial">
          <label class="upload-picker">
            <input
              ref="fileInput"
              accept="application/pdf,.pdf"
              type="file"
              @change="selectFile"
            />
            <span>{{ selectedFile?.name ?? "选择 PDF 文件" }}</span>
          </label>
          <button type="submit" :disabled="uploading">
            {{ uploading ? "正在建立索引…" : "上传资料" }}
          </button>
        </form>
        <p v-if="documentNotice" class="success side-message" role="status">
          {{ documentNotice }}
        </p>
        <p v-if="documentError" class="error side-message" role="alert">
          {{ documentError }}
        </p>
        <div class="document-list" aria-label="当前课程资料列表">
          <p v-if="documentsLoading" class="side-muted">正在读取资料…</p>
          <p v-else-if="!documents.length" class="side-muted">尚未添加资料。</p>
          <article
            v-for="document in documents"
            :key="document.id"
            class="document-item"
          >
            <div>
              <strong>{{ document.displayName }}</strong>
              <small>
                {{ document.pageCount }} 页 ·
                {{ document.status === "READY" ? "已就绪" : "处理中" }}
              </small>
            </div>
            <button
              type="button"
              :aria-label="`删除 ${document.displayName}`"
              @click="removeDocument(document)"
            >
              ×
            </button>
          </article>
        </div>
        <div class="side-note">
          <strong>让回答更有依据</strong><br />添加文字型 PDF
          后，回答会附上页码和原文片段。
        </div>
      </aside>
    </div>
  </section>
</template>
