<script setup lang="ts">
import { PlusIcon } from "@lucide/vue";
import { Button } from "@/shared/ui/button";
</script>

<template>
  <section class="grid min-h-[calc(100vh-4rem)] place-items-center px-6 py-16">
    <div class="max-w-md text-center">
      <h1
        class="editorial-title text-balance text-4xl font-semibold sm:text-5xl"
      >
        从一门课程开始
      </h1>
      <p class="mt-4 text-sm leading-6 text-muted-foreground">
        选择已有课程，或新建一门课程。
      </p>
      <Button as-child class="mt-7">
        <RouterLink to="/projects?create=1"><PlusIcon />新建课程</RouterLink>
      </Button>
    </div>
  </section>
</template>
