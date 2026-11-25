<script setup lang="ts">
import { onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useRequestsStore } from "@/stores/requests";
import { requestsApi } from "@/api/requests.api";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import StatusBadge from "@/components/ui/StatusBadge.vue";

const store = useRequestsStore();
const { items, loading, error } = storeToRefs(store);
const actionError = ref<string | null>(null);

async function remove(id: string) {
  if (!window.confirm("Opravdu chcete požadavek odstranit?")) {
    return;
  }
  actionError.value = null;
  try {
    await requestsApi.delete(id);
    await store.fetchList();
  } catch (cause) {
    actionError.value =
      cause instanceof Error ? cause.message : "Odstranění se nepodařilo.";
  }
}

onMounted(() => {
  void store.fetchList();
});
</script>

<template>
  <AppLayout>
    <PageContainer
      title="Historie"
      subtitle="Vaše požadavky a jejich výsledky."
      data-testid="history-view"
    >
      <template #actions>
        <RouterLink
          to="/requests/new"
          class="app-button"
          data-testid="new-request-link"
        >
          Nový požadavek
        </RouterLink>
      </template>

      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="actionError" class="alert alert--error">{{ actionError }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <div v-if="!loading && items.length === 0" class="empty-state">
        Zatím nemáte žádný požadavek. Začněte tlačítkem „Nový požadavek“.
      </div>

      <div v-else class="table-wrap">
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
                <div class="row">
                  <RouterLink :to="'/requests/' + item.id">Detail</RouterLink>
                  <button
                    type="button"
                    class="app-button app-button--danger app-button--sm"
                    :data-testid="'delete-request-' + item.id"
                    @click="remove(item.id)"
                  >
                    Odstranit
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </PageContainer>
  </AppLayout>
</template>
