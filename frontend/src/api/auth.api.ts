import { request } from "./http";
import type { LoginRequest, TokenResponse, User } from "@/types";

export const authApi = {
  login(body: LoginRequest): Promise<TokenResponse> {
    return request<TokenResponse>("/auth/login", {
      method: "POST",
      body,
      skipRefresh: true,
    });
  },

  refresh(): Promise<TokenResponse> {
    return request<TokenResponse>("/auth/refresh", {
      method: "POST",
      skipRefresh: true,
    });
  },

  logout(): Promise<void> {
    return request<void>("/auth/logout", {
      method: "POST",
      skipRefresh: true,
    });
  },

  me(): Promise<User> {
    return request<User>("/auth/me");
  },
};
