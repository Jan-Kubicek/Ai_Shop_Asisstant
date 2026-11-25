<script setup lang="ts">
withDefaults(
  defineProps<{
    id: string;
    label: string;
    type?: string;
    placeholder?: string;
    autocomplete?: string;
    required?: boolean;
    hint?: string;
    error?: string;
  }>(),
  {
    type: "text",
    required: false,
  },
);

const model = defineModel<string>({ default: "" });
</script>

<template>
  <div class="form-field">
    <label class="form-label" :for="id">{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      class="app-input"
      :type="type"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :required="required"
    />
    <span v-if="hint" class="form-hint">{{ hint }}</span>
    <span v-if="error" class="form-error">{{ error }}</span>
  </div>
</template>
