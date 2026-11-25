import { request, requestBlob } from "./http";
import type {
  AuditEvent,
  CreateUserRequest,
  Page,
  ShopQueueItem,
  Stats,
  UpdateUserRequest,
  User,
} from "@/types";

export const adminApi = {
  shopQueue(page = 0, size = 20): Promise<Page<ShopQueueItem>> {
    return request<Page<ShopQueueItem>>(
      "/admin/shop-queue?page=" + page + "&size=" + size,
    );
  },

  markImported(id: string): Promise<void> {
    return request<void>("/admin/shop-queue/" + id + "/mark-imported", {
      method: "POST",
    });
  },

  exportUrl(id: string): string {
    return "/api/v1/admin/shop-queue/" + id + "/export";
  },

  exportJson(id: string): Promise<Blob> {
    return requestBlob("/admin/shop-queue/" + id + "/export");
  },

  users(page = 0, size = 20): Promise<Page<User>> {
    return request<Page<User>>("/admin/users?page=" + page + "&size=" + size);
  },

  createUser(body: CreateUserRequest): Promise<User> {
    return request<User>("/admin/users", { method: "POST", body });
  },

  updateUser(id: string, body: UpdateUserRequest): Promise<User> {
    return request<User>("/admin/users/" + id, { method: "PATCH", body });
  },

  audit(page = 0, size = 50): Promise<Page<AuditEvent>> {
    return request<Page<AuditEvent>>("/admin/audit?page=" + page + "&size=" + size);
  },

  stats(): Promise<Stats> {
    return request<Stats>("/admin/stats");
  },
};
