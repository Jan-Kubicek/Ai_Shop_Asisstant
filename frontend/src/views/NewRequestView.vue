<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { onMounted } from "vue";
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
const draftRestored = ref(false);

const DRAFT_KEY = "new-request-draft";

const defaultOptions: RequestOptions = {
  generateWebText: true,
  generateSocialPost: true,
  generateModelPhoto: false,
  modelDescription: "",
  sizeFrom: "",
  sizeTo: "",
  basePrice: undefined,
  priceIncreasePercent: undefined,
};

const options = reactive<RequestOptions>({ ...defaultOptions });

function saveDraft() {
  try {
    localStorage.setItem(
      DRAFT_KEY,
      JSON.stringify({ description: description.value, options })
    );
  } catch {
    // ignore
  }
}

function restoreDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw) as { description?: string; options?: Partial<RequestOptions> };
    if (parsed.description) description.value = parsed.description;
    if (parsed.options) Object.assign(options, defaultOptions, parsed.options);
    return true;
  } catch {
    return false;
  }
}

function clearDraft() {
  try {
    localStorage.removeItem(DRAFT_KEY);
  } catch {
    // ignore
  }
}

onMounted(() => {
  if (restoreDraft()) {
    draftRestored.value = true;
  }
});

watch([description, options], saveDraft, { deep: true });

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
    clearDraft();
    await router.push("/requests/" + created.id);
  } catch (cause) {
    error.value =
      cause instanceof Error ? cause.message : "Požadavek se nepodařilo vytvořit.";
  } finally {
    submitting.value = false;
  }
}

function discardDraft() {
  description.value = "";
  Object.assign(options, defaultOptions);
  clearDraft();
  draftRestored.value = false;
}
</script>

<template>
  <AppLayout>
    <PageContainer
      title="Nový požadavek"
      subtitle="Nahrajte fotku, napište pár slov a vyberte, co chcete vytvořit."
    >
      <p v-if="draftRestored" class="alert alert--info" role="status">
        Obnovili jsme rozpracovaný požadavek (text a volby). Fotku je potřeba vybrat znovu.
        <button type="button" class="link-button" @click="discardDraft">Zahodit</button>
      </p>
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

          <div class="form-field">
            <label class="form-label" for="model-description">
              Popis modelu (kdo má být na fotce, co má mít na sobě)
            </label>
            <textarea
              id="model-description"
              v-model="options.modelDescription"
              class="app-textarea"
              data-testid="model-description"
              placeholder="Např. dívka 5 let, šedá čepice z popisu produktu, zimní bunda, neutrální studiové pozadí, fotografie."
              rows="3"
            ></textarea>
            <span class="form-hint">
              Použije se jen pokud zaškrtnete fotku produktu na modelovi.
              Popis nesmí obsahovat nahotu ani sexualizaci.
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
