import { ref } from "vue";
import { defineStore } from "pinia";
import { generateMockExam } from "../api/requests";

const pending = new Map<number, Promise<number>>();

export const useExamTaskStore = defineStore("exam-tasks", () => {
  const active = ref<Record<number, boolean>>({});

  function generate(projectId: number, instructions: string): Promise<number> {
    const existing = pending.get(projectId);
    if (existing) return existing;
    active.value[projectId] = true;
    const task = generateMockExam(projectId, instructions).finally(() => {
      pending.delete(projectId);
      active.value[projectId] = false;
    });
    pending.set(projectId, task);
    return task;
  }

  function current(projectId: number) {
    return pending.get(projectId);
  }

  return { active, generate, current };
});
