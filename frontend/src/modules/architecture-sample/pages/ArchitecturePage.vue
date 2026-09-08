<script setup lang="ts">
import { onMounted, ref } from "vue";
import { toAppError } from "@/shared/api/http";
import { fetchArchitectureStatus } from "../api/requests";
import type { ArchitectureStatus } from "../types/domain";
const boundaries = [
  "app 只装配模块",
  "业务模块只依赖 shared",
  "外部数据先校验再映射",
];
const status = ref<ArchitectureStatus>();
const errorMessage = ref("");
onMounted(async () => {
  try {
    status.value = await fetchArchitectureStatus();
  } catch (error) {
    errorMessage.value = toAppError(error).message;
  }
});
</script>
<template>
  <section class="panel">
    <h1>前端架构骨架</h1>
    <p>此页仅验证路由和模块边界，不包含产品功能。</p>
    <ul>
      <li v-for="boundary in boundaries" :key="boundary">{{ boundary }}</li>
    </ul>
    <section class="status-card" aria-live="polite">
      <p v-if="!status && !errorMessage">正在验证 Java 与本地 AI 服务…</p>
      <p v-else-if="errorMessage">依赖检查失败：{{ errorMessage }}</p>
      <template v-else-if="status">
        <strong>基础链路：{{ status.status }}</strong>
        <span
          >本地 AI：{{
            status.ai.modelReady ? "模型已就绪" : "服务已启动，模型未就绪"
          }}</span
        >
        <span
          >资料分块：{{ status.ai.chunks }} · 排队：{{
            status.ai.pending
          }}</span
        >
      </template>
    </section>
  </section>
</template>
