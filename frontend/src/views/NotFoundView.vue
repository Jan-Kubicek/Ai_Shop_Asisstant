<script setup lang="ts">
import { RouterLink } from "vue-router";
</script>

<template>
  <main class="login-page">
    <section class="app-card login-card">
      <div class="app-card__body">
        <h1>Stránka nenalezena</h1>
        <p class="muted">Odkaz je neplatný nebo stránka už neexistuje.</p>
        <RouterLink to="/history" class="app-button">Zpět na historii</RouterLink>
      </div>
    </section>
  </main>
</template>
