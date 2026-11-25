<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{ value: unknown }>();

const pretty = computed(() => JSON.stringify(props.value, null, 2));
</script>

<template>
  <pre class="json-viewer">{{ pretty }}</pre>
</template>
