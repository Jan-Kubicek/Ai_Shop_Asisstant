<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { Stats } from "@/types";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";

const stats = ref<Stats | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = null;
  try {
    stats.value = await adminApi.stats();
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Statistiky se nepodařilo načíst.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <AppLayout>
    <PageContainer title="Statistiky" subtitle="Přehled využití aplikace.">
      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <div v-if="stats" class="stats-grid">
        <article class="stat-card">
          <div class="stat-card__value">{{ stats.requestsTotal }}</div>
          <div class="stat-card__label">Požadavků celkem</div>
        </article>
        <article class="stat-card">
          <div class="stat-card__value">{{ stats.requestsToday }}</div>
          <div class="stat-card__label">Požadavků dnes</div>
        </article>
        <article class="stat-card">
          <div class="stat-card__value">{{ stats.failedTotal }}</div>
          <div class="stat-card__label">Neúspěšných</div>
        </article>
        <article class="stat-card">
          <div class="stat-card__value">{{ stats.aiTokensTotal }}</div>
          <div class="stat-card__label">Spotřebovaných tokenů</div>
        </article>
        <article class="stat-card">
          <div class="stat-card__value">{{ stats.activeUsers }}</div>
          <div class="stat-card__label">Aktivních uživatelů</div>
        </article>
      </div>
    </PageContainer>
  </AppLayout>
</template>
