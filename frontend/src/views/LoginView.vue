<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import AppButton from "@/components/ui/AppButton.vue";
import AppCard from "@/components/ui/AppCard.vue";
import AppInput from "@/components/ui/AppInput.vue";

const email = ref("");
const password = ref("");
const error = ref("");
const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

async function submit() {
  error.value = "";
  try {
    await auth.login(email.value, password.value);
    const redirect =
      typeof route.query.redirect === "string" ? route.query.redirect : "/history";
    await router.replace(redirect);
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Přihlášení se nepodařilo.";
  }
}
</script>

<template>
  <main class="login-page">
    <AppCard class="login-card">
      <h1>AI Shop Assistant</h1>
      <p class="muted">Přihlaste se prosím svým účtem.</p>

      <form data-testid="login-form" @submit.prevent="submit">
        <AppInput
          id="email"
          v-model="email"
          label="E-mail"
          type="email"
          autocomplete="username"
          required
        />
        <AppInput
          id="password"
          v-model="password"
          label="Heslo"
          type="password"
          autocomplete="current-password"
          required
        />

        <p v-if="error" class="form-error" role="alert">{{ error }}</p>

        <AppButton type="submit" block :loading="auth.loading">Přihlásit se</AppButton>
      </form>
    </AppCard>
  </main>
</template>
