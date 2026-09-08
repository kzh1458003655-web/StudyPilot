<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { toAppError } from "@/shared/api/http";
import { createProject } from "../api/requests";

const router = useRouter();
const name = ref("");
const description = ref("");
const submitting = ref(false);
const errorMessage = ref("");

async function submit() {
  if (!name.value.trim()) {
    errorMessage.value = "请填写课程或备考项目名称。";
    return;
  }
  submitting.value = true;
  errorMessage.value = "";
  try {
    const project = await createProject(
      name.value.trim(),
      description.value.trim(),
    );
    await router.push(`/projects/${project.id}/qa`);
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="panel project-start">
    <p class="eyebrow">本地资料 · 可追溯回答</p>
    <h1>开始一个学习项目</h1>
    <p class="subtle">不同课程的资料、问答和模拟考会分别保存，互不混用。</p>
    <form class="form-stack" @submit.prevent="submit">
      <label
        >项目名称
        <input
          v-model="name"
          maxlength="100"
          placeholder="例如：操作系统期末复习"
      /></label>
      <label
        >说明（可选）
        <textarea
          v-model="description"
          maxlength="1000"
          placeholder="写下这门课程的学习目标"
        />
      </label>
      <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
      <button :disabled="submitting" type="submit">
        {{ submitting ? "正在创建…" : "创建并进入问答" }}
      </button>
    </form>
  </section>
</template>
