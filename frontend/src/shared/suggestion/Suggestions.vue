<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { ScrollArea, ScrollBar } from "@/shared/ui/scroll-area";
import { cn } from "@/shared/utils";

interface SuggestionsProps {
  class?: HTMLAttributes["class"];
}

const props = defineProps<SuggestionsProps>();
</script>

<template>
  <ScrollArea class="w-full overflow-x-auto whitespace-nowrap" v-bind="$attrs">
    <div :class="cn('flex w-max flex-nowrap items-center gap-2', props.class)">
      <slot />
    </div>
    <ScrollBar class="hidden" orientation="horizontal" />
  </ScrollArea>
</template>
