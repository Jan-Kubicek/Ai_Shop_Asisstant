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
const selected = ref<Set<string>>(new Set());
const exporting = ref(false);
const queuing = ref(false);

function toggle(id: string) {
  if (selected.value.has(id)) {
    selected.value.delete(id);
  } else {
    selected.value.add(id);
  }
  selected.value = new Set(selected.value);
}

async function queueSelected() {
  if (selected.value.size === 0) {
    actionError.value = "Vyberte alespoň jeden požadavek.";
    return;
  }
  queuing.value = true;
  actionError.value = null;
  try {
    await Promise.all([...selected.value].map((id) => requestsApi.approve(id)));
    selected.value = new Set();
    await store.fetchList();
  } catch (cause) {
    actionError.value =
      cause instanceof Error ? cause.message : "Přidání do fronty se nepodařilo.";
  } finally {
    queuing.value = false;
  }
}

async function remove(id: string) {
  if (!window.confirm("Opravdu chcete požadavek odstranit?")) {
    return;
  }
  actionError.value = null;
  try {
    await requestsApi.delete(id);
    selected.value.delete(id);
    selected.value = new Set(selected.value);
    await store.fetchList();
  } catch (cause) {
    actionError.value =
      cause instanceof Error ? cause.message : "Odstranění se nepodařilo.";
  }
}

async function exportCombined() {
  if (selected.value.size === 0) {
    actionError.value = "Vyberte alespoň jeden požadavek.";
    return;
  }
  exporting.value = true;
  actionError.value = null;
  try {
    const results = await Promise.all(
      [...selected.value].map((id) => requestsApi.result(id))
    );
    const combined = results
      .map((r) => r.wooJson)
      .filter((w) => w !== undefined && w !== null);
    const blob = new Blob([JSON.stringify(combined, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "woo-export.json";
    a.click();
    URL.revokeObjectURL(url);
  } catch (cause) {
    actionError.value =
      cause instanceof Error ? cause.message : "Export se nepodařil.";
  } finally {
    exporting.value = false;
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
        <div class="row">
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="selected.size === 0 || queuing"
            @click="queueSelected"
          >
            {{ queuing ? "Přidávám…" : "Přidat do fronty (" + selected.size + ")" }}
          </button>
          <button
            type="button"
            class="app-button app-button--secondary"
            :disabled="selected.size === 0 || exporting"
            @click="exportCombined"
          >
            {{ exporting ? "Exportuji…" : "Exportovat JSON (" + selected.size + ")" }}
          </button>
          <RouterLink
            to="/requests/new"
            class="app-button"
            data-testid="new-request-link"
          >
            Nový požadavek
          </RouterLink>
        </div>
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
              <th></th>
              <th>Vytvořeno</th>
              <th>Popis</th>
              <th>Stav</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>
                <input
                  type="checkbox"
                  :checked="selected.has(item.id)"
                  @change="toggle(item.id)"
                />
              </td>
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