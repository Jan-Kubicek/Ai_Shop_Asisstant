import { onScopeDispose, ref } from "vue";
import { requestsApi } from "@/api/requests.api";
import type { RequestStatus, ShopRequest } from "@/types";

const TERMINAL_STATUSES: RequestStatus[] = [
  "REVIEW",
  "QUEUED_FOR_SHOP",
  "IMPORTED",
  "FAILED",
];

export function useRequestStatus(requestId: string, intervalMs = 3000) {
  const request = ref<ShopRequest | null>(null);
  const error = ref<string | null>(null);
  const polling = ref(false);
  let timer: number | undefined;

  async function load() {
    try {
      request.value = await requestsApi.get(requestId);
      error.value = null;
      if (request.value && TERMINAL_STATUSES.includes(request.value.status)) {
        stop();
      }
    } catch (cause) {
      error.value =
        cause instanceof Error ? cause.message : "Stav se nepodařilo načíst.";
    }
  }

  function start() {
    if (polling.value) {
      return;
    }
    polling.value = true;
    void load();
    timer = window.setInterval(() => {
      void load();
    }, intervalMs);
  }

  function stop() {
    polling.value = false;
    if (timer !== undefined) {
      window.clearInterval(timer);
      timer = undefined;
    }
  }

  onScopeDispose(stop);

  return { request, error, polling, start, stop, reload: load };
}
