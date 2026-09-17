<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { CommandEmpty } from "@/shared/ui/command";
import { cn } from "@/shared/utils";

type PromptInputCommandEmptyProps = InstanceType<typeof CommandEmpty>["$props"];

interface Props extends /* @vue-ignore */ PromptInputCommandEmptyProps {
  class?: HTMLAttributes["class"];
}

const props = defineProps<Props>();
</script>

<template>
  <CommandEmpty :class="cn(props.class)" v-bind="props">
    <slot />
  </CommandEmpty>
</template>
