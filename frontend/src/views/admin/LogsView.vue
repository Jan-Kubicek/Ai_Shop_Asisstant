<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { AuditEvent } from "@/types";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";

const events = ref<AuditEvent[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const action = ref("");
const account = ref("");
const newestFirst = ref(true);

function detailsText(event: AuditEvent): string {
  if (!event.details) {
    return "–";
  }
  return JSON.stringify(event.details);
}

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const page = await adminApi.audit(
      0,
      50,
      action.value,
      account.value,
      newestFirst.value,
    );
    events.value = page.items;
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Logy se nepodařilo načíst.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <AppLayout>
    <PageContainer title="Logy" subtitle="Auditní záznamy akcí v aplikaci.">
      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <form class="form-grid" @submit.prevent="load">
        <div class="form-field">
          <label class="form-label" for="log-action">Akce</label>
          <input
            id="log-action"
            v-model="action"
            class="app-input"
            data-testid="log-action"
            placeholder="např. LOGIN"
          />
        </div>
        <div class="form-field">
          <label class="form-label" for="log-account">Účet (ID nebo e-mail)</label>
          <input
            id="log-account"
            v-model="account"
            class="app-input"
            data-testid="log-account"
            placeholder="např. admin@example.com"
          />
        </div>
        <div class="form-field">
          <label class="form-label" for="log-order">Řazení</label>
          <select
            id="log-order"
            v-model="newestFirst"
            class="app-select"
            data-testid="log-order"
          >
            <option :value="true">Nejnovější první</option>
            <option :value="false">Nejstarší první</option>
          </select>
        </div>
        <div class="form-field">
          <button type="submit" class="app-button" data-testid="log-search">
            Hledat
          </button>
        </div>
      </form>

      <div v-if="!loading && events.length === 0" class="empty-state">
        Zatím nejsou žádné záznamy.
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Čas</th>
              <th>Akce</th>
              <th>Uživatel</th>
              <th>IP</th>
              <th>Detaily</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="event in events" :key="event.id">
              <td>{{ formatDateTime(event.timestamp) }}</td>
              <td>{{ event.action }}</td>
              <td>{{ event.userId ?? "–" }}</td>
              <td>{{ event.ip ?? "–" }}</td>
              <td><code>{{ detailsText(event) }}</code></td>
            </tr>
          </tbody>
        </table>
      </div>
    </PageContainer>
  </AppLayout>
</template>
