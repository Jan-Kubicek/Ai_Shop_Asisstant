<script setup lang="ts">
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import AppButton from "@/components/ui/AppButton.vue";

const auth = useAuthStore();
const router = useRouter();

async function logout() {
  await auth.logout();
  await router.push({ name: "login" });
}
</script>

<template>
  <header class="app-header">
    <div class="row">
      <slot />
    </div>
    <div class="row">
      <span class="muted">{{ auth.user?.email }}</span>
      <AppButton variant="ghost" size="sm" @click="logout">Odhlásit</AppButton>
    </div>
  </header>
</template>
