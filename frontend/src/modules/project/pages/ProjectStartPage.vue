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
  <section class="course-start">
    <div class="course-start-copy">
      <p class="eyebrow">本地资料 · 可追溯回答</p>
      <h1>新建一门课程</h1>
      <p class="subtle">
        课程是你的独立学习空间。资料、问答、模拟考和测评记录都只保存在当前课程内。
      </p>
      <div class="course-start-note">
        <span>□</span>
        <p>
          <strong>课程之间互不影响</strong
          ><br />切换课程时，系统只检索该课程已上传的资料。
        </p>
      </div>
    </div>
    <form class="course-form" @submit.prevent="submit">
      <h2>课程信息</h2>
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
        {{ submitting ? "正在创建…" : "创建课程并进入问答" }}
      </button>
    </form>
  </section>
</template>
