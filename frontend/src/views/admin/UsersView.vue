<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { User } from "@/types";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppButton from "@/components/ui/AppButton.vue";

const users = ref<User[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const page = await adminApi.users();
    users.value = page.items;
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Uživatele se nepodařilo načíst.";
  } finally {
    loading.value = false;
  }
}

async function toggleEnabled(user: User) {
  try {
    await adminApi.updateUser(user.id, { enabled: !user.enabled });
    await load();
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Změnu se nepodařilo uložit.";
  }
}

onMounted(load);
</script>

<template>
  <AppLayout>
    <PageContainer title="Uživatelé" subtitle="Účty, role a denní limity.">
      <p v-if="error" class="alert alert--error">{{ error }}</p>
      <p v-if="loading" class="muted">Načítám…</p>

      <div v-if="!loading && users.length === 0" class="empty-state">
        Zatím nejsou vytvořeny žádné účty.
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>E-mail</th>
              <th>Role</th>
              <th>Denní limit</th>
              <th>Stav</th>
              <th>Vytvořeno</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.email }}</td>
              <td>{{ user.role }}</td>
              <td>{{ user.dailyLimit }}</td>
              <td>{{ user.enabled ? "Aktivní" : "Zablokovaný" }}</td>
              <td>{{ formatDateTime(user.createdAt) }}</td>
              <td>
                <AppButton size="sm" variant="secondary" @click="toggleEnabled(user)">
                  {{ user.enabled ? "Zablokovat" : "Aktivovat" }}
                </AppButton>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </PageContainer>
  </AppLayout>
</template>
