import type { ApiErrorBody } from "@/types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

let accessToken: string | null = null;
let refreshPromise: Promise<boolean> | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId?: string;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || "Požadavek se nepodařil.");
    this.name = "ApiError";
    this.status = status;
    this.code = body.code || "UNKNOWN";
    this.traceId = body.traceId;
  }
}

async function tryRefresh(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(API_BASE + "/auth/refresh", {
          method: "POST",
          credentials: "include",
        });
        if (!response.ok) {
          return false;
        }
        const data = (await response.json()) as { accessToken?: string };
        if (!data.accessToken) {
          return false;
        }
        accessToken = data.accessToken;
        return true;
      } catch {
        return false;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

export interface RequestOptions {
  method?: string;
  headers?: HeadersInit;
  body?: unknown;
  rawBody?: BodyInit;
  skipRefresh?: boolean;
  signal?: AbortSignal;
}

function buildInit(
  options: RequestOptions,
  headers: Headers,
  body: BodyInit | undefined,
): RequestInit {
  return {
    method: options.method ?? "GET",
    headers,
    body,
    credentials: "include",
    signal: options.signal,
  };
}

export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  let body: BodyInit | undefined = options.rawBody;

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.body);
  }

  if (accessToken) {
    headers.set("Authorization", "Bearer " + accessToken);
  }

  const response = await fetch(API_BASE + path, buildInit(options, headers, body));

  if (response.status === 401 && !options.skipRefresh) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request<T>(path, { ...options, skipRefresh: true });
    }
    // Refresh selhal (např. secret se změnil) – vyčistit starý token,
    // aby se dál neposílal a uživatel se mohl znovu přihlásit.
    accessToken = null;
  }

  if (!response.ok) {
    let errorBody: ApiErrorBody = {
      status: response.status,
      code: "HTTP_ERROR",
      message: response.statusText || "Požadavek se nepodařil.",
    };
    try {
      errorBody = (await response.json()) as ApiErrorBody;
    } catch {
      // odpověď není JSON
    }
    throw new ApiError(response.status, errorBody);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function requestBlob(
  path: string,
  options: RequestOptions = {},
): Promise<Blob> {
  const headers = new Headers(options.headers);
  if (accessToken) {
    headers.set("Authorization", "Bearer " + accessToken);
  }

  const response = await fetch(
    API_BASE + path,
    buildInit(options, headers, options.rawBody),
  );

  if (response.status === 401 && !options.skipRefresh) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return requestBlob(path, { ...options, skipRefresh: true });
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, {
      status: response.status,
      code: "DOWNLOAD_FAILED",
      message: "Stažení souboru se nepodařilo.",
    });
  }

  return response.blob();
}
