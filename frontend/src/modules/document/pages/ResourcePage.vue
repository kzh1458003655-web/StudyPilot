<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { toAppError } from "@/shared/api/http";
import { uploadDocument } from "../api/requests";

const route = useRoute();
const projectId = computed(() => Number(route.params.projectId));
const file = ref<File>();
const documentType = ref("LECTURE");
const submitting = ref(false);
const notice = ref("");
const errorMessage = ref("");

function selectFile(event: Event) {
  const target = event.target as HTMLInputElement;
  file.value = target.files?.[0];
}
async function submit() {
  if (!file.value) {
    errorMessage.value = "请选择一份文字型 PDF。";
    return;
  }
  submitting.value = true;
  errorMessage.value = "";
  notice.value = "";
  try {
    const result = await uploadDocument(
      projectId.value,
      documentType.value,
      file.value,
    );
    notice.value = `资料编号 ${result.id}：已提取 ${result.pageCount} 页、建立 ${result.chunkCount} 个检索片段。`;
    file.value = undefined;
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="workspace">
    <div class="workspace-head">
      <div>
        <p class="eyebrow">本课程 · 独立资料库</p>
        <h1>资料管理</h1>
      </div>
      <div class="workspace-links">
        <RouterLink :to="`/projects/${projectId}/qa`"> 知识问答 </RouterLink>
        <RouterLink :to="`/projects/${projectId}/exams`">
          真题分析与模拟考
        </RouterLink>
      </div>
    </div>
    <p class="subtle">
      当前支持可直接提取文字的
      PDF。教材、讲义和知识点资料会进入问答检索范围；历年真题留给模拟考模块使用。
    </p>
    <form class="form-stack upload-card" @submit.prevent="submit">
      <label
        >资料类型<select v-model="documentType">
          <option value="TEXTBOOK">教材</option>
          <option value="LECTURE">讲义</option>
          <option value="KNOWLEDGE">知识点</option>
          <option value="PAST_EXAM">历年真题</option>
          <option value="REFERENCE_ANSWER">参考答案</option>
        </select></label
      ><label
        >PDF 文件<input
          accept="application/pdf,.pdf"
          type="file"
          @change="selectFile"
      /></label>
      <p v-if="notice" class="success" role="status">{{ notice }}</p>
      <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
      <button type="submit" :disabled="submitting">
        {{ submitting ? "正在处理…" : "上传并建立索引" }}
      </button>
    </form>
  </section>
</template>
