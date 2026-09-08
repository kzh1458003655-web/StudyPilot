import type { RouteRecordRaw } from "vue-router";

export const documentRoutes: RouteRecordRaw[] = [
  {
    path: "/projects/:projectId/resources",
    name: "project-resources",
    component: () => import("./pages/ResourcePage.vue"),
    meta: { title: "资料管理" },
  },
];
