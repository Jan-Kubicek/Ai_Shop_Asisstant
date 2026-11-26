<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { requestsApi } from "@/api/requests.api";
import { useRequestStatus } from "@/composables/useRequestStatus";
import { useAuthStore } from "@/stores/auth";
import type { RequestResult } from "@/types";
import { formatDateTime } from "@/utils/format";
import AppLayout from "@/components/layout/AppLayout.vue";
import PageContainer from "@/components/layout/PageContainer.vue";
import AppButton from "@/components/ui/AppButton.vue";
import AppCard from "@/components/ui/AppCard.vue";
import StatusBadge from "@/components/ui/StatusBadge.vue";
import ImagePreview from "@/components/result/ImagePreview.vue";
import JsonViewer from "@/components/result/JsonViewer.vue";
import TextResultCard from "@/components/result/TextResultCard.vue";

const route = useRoute();
const auth = useAuthStore();
const id = computed(() => String(route.params.id));

const { request, error, start, reload } = useRequestStatus(id.value, 1500);
const result = ref<RequestResult | null>(null);
const resultError = ref<string | null>(null);
const downloading = ref(false);
const saving = ref(false);
const approving = ref(false);
const editing = ref(false);
const regenerating = ref(false);
const regeneratePart = ref("webText");

const form = reactive({
  title: "",
  shortDescription: "",
  longDescription: "",
  keywords: "",
  instagramCaption: "",
  hashtags: "",
  facebookText: "",
});

const instagramText = computed(() => {
  const social = result.value?.socialText;
  if (!social) {
    return "";
  }
  return social.instagram.caption + "\n\n" + social.instagram.hashtags.join(" ");
});

const facebookText = computed(() => result.value?.socialText?.facebook.text ?? "");

function fillForm() {
  if (!result.value) {
    return;
  }
  const web = result.value.webText;
  const social = result.value.socialText;
  if (web) {
    form.title = web.title;
    form.shortDescription = web.shortDescription;
    form.longDescription = web.longDescription;
    form.keywords = web.keywords.join(", ");
  }
  if (social) {
    form.instagramCaption = social.instagram.caption;
    form.hashtags = social.instagram.hashtags.join(" ");
    form.facebookText = social.facebook.text;
  }
}

async function loadResult() {
  try {
    result.value = await requestsApi.result(id.value);
    fillForm();
    resultError.value = null;
  } catch {
    resultError.value = "Výsledek ještě není k dispozici.";
  }
}

async function save() {
  saving.value = true;
  try {
    const webText = {
      title: form.title,
      shortDescription: form.shortDescription,
      longDescription: form.longDescription,
      keywords: form.keywords.split(",").map((item) => item.trim()).filter(Boolean),
    };
    const socialText = {
      instagram: {
        caption: form.instagramCaption,
        hashtags: form.hashtags.split(/\s+/).map((item) => item.trim()).filter(Boolean),
      },
      facebook: { text: form.facebookText },
    };
    result.value = await requestsApi.updateResult(id.value, { webText, socialText });
    editing.value = false;
  } catch (cause) {
    resultError.value = cause instanceof Error ? cause.message : "Úpravu se nepodařilo uložit.";
  } finally {
    saving.value = false;
  }
}

async function approve() {
  approving.value = true;
  try {
    await requestsApi.approve(id.value);
    await reload();
    await loadResult();
  } catch (cause) {
    resultError.value = cause instanceof Error ? cause.message : "Schválení se nepodařilo.";
  } finally {
    approving.value = false;
  }
}

async function regenerate() {
  regenerating.value = true;
  resultError.value = null;
  try {
    await requestsApi.regenerate(id.value, regeneratePart.value);
    await loadResult();
  } catch (cause) {
    resultError.value =
      cause instanceof Error ? cause.message : "Opakované generování se nepodařilo.";
  } finally {
    regenerating.value = false;
  }
}

async function download() {
  downloading.value = true;
  try {
    const blob = await requestsApi.download(id.value);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "request-" + id.value + ".zip";
    link.click();
    URL.revokeObjectURL(url);
  } catch (cause) {
    resultError.value = cause instanceof Error ? cause.message : "Stažení se nepodařilo.";
  } finally {
    downloading.value = false;
  }
}

onMounted(async () => {
  start();
  await loadResult();
});

watch(
  () => request.value?.status,
  (status) => {
    if (status === "REVIEW" || status === "QUEUED_FOR_SHOP" || status === "IMPORTED") {
      void loadResult();
    }
  },
);
</script>

<template>
  <AppLayout>
    <PageContainer title="Detail požadavku" subtitle="Průběh a výsledky zpracování.">
      <template #actions>
        <div class="row">
          <AppButton
            data-testid="download-request"
            :loading="downloading"
            :disabled="!result"
            @click="download"
          >
            Stáhnout vše
          </AppButton>
          <AppButton
            v-if="auth.isAdmin && result && request?.status === 'REVIEW'"
            data-testid="approve-request"
            :loading="approving"
            @click="approve"
          >
            Schválit
          </AppButton>
        </div>
      </template>

      <p v-if="error" class="alert alert--error">{{ error }}</p>

      <div data-testid="request-status-wrapper" class="detail-grid">
        <div class="stack">
          <AppCard title="Stav">
            <div class="row row--between">
              <StatusBadge v-if="request" data-testid="request-status" :status="request.status" />
              <span class="muted">{{ request ? formatDateTime(request.createdAt) : "" }}</span>
            </div>
            <p v-if="request?.errorMessage" class="alert alert--error">{{ request.errorMessage }}</p>
            <p class="muted">{{ request?.description }}</p>
          </AppCard>

          <AppCard
            v-if="request?.sizePrices && request.sizePrices.length > 0"
            title="Velikosti a ceny"
          >
            <div data-testid="size-price-table" class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>Velikost</th>
                    <th>Cena (Kč)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in request.sizePrices" :key="item.size">
                    <td>{{ item.size }}</td>
                    <td>{{ item.price }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </AppCard>

          <template v-if="result?.webText && !editing">
            <AppCard title="Výsledky">
              <div data-testid="result-ready" class="stack">
                <TextResultCard title="Popis pro web" :text="result.webText.longDescription" />
                <TextResultCard title="Krátký popis" :text="result.webText.shortDescription" />
                <TextResultCard title="Instagram" :text="instagramText" />
                <TextResultCard title="Facebook" :text="facebookText" />
                <AppCard v-if="result.marketingText" title="Marketing a byznys">
                  <JsonViewer :value="result.marketingText" />
                </AppCard>
              </div>
              <div class="row">
                <AppButton data-testid="edit-result" variant="secondary" size="sm" @click="editing = true">
                  Upravit výsledek
                </AppButton>
              </div>

              <div class="row">
                <select
                  v-model="regeneratePart"
                  class="app-select"
                  data-testid="regenerate-part"
                  aria-label="Část k znovuvygenerování"
                >
                  <option value="webText">Popis pro web</option>
                  <option value="socialText">Instagram a Facebook</option>
                  <option value="marketingText">Marketing a byznys</option>
                  <option value="modelImage">Fotka modelu</option>
                </select>
                <AppButton
                  data-testid="regenerate-result"
                  variant="secondary"
                  size="sm"
                  :loading="regenerating"
                  @click="regenerate"
                >
                  Vygenerovat znovu
                </AppButton>
              </div>
            </AppCard>
          </template>

          <AppCard v-else-if="editing" title="Úprava výsledku">
            <div class="stack">
              <div class="form-field">
                <label class="form-label" for="edit-title">Název</label>
                <input id="edit-title" v-model="form.title" class="app-input" />
              </div>
              <div class="form-field">
                <label class="form-label" for="edit-short">Krátký popis</label>
                <textarea id="edit-short" v-model="form.shortDescription" class="app-textarea" rows="3"></textarea>
              </div>
              <div class="form-field">
                <label class="form-label" for="edit-long">Dlouhý popis</label>
                <textarea id="edit-long" v-model="form.longDescription" class="app-textarea" rows="6"></textarea>
              </div>
              <div class="form-field">
                <label class="form-label" for="edit-keywords">Klíčová slova (čárkou)</label>
                <input id="edit-keywords" v-model="form.keywords" class="app-input" />
              </div>
              <div class="form-field">
                <label class="form-label" for="edit-instagram">Instagram</label>
                <textarea id="edit-instagram" v-model="form.instagramCaption" class="app-textarea" rows="3"></textarea>
              </div>
              <div class="form-field">
                <label class="form-label" for="edit-hashtags">Hashtagy</label>
                <input id="edit-hashtags" v-model="form.hashtags" class="app-input" />
              </div>
              <div class="form-field">
                <label class="form-label" for="edit-facebook">Facebook</label>
                <textarea id="edit-facebook" v-model="form.facebookText" class="app-textarea" rows="3"></textarea>
              </div>
              <div class="row">
                <AppButton data-testid="save-result" :loading="saving" @click="save">Uložit</AppButton>
                <AppButton variant="secondary" @click="editing = false">Zrušit</AppButton>
              </div>
            </div>
          </AppCard>

          <p v-else class="muted">{{ resultError }}</p>

          <p v-if="resultError && result" class="alert alert--error">{{ resultError }}</p>
        </div>

        <div class="stack">
          <ImagePreview
            v-if="result?.modelImageUrl"
            :src="result.modelImageUrl"
            alt="Fotka produktu"
          />
          <AppCard v-if="result?.wooJson" title="JSON pro WordPress">
            <JsonViewer :value="result.wooJson" />
          </AppCard>
        </div>
      </div>
    </PageContainer>
  </AppLayout>
</template>
