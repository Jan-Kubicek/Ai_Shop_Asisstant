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

  // Nejdřív obnovit přihlášení, teprve potom spustit router.
  // Jinak guard vyhodnotí uživatele jako nepřihlášeného a přesměruje na login.
  const auth = useAuthStore();
  await auth.bootstrap();

  app.use(router);
  await router.isReady();
  app.mount("#app");
}

void bootstrap();
