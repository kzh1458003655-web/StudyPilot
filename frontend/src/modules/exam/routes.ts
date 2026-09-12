import type { RouteRecordRaw } from "vue-router";
export const examRoutes: RouteRecordRaw[] = [
  {
    path: "/projects/:projectId/exams",
    name: "project-exams",
    component: () => import("./pages/ExamPage.vue"),
    meta: { title: "智能组卷" },
  },
  {
    path: "/projects/:projectId/frequency",
    name: "project-frequency",
    component: () => import("./pages/FrequencyPage.vue"),
    meta: { title: "考频分析" },
  },
];
