<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { CommandItem } from "@/shared/ui/command";
import { cn } from "@/shared/utils";

type PromptInputCommandItemProps = InstanceType<typeof CommandItem>["$props"];

interface Props extends /* @vue-ignore */ PromptInputCommandItemProps {
  class?: HTMLAttributes["class"];
}

const props = defineProps<Props>();
</script>

<template>
  <CommandItem v-bind="props" :class="cn(props.class)">
    <slot />
  </CommandItem>
</template>
