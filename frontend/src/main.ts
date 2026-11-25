import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import { router } from "./router";
import { useAuthStore } from "./stores/auth";
import "./styles/tokens.css";
import "./styles/base.css";
import "./styles/components.css";

async function bootstrap() {
  const app = createApp(App);
  app.use(createPinia());
  app.use(router);

  const auth = useAuthStore();
  await auth.bootstrap();

  await router.isReady();
  app.mount("#app");
}

void bootstrap();
