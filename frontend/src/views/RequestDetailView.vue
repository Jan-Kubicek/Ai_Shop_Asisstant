<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { requestsApi } from "@/api/requests.api";
import { useRequestStatus } from "@/composables/useRequestStatus";
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
const id = computed(() => String(route.params.id));

const { request, error, start } = useRequestStatus(id.value);
const result = ref<RequestResult | null>(null);
const resultError = ref<string | null>(null);
const downloading = ref(false);

const instagramText = computed(() => {
  const social = result.value?.socialText;
  if (!social) {
    return "";
  }
  return social.instagram.caption + "\n\n" + social.instagram.hashtags.join(" ");
});

const facebookText = computed(() => result.value?.socialText?.facebook.text ?? "");

async function loadResult() {
  try {
    result.value = await requestsApi.result(id.value);
    resultError.value = null;
  } catch {
    resultError.value = "Výsledek ještě není k dispozici.";
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
    resultError.value =
      cause instanceof Error ? cause.message : "Stažení se nepodařilo.";
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
        <AppButton :loading="downloading" :disabled="!result" @click="download">
          Stáhnout vše
        </AppButton>
      </template>

      <p v-if="error" class="alert alert--error">{{ error }}</p>

      <div class="detail-grid">
        <div class="stack">
          <AppCard title="Stav">
            <div class="row row--between">
              <StatusBadge
                v-if="request"
                data-testid="request-status"
                :status="request.status"
              />
              <span class="muted">
                {{ request ? formatDateTime(request.createdAt) : "" }}
              </span>
            </div>

            <p v-if="request?.errorMessage" class="alert alert--error">
              {{ request.errorMessage }}
            </p>
            <p class="muted">{{ request?.description }}</p>
          </AppCard>

          <template v-if="result">
            <TextResultCard
              v-if="result.webText"
              title="Popis pro web"
              :text="result.webText.longDescription"
            />
            <TextResultCard
              v-if="result.webText"
              title="Krátký popis"
              :text="result.webText.shortDescription"
            />
            <TextResultCard
              v-if="result.socialText"
              title="Instagram"
              :text="instagramText"
            />
            <TextResultCard
              v-if="result.socialText"
              title="Facebook"
              :text="facebookText"
            />
          </template>
          <p v-else class="muted">{{ resultError }}</p>
        </div>

        <div class="stack">
          <ImagePreview
            v-if="result?.modelImageUrl"
            :src="result.modelImageUrl"
            alt="Fotka produktu na modelovi"
          />

          <AppCard v-if="result?.wooJson" title="JSON pro WordPress">
            <JsonViewer :value="result.wooJson" />
          </AppCard>
        </div>
      </div>
    </PageContainer>
  </AppLayout>
</template>
