<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { CommandList } from "@/shared/ui/command";
import { cn } from "@/shared/utils";

type PromptInputCommandListProps = InstanceType<typeof CommandList>["$props"];

interface Props extends /* @vue-ignore */ PromptInputCommandListProps {
  class?: HTMLAttributes["class"];
}

const props = defineProps<Props>();
</script>

<template>
  <CommandList :class="cn(props.class)" v-bind="props" />
</template>
