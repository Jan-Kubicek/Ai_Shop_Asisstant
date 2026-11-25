<script setup lang="ts">
import { ref } from "vue";

const props = defineProps<{
  title: string;
  text: string;
}>();

const copied = ref(false);

async function copy() {
  await navigator.clipboard.writeText(props.text);
  copied.value = true;
  window.setTimeout(() => {
    copied.value = false;
  }, 2000);
}
</script>

<template>
  <article class="result-card">
    <div class="row row--between">
      <h3 class="result-card__title">{{ title }}</h3>
      <button
        type="button"
        class="app-button app-button--ghost app-button--sm"
        @click="copy"
      >
        {{ copied ? "Zkopírováno" : "Kopírovat" }}
      </button>
    </div>
    <p class="result-card__text">{{ text }}</p>
  </article>
</template>
