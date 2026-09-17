<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { ArrowRight } from "@lucide/vue";
import { useCarousel } from "@/shared/ui/carousel";
import { cn } from "@/shared/utils";

const props = defineProps<{
  class?: HTMLAttributes["class"];
}>();

const { scrollNext } = useCarousel();
</script>

<template>
  <button
    :class="cn('shrink-0', props.class)"
    type="button"
    aria-label="Next"
    @click="scrollNext"
  >
    <ArrowRight class="size-4 text-muted-foreground" />
  </button>
</template>
