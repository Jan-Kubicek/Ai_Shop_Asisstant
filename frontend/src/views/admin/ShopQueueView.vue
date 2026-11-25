<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { ShopQueueItem } from "@/types";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppButton from "@/components/ui/AppButton.vue";

const items = ref<ShopQueueItem[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const page = await adminApi.shopQueue();
    items.value = page.items;
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Frontu se nepodařilo načíst.";
  } finally {
    loading.value = false;
  }
}

async function markImported(item: ShopQueueItem) {
  try {
    await adminApi.markImported(item.id);
    await load();
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Změnu se nepodařilo uložit.";
  }
}

async function download(item: ShopQueueItem) {
  try {
    const blob = await adminApi.exportJson(item.id);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "woo-" + item.requestId + ".json";
    link.click();
    URL.revokeObjectURL(url);
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Export se nepodařilo stáhnout.";
  }
}

onMounted(load);
</script>

<template>
  <AppLayout>
    <PageContainer
      title="Fronta e-shopu"
      subtitle="Požadavky připravené k importu do WooCommerce."
    >
      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <div v-if="!loading && items.length === 0" class="empty-state">
        Fronta je prázdná.
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Vytvořeno</th>
              <th>ID požadavku</th>
              <th>Stav</th>
              <th>Poznámka</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ item.requestId }}</td>
              <td>{{ item.status }}</td>
              <td>{{ item.note ?? "–" }}</td>
              <td>
                <div class="row">
                  <AppButton size="sm" variant="secondary" @click="download(item)">
                    Stáhnout JSON
                  </AppButton>
                  <AppButton size="sm" @click="markImported(item)">
                    Importováno
                  </AppButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </PageContainer>
  </AppLayout>
</template>
