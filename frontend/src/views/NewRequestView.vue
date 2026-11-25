<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { requestsApi } from "@/api/requests.api";
import type { RequestOptions } from "@/types";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppButton from "@/components/ui/AppButton.vue";
import AppCard from "@/components/ui/AppCard.vue";
import PhotoUpload from "@/components/upload/PhotoUpload.vue";

const router = useRouter();
const photo = ref<File | null>(null);
const description = ref("");
const submitting = ref(false);
const error = ref<string | null>(null);

const options = reactive<RequestOptions>({
  generateWebText: true,
  generateSocialPost: true,
  generateModelPhoto: false,
  sizeFrom: "",
  sizeTo: "",
  basePrice: undefined,
  priceIncreasePercent: undefined,
});

async function submit() {
  error.value = null;

  if (!photo.value) {
    error.value = "Vyberte prosím fotku produktu.";
    return;
  }
  if (description.value.trim().length < 10) {
    error.value = "Napište prosím alespoň pár slov o produktu.";
    return;
  }

  submitting.value = true;
  try {
    const created = await requestsApi.create({
      photo: photo.value,
      description: description.value.trim(),
      options,
    });
    await router.push("/requests/" + created.id);
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Požadavek se nepodařilo vytvořit.";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <AppLayout>
    <PageContainer
      title="Nový požadavek"
      subtitle="Nahrajte fotku, napište pár slov a vyberte, co chcete vytvořit."
    >
      <form class="stack" data-testid="new-request-form" @submit.prevent="submit">
        <AppCard title="1. Fotka produktu">
          <PhotoUpload v-model="photo" />
        </AppCard>

        <AppCard title="2. Popis produktu">
          <div class="form-field">
            <label class="form-label" for="description">Popis produktu</label>
            <textarea
              id="description"
              v-model="description"
              class="app-textarea"
              placeholder="Např. ručně pletená čepice, šedá, 100% vlna, velikost univerzální."
              required
            ></textarea>
            <span class="form-hint">
              Stačí pár vět. Čím konkrétnější popis, tím lepší výsledek.
            </span>
          </div>
        </AppCard>

        <AppCard title="3. Co vytvořit">
          <div class="stack">
            <label class="checkbox">
              <input v-model="options.generateWebText" type="checkbox" />
              <span>Popis produktu pro e-shop</span>
            </label>
            <label class="checkbox">
              <input v-model="options.generateSocialPost" type="checkbox" />
              <span>Příspěvek na Instagram a Facebook</span>
            </label>
            <label class="checkbox">
              <input v-model="options.generateModelPhoto" type="checkbox" />
              <span>Fotka produktu na modelovi (volitelné, může trvat déle)</span>
            </label>
          </div>
        </AppCard>

        <AppCard title="4. Velikosti a cena">
          <div class="form-grid">
            <div class="form-field">
              <label class="form-label" for="size-from">Počáteční velikost</label>
              <input
                id="size-from"
                v-model="options.sizeFrom"
                class="app-input"
                placeholder="např. XS"
              />
            </div>
            <div class="form-field">
              <label class="form-label" for="size-to">Koncová velikost</label>
              <input
                id="size-to"
                v-model="options.sizeTo"
                class="app-input"
                placeholder="např. XXL"
              />
            </div>
            <div class="form-field">
              <label class="form-label" for="base-price">Počáteční cena (Kč)</label>
              <input
                id="base-price"
                v-model.number="options.basePrice"
                class="app-input"
                type="number"
                min="0"
                step="1"
                placeholder="např. 490"
              />
            </div>
            <div class="form-field">
              <label class="form-label" for="price-increase-percent">
                Navýšení ceny o % (každou další velikost)
              </label>
              <input
                id="price-increase-percent"
                v-model.number="options.priceIncreasePercent"
                class="app-input"
                type="number"
                min="0"
                step="0.1"
                placeholder="např. 5"
              />
            </div>
          </div>
          <span class="form-hint">
            Vyplňte samostatně počáteční a koncovou velikost, počáteční cenu
            a navýšení ceny v procentech pro každou další velikost (krok 6).
          </span>
        </AppCard>

        <p v-if="error" class="alert alert--error" role="alert">{{ error }}</p>

        <div class="row">
          <AppButton type="submit" :loading="submitting">Vytvořit</AppButton>
          <RouterLink to="/history" class="app-button app-button--secondary">
            Zrušit
          </RouterLink>
        </div>
      </form>
    </PageContainer>
  </AppLayout>
</template>
