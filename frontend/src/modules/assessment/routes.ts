import type { RouteRecordRaw } from "vue-router";
export const assessmentRoutes: RouteRecordRaw[] = [
  {
    path: "/projects/:projectId/assessment",
    name: "project-assessment",
    component: () => import("./pages/AssessmentPage.vue"),
  },
  {
    path: "/projects/:projectId/exams/:examId/take",
    name: "project-exam-take",
    component: () => import("./pages/AssessmentPage.vue"),
  },
];
