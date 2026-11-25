import { onScopeDispose, ref } from "vue";
import { requestsApi } from "@/api/requests.api";
import { getAccessToken } from "@/api/http";
import type { RequestStatus, ShopRequest } from "@/types";

const TERMINAL_STATUSES: RequestStatus[] = [
  "REVIEW",
  "QUEUED_FOR_SHOP",
  "IMPORTED",
  "FAILED",
];

interface StatusEvent {
  requestId: string;
  status: RequestStatus;
  errorMessage?: string;
  description?: string;
  createdAt?: string;
  finishedAt?: string;
}

/**
 * Sleduje stav požadavku. Nejdřív zkusí SSE, pokud ho server nepodporuje,
 * přepne se na polling.
 */
export function useRequestStatus(requestId: string, intervalMs = 3000) {
  const request = ref<ShopRequest | null>(null);
  const error = ref<string | null>(null);
  const polling = ref(false);
  const streaming = ref(false);
  let timer: number | undefined;
  let abort: AbortController | undefined;

  function apply(event: StatusEvent) {
    request.value = {
      id: event.requestId,
      description: event.description ?? request.value?.description ?? "",
      options: request.value?.options ?? {
        generateWebText: true,
        generateSocialPost: true,
        generateModelPhoto: false,
      },
      status: event.status,
      errorMessage: event.errorMessage ?? undefined,
      createdAt: event.createdAt ?? request.value?.createdAt ?? "",
      finishedAt: event.finishedAt ?? undefined,
    };
    error.value = null;
    if (TERMINAL_STATUSES.includes(event.status)) {
      stop();
    }
  }

  async function startStream() {
    const base = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
    const controller = new AbortController();
    abort = controller;

    const response = await fetch(`${base}/requests/${requestId}/events`, {
      headers: {
        Accept: "text/event-stream",
        Authorization: `Bearer ${getAccessToken() ?? ""}`,
      },
      credentials: "include",
      signal: controller.signal,
    });
    if (!response.ok || !response.body) {
      throw new Error("SSE není dostupné.");
    }

    streaming.value = true;
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });

      let end = buffer.indexOf("\n\n");
      while (end !== -1) {
        const chunk = buffer.slice(0, end);
        buffer = buffer.slice(end + 2);
        const dataLine = chunk.split("\n").find((line) => line.startsWith("data:"));
        if (dataLine) {
          try {
            apply(JSON.parse(dataLine.slice(5).trim()) as StatusEvent);
          } catch {
            //Ignorujeme neplatnou událost.
          }
        }
        end = buffer.indexOf("\n\n");
      }
    }
  }

  async function load() {
    try {
      request.value = await requestsApi.get(requestId);
      error.value = null;
      if (request.value && TERMINAL_STATUSES.includes(request.value.status)) {
        stop();
      }
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : "Stav se nepodařilo načíst.";
    }
  }

  function startPolling() {
    if (polling.value) {
      return;
    }
    polling.value = true;
    void load();
    timer = window.setInterval(() => {
      void load();
    }, intervalMs);
  }

  function start() {
    void startStream().catch(() => {
      if (!abort?.signal.aborted) {
        startPolling();
      }
    });
  }

  function stop() {
    abort?.abort();
    abort = undefined;
    streaming.value = false;
    if (timer !== undefined) {
      window.clearInterval(timer);
      timer = undefined;
    }
    polling.value = false;
  }

  onScopeDispose(stop);

  return { request, error, polling, streaming, start, stop, reload: load };
}
