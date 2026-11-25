<script setup lang="ts">
import AppButton from "./AppButton.vue";

defineProps<{ title: string }>();
const open = defineModel<boolean>("open", { default: false });
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="open = false">
      <div class="modal" role="dialog" aria-modal="true" :aria-label="title">
        <header class="modal__header">
          <h2>{{ title }}</h2>
        </header>
        <div class="modal__body">
          <slot />
        </div>
        <footer class="modal__footer">
          <slot name="footer">
            <AppButton variant="secondary" @click="open = false">Zavřít</AppButton>
          </slot>
        </footer>
      </div>
    </div>
  </Teleport>
</template>
