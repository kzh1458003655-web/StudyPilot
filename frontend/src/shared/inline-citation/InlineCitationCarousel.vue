<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { Carousel } from "@/shared/ui/carousel";
import { cn } from "@/shared/utils";

const props = defineProps<{
  class?: HTMLAttributes["class"];
}>();
</script>

<template>
  <Carousel :class="cn('w-full', props.class)">
    <slot />
  </Carousel>
</template>
