<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { SelectValue } from "@/shared/ui/select";
import { cn } from "@/shared/utils";

type PromptInputSelectValueProps = InstanceType<typeof SelectValue>["$props"];

interface Props extends /* @vue-ignore */ PromptInputSelectValueProps {
  class?: HTMLAttributes["class"];
}

const props = defineProps<Props>();
</script>

<template>
  <SelectValue :class="cn(props.class)" v-bind="props">
    <slot />
  </SelectValue>
</template>
