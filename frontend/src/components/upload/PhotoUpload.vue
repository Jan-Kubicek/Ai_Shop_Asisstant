<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { useImageCompress } from "@/composables/useImageCompress";
import { formatBytes } from "@/utils/format";
import AppButton from "@/components/ui/AppButton.vue";

const file = defineModel<File | null>({ default: null });
const { compress, compressing, error } = useImageCompress();
const input = ref<HTMLInputElement | null>(null);
const previewUrl = ref<string | null>(null);
const originalSize = ref(0);

const summary = computed(() => {
  if (!file.value) {
    return "Vyberte fotku produktu (JPG, PNG nebo WebP).";
  }
  return file.value.name + " · " + formatBytes(file.value.size);
});

async function onChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const selected = target.files?.[0];
  if (!selected) {
    return;
  }

  originalSize.value = selected.size;
  const compressed = await compress(selected);
  file.value = compressed;

  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
  try {
    previewUrl.value = URL.createObjectURL(compressed);
  } catch {
    previewUrl.value = null;
  }
}

function openPicker() {
  input.value?.click();
}

onBeforeUnmount(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
});
</script>

<template>
  <div class="photo-upload">
    <input
      ref="input"
      data-testid="photo-input"
      class="sr-only"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      @change="onChange"
    />

    <div v-if="previewUrl" class="photo-upload__preview">
      <img :src="previewUrl" alt="Náhled vybrané fotky" />
    </div>
    <button v-else type="button" class="photo-upload__dropzone" @click="openPicker">
      <strong>Vybrat fotku</strong>
      <span class="muted">{{ summary }}</span>
    </button>

    <div v-if="previewUrl" class="row row--between">
      <span class="muted">{{ summary }}</span>
      <AppButton variant="secondary" size="sm" @click="openPicker">Změnit fotku</AppButton>
    </div>

    <p v-if="compressing" class="muted">Zmenšuji fotku…</p>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="originalSize > 0 && file && file.size < originalSize" class="form-hint">
      Fotka byla před nahráním zmenšena.
    </p>
  </div>
</template>
