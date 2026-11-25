<script setup lang="ts">
defineProps<{ title?: string }>();
</script>

<template>
  <section class="app-card">
    <header v-if="title || $slots.header" class="app-card__header">
      <slot name="header">
        <h2>{{ title }}</h2>
      </slot>
    </header>
    <div class="app-card__body">
      <slot />
    </div>
  </section>
</template>
