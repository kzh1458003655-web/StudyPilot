import type { RouteRecordRaw } from "vue-router";
export const examRoutes: RouteRecordRaw[] = [
  {
    path: "/projects/:projectId/exams",
    name: "project-exams",
    component: () => import("./pages/ExamPage.vue"),
    meta: { title: "真题分析与模拟考" },
  },
];
