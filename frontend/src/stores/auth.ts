import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { authApi } from "@/api/auth.api";
import { getAccessToken, setAccessToken } from "@/api/http";
import type { User } from "@/types";

export const useAuthStore = defineStore("auth", () => {
  const user = ref<User | null>(null);
  const loading = ref(false);
  const initialized = ref(false);

  const isAuthenticated = computed(() => user.value !== null);
  const isAdmin = computed(() => user.value?.role === "ADMIN");

  async function bootstrap() {
    if (initialized.value) {
      return;
    }
    try {
      const tokens = await authApi.refresh();
      setAccessToken(tokens.accessToken);
      user.value = tokens.user;
    } catch {
      setAccessToken(null);
      user.value = null;
    } finally {
      initialized.value = true;
    }
  }

  async function login(email: string, password: string) {
    loading.value = true;
    try {
      const tokens = await authApi.login({ email, password });
      setAccessToken(tokens.accessToken);
      user.value = tokens.user;
    } finally {
      loading.value = false;
    }
  }

  async function loadMe() {
    if (!getAccessToken()) {
      return;
    }
    try {
      user.value = await authApi.me();
    } catch {
      clear();
    }
  }

  function clear() {
    setAccessToken(null);
    user.value = null;
  }

  async function logout() {
    try {
      await authApi.logout();
    } finally {
      clear();
    }
  }

  return {
    user,
    loading,
    initialized,
    isAuthenticated,
    isAdmin,
    bootstrap,
    login,
    loadMe,
    logout,
    clear,
  };
});
