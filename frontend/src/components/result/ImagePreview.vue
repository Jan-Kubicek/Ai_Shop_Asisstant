<script setup lang="ts">
defineProps<{
  src: string;
  alt?: string;
}>();
</script>

<template>
  <figure class="image-preview">
    <img :src="src" :alt="alt ?? 'Náhled obrázku'" loading="lazy" />
    <figcaption v-if="alt" class="muted">{{ alt }}</figcaption>
  </figure>
</template>
