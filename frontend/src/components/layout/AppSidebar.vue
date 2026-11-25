<script setup lang="ts">
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
</script>

<template>
  <aside class="app-sidebar">
    <RouterLink to="/history" class="brand">AI Shop Assistant</RouterLink>

    <nav class="sidebar-nav" aria-label="Hlavní navigace">
      <RouterLink to="/history" class="sidebar-link" active-class="sidebar-link--active">
        Historie
      </RouterLink>
      <RouterLink to="/requests/new" class="sidebar-link" active-class="sidebar-link--active">
        Nový požadavek
      </RouterLink>

      <template v-if="auth.isAdmin">
        <p class="sidebar-section">Administrace</p>
        <RouterLink
          to="/admin/shop-queue"
          class="sidebar-link"
          active-class="sidebar-link--active"
        >
          Fronta e-shopu
        </RouterLink>
        <RouterLink to="/admin/users" class="sidebar-link" active-class="sidebar-link--active">
          Uživatelé
        </RouterLink>
        <RouterLink to="/admin/logs" class="sidebar-link" active-class="sidebar-link--active">
          Logy
        </RouterLink>
        <RouterLink to="/admin/stats" class="sidebar-link" active-class="sidebar-link--active">
          Statistiky
        </RouterLink>
      </template>
    </nav>
  </aside>
</template>
