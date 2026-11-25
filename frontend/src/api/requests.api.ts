import { request, requestBlob } from "./http";
import type {
  CreateRequestBody,
  Page,
  RequestResult,
  ShopRequest,
} from "@/types";

export const requestsApi = {
  create(body: CreateRequestBody): Promise<ShopRequest> {
    const form = new FormData();
    form.append("photo", body.photo);
    form.append("description", body.description);
    form.append("options", JSON.stringify(body.options));

    return request<ShopRequest>("/requests", {
      method: "POST",
      rawBody: form,
    });
  },

  list(page = 0, size = 20): Promise<Page<ShopRequest>> {
    return request<Page<ShopRequest>>("/requests?page=" + page + "&size=" + size);
  },

  get(id: string): Promise<ShopRequest> {
    return request<ShopRequest>("/requests/" + id);
  },

  result(id: string): Promise<RequestResult> {
    return request<RequestResult>("/requests/" + id + "/result");
  },

  updateResult(id: string, body: Partial<RequestResult>): Promise<RequestResult> {
    return request<RequestResult>("/requests/" + id + "/result", {
      method: "PATCH",
      body,
    });
  },

  regenerate(id: string, part: string): Promise<void> {
    return request<void>("/requests/" + id + "/regenerate", {
      method: "POST",
      body: { part },
    });
  },

  approve(id: string): Promise<void> {
    return request<void>("/requests/" + id + "/approve", { method: "POST" });
  },

  download(id: string): Promise<Blob> {
    return requestBlob("/requests/" + id + "/download");
  },

  downloadUrl(id: string): string {
    return "/api/v1/requests/" + id + "/download";
  },
};
