import type { RouteRecordRaw } from "vue-router";
import { assessmentRoutes } from "@/modules/assessment";
import { architectureRoutes } from "@/modules/architecture-sample";
import { documentRoutes } from "@/modules/document";
import { examRoutes } from "@/modules/exam";
import { projectRoutes } from "@/modules/project";
import { qaRoutes } from "@/modules/qa";
export const routes: RouteRecordRaw[] = [
  { path: "/", redirect: "/projects" },
  ...architectureRoutes,
  ...assessmentRoutes,
  ...projectRoutes,
  ...documentRoutes,
  ...examRoutes,
  ...qaRoutes,
];
