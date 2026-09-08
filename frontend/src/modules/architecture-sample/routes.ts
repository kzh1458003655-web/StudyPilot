import type { RouteRecordRaw } from "vue-router";
export const architectureRoutes: RouteRecordRaw[] = [
  {
    path: "/architecture",
    name: "architecture-home",
    component: () => import("./pages/ArchitecturePage.vue"),
    meta: { title: "架构骨架" },
  },
];
