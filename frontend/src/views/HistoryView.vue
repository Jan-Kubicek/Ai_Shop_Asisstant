<script setup lang="ts">
import { onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useRequestsStore } from "@/stores/requests";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import StatusBadge from "@/components/ui/StatusBadge.vue";

const store = useRequestsStore();
const { items, loading, error } = storeToRefs(store);

onMounted(() => {
  void store.fetchList();
});
</script>

<template>
  <AppLayout>
    <PageContainer title="Historie" subtitle="Vaše požadavky a jejich výsledky.">
      <template #actions>
        <RouterLink to="/requests/new" class="app-button">Nový požadavek</RouterLink>
      </template>

      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <div v-if="!loading && items.length === 0" class="empty-state">
        Zatím nemáte žádný požadavek. Začněte tlačítkem „Nový požadavek“.
      </div>

      <div v-else data-testid="history-view" class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Vytvořeno</th>
              <th>Popis</th>
              <th>Stav</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ formatDateTime(item.createdAt) }}</td>
              <td>{{ item.description }}</td>
              <td><StatusBadge :status="item.status" /></td>
              <td>
                <RouterLink :to="'/requests/' + item.id">Detail</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </PageContainer>
  </AppLayout>
</template>
