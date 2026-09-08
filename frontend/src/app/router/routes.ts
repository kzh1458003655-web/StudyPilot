import type { RouteRecordRaw } from "vue-router";
import { architectureRoutes } from "@/modules/architecture-sample";
export const routes: RouteRecordRaw[] = [
  { path: "/", redirect: "/architecture" },
  ...architectureRoutes,
];
