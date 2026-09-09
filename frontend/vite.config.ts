import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import { configDefaults } from "vitest/config";
import vue from "@vitejs/plugin-vue";
export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  server: {
    proxy: { "/api": { target: "http://127.0.0.1:8080", changeOrigin: true } },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./tests/setup.ts"],
    // Playwright 场景由 `pnpm run test:e2e` 单独运行，不能作为 Vitest 模块导入。
    exclude: [...configDefaults.exclude, "tests/e2e/**"],
  },
});
