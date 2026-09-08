import type { RouteRecordRaw } from "vue-router";

export const projectRoutes: RouteRecordRaw[] = [
  {
    path: "/projects",
    name: "project-start",
    component: () => import("./pages/ProjectStartPage.vue"),
    meta: { title: "学习项目" },
  },
];
