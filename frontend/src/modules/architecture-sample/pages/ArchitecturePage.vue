<script setup lang="ts">
const boundaries = [
  "app 只装配模块",
  "业务模块只依赖 shared",
  "外部数据先校验再映射",
];
</script>
<template>
  <section class="panel">
    <h1>前端架构骨架</h1>
    <p>此页仅验证路由和模块边界，不包含产品功能。</p>
    <ul>
      <li v-for="boundary in boundaries" :key="boundary">{{ boundary }}</li>
    </ul>
  </section>
</template>
