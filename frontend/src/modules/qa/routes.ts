import type { RouteRecordRaw } from "vue-router";

export const qaRoutes: RouteRecordRaw[] = [
  {
    path: "/projects/:projectId/qa",
    name: "project-qa",
    component: () => import("./pages/QaPage.vue"),
    meta: { title: "知识问答" },
  },
];
