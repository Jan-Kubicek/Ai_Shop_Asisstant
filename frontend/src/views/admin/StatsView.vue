<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { Stats } from "@/types";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppCard from "@/components/ui/AppCard.vue";

const stats = ref<Stats | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

const requestBars = computed(() => {
  const data = stats.value?.requestsDaily ?? [];
  const max = Math.max(1, ...data.map((item) => item.requests));
  return data.map((item) => ({
    label: item.date.slice(5),
    height: Math.max(2, Math.round((item.requests / max) * 100)),
    value: item.requests,
    failed: item.failed,
  }));
});

const tokenBars = computed(() => {
  const data = stats.value?.tokensDaily ?? [];
  const max = Math.max(1, ...data.map((item) => item.tokens));
  return data.map((item) => ({
    label: item.date.slice(5),
    height: Math.max(2, Math.round((item.tokens / max) * 100)),
    value: item.tokens,
  }));
});

const cost = computed(() =>
  stats.value ? `$${stats.value.estimatedCostUsd.toFixed(4)}` : "—",
);

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
      <template #actions>
        <button type="button" class="app-button app-button--secondary" @click="load">
          Obnovit
        </button>
      </template>

      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <div v-if="stats" class="stack">
        <div class="stats-grid">
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
          <article class="stat-card stat-card--accent">
            <div class="stat-card__value" data-testid="estimated-cost">{{ cost }}</div>
            <div class="stat-card__label">Odhad nákladů na AI (USD)</div>
          </article>
        </div>

        <div class="chart-grid">
          <AppCard title="Požadavky za posledních 7 dní">
            <div class="chart" data-testid="requests-chart">
              <div v-for="bar in requestBars" :key="bar.label" class="chart__column">
                <span class="chart__value">{{ bar.value }}</span>
                <div class="chart__bar" :style="{ height: bar.height + '%' }"></div>
                <span class="chart__label">{{ bar.label }}</span>
              </div>
            </div>
          </AppCard>

          <AppCard title="Tokeny za posledních 7 dní">
            <div class="chart" data-testid="tokens-chart">
              <div v-for="bar in tokenBars" :key="bar.label" class="chart__column">
                <span class="chart__value">{{ bar.value }}</span>
                <div
                  class="chart__bar chart__bar--tokens"
                  :style="{ height: bar.height + '%' }"
                ></div>
                <span class="chart__label">{{ bar.label }}</span>
              </div>
            </div>
          </AppCard>
        </div>

        <p class="muted">
          Cena je orientační: vychází z počtu vstupních a výstupních tokenů
          v <code>ai_calls</code> a z ceníku nastaveného v <code>.env</code>
          (<code>AI_COST_INPUT_USD_PER_M</code>, <code>AI_COST_OUTPUT_USD_PER_M</code>).
        </p>
      </div>
    </PageContainer>
  </AppLayout>
</template>
