<script setup lang="ts">
import AppShell from "./layouts/AppShell.vue";
import { Toaster } from "@/shared/ui/sonner";
</script>
<template>
  <AppShell><RouterView /></AppShell>
  <Toaster position="bottom-right" rich-colors />
</template>
