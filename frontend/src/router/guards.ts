import type { Router } from "vue-router";
import { useAuthStore } from "@/stores/auth";

export function setupGuards(router: Router) {
  router.beforeEach((to) => {
    const auth = useAuthStore();

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return { name: "login", query: { redirect: to.fullPath } };
    }

    if (to.meta.requiresAdmin && !auth.isAdmin) {
      return { name: "history" };
    }

    if (to.name === "login" && auth.isAuthenticated) {
      return { name: "history" };
    }

    return true;
  });
}
