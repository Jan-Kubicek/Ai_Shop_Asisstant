<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: "primary" | "secondary" | "ghost" | "danger";
    size?: "md" | "sm";
    type?: "button" | "submit" | "reset";
    loading?: boolean;
    block?: boolean;
    disabled?: boolean;
  }>(),
  {
    variant: "primary",
    size: "md",
    type: "button",
    loading: false,
    block: false,
    disabled: false,
  },
);
</script>

<template>
  <button
    :type="type"
    class="app-button"
    :class="[
      'app-button--' + variant,
      size === 'sm' ? 'app-button--sm' : '',
      block ? 'app-button--block' : '',
    ]"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="spinner" aria-hidden="true"></span>
    <slot />
  </button>
</template>
