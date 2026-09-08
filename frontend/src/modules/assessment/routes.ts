import type { RouteRecordRaw } from "vue-router";
export const assessmentRoutes: RouteRecordRaw[] = [
  {
    path: "/projects/:projectId/assessment",
    name: "project-assessment",
    component: () => import("./pages/AssessmentPage.vue"),
  },
];
