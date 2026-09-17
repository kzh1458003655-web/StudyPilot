<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { ArrowLeft } from "@lucide/vue";
import { useCarousel } from "@/shared/ui/carousel";
import { cn } from "@/shared/utils";

const props = defineProps<{
  class?: HTMLAttributes["class"];
}>();

const { scrollPrev } = useCarousel();
</script>

<template>
  <button
    :class="cn('shrink-0', props.class)"
    type="button"
    aria-label="Previous"
    @click="scrollPrev"
  >
    <ArrowLeft class="size-4 text-muted-foreground" />
  </button>
</template>
