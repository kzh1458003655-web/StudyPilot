import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import { router } from "./router";
import "@/shared/styles/index.css";

async function enableFrontendMock(): Promise<void> {
  if (import.meta.env.MODE !== "mock") return;
  const { mockWorker } = await import("@/mocks/browser");
  await mockWorker.start({
    onUnhandledRequest: "bypass",
    serviceWorker: { url: "/mockServiceWorker.js" },
  });
}

void enableFrontendMock().then(() => {
  createApp(App).use(createPinia()).use(router).mount("#app");
});
