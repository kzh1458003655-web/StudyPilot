import { ref } from "vue";
import { defineStore } from "pinia";
import { askQuestion } from "../api/requests";
import type { QaAnswer } from "../types/domain";

const pending = new Map<number, Promise<QaAnswer>>();

export const useQaTaskStore = defineStore("qa-tasks", () => {
  const active = ref<Record<number, boolean>>({});

  function ask(
    projectId: number,
    question: string,
    sessionId?: number,
  ): Promise<QaAnswer> {
    const existing = pending.get(projectId);
    if (existing) return existing;
    active.value[projectId] = true;
    const task = askQuestion(projectId, question, sessionId).finally(() => {
      pending.delete(projectId);
      active.value[projectId] = false;
    });
    pending.set(projectId, task);
    return task;
  }

  function current(projectId: number) {
    return pending.get(projectId);
  }

  return { active, ask, current };
});
