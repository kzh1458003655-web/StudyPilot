import type { RouteRecordRaw } from "vue-router";
import { architectureRoutes } from "@/modules/architecture-sample";
import { documentRoutes } from "@/modules/document";
import { projectRoutes } from "@/modules/project";
import { qaRoutes } from "@/modules/qa";
export const routes: RouteRecordRaw[] = [
  { path: "/", redirect: "/projects" },
  ...architectureRoutes,
  ...projectRoutes,
  ...documentRoutes,
  ...qaRoutes,
];
