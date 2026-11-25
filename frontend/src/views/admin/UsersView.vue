<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { User } from "@/types";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppButton from "@/components/ui/AppButton.vue";
import AppCard from "@/components/ui/AppCard.vue";

const users = ref<User[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const newEmail = ref("");
const newPassword = ref("");
const creating = ref(false);

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

async function createUser() {
  error.value = null;
  if (!newEmail.value.trim() || !newPassword.value) {
    error.value = "Vyplňte e-mail a heslo.";
    return;
  }
  creating.value = true;
  try {
    await adminApi.createUser({
      email: newEmail.value.trim(),
      password: newPassword.value,
      role: "USER",
      dailyLimit: 20,
    });
    newEmail.value = "";
    newPassword.value = "";
    await load();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Uživatele se nepodařilo přidat.";
  } finally {
    creating.value = false;
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

      <AppCard class="create-user-card" title="Nový uživatel">
        <form class="form-grid" @submit.prevent="createUser">
          <div class="form-field">
            <label class="form-label" for="new-user-email">E-mail</label>
            <input
              id="new-user-email"
              v-model="newEmail"
              class="app-input"
              type="email"
              data-testid="new-user-email"
              autocomplete="off"
              required
            />
          </div>
          <div class="form-field">
            <label class="form-label" for="new-user-password">Heslo</label>
            <input
              id="new-user-password"
              v-model="newPassword"
              class="app-input"
              type="password"
              data-testid="new-user-password"
              autocomplete="new-password"
              required
            />
          </div>
          <div class="form-field">
            <AppButton type="submit" data-testid="create-user" :loading="creating">
              Přidat uživatele
            </AppButton>
          </div>
        </form>
      </AppCard>

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
