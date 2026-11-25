import { ref } from "vue";
import { defineStore } from "pinia";
import { requestsApi } from "@/api/requests.api";
import type { Page, ShopRequest } from "@/types";

export const useRequestsStore = defineStore("requests", () => {
  const items = ref<ShopRequest[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchList(page = 0, size = 20) {
    loading.value = true;
    error.value = null;
    try {
      const result: Page<ShopRequest> = await requestsApi.list(page, size);
      items.value = result.items;
      total.value = result.total;
    } catch (cause) {
      error.value =
        cause instanceof Error ? cause.message : "Seznam se nepodařilo načíst.";
    } finally {
      loading.value = false;
    }
  }

  return { items, total, loading, error, fetchList };
});
