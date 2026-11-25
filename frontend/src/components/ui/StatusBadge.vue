<script setup lang="ts">
import { computed } from "vue";
import type { RequestStatus } from "@/types";

const props = defineProps<{ status: RequestStatus }>();

const labels: Record<RequestStatus, string> = {
  CREATED: "Vytvořeno",
  VALIDATING: "Kontrola",
  PROCESSING: "Zpracovává se",
  REVIEW: "Ke kontrole",
  QUEUED_FOR_SHOP: "Ve frontě e-shopu",
  IMPORTED: "Importováno",
  FAILED: "Chyba",
};

const variants: Record<RequestStatus, string> = {
  CREATED: "badge--info",
  VALIDATING: "badge--info",
  PROCESSING: "badge--progress",
  REVIEW: "badge--warning",
  QUEUED_FOR_SHOP: "badge--warning",
  IMPORTED: "badge--success",
  FAILED: "badge--danger",
};

const label = computed(() => labels[props.status]);
const variant = computed(() => variants[props.status]);
</script>

<template>
  <span class="badge" :class="variant">{{ label }}</span>
</template>
