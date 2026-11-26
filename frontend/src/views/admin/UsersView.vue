<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type { Role, User } from "@/types";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppButton from "@/components/ui/AppButton.vue";
import AppCard from "@/components/ui/AppCard.vue";
import AppModal from "@/components/ui/AppModal.vue";

const users = ref<User[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const newEmail = ref("");
const newPassword = ref("");
const creating = ref(false);

const editOpen = ref(false);
const editingUser = ref<User | null>(null);
const editRole = ref<Role>("USER");
const editLimit = ref(20);
const editEnabled = ref(true);
const saving = ref(false);

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

function openEdit(user: User) {
  editingUser.value = user;
  editRole.value = user.role;
  editLimit.value = user.dailyLimit;
  editEnabled.value = user.enabled;
  error.value = null;
  editOpen.value = true;
}

async function saveUser() {
  if (!editingUser.value) {
    return;
  }
  const limit = Number(editLimit.value);
  if (!Number.isInteger(limit) || limit < 1 || limit > 10000) {
    error.value = "Denní limit musí být celé číslo mezi 1 a 10000.";
    return;
  }
  saving.value = true;
  error.value = null;
  try {
    const updated = await adminApi.updateUser(editingUser.value.id, {
      role: editRole.value,
      enabled: editEnabled.value,
      dailyLimit: limit,
    });
    const index = users.value.findIndex((user) => user.id === updated.id);
    if (index >= 0) {
      users.value[index] = updated;
    }
    editOpen.value = false;
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Změnu se nepodařilo uložit.";
  } finally {
    saving.value = false;
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
              minlength="8"
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
                <div class="row">
                  <AppButton size="sm" variant="secondary" @click="openEdit(user)">
                    Upravit
                  </AppButton>
                  <AppButton size="sm" variant="secondary" @click="toggleEnabled(user)">
                    {{ user.enabled ? "Zablokovat" : "Aktivovat" }}
                  </AppButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppModal v-model:open="editOpen" title="Úprava uživatele">
        <form class="form-grid" @submit.prevent="saveUser">
          <p v-if="editingUser" class="muted">{{ editingUser.email }}</p>
          <div class="form-field">
            <label class="form-label" for="edit-user-role">Role</label>
            <select
              id="edit-user-role"
              v-model="editRole"
              class="app-select"
              data-testid="edit-user-role"
            >
              <option value="USER">Uživatel</option>
              <option value="ADMIN">Administrátor</option>
            </select>
          </div>
          <div class="form-field">
            <label class="form-label" for="edit-user-limit">Denní limit</label>
            <input
              id="edit-user-limit"
              v-model.number="editLimit"
              class="app-input"
              type="number"
              min="1"
              max="10000"
              data-testid="edit-user-limit"
            />
          </div>
          <div class="form-field">
            <label class="form-label" for="edit-user-enabled">Stav účtu</label>
            <select
              id="edit-user-enabled"
              v-model="editEnabled"
              class="app-select"
              data-testid="edit-user-enabled"
            >
              <option :value="true">Aktivní</option>
              <option :value="false">Zablokovaný</option>
            </select>
          </div>
        </form>

        <template #footer>
          <AppButton variant="secondary" @click="editOpen = false">Zrušit</AppButton>
          <AppButton data-testid="save-user" :loading="saving" @click="saveUser">
            Uložit
          </AppButton>
        </template>
      </AppModal>
    </PageContainer>
  </AppLayout>
</template>
